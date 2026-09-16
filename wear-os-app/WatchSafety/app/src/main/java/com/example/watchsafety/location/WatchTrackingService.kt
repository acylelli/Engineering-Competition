package com.example.watchsafety.location

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.watchsafety.R
import com.example.watchsafety.data.WatchLocationSyncManager
import com.example.watchsafety.pairing.PairingInfoResponse
import com.example.watchsafety.pairing.PairingManager
import com.example.watchsafety.ui.MainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 앱 화면과 별도로 위치를 수집하며, 정상 연결 상태는 주기적으로 조회하지 않는다. */
class WatchTrackingService : Service() {
    companion object {
        private const val TAG = "WatchTrackingService"
        private const val NOTIFICATION_CHANNEL_ID = "watch_location_tracking"
        private const val NOTIFICATION_CHANNEL_NAME = "위치 추적"
        private const val NOTIFICATION_ID = 41001

        /** 최초 시작, 앱 복귀, 페어링 완료 시 연결을 한 번 확인한다. */
        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, WatchTrackingService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WatchTrackingService::class.java))
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val syncMutex = Mutex()
    private lateinit var locationManager: WatchLocationManager
    private lateinit var locationSyncManager: WatchLocationSyncManager
    private val pairingManager = PairingManager()
    private val connection = TrackingConnectionState()
    private var confirmedPairing: PairingInfoResponse? = null
    private var pairingCheckJob: Job? = null
    private var trackingStarted = false
    private var networkCallbackRegistered = false
    private val connectivityManager by lazy {
        getSystemService(ConnectivityManager::class.java)
    }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            serviceScope.launch {
                connection.onNetworkAvailable()
                // 인터넷 복구는 이벤트당 한 번만 확인한다. 정상 연결 중 타이머 조회는 없다.
                requestPairingRefresh()
                syncLatestLocation()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = WatchLocationManager(applicationContext)
        locationSyncManager = WatchLocationSyncManager()
        createNotificationChannel()
        startAsForegroundService()
        startTrackingIfPossible()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startTrackingIfPossible()
        if (trackingStarted) requestPairingRefresh()
        return START_STICKY
    }

    private fun startTrackingIfPossible() {
        if (trackingStarted) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }
        trackingStarted = true
        locationManager.start()
        serviceScope.launch {
            // 연결 화면과 MainActivity에서 확인한 결과를 서버 재조회 없이 즉시 받는다.
            PairingManager.latestInfo.filterNotNull().collect { info ->
                syncMutex.withLock {
                    if (confirmedPairing != info) {
                        locationSyncManager.reset()
                        confirmedPairing = info
                    }
                    connection.onPairingConfirmed(
                        info.isPaired && !info.guardianId.isNullOrBlank() && !info.wearerId.isNullOrBlank()
                    )
                }
                syncLatestLocation()
            }
        }
        serviceScope.launch {
            locationManager.location.filterNotNull().collect { syncLatestLocation() }
        }
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
            networkCallbackRegistered = true
        } catch (error: RuntimeException) {
            Log.w(TAG, "네트워크 복구 감시 등록 실패", error)
        }
    }

    private fun requestPairingRefresh() {
        if (pairingCheckJob?.isActive == true) return
        pairingCheckJob = serviceScope.launch {
            var retryDelay = 5_000L
            while (isActive) {
                try {
                    pairingManager.getPairingInfo()
                    return@launch // 연결됨/미연결 모두 성공 응답 후 반복 확인을 종료한다.
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.w(TAG, "연결 조회 통신 실패: 복구 재시도 예정", error)
                    delay(retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(300_000L)
                }
            }
        }
    }

    private suspend fun syncLatestLocation() = syncMutex.withLock {
        val location = locationManager.location.value ?: return@withLock
        if (!connection.canSend(SystemClock.elapsedRealtime())) return@withLock
        try {
            locationSyncManager.syncIfNeeded(location)
            connection.onSyncSucceeded()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (error.message?.contains("Watch is not paired", ignoreCase = true) == true) {
                // 서버가 연결 해제를 확인한 경우에만 전송을 중지한다.
                connection.onPairingConfirmed(false)
                PairingManager.markUnpaired()
                locationSyncManager.reset()
                requestPairingRefresh()
            } else {
                // 통신 실패를 연결 해제로 취급하지 않고 다음 위치에서 제한적으로 재시도한다.
                connection.onSyncFailed(SystemClock.elapsedRealtime())
            }
            Log.w(TAG, "위치 저장 실패", error)
        }
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.O
        ) {

            return
        }


        val notificationManager =
            getSystemService(
                NotificationManager::class.java
            )


        val channel =
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager
                    .IMPORTANCE_LOW
            ).apply {

                description =
                    "보호자에게 현재 위치를 전달하기 위한 워치 위치 추적"

                setShowBadge(
                    false
                )
            }


        notificationManager
            .createNotificationChannel(
                channel
            )
    }


    private fun startAsForegroundService() {

        val openAppIntent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            }


        val pendingIntent =
            PendingIntent
                .getActivity(
                    this,
                    0,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                )


        val notification =
            NotificationCompat
                .Builder(
                    this,
                    NOTIFICATION_CHANNEL_ID
                )
                .setSmallIcon(
                    R.mipmap.ic_launcher
                )
                .setContentTitle(
                    "Watch Safety"
                )
                .setContentText(
                    "보호자에게 현재 위치를 전달하고 있어요"
                )
                .setContentIntent(
                    pendingIntent
                )
                .setOngoing(
                    true
                )
                .setOnlyAlertOnce(
                    true
                )
                .setCategory(
                    NotificationCompat
                        .CATEGORY_SERVICE
                )
                .setPriority(
                    NotificationCompat
                        .PRIORITY_LOW
                )
                .build()


        val foregroundServiceType =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {

                ServiceInfo
                    .FOREGROUND_SERVICE_TYPE_LOCATION

            } else {

                0
            }


        ServiceCompat
            .startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                foregroundServiceType
            )


        Log.d(
            TAG,
            "Foreground 상태 진입 완료"
        )
    }


    /*
     * =====================================================
     * 종료
     * =====================================================
     */
    override fun onDestroy() {
        if (networkCallbackRegistered) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
        serviceScope.cancel()
        locationManager.stop()
        locationSyncManager.reset()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}