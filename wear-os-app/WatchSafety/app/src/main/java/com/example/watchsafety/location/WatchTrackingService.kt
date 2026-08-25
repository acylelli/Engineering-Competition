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
import android.os.Build
import android.os.IBinder
import android.util.Log

import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

import com.example.watchsafety.R
import com.example.watchsafety.data.WatchLocationSyncManager
import com.example.watchsafety.pairing.PairingManager
import com.example.watchsafety.ui.MainActivity

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


/*
 * =========================================================
 * WatchTrackingService
 * =========================================================
 *
 * 역할:
 *
 * 1. Foreground Service로 살아있는다.
 * 2. Activity와 별개의 WatchLocationManager로 GPS를 계속 받는다.
 * 3. 현재 워치가 보호자와 페어링되어 있을 때만
 *    WatchLocationSyncManager를 통해 Supabase locations에 저장한다.
 * 4. 기존 저장 정책은 그대로 사용한다.
 *
 *    - 첫 위치
 *    - 30m 이상 이동
 *    - 1분 경과
 *
 * 앱 화면이 꺼지거나 MainActivity가 onDestroy 되어도
 * 이 서비스가 살아있는 동안 위치 동기화는 계속된다.
 */
class WatchTrackingService :
    Service() {


    companion object {

        private const val TAG =
            "WatchTrackingService"


        private const val NOTIFICATION_CHANNEL_ID =
            "watch_location_tracking"


        private const val NOTIFICATION_CHANNEL_NAME =
            "위치 추적"


        private const val NOTIFICATION_ID =
            41001


        /*
         * 페어링이 안 되어 있을 때도 서비스는 유지한다.
         * 이후 워치-보호자 페어링이 완료될 수 있으므로
         * 주기적으로 페어링 상태를 다시 확인한다.
         */
        private const val PAIRING_CHECK_INTERVAL_MILLIS =
            15_000L


        fun start(
            context: Context
        ) {

            val intent =
                Intent(
                    context,
                    WatchTrackingService::class.java
                )


            ContextCompat
                .startForegroundService(
                    context,
                    intent
                )


            Log.d(
                TAG,
                "Foreground 위치 서비스 시작 요청"
            )
        }


        fun stop(
            context: Context
        ) {

            context
                .stopService(
                    Intent(
                        context,
                        WatchTrackingService::class.java
                    )
                )


            Log.d(
                TAG,
                "Foreground 위치 서비스 종료 요청"
            )
        }
    }


    /*
     * Service 전용 CoroutineScope.
     *
     * Activity lifecycle과 완전히 분리된다.
     */
    private val serviceScope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.IO
        )


    private lateinit var locationManager:
            WatchLocationManager


    private lateinit var locationSyncManager:
            WatchLocationSyncManager


    private lateinit var pairingManager:
            PairingManager


    private var locationCollectJob:
            Job? = null


    private var pairingCheckJob:
            Job? = null


    /*
     * 현재 RPC 기준으로 실제 페어링된 워치인지 여부.
     */
    @Volatile
    private var isPaired:
            Boolean = false


    /*
     * onStartCommand가 여러 번 와도
     * 수집 Job을 중복 생성하지 않기 위한 값.
     */
    private var trackingStarted:
            Boolean = false


    override fun onCreate() {

        super.onCreate()


        Log.d(
            TAG,
            "onCreate"
        )


        locationManager =
            WatchLocationManager(
                applicationContext
            )


        locationSyncManager =
            WatchLocationSyncManager()


        pairingManager =
            PairingManager()


        /*
         * Android는 startForegroundService() 후 짧은 시간 내
         * 반드시 startForeground()가 호출되어야 한다.
         *
         * 네트워크/Auth 작업보다 알림 표시를 먼저 한다.
         */
        createNotificationChannel()


        startAsForegroundService()


        startTrackingIfPossible()
    }


    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        Log.d(
            TAG,
            "onStartCommand startId=$startId"
        )


        /*
         * 시스템이 같은 Service에 start 요청을 다시 보내도
         * 기존 Job은 그대로 유지한다.
         */
        startTrackingIfPossible()


        /*
         * 메모리 부족 등으로 프로세스가 종료된 경우
         * 가능한 상황에서 시스템이 서비스를 다시 만들도록 요청.
         *
         * 사용자가 앱을 강제 종료(Force stop)한 경우에는
         * Android 정책상 자동 재시작되지 않는다.
         */
        return START_STICKY
    }


    /*
     * =====================================================
     * 실제 추적 시작
     * =====================================================
     */
    private fun startTrackingIfPossible() {

        if (
            trackingStarted
        ) {

            return
        }


        val fineLocationGranted =
            ContextCompat
                .checkSelfPermission(
                    this,
                    Manifest.permission
                        .ACCESS_FINE_LOCATION
                ) ==
                    PackageManager
                        .PERMISSION_GRANTED


        if (
            !fineLocationGranted
        ) {

            Log.w(
                TAG,
                "ACCESS_FINE_LOCATION 권한 없음 → 위치 서비스 종료"
            )


            stopSelf()

            return
        }


        trackingStarted =
            true


        Log.d(
            TAG,
            "백그라운드 GPS 추적 시작"
        )


        /*
         * Service 자체 GPS 요청 시작.
         */
        locationManager
            .start()


        startPairingWatcher()


        startLocationCollector()
    }


    /*
     * =====================================================
     * 페어링 상태 감시
     * =====================================================
     *
     * 위치마다 RPC를 호출하지 않고 15초에 한 번만 확인한다.
     */
    private fun startPairingWatcher() {

        pairingCheckJob
            ?.cancel()


        pairingCheckJob =
            serviceScope
                .launch {

                    while (
                        isActive
                    ) {

                        refreshPairingStatus()


                        delay(
                            PAIRING_CHECK_INTERVAL_MILLIS
                        )
                    }
                }
    }


    private suspend fun refreshPairingStatus() {

        runCatching {

            pairingManager
                .getPairingInfo()

        }.onSuccess {
                info ->


            val newIsPaired =
                info.isPaired &&
                        !info.guardianId
                            .isNullOrBlank() &&
                        !info.wearerId
                            .isNullOrBlank()


            if (
                newIsPaired !=
                isPaired
            ) {

                Log.d(
                    TAG,
                    "페어링 상태 변경 " +
                            "$isPaired → $newIsPaired"
                )
            }


            isPaired =
                newIsPaired


            if (
                newIsPaired
            ) {

                Log.d(
                    TAG,
                    "페어링 확인 완료 " +
                            "guardian=${info.guardianId}, " +
                            "wearer=${info.wearerId}"
                )

            } else {

                Log.d(
                    TAG,
                    "현재 페어링 없음 → 위치 DB 저장 대기"
                )
            }

        }.onFailure {
                error ->


            /*
             * 일시적인 네트워크 실패 때문에
             * 기존 true 상태를 바로 false로 내리지 않는다.
             *
             * 이미 페어링이 확인된 상태라면 위치 동기화를 계속 시도하고,
             * 다음 주기에 페어링 정보를 다시 확인한다.
             */
            Log.w(
                TAG,
                "페어링 상태 확인 실패: ${error.message}",
                error
            )
        }
    }


    /*
     * =====================================================
     * GPS → Supabase
     * =====================================================
     */
    private fun startLocationCollector() {

        locationCollectJob
            ?.cancel()


        locationCollectJob =
            serviceScope
                .launch {

                    locationManager
                        .location
                        .filterNotNull()
                        .collect {
                                location ->


                            Log.d(
                                TAG,
                                "백그라운드 GPS 수신 " +
                                        "lat=${location.latitude}, " +
                                        "lng=${location.longitude}, " +
                                        "accuracy=${location.accuracyMeters}"
                            )


                            if (
                                !isPaired
                            ) {

                                Log.d(
                                    TAG,
                                    "페어링 전이므로 Supabase 위치 저장 생략"
                                )

                                return@collect
                            }


                            runCatching {

                                locationSyncManager
                                    .syncIfNeeded(
                                        location
                                    )

                            }.onSuccess {

                                Log.d(
                                    TAG,
                                    "백그라운드 위치 동기화 처리 완료"
                                )

                            }.onFailure {
                                    error ->


                                Log.e(
                                    TAG,
                                    "백그라운드 위치 동기화 실패",
                                    error
                                )
                            }
                        }
                }
    }


    /*
     * =====================================================
     * Foreground Notification
     * =====================================================
     */
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

        Log.d(
            TAG,
            "onDestroy"
        )


        locationCollectJob
            ?.cancel()


        pairingCheckJob
            ?.cancel()


        locationManager
            .stop()


        locationSyncManager
            .reset()


        serviceScope
            .cancel()


        trackingStarted =
            false


        super.onDestroy()
    }


    override fun onBind(
        intent: Intent?
    ): IBinder? =
        null
}