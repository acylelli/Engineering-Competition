package com.example.watchsafety.ui

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.util.Log
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.core.content.ContextCompat

import androidx.lifecycle.lifecycleScope

import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

import com.example.watchsafety.data.HomeSafeZoneManager
import com.example.watchsafety.data.EmergencyCallManager
import com.example.watchsafety.data.EmergencyCallStatus
import com.example.watchsafety.data.ReturnHomeRequestStore
import com.example.watchsafety.data.ReturnHomeRealtimeManager
import com.example.watchsafety.data.WatchFcmTokenManager
import com.example.watchsafety.data.WatchLocationSyncManager
import com.example.watchsafety.data.WatchSafetyEventManager
import com.example.watchsafety.data.WatchStatusManager
import com.example.watchsafety.health.HeartRateManager
import com.example.watchsafety.location.WatchLocation
import com.example.watchsafety.location.WatchLocationManager
import com.example.watchsafety.location.WatchTrackingService
import com.example.watchsafety.navigation.TmapRouteClient
import com.example.watchsafety.navigation.TmapRouteResult
import com.example.watchsafety.notification.WatchFirebaseMessagingService
import com.example.watchsafety.pairing.PairingManager
import com.example.watchsafety.safety.DemoSafetyService
import com.example.watchsafety.safety.FallEventState
import com.example.watchsafety.safety.FallHealthServiceManager

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/*
 * =========================================================
 * 앱 화면
 * =========================================================
 */

enum class AppScreen {

    HOME,

    FALL_DETECTED,

    SOS_SENT,

    COMPASS,

    OUT_OF_SAFE_ZONE,

    MEDICATION_ALERT,

    PAIRING,

    PAIRING_SUCCESS,

    RETURN_HOME_REQUEST
}


/*
 * =========================================================
 * MainActivity
 * =========================================================
 */

class MainActivity :
    ComponentActivity() {


    companion object {

        /*
         * =================================================
         * 현재 MainActivity가 사용자에게 보이는 상태인지 확인
         * =================================================
         *
         * WatchFirebaseMessagingService에서
         * 귀가 요청 FCM을 받았을 때
         *
         * true
         * → 알림 대신 앱 내부 귀가 요청 화면 즉시 표시
         *
         * false
         * → Notification 표시
         */
        @Volatile
        var isInForeground:
                Boolean = false

            private set
    }


    private lateinit var heartRateManager:
            HeartRateManager


    private lateinit var locationManager:
            WatchLocationManager


    private lateinit var fallManager:
            FallHealthServiceManager


    /*
     * 배터리 / 연결상태
     */
    private lateinit var watchStatusManager:
            WatchStatusManager


    /*
     * GPS → Supabase
     */
    private lateinit var watchLocationSyncManager:
            WatchLocationSyncManager


    /*
     * 낙상 / SOS → Supabase
     */
    private lateinit var watchSafetyEventManager:
            WatchSafetyEventManager


    /*
     * SOS / 낙상 시 LTE 보호자 직접 통화
     */
    private lateinit var emergencyCallManager:
            EmergencyCallManager


    private val emergencyCallStatusState =
        mutableStateOf(
            EmergencyCallStatus.IDLE
        )


    /*
     * 보호자 귀가 요청 Realtime
     */
    private lateinit var returnHomeRealtimeManager:
            ReturnHomeRealtimeManager


    /*
     * 실제 페어링 정보
     */
    private lateinit var pairingManager:
            PairingManager


    /*
     * 실제 집 안전구역
     */
    private lateinit var homeSafeZoneManager:
            HomeSafeZoneManager


    /*
     * Firebase FCM Token
     */
    private lateinit var watchFcmTokenManager:
            WatchFcmTokenManager


    /*
     * 귀가 요청 중복 처리 방지
     */
    private lateinit var returnHomeRequestStore:
            ReturnHomeRequestStore


    /*
     * Realtime 수신 시
     * 현재 Activity가 실제 화면에 보이는지 확인
     */
    private var isActivityResumed:
            Boolean = false


    /*
     * Foreground FCM BroadcastReceiver 등록 여부
     */
    private var returnHomePushReceiverRegistered:
            Boolean = false


    /*
     * =====================================================
     * Foreground 귀가 요청 FCM Receiver
     * =====================================================
     *
     * WatchFirebaseMessagingService가
     * 앱이 현재 화면에 떠 있다고 판단하면
     * Notification 대신 Broadcast를 보낸다.
     *
     * 여기서 즉시 RETURN_HOME_REQUEST 화면으로 전환.
     */
    private val returnHomePushReceiver =

        object :
            BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {

                if (
                    intent?.action !=
                    WatchFirebaseMessagingService
                        .ACTION_RETURN_HOME_REQUEST_RECEIVED
                ) {

                    return
                }


                val requestId =
                    intent.getStringExtra(
                        WatchFirebaseMessagingService
                            .EXTRA_REQUEST_ID
                    )


                val pushGuardianId =
                    intent.getStringExtra(
                        WatchFirebaseMessagingService
                            .EXTRA_GUARDIAN_ID
                    )


                val pushWearerId =
                    intent.getStringExtra(
                        WatchFirebaseMessagingService
                            .EXTRA_WEARER_ID
                    )


                Log.d(
                    "ReturnHomeFCM",
                    "Foreground 귀가 요청 Broadcast 수신 requestId=$requestId"
                )


                showReturnHomeRequest(

                    requestId =
                        requestId,

                    guardianId =
                        pushGuardianId,

                    wearerId =
                        pushWearerId,

                    source =
                        "FCM_FOREGROUND",
                )
            }
        }


    private lateinit var fusedLocationClient:
            FusedLocationProviderClient


    /*
     * 기존 안전구역 계산용
     */
    private val myLocationState =
        mutableStateOf<Location?>(
            null
        )


    private val currentScreenState =
        mutableStateOf(
            AppScreen.HOME
        )


    /*
     * 현재 보호자 귀가 요청 ID
     */
    private val currentReturnHomeRequestIdState =
        mutableStateOf<String?>(
            null
        )


    /*
     * 실제 페어링 ID
     */
    private val guardianIdState =
        mutableStateOf<String?>(
            null
        )


    private val wearerIdState =
        mutableStateOf<String?>(
            null
        )


    /*
     * DB의 실제 집 안전구역
     */
    private val homeLatitudeState =
        mutableStateOf<Double?>(
            null
        )


    private val homeLongitudeState =
        mutableStateOf<Double?>(
            null
        )


    private val homeRadiusMetersState =
        mutableStateOf<Double?>(
            null
        )


    /*
     * =====================================================
     * onCreate
     * =====================================================
     */

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        /*
         * =================================================
         * 잠금 화면에서도 긴급화면 표시
         * =================================================
         */

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O_MR1
        ) {

            setShowWhenLocked(
                true
            )

            setTurnScreenOn(
                true
            )

        } else {

            @Suppress("DEPRECATION")

            window.addFlags(

                android.view.WindowManager
                    .LayoutParams
                    .FLAG_SHOW_WHEN_LOCKED or

                        android.view.WindowManager
                            .LayoutParams
                            .FLAG_TURN_SCREEN_ON or

                        android.view.WindowManager
                            .LayoutParams
                            .FLAG_KEEP_SCREEN_ON
            )
        }


        /*
         * =================================================
         * Manager 초기화
         * =================================================
         */

        heartRateManager =
            HeartRateManager(
                this
            )


        locationManager =
            WatchLocationManager(
                this
            )


        fallManager =
            FallHealthServiceManager(
                this
            )


        watchStatusManager =
            WatchStatusManager(
                this
            )


        watchLocationSyncManager =
            WatchLocationSyncManager()


        watchSafetyEventManager =
            WatchSafetyEventManager()


        emergencyCallManager =
            EmergencyCallManager(
                applicationContext
            )


        pairingManager =
            PairingManager()


        homeSafeZoneManager =
            HomeSafeZoneManager()


        watchFcmTokenManager =
            WatchFcmTokenManager()


        returnHomeRequestStore =
            ReturnHomeRequestStore(
                applicationContext
            )


        returnHomeRealtimeManager =
            ReturnHomeRealtimeManager(
                lifecycleScope
            )


        fusedLocationClient =
            LocationServices
                .getFusedLocationProviderClient(
                    this
                )


        /*
         * =================================================
         * 배터리 상태 감시
         * =================================================
         */

        watchStatusManager
            .start(
                lifecycleScope
            )


        /*
         * =================================================
         * 실제 페어링 정보 조회 후 Realtime 시작
         * =================================================
         */

        refreshPairingAndStartRealtime()


        /*
         * =================================================
         * Notification 클릭 Intent 처리
         * =================================================
         */

        handleIntent(
            intent
        )


        /*
         * =================================================
         * Compose
         * =================================================
         */

        setContent {


            val heartRate by
            heartRateManager
                .heartRate
                .collectAsState()


            /*
             * WatchLocationManager의
             * 실제 지속 GPS
             */
            val location by
            locationManager
                .location
                .collectAsState()


            val isFallDetected by
            FallEventState
                .fallDetected
                .collectAsState()


            /*
             * =================================================
             * 실제 낙상 감지
             * =================================================
             */

            LaunchedEffect(
                isFallDetected
            ) {

                if (
                    isFallDetected
                ) {


                    /*
                     * 우선 확인 화면 표시
                     */
                    currentScreenState.value =
                        AppScreen.FALL_DETECTED


                    val currentLocation =
                        location


                    /*
                     * 낙상 순간의 GPS는
                     * 30m / 1분 정책과 관계없이
                     * 즉시 locations에 저장
                     */
                    if (
                        currentLocation != null
                    ) {

                        runCatching {

                            watchLocationSyncManager
                                .forceSync(
                                    currentLocation
                                )

                        }.onFailure { error ->

                            Log.w(
                                "WatchFall",
                                "낙상 위치 저장 실패: ${error.message}",
                                error
                            )
                        }
                    }


                    /*
                     * safety_events에
                     * FALL_SUSPECTED 저장
                     */
                    runCatching {

                        watchSafetyEventManager
                            .recordFallSuspected(
                                currentLocation
                            )

                    }.onFailure { error ->

                        Log.w(
                            "WatchFall",
                            "낙상 이벤트 저장 실패: ${error.message}",
                            error
                        )
                    }
                }
            }


            /*
             * =================================================
             * 권한 요청
             * =================================================
             */

            val permissionLauncher =
                rememberLauncherForActivityResult(

                    ActivityResultContracts
                        .RequestMultiplePermissions()

                ) { permissions ->


                    val callPhoneGranted =
                        permissions[
                            Manifest.permission
                                .CALL_PHONE
                        ] == true ||
                                emergencyCallManager
                                    .hasCallPermission()


                    if (
                        emergencyCallStatusState.value ==
                        EmergencyCallStatus.PERMISSION_REQUIRED
                    ) {
                        if (
                            callPhoneGranted
                        ) {
                            startEmergencyCall()
                        } else {
                            emergencyCallStatusState.value =
                                EmergencyCallStatus.PERMISSION_DENIED
                        }
                    }


                    val fineLocationGranted =
                        permissions[
                            Manifest.permission
                                .ACCESS_FINE_LOCATION
                        ] == true ||
                                ContextCompat
                                    .checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission
                                            .ACCESS_FINE_LOCATION
                                    ) ==
                                android.content.pm.PackageManager
                                    .PERMISSION_GRANTED


                    val bodySensorsGranted =
                        permissions[
                            Manifest.permission
                                .BODY_SENSORS
                        ] == true ||
                                ContextCompat
                                    .checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission
                                            .BODY_SENSORS
                                    ) ==
                                android.content.pm.PackageManager
                                    .PERMISSION_GRANTED


                    val activityRecognitionGranted =
                        permissions[
                            Manifest.permission
                                .ACTIVITY_RECOGNITION
                        ] == true ||
                                ContextCompat
                                    .checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission
                                            .ACTIVITY_RECOGNITION
                                    ) ==
                                android.content.pm.PackageManager
                                    .PERMISSION_GRANTED


                    val notificationGranted =
                        if (
                            Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.TIRAMISU
                        ) {

                            permissions[
                                Manifest.permission
                                    .POST_NOTIFICATIONS
                            ] == true ||
                                    ContextCompat
                                        .checkSelfPermission(
                                            this@MainActivity,
                                            Manifest.permission
                                                .POST_NOTIFICATIONS
                                        ) ==
                                    android.content.pm.PackageManager
                                        .PERMISSION_GRANTED

                        } else {

                            true
                        }


                    Log.d(
                        "WatchPermission",
                        "권한 상태 " +
                                "location=$fineLocationGranted, " +
                                "bodySensors=$bodySensorsGranted, " +
                                "activityRecognition=$activityRecognitionGranted, " +
                                "notification=$notificationGranted, " +
                                "callPhone=$callPhoneGranted"
                    )


                    /*
                     * GPS
                     */
                    if (
                        fineLocationGranted
                    ) {

                        Log.d(
                            "WatchGPS",
                            "위치 권한 확인 완료 → GPS 시작"
                        )


                        locationManager
                            .start()


                        /*
                         * 기존 안전구역 계산용 위치 1회 조회
                         */
                        getLocation()


                        /*
                         * 백그라운드 위치 추적
                         */
                        WatchTrackingService
                            .start(
                                this@MainActivity
                            )

                    } else {

                        Log.w(
                            "WatchGPS",
                            "ACCESS_FINE_LOCATION 권한 없음"
                        )
                    }


                    /*
                     * 심박수
                     */
                    if (
                        bodySensorsGranted
                    ) {

                        startHeartRateMeasurement()

                    } else {

                        Log.w(
                            "WatchPermission",
                            "BODY_SENSORS 권한 없음"
                        )
                    }


                    /*
                     * Health Services 낙상 감지
                     */
                    if (
                        activityRecognitionGranted
                    ) {

                        lifecycleScope.launch {

                            if (
                                fallManager
                                    .isFallDetectionSupported()
                            ) {

                                fallManager
                                    .registerFallDetection()
                            }
                        }

                    } else {

                        Log.w(
                            "WatchPermission",
                            "ACTIVITY_RECOGNITION 권한 없음"
                        )
                    }


                    /*
                     * 기존 데모 안전 서비스
                     */
                    startService(

                        Intent(
                            this@MainActivity,
                            DemoSafetyService::class.java
                        )
                    )


                    if (
                        !fineLocationGranted ||
                        !bodySensorsGranted ||
                        !activityRecognitionGranted ||
                        !notificationGranted
                    ) {

                        Toast
                            .makeText(
                                this@MainActivity,
                                "일부 권한이 허용되지 않았습니다.",
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }


            /*
             * 앱 시작 권한 요청
             */
            LaunchedEffect(
                Unit
            ) {

                permissionLauncher
                    .launch(

                        arrayOf(

                            Manifest.permission
                                .BODY_SENSORS,

                            Manifest.permission
                                .ACCESS_FINE_LOCATION,

                            Manifest.permission
                                .ACCESS_COARSE_LOCATION,

                            Manifest.permission
                                .ACTIVITY_RECOGNITION,

                            Manifest.permission
                                .POST_NOTIFICATIONS,

                            Manifest.permission
                                .CALL_PHONE
                        )
                    )
            }


            /*
             * =================================================
             * UI
             * =================================================
             */

            MaterialTheme {

                EmergencyManager(

                    currentScreen =
                        currentScreenState.value,


                    onScreenChange = {
                            newScreen ->

                        currentScreenState.value =
                            newScreen
                    },


                    returnHomeRequestId =
                        currentReturnHomeRequestIdState.value,


                    returnHomeRealtimeManager =
                        returnHomeRealtimeManager,


                    guardianId =
                        guardianIdState.value,


                    wearerId =
                        wearerIdState.value,


                    homeLatitude =
                        homeLatitudeState.value,


                    homeLongitude =
                        homeLongitudeState.value,


                    homeRadiusMeters =
                        homeRadiusMetersState.value,


                    pairingManager =
                        pairingManager,


                    onPairingCompleted = {

                        refreshPairingAndStartRealtime()
                    },


                    onClearReturnHomeRequest = {

                        currentReturnHomeRequestIdState.value =
                            null
                    },


                    onReturnHomeRequestHandled = {
                            requestId ->


                        returnHomeRequestStore
                            .markHandled(
                                requestId
                            )


                        cancelReturnHomeNotification(
                            requestId
                        )
                    },


                    myLocation =
                        myLocationState.value,


                    watchLocation =
                        location,


                    heartRate =
                        heartRate
                            ?.toString()
                            ?.toFloatOrNull(),


                    watchStatusManager =
                        watchStatusManager,


                    watchLocationSyncManager =
                        watchLocationSyncManager,


                    watchSafetyEventManager =
                        watchSafetyEventManager,


                    emergencyCallStatus =
                        emergencyCallStatusState.value,


                    onEmergencyCallRequest = {
                        if (
                            emergencyCallManager
                                .hasCallPermission()
                        ) {
                            startEmergencyCall()
                        } else {
                            emergencyCallStatusState.value =
                                EmergencyCallStatus.PERMISSION_REQUIRED

                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission
                                        .CALL_PHONE
                                )
                            )
                        }
                    },
                )
            }
        }
    }


    /*
     * =====================================================
     * 보호자 직접 통화
     * =====================================================
     */

    private fun startEmergencyCall() {
        if (
            emergencyCallStatusState.value ==
            EmergencyCallStatus.CONNECTING
        ) {
            return
        }

        emergencyCallStatusState.value =
            EmergencyCallStatus.CONNECTING

        lifecycleScope.launch {
            emergencyCallStatusState.value =
                emergencyCallManager
                    .placePrimaryGuardianCall()
        }
    }


    /*
     * =====================================================
     * 실제 페어링 정보 조회 + Realtime 시작
     * =====================================================
     */

    private fun refreshPairingAndStartRealtime() {

        lifecycleScope.launch {

            runCatching {

                pairingManager
                    .getPairingInfo()

            }.onSuccess { info ->


                if (
                    !info.isPaired ||
                    info.guardianId.isNullOrBlank() ||
                    info.wearerId.isNullOrBlank()
                ) {

                    guardianIdState.value =
                        null

                    wearerIdState.value =
                        null

                    homeLatitudeState.value =
                        null

                    homeLongitudeState.value =
                        null

                    homeRadiusMetersState.value =
                        null


                    returnHomeRealtimeManager
                        .stop()


                    Log.d(
                        "PairingInfo",
                        "현재 페어링 정보 없음"
                    )


                    return@onSuccess
                }


                val guardianId =
                    info.guardianId
                        ?: return@onSuccess


                val wearerId =
                    info.wearerId
                        ?: return@onSuccess


                guardianIdState.value =
                    guardianId


                wearerIdState.value =
                    wearerId


                runCatching {
                    emergencyCallManager
                        .refreshContact()
                }.onFailure { error ->
                    Log.w(
                        "EmergencyCall",
                        "긴급 연락처 사전 동기화 실패: ${error.message}",
                        error,
                    )
                }


                Log.d(
                    "PairingInfo",
                    "실제 페어링 정보 조회 성공 guardianId=$guardianId wearerId=$wearerId"
                )


                /*
                 * 현재 워치 FCM Token
                 * → devices.watch_fcm_token 동기화
                 */
                runCatching {

                    watchFcmTokenManager
                        .syncCurrentToken()

                }.onSuccess {

                    Log.d(
                        "WatchFCM",
                        "현재 FCM 토큰 동기화 성공"
                    )

                }.onFailure { error ->

                    Log.e(
                        "WatchFCM",
                        "현재 FCM 토큰 동기화 실패",
                        error
                    )
                }


                /*
                 * HOME 안전구역 조회
                 */
                refreshHomeSafeZone(
                    source = "PAIRING_REFRESH"
                )


                /*
                 * 기존 Realtime 제거 후 다시 시작
                 */
                returnHomeRealtimeManager
                    .stop()


                returnHomeRealtimeManager
                    .start(

                        guardianId =
                            guardianId,

                        wearerId =
                            wearerId

                    ) { requestId ->


                        if (
                            returnHomeRequestStore
                                .isHandled(
                                    requestId
                                )
                        ) {

                            Log.d(
                                "ReturnHomeRealtime",
                                "이미 처리한 귀가 요청 무시: $requestId"
                            )


                            return@start
                        }


                        /*
                         * 보호자 앱에서 HOME 안전구역이 변경됐을 수 있으므로
                         * 새 귀가 요청을 받을 때마다 최신 HOME을 다시 조회한다.
                         *
                         * 조회가 비동기로 진행되더라도 TmapRouteTestScreen은
                         * homeLatitude / homeLongitude / homeRadiusMeters가
                         * 갱신되면 LaunchedEffect가 다시 실행된다.
                         */
                        refreshHomeSafeZone(
                            source = "REALTIME_REQUEST"
                        )


                        /*
                         * Realtime에서도 동일한 요청 화면 표시.
                         *
                         * FCM이 먼저 와도 괜찮고
                         * Realtime이 먼저 와도 괜찮다.
                         */
                        currentReturnHomeRequestIdState.value =
                            requestId


                        currentScreenState.value =
                            AppScreen.RETURN_HOME_REQUEST


                        if (
                            isActivityResumed
                        ) {

                            /*
                             * 앱 화면이 이미 보이면
                             * Realtime이 화면을 직접 띄웠기 때문에
                             * 동일 요청 Notification 제거.
                             */
                            returnHomeRequestStore
                                .markNotified(
                                    requestId
                                )


                            cancelReturnHomeNotification(
                                requestId
                            )
                        }


                        Log.d(
                            "ReturnHomeRealtime",
                            "워치 귀가 요청 수신: $requestId"
                        )
                    }


            }.onFailure { error ->

                Log.e(
                    "PairingInfo",
                    "페어링 정보 조회 실패",
                    error
                )
            }
        }
    }


    /*
     * =====================================================
     * DB의 실제 집 안전구역 조회
     * =====================================================
     */

    private fun refreshHomeSafeZone(
        source: String = "UNKNOWN"
    ) {

        Log.d(
            "HomeSafeZone",
            "HOME 안전구역 최신 조회 시작 source=$source"
        )


        lifecycleScope.launch {

            runCatching {

                homeSafeZoneManager
                    .getHomeSafeZone()

            }.onSuccess { home ->


                if (
                    !home.isConfigured ||
                    home.centerLatitude == null ||
                    home.centerLongitude == null
                ) {

                    homeLatitudeState.value =
                        null


                    homeLongitudeState.value =
                        null


                    homeRadiusMetersState.value =
                        null


                    Log.d(
                        "HomeSafeZone",
                        "등록된 집 안전구역이 없습니다. source=$source"
                    )


                    return@onSuccess
                }


                homeLatitudeState.value =
                    home.centerLatitude


                homeLongitudeState.value =
                    home.centerLongitude


                homeRadiusMetersState.value =
                    home.radiusMeters


                Log.d(
                    "HomeSafeZone",
                    "집 안전구역 최신 조회 성공 " +
                            "source=$source " +
                            "latitude=${home.centerLatitude} " +
                            "longitude=${home.centerLongitude} " +
                            "radius=${home.radiusMeters}"
                )


            }.onFailure { error ->

                Log.e(
                    "HomeSafeZone",
                    "집 안전구역 최신 조회 실패 source=$source",
                    error
                )
            }
        }
    }


    /*
     * =====================================================
     * 새 Intent
     * =====================================================
     */

    override fun onNewIntent(
        intent: Intent
    ) {

        super.onNewIntent(
            intent
        )


        setIntent(
            intent
        )


        handleIntent(
            intent
        )
    }


    /*
     * =====================================================
     * Notification Intent 처리
     * =====================================================
     */

    private fun handleIntent(
        intent: Intent?
    ) {

        val emergencyType =
            intent
                ?.getStringExtra(
                    WatchFirebaseMessagingService
                        .EXTRA_EMERGENCY_TYPE
                )


        when (
            emergencyType
        ) {


            WatchFirebaseMessagingService
                .TYPE_RETURN_HOME_REQUEST -> {


                val requestId =
                    intent
                        .getStringExtra(
                            WatchFirebaseMessagingService
                                .EXTRA_REQUEST_ID
                        )


                val pushGuardianId =
                    intent
                        .getStringExtra(
                            WatchFirebaseMessagingService
                                .EXTRA_GUARDIAN_ID
                        )


                val pushWearerId =
                    intent
                        .getStringExtra(
                            WatchFirebaseMessagingService
                                .EXTRA_WEARER_ID
                        )


                showReturnHomeRequest(

                    requestId =
                        requestId,

                    guardianId =
                        pushGuardianId,

                    wearerId =
                        pushWearerId,

                    source =
                        "NOTIFICATION_CLICK",
                )
            }


            "FALL_DETECTED" -> {

                currentScreenState.value =
                    AppScreen.FALL_DETECTED
            }


            "OUT_OF_SAFE_ZONE" -> {

                currentScreenState.value =
                    AppScreen.OUT_OF_SAFE_ZONE
            }


            "MEDICATION_ALERT" -> {

                currentScreenState.value =
                    AppScreen.MEDICATION_ALERT
            }
        }
    }


    /*
     * =====================================================
     * 귀가 요청 화면 공통 표시
     * =====================================================
     *
     * Notification 클릭
     * Foreground FCM
     *
     * 두 경로 모두 이 함수를 사용한다.
     */

    private fun showReturnHomeRequest(

        requestId: String?,

        guardianId: String?,

        wearerId: String?,

        source: String,
    ) {


        if (
            requestId.isNullOrBlank()
        ) {

            Log.w(
                "ReturnHomeFCM",
                "귀가 요청 request_id가 없습니다. source=$source"
            )


            return
        }


        /*
         * 이미 사용자가
         * 집으로 가기를 눌러 처리한 요청이면 무시.
         */
        if (
            returnHomeRequestStore
                .isHandled(
                    requestId
                )
        ) {

            Log.d(
                "ReturnHomeFCM",
                "이미 처리된 귀가 요청 무시 " +
                        "requestId=$requestId source=$source"
            )


            cancelReturnHomeNotification(
                requestId
            )


            return
        }


        /*
         * 보호자 앱에서 HOME 설정을 바꾼 뒤
         * 워치 앱이 계속 실행 중일 수 있으므로
         * FCM / Notification 경로에서도 최신 HOME을 다시 조회한다.
         */
        refreshHomeSafeZone(
            source = source
        )


        /*
         * 이 요청을 이미 알렸다는 기록.
         */
        returnHomeRequestStore
            .markNotified(
                requestId
            )


        /*
         * 혹시 동일 Notification이 있으면 제거.
         */
        cancelReturnHomeNotification(
            requestId
        )


        /*
         * 현재 요청 ID 저장.
         */
        currentReturnHomeRequestIdState.value =
            requestId


        /*
         * FCM에서 같이 받은 페어링 정보 저장.
         */
        if (
            !guardianId.isNullOrBlank()
        ) {

            guardianIdState.value =
                guardianId
        }


        if (
            !wearerId.isNullOrBlank()
        ) {

            wearerIdState.value =
                wearerId
        }


        /*
         * =================================================
         * 핵심
         * =================================================
         *
         * 앱이 현재 어떤 화면을 보고 있든
         * 귀가 요청 화면으로 바로 변경.
         */
        currentScreenState.value =
            AppScreen.RETURN_HOME_REQUEST


        Log.d(
            "ReturnHomeFCM",
            "귀가 요청 화면 즉시 표시 " +
                    "requestId=$requestId source=$source"
        )
    }


    /*
     * =====================================================
     * 동일 귀가 요청 Notification 제거
     * =====================================================
     */

    private fun cancelReturnHomeNotification(
        requestId: String
    ) {

        val manager =
            getSystemService(
                NotificationManager::class.java
            )


        manager
            .cancel(
                WatchFirebaseMessagingService
                    .notificationId(
                        requestId
                    )
            )
    }


    /*
     * =====================================================
     * 안전구역 계산용 위치
     * =====================================================
     */

    @android.annotation.SuppressLint(
        "MissingPermission"
    )
    private fun getLocation() {

        fusedLocationClient
            .getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            )
            .addOnSuccessListener {
                    location:
                    Location? ->


                if (
                    location != null
                ) {

                    myLocationState.value =
                        location
                }
            }
    }


    /*
     * =====================================================
     * Activity 화면 상태
     * =====================================================
     */

    override fun onStart() {

        super.onStart()


        /*
         * Foreground FCM Broadcast 수신 등록
         */
        if (
            !returnHomePushReceiverRegistered
        ) {

            val filter =
                IntentFilter(
                    WatchFirebaseMessagingService
                        .ACTION_RETURN_HOME_REQUEST_RECEIVED
                )


            ContextCompat
                .registerReceiver(

                    this,

                    returnHomePushReceiver,

                    filter,

                    ContextCompat
                        .RECEIVER_NOT_EXPORTED,
                )


            returnHomePushReceiverRegistered =
                true


            Log.d(
                "ReturnHomeFCM",
                "Foreground 귀가 요청 Receiver 등록"
            )
        }
    }


    private fun startHeartRateMeasurement() {
        if (!isActivityResumed ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        lifecycleScope.launch {
            try {
                if (heartRateManager.isHeartRateSupported() && isActivityResumed) {
                    heartRateManager.start()
                }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w("WatchHeartRate", "심박수 측정을 시작하지 못했습니다.", error)
            }
        }
    }

    override fun onResume() {

        super.onResume()


        isActivityResumed =
            true

        startHeartRateMeasurement()

        isInForeground =
            true


        Log.d(
            "ReturnHomeFCM",
            "MainActivity Foreground"
        )


        /*
         * 보호자 앱에서 HOME 안전구역을 변경한 뒤
         * 워치 앱으로 돌아온 경우 최신 HOME 정보를 다시 받는다.
         */
        if (
            !guardianIdState.value.isNullOrBlank() &&
            !wearerIdState.value.isNullOrBlank()
        ) {

            refreshHomeSafeZone(
                source = "ON_RESUME"
            )
        }
    }


    override fun onPause() {

        isActivityResumed =
            false

        heartRateManager.stop()

        isInForeground =
            false


        Log.d(
            "ReturnHomeFCM",
            "MainActivity Background"
        )


        super.onPause()
    }


    override fun onStop() {

        /*
         * Foreground Receiver 제거
         */
        if (
            returnHomePushReceiverRegistered
        ) {

            unregisterReceiver(
                returnHomePushReceiver
            )


            returnHomePushReceiverRegistered =
                false


            Log.d(
                "ReturnHomeFCM",
                "Foreground 귀가 요청 Receiver 해제"
            )
        }


        super.onStop()
    }


    /*
     * =====================================================
     * 종료
     * =====================================================
     */

    override fun onDestroy() {


        isInForeground =
            false


        isActivityResumed =
            false


        /*
         * 혹시 onStop 이전에 종료되는 경우를 대비.
         */
        if (
            returnHomePushReceiverRegistered
        ) {

            runCatching {

                unregisterReceiver(
                    returnHomePushReceiver
                )
            }


            returnHomePushReceiverRegistered =
                false
        }


        returnHomeRealtimeManager
            .stop()


        watchStatusManager
            .stop()


        watchLocationSyncManager
            .reset()


        heartRateManager
            .stop()


        locationManager
            .stop()


        super.onDestroy()
    }
}


/*
 * =========================================================
 * EmergencyManager
 * =========================================================
 */

@Composable
fun EmergencyManager(

    currentScreen:
    AppScreen,


    onScreenChange:
        (AppScreen) -> Unit,


    returnHomeRequestId:
    String?,


    returnHomeRealtimeManager:
    ReturnHomeRealtimeManager,


    guardianId:
    String?,


    wearerId:
    String?,


    homeLatitude:
    Double?,


    homeLongitude:
    Double?,


    homeRadiusMeters:
    Double?,


    pairingManager:
    PairingManager,


    onPairingCompleted:
        () -> Unit,


    onClearReturnHomeRequest:
        () -> Unit,


    onReturnHomeRequestHandled:
        (String) -> Unit,


    myLocation:
    Location?,


    watchLocation:
    WatchLocation?,


    heartRate:
    Float?,


    watchStatusManager:
    WatchStatusManager,


    watchLocationSyncManager:
    WatchLocationSyncManager,


    watchSafetyEventManager:
    WatchSafetyEventManager,


    emergencyCallStatus:
    EmergencyCallStatus,


    onEmergencyCallRequest:
        () -> Unit,

) {


    val context =
        LocalContext.current


    val eventScope =
        rememberCoroutineScope()


    /*
     * =====================================================
     * 실제 페어링 여부
     * =====================================================
     */

    val guardianConnected =
        guardianId != null &&
                wearerId != null


    /*
     * =====================================================
     * 연결된 경우 배터리 즉시 저장
     * =====================================================
     */

    LaunchedEffect(
        guardianConnected
    ) {

        if (
            guardianConnected
        ) {

            runCatching {

                watchStatusManager
                    .syncBatteryNow()

            }.onFailure { error ->

                Log.w(
                    "WatchStatus",
                    "배터리 즉시 동기화 실패: ${error.message}",
                    error
                )
            }
        }
    }


    /*
     * =====================================================
     * DB에서 조회한 실제 HOME 위치
     * =====================================================
     */

    val homeLocation =
        remember(
            homeLatitude,
            homeLongitude
        ) {

            if (
                homeLatitude != null &&
                homeLongitude != null
            ) {

                Location(
                    ""
                ).apply {

                    latitude =
                        homeLatitude

                    longitude =
                        homeLongitude
                }

            } else {

                null
            }
        }


    /*
     * WatchLocationManager가 주기적으로 전달하는 최신 GPS를
     * 안전구역 판정에도 사용한다.
     * 첫 지속 위치가 오기 전에는 기존 1회 위치를 fallback으로 사용한다.
     */
    val currentSafeZoneLocation =
        remember(
            watchLocation,
            myLocation
        ) {

            watchLocation
                ?.let { current ->

                    Location(
                        "watch_continuous_gps"
                    ).apply {

                        latitude =
                            current.latitude

                        longitude =
                            current.longitude

                        accuracy =
                            current.accuracyMeters
                    }
                }
                ?: myLocation
        }


    val safeZoneStatus =
        remember(
            currentSafeZoneLocation,
            homeLocation,
            homeRadiusMeters
        ) {

            val currentLocation =
                currentSafeZoneLocation

            val currentHome =
                homeLocation

            val radius =
                homeRadiusMeters

            if (
                currentLocation == null ||
                currentHome == null ||
                radius == null
            ) {

                SafeZoneStatus.CHECKING

            } else if (
                currentLocation.distanceTo(currentHome) <=
                radius.toFloat()
            ) {

                SafeZoneStatus.SAFE

            } else {

                SafeZoneStatus.OUTSIDE
            }
        }


    /*
     * =====================================================
     * 안전구역 감시
     * =====================================================
     */

    var hasTriggeredSafeZoneAlert by
    remember {

        mutableStateOf(
            false
        )
    }


    LaunchedEffect(
        currentSafeZoneLocation,
        homeLocation,
        homeRadiusMeters
    ) {

        val currentLocation =
            currentSafeZoneLocation
                ?: return@LaunchedEffect


        val currentHome =
            homeLocation
                ?: return@LaunchedEffect


        val radius =
            homeRadiusMeters
                ?: return@LaunchedEffect


        val distance =
            currentLocation
                .distanceTo(
                    currentHome
                )


        if (
            distance >
            radius.toFloat() &&
            !hasTriggeredSafeZoneAlert
        ) {

            hasTriggeredSafeZoneAlert =
                true


            onScreenChange(
                AppScreen.OUT_OF_SAFE_ZONE
            )

        } else if (
            distance <=
            radius.toFloat()
        ) {

            hasTriggeredSafeZoneAlert =
                false
        }
    }


    /*
     * =====================================================
     * 화면 전환
     * =====================================================
     */

    when (
        currentScreen
    ) {


        /*
         * =================================================
         * 홈
         * =================================================
         */

        AppScreen.HOME -> {

            HomeScreen(

                heartRate = heartRate,

                guardianConnected =
                    guardianConnected,


                safeZoneStatus =
                    safeZoneStatus,


                onGoHomeClick = {

                    onClearReturnHomeRequest()


                    onScreenChange(
                        AppScreen.COMPASS
                    )
                },


                onSosClick = {

                    val locationSnapshot =
                        watchLocation


                    onScreenChange(
                        AppScreen.SOS_SENT
                    )


                    onEmergencyCallRequest()


                    eventScope.launch {


                        if (
                            locationSnapshot != null
                        ) {

                            runCatching {

                                watchLocationSyncManager
                                    .forceSync(
                                        locationSnapshot
                                    )

                            }.onFailure { error ->

                                Log.w(
                                    "WatchSOS",
                                    "SOS 위치 저장 실패: ${error.message}",
                                    error
                                )
                            }
                        }


                        runCatching {

                            watchSafetyEventManager
                                .recordManualSos(
                                    locationSnapshot
                                )

                        }.onFailure { error ->

                            Log.w(
                                "WatchSOS",
                                "SOS 저장 실패: ${error.message}",
                                error
                            )
                        }
                    }
                },


                onGuardianConnectClick = {

                    if (
                        !guardianConnected
                    ) {

                        onScreenChange(
                            AppScreen.PAIRING
                        )
                    }
                }
            )
        }


        /*
         * =================================================
         * 보호자 페어링
         * =================================================
         */

        AppScreen.PAIRING -> {

            PairingScreen(

                pairingManager =
                    pairingManager,


                onConnected = {

                    onPairingCompleted()


                    onScreenChange(
                        AppScreen.PAIRING_SUCCESS
                    )
                }
            )
        }


        /*
         * =================================================
         * 페어링 완료
         * =================================================
         */

        AppScreen.PAIRING_SUCCESS -> {

            PairingSuccessScreen(

                onFinished = {

                    onScreenChange(
                        AppScreen.HOME
                    )
                }
            )
        }


        /*
         * =================================================
         * 보호자 귀가 요청
         * =================================================
         */

        AppScreen.RETURN_HOME_REQUEST -> {

            ReturnHomeRequestScreen(

                /*
                 * =============================================
                 * 집으로 가기
                 *
                 * REQUESTED → ACCEPTED
                 * =============================================
                 */
                onGoHomeClick = {


                    val requestId =
                        returnHomeRequestId


                    val currentGuardianId =
                        guardianId


                    val currentWearerId =
                        wearerId


                    if (
                        requestId == null ||
                        currentGuardianId == null ||
                        currentWearerId == null
                    ) {

                        Toast
                            .makeText(
                                context,
                                "귀가 요청 정보를 확인할 수 없습니다.",
                                Toast.LENGTH_SHORT
                            )
                            .show()

                    } else {

                        eventScope.launch {

                            runCatching {

                                returnHomeRealtimeManager
                                    .acceptRequest(

                                        requestId =
                                            requestId,

                                        guardianId =
                                            currentGuardianId,

                                        wearerId =
                                            currentWearerId
                                    )

                            }.onSuccess {


                                /*
                                 * 같은 요청이 FCM / Realtime으로
                                 * 다시 표시되지 않도록 처리.
                                 *
                                 * requestId 자체는 지우지 않는다.
                                 * 이후 NAVIGATING / COMPLETED에서 사용한다.
                                 */
                                onReturnHomeRequestHandled(
                                    requestId
                                )


                                Log.d(
                                    "ReturnHome",
                                    "귀가 요청 ACCEPTED 성공: $requestId"
                                )


                                onScreenChange(
                                    AppScreen.COMPASS
                                )


                            }.onFailure { error ->

                                Log.e(
                                    "ReturnHome",
                                    "귀가 요청 수락 실패",
                                    error
                                )


                                Toast
                                    .makeText(
                                        context,
                                        "귀가 요청 수락에 실패했습니다.",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                            }
                        }
                    }
                },


                /*
                 * =============================================
                 * 나중에
                 *
                 * REQUESTED → CANCELLED
                 *
                 * 보호자 앱이 Realtime으로 CANCELLED를 수신하면
                 * 귀가 요청 버튼이 다시 활성화된다.
                 * =============================================
                 */
                onDismissClick = {


                    val requestId =
                        returnHomeRequestId


                    val currentGuardianId =
                        guardianId


                    val currentWearerId =
                        wearerId


                    if (
                        requestId == null ||
                        currentGuardianId == null ||
                        currentWearerId == null
                    ) {

                        Toast
                            .makeText(
                                context,
                                "귀가 요청 정보를 확인할 수 없습니다.",
                                Toast.LENGTH_SHORT
                            )
                            .show()

                    } else {

                        eventScope.launch {

                            runCatching {

                                returnHomeRealtimeManager
                                    .cancelRequest(

                                        requestId =
                                            requestId,

                                        guardianId =
                                            currentGuardianId,

                                        wearerId =
                                            currentWearerId
                                    )

                            }.onSuccess {


                                onReturnHomeRequestHandled(
                                    requestId
                                )


                                /*
                                 * CANCELLED된 요청은 더 이상
                                 * 현재 귀가 요청으로 유지하지 않는다.
                                 */
                                onClearReturnHomeRequest()


                                Log.d(
                                    "ReturnHome",
                                    "귀가 요청 CANCELLED 성공: $requestId"
                                )


                                onScreenChange(
                                    AppScreen.HOME
                                )


                            }.onFailure { error ->

                                Log.e(
                                    "ReturnHome",
                                    "귀가 요청 CANCELLED 실패",
                                    error
                                )


                                Toast
                                    .makeText(
                                        context,
                                        "귀가 요청 응답에 실패했습니다.",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                            }
                        }
                    }
                }
            )
        }


        /*
         * =================================================
         * 낙상 감지
         * =================================================
         */

        AppScreen.FALL_DETECTED -> {

            FallDetectScreen(


                onOkayClick = {

                    val locationSnapshot =
                        watchLocation


                    FallEventState
                        .reset()


                    onScreenChange(
                        AppScreen.HOME
                    )


                    eventScope.launch {

                        runCatching {

                            watchSafetyEventManager
                                .recordFallConfirmedSafe(
                                    locationSnapshot
                                )

                        }.onFailure { error ->

                            Log.w(
                                "WatchSafetyEvent",
                                "안전 확인 이벤트 저장 실패: ${error.message}",
                                error
                            )
                        }
                    }
                },


                onHelpClick = {

                    val locationSnapshot =
                        watchLocation


                    onScreenChange(
                        AppScreen.SOS_SENT
                    )


                    onEmergencyCallRequest()


                    eventScope.launch {


                        if (
                            locationSnapshot != null
                        ) {

                            runCatching {

                                watchLocationSyncManager
                                    .forceSync(
                                        locationSnapshot
                                    )

                            }.onFailure { error ->

                                Log.w(
                                    "WatchSafetyEvent",
                                    "SOS 위치 저장 실패: ${error.message}",
                                    error
                                )
                            }
                        }


                        runCatching {

                            watchSafetyEventManager
                                .recordManualSos(
                                    locationSnapshot
                                )

                        }.onFailure { error ->

                            Log.w(
                                "WatchSafetyEvent",
                                "수동 SOS 저장 실패: ${error.message}",
                                error
                            )
                        }
                    }
                },


                onTimeout = {

                    val locationSnapshot =
                        watchLocation


                    onScreenChange(
                        AppScreen.SOS_SENT
                    )


                    onEmergencyCallRequest()


                    eventScope.launch {


                        if (
                            locationSnapshot != null
                        ) {

                            runCatching {

                                watchLocationSyncManager
                                    .forceSync(
                                        locationSnapshot
                                    )

                            }.onFailure { error ->

                                Log.w(
                                    "WatchSafetyEvent",
                                    "자동 SOS 위치 저장 실패: ${error.message}",
                                    error
                                )
                            }
                        }


                        runCatching {

                            watchSafetyEventManager
                                .recordAutomaticSos(
                                    locationSnapshot
                                )

                        }.onFailure { error ->

                            Log.w(
                                "WatchSafetyEvent",
                                "자동 SOS 저장 실패: ${error.message}",
                                error
                            )
                        }
                    }
                }
            )
        }


        /*
         * =================================================
         * SOS 전송 완료
         * =================================================
         */

        AppScreen.SOS_SENT -> {

            SosSentScreen(

                callStatus =
                    emergencyCallStatus,

                onReturnHome = {

                    FallEventState
                        .reset()


                    onScreenChange(
                        AppScreen.HOME
                    )
                }
            )
        }


        /*
         * =================================================
         * 집으로 가기
         * =================================================
         */

        AppScreen.COMPASS -> {

            TmapRouteTestScreen(

                returnHomeRequestId =
                    returnHomeRequestId,

                returnHomeRealtimeManager =
                    returnHomeRealtimeManager,

                guardianId =
                    guardianId,

                wearerId =
                    wearerId,

                homeLatitude =
                    homeLatitude,

                homeLongitude =
                    homeLongitude,

                homeRadiusMeters =
                    homeRadiusMeters,

                /*
                 * MainActivity에서 이미 수신 중인
                 * 실제 워치 GPS 위치를 그대로 전달한다.
                 *
                 * TmapRouteTestScreen 내부에서
                 * WatchLocationManager를 새로 만들지 않는다.
                 */
                watchLocation =
                    watchLocation,

                /*
                 * HOME 반경 진입 후
                 * NAVIGATING → COMPLETED 성공 시 호출.
                 */
                onReturnHomeCompleted = {
                        requestId ->


                    onReturnHomeRequestHandled(
                        requestId
                    )


                    onClearReturnHomeRequest()


                    Log.d(
                        "ReturnHome",
                        "귀가 완료 처리 성공: $requestId"
                    )


                    onScreenChange(
                        AppScreen.HOME
                    )
                }
            )
        }


        /*
         * =================================================
         * 안전구역 이탈
         * =================================================
         */

        AppScreen.OUT_OF_SAFE_ZONE -> {

            OutOfSafeZoneScreen(

                onGoHomeClick = {

                    onClearReturnHomeRequest()


                    onScreenChange(
                        AppScreen.COMPASS
                    )
                },


                onDismissClick = {

                    onScreenChange(
                        AppScreen.HOME
                    )
                }
            )
        }


        /*
         * =================================================
         * 복약 알림
         * =================================================
         */

        AppScreen.MEDICATION_ALERT -> {

            val sharedPref =
                remember {

                    context
                        .getSharedPreferences(
                            "WatchSafetyPrefs",
                            Context.MODE_PRIVATE
                        )
                }


            MedicationAlertScreen(

                onTakenClick = {


                    val currentDate =
                        java.text
                            .SimpleDateFormat(
                                "yyyy-MM-dd",
                                java.util.Locale
                                    .getDefault()
                            )
                            .format(
                                java.util.Date()
                            )


                    sharedPref
                        .edit()
                        .putBoolean(
                            "isMedicationTaken",
                            true
                        )
                        .putString(
                            "lastTakenDate",
                            currentDate
                        )
                        .apply()


                    onScreenChange(
                        AppScreen.HOME
                    )
                },


                onSnoozeClick = {

                    onScreenChange(
                        AppScreen.HOME
                    )
                }
            )
        }
    }
}


/*
 * =========================================================
 * 보호자 귀가 요청 화면
 * =========================================================
 */

@Composable
fun ReturnHomeRequestScreen(

    onGoHomeClick:
        () -> Unit,

    onDismissClick:
        () -> Unit

) {

    val context =
        LocalContext.current


    val vibrator =
        remember {

            context
                .getSystemService(
                    Context.VIBRATOR_SERVICE
                ) as android.os.Vibrator
        }


    LaunchedEffect(
        Unit
    ) {

        val pattern =
            longArrayOf(
                0,
                350,
                150,
                350,
                150,
                600
            )


        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            vibrator.vibrate(

                VibrationEffect
                    .createWaveform(
                        pattern,
                        -1
                    )
            )

        } else {

            @Suppress("DEPRECATION")

            vibrator.vibrate(
                pattern,
                -1
            )
        }
    }


    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(
                        0xFF1976D2
                    )
                )
                .padding(
                    16.dp
                ),

        verticalArrangement =
            Arrangement.Center,

        horizontalAlignment =
            Alignment.CenterHorizontally

    ) {

        Icon(

            imageVector =
                Icons.Default.Home,

            contentDescription =
                "귀가 요청",

            modifier =
                Modifier.size(
                    36.dp
                ),

            tint =
                Color.White
        )


        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )


        Text(

            text =
                "보호자가 귀가를\n요청했어요",

            color =
                Color.White,

            fontSize =
                16.sp,

            fontWeight =
                FontWeight.Bold,

            textAlign =
                TextAlign.Center
        )


        Spacer(
            modifier =
                Modifier.height(
                    14.dp
                )
        )


        Button(

            onClick =
                onGoHomeClick,

            colors =
                ButtonDefaults
                    .buttonColors(

                        backgroundColor =
                            Color.White,

                        contentColor =
                            Color(
                                0xFF1976D2
                            )
                    ),

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        40.dp
                    )

        ) {

            Row(

                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.Center
            ) {

                Icon(

                    imageVector =
                        Icons.Default.Home,

                    contentDescription =
                        "집으로 가기",

                    modifier =
                        Modifier.size(
                            16.dp
                        )
                )


                Spacer(
                    modifier =
                        Modifier.width(
                            4.dp
                        )
                )


                Text(

                    text =
                        "집으로 가기",

                    fontSize =
                        14.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )


        Button(

            onClick =
                onDismissClick,

            colors =
                ButtonDefaults
                    .buttonColors(

                        backgroundColor =
                            Color.DarkGray,

                        contentColor =
                            Color.White
                    ),

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        32.dp
                    )

        ) {

            Text(

                text =
                    "나중에",

                fontSize =
                    12.sp
            )
        }
    }
}


/*
 * =========================================================
 * 안전구역 이탈 화면
 * =========================================================
 */

@Composable
fun OutOfSafeZoneScreen(

    onGoHomeClick:
        () -> Unit,

    onDismissClick:
        () -> Unit

) {

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(
                        0xFFE64A19
                    )
                )
                .padding(
                    16.dp
                ),

        verticalArrangement =
            Arrangement.Center,

        horizontalAlignment =
            Alignment.CenterHorizontally

    ) {

        Icon(

            imageVector =
                Icons.Default.LocationOff,

            contentDescription =
                "경로 이탈",

            modifier =
                Modifier.size(
                    36.dp
                ),

            tint =
                Color.White
        )


        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )


        Text(

            text =
                "안전구역을 벗어났습니다!",

            color =
                Color.White,

            fontSize =
                14.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )


        Button(

            onClick =
                onGoHomeClick,

            colors =
                ButtonDefaults
                    .buttonColors(

                        backgroundColor =
                            Color.White,

                        contentColor =
                            Color(
                                0xFFE64A19
                            )
                    ),

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        40.dp
                    )

        ) {

            Row(

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(

                    imageVector =
                        Icons.Default.Home,

                    contentDescription =
                        "집으로",

                    modifier =
                        Modifier.size(
                            16.dp
                        )
                )


                Spacer(
                    modifier =
                        Modifier.width(
                            4.dp
                        )
                )


                Text(

                    text =
                        "집으로 안내받기",

                    fontSize =
                        14.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )


        Button(

            onClick =
                onDismissClick,

            colors =
                ButtonDefaults
                    .buttonColors(

                        backgroundColor =
                            Color.DarkGray,

                        contentColor =
                            Color.White
                    ),

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        32.dp
                    )

        ) {

            Text(

                text =
                    "괜찮아요 (알림 닫기)",

                fontSize =
                    12.sp
            )
        }
    }
}


/*
 * =========================================================
 * 낙상 감지 화면
 * =========================================================
 */

@Composable
fun FallDetectScreen(

    onOkayClick:
        () -> Unit,

    onHelpClick:
        () -> Unit,

    onTimeout:
        () -> Unit

) {

    var timeLeft by
    remember {

        mutableStateOf(
            10
        )
    }


    LaunchedEffect(
        timeLeft
    ) {

        if (
            timeLeft > 0
        ) {

            delay(
                1000L
            )


            timeLeft--

        } else {

            onTimeout()
        }
    }


    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(
                        0xFFFF9800
                    )
                )
                .padding(
                    horizontal =
                        16.dp
                ),

        verticalArrangement =
            Arrangement.Center,

        horizontalAlignment =
            Alignment.CenterHorizontally

    ) {

        Text(

            text =
                "${timeLeft}초 후 구조 요청",

            color =
                Color.White,

            fontSize =
                16.sp,

            fontWeight =
                FontWeight.Bold
        )


        Text(

            text =
                "괜찮으신가요?",

            color =
                Color.White,

            fontSize =
                18.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )


        Button(

            onClick =
                onOkayClick,

            colors =
                ButtonDefaults
                    .buttonColors(

                        backgroundColor =
                            Color(
                                0xFF4CAF50
                            ),

                        contentColor =
                            Color.White
                    ),

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        40.dp
                    )

        ) {

            Row(

                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.Center

            ) {

                Icon(

                    imageVector =
                        Icons.Default.Check,

                    contentDescription =
                        "괜찮아요",

                    modifier =
                        Modifier.size(
                            16.dp
                        )
                )


                Spacer(
                    modifier =
                        Modifier.width(
                            4.dp
                        )
                )


                Text(

                    text =
                        "괜찮아요",

                    fontSize =
                        14.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )


        Button(

            onClick =
                onHelpClick,

            colors =
                ButtonDefaults
                    .buttonColors(

                        backgroundColor =
                            Color(
                                0xFFD32F2F
                            ),

                        contentColor =
                            Color.White
                    ),

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        40.dp
                    )

        ) {

            Row(

                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.Center

            ) {

                Icon(

                    imageVector =
                        Icons.Default.Warning,

                    contentDescription =
                        "도와주세요",

                    modifier =
                        Modifier.size(
                            16.dp
                        )
                )


                Spacer(
                    modifier =
                        Modifier.width(
                            4.dp
                        )
                )


                Text(

                    text =
                        "도와주세요!",

                    fontSize =
                        14.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}


/*
 * =========================================================
 * SOS 전송 완료
 * =========================================================
 */

@Composable
fun SosSentScreen(

    callStatus:
    EmergencyCallStatus =
        EmergencyCallStatus.IDLE,

    onReturnHome:
        () -> Unit

) {

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(
                        0xFFD32F2F
                    )
                )
                .padding(
                    horizontal =
                        16.dp
                ),

        verticalArrangement =
            Arrangement.Center,

        horizontalAlignment =
            Alignment.CenterHorizontally

    ) {

        Icon(

            imageVector =
                Icons.Default.Call,

            contentDescription =
                "전송 완료",

            modifier =
                Modifier.size(
                    40.dp
                ),

            tint =
                Color.White
        )


        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )


        Text(

            text =
                "보호자에게 구조를\n요청했습니다.",

            color =
                Color.White,

            fontSize =
                16.sp,

            fontWeight =
                FontWeight.Bold,

            textAlign =
                TextAlign.Center
        )


        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )


        Text(
            text =
                when (
                    callStatus
                ) {
                    EmergencyCallStatus.IDLE ->
                        "보호자 통화를 준비합니다."

                    EmergencyCallStatus.CONNECTING ->
                        "보호자에게 연결 중..."

                    EmergencyCallStatus.CALL_STARTED ->
                        "보호자에게 전화를 걸었습니다."

                    EmergencyCallStatus.CONTACT_NOT_CONFIGURED ->
                        "연락처가 없어 알림만 전송했습니다."

                    EmergencyCallStatus.PERMISSION_REQUIRED ->
                        "전화 권한을 확인하고 있습니다."

                    EmergencyCallStatus.PERMISSION_DENIED ->
                        "전화 권한이 없어 알림만 전송했습니다."

                    EmergencyCallStatus.CALLING_UNAVAILABLE ->
                        "통화할 수 없어 알림만 전송했습니다."

                    EmergencyCallStatus.FAILED ->
                        "통화 연결 실패, 알림은 전송했습니다."
                },
            color =
                Color.White.copy(
                    alpha =
                        0.9f
                ),
            fontSize =
                11.sp,
            textAlign =
                TextAlign.Center,
        )


        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )


        Button(

            onClick =
                onReturnHome,

            colors =
                ButtonDefaults
                    .buttonColors(

                        backgroundColor =
                            Color.White,

                        contentColor =
                            Color.Black
                    ),

            modifier =
                Modifier.height(
                    32.dp
                )

        ) {

            Text(

                text =
                    "확인",

                fontSize =
                    12.sp
            )
        }
    }
}


/*
 * =========================================================
 * 기존 Compass 화면
 * =========================================================
 */

@Composable
fun CompassScreen(

    myLocation:
    Location?,

    homeLocation:
    Location,

    onCloseClick:
        () -> Unit

) {

    val context =
        LocalContext.current


    var routeResult by
    remember {

        mutableStateOf<TmapRouteResult?>(
            null
        )
    }


    var isRouting by
    remember {

        mutableStateOf(
            false
        )
    }


    val tmapClient =
        remember {

            TmapRouteClient()
        }


    LaunchedEffect(
        myLocation
    ) {

        if (
            myLocation != null &&
            routeResult == null &&
            !isRouting
        ) {

            isRouting =
                true


            try {

                routeResult =
                    tmapClient
                        .getPedestrianRoute(

                            startLongitude =
                                myLocation.longitude,

                            startLatitude =
                                myLocation.latitude,

                            endLongitude =
                                homeLocation.longitude,

                            endLatitude =
                                homeLocation.latitude
                        )

            } catch (
                e: Exception
            ) {

                Toast
                    .makeText(
                        context,
                        "경로 탐색 실패",
                        Toast.LENGTH_SHORT
                    )
                    .show()

            } finally {

                isRouting =
                    false
            }
        }
    }


    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(
                        0xFF1976D2
                    )
                )
                .padding(
                    16.dp
                ),

        verticalArrangement =
            Arrangement.Center,

        horizontalAlignment =
            Alignment.CenterHorizontally

    ) {

        Text(

            text =
                "T맵 도보 길안내",

            color =
                Color.White,

            fontSize =
                14.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )


        when {


            myLocation == null -> {

                Text(

                    text =
                        "GPS 위치 탐색중...",

                    color =
                        Color.White,

                    fontSize =
                        12.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }


            isRouting -> {

                Text(

                    text =
                        "경로 탐색 중...",

                    color =
                        Color.Yellow,

                    fontSize =
                        12.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }


            routeResult != null -> {

                val result =
                    routeResult!!


                Text(

                    text =
                        "남은 거리: ${result.totalDistanceMeters}m",

                    color =
                        Color.Yellow,

                    fontSize =
                        16.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            2.dp
                        )
                )


                Text(

                    text =
                        "예상 시간: ${result.totalTimeSeconds / 60}분",

                    color =
                        Color.White,

                    fontSize =
                        12.sp
                )


                val nextStep =
                    result.steps
                        .firstOrNull {

                            it.description
                                .isNotBlank()
                        }


                if (
                    nextStep != null
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )


                    Text(

                        text =
                            "▶ ${nextStep.description}",

                        color =
                            Color.White,

                        fontSize =
                            11.sp,

                        textAlign =
                            TextAlign.Center
                    )
                }
            }


            else -> {

                Text(

                    text =
                        "경로를 찾을 수 없습니다.",

                    color =
                        Color.White,

                    fontSize =
                        12.sp
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )


        Button(

            onClick =
                onCloseClick,

            colors =
                ButtonDefaults
                    .buttonColors(

                        backgroundColor =
                            Color.White,

                        contentColor =
                            Color(
                                0xFF1976D2
                            )
                    ),

            modifier =
                Modifier.height(
                    28.dp
                )

        ) {

            Row(

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(

                    imageVector =
                        Icons.Default.Close,

                    contentDescription =
                        "안내 종료",

                    modifier =
                        Modifier.size(
                            12.dp
                        )
                )


                Spacer(
                    modifier =
                        Modifier.width(
                            4.dp
                        )
                )


                Text(

                    text =
                        "안내 종료",

                    fontSize =
                        11.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}


/*
 * =========================================================
 * 복약 알림
 * =========================================================
 */

@Composable
fun MedicationAlertScreen(

    onTakenClick:
        () -> Unit,

    onSnoozeClick:
        () -> Unit

) {

    val context =
        LocalContext.current


    val vibrator =
        remember {

            context
                .getSystemService(
                    Context.VIBRATOR_SERVICE
                ) as android.os.Vibrator
        }


    LaunchedEffect(
        Unit
    ) {

        val pattern =
            longArrayOf(
                0,
                500,
                200,
                500
            )


        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            vibrator.vibrate(

                VibrationEffect
                    .createWaveform(
                        pattern,
                        -1
                    )
            )

        } else {

            @Suppress("DEPRECATION")

            vibrator.vibrate(
                pattern,
                -1
            )
        }
    }


    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(
                        0xFF1976D2
                    )
                )
                .padding(
                    16.dp
                ),

        verticalArrangement =
            Arrangement.Center,

        horizontalAlignment =
            Alignment.CenterHorizontally

    ) {

        Icon(

            imageVector =
                Icons.Default.Notifications,

            contentDescription =
                "약 알림",

            modifier =
                Modifier.size(
                    36.dp
                ),

            tint =
                Color.Yellow
        )


        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )


        Text(

            text =
                "약 드실 시간입니다!",

            color =
                Color.White,

            fontSize =
                16.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )


        Button(

            onClick =
                onTakenClick,

            colors =
                ButtonDefaults
                    .buttonColors(

                        backgroundColor =
                            Color(
                                0xFF4CAF50
                            ),

                        contentColor =
                            Color.White
                    ),

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        36.dp
                    )

        ) {

            Row(

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(

                    imageVector =
                        Icons.Default.Check,

                    contentDescription =
                        "먹음",

                    modifier =
                        Modifier.size(
                            16.dp
                        )
                )


                Spacer(
                    modifier =
                        Modifier.width(
                            4.dp
                        )
                )


                Text(

                    text =
                        "지금 먹었어요!",

                    fontSize =
                        13.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )


        Button(

            onClick =
                onSnoozeClick,

            colors =
                ButtonDefaults
                    .buttonColors(

                        backgroundColor =
                            Color.DarkGray,

                        contentColor =
                            Color.White
                    ),

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        28.dp
                    )

        ) {

            Row(

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(

                    imageVector =
                        Icons.Default.Close,

                    contentDescription =
                        "나중에",

                    modifier =
                        Modifier.size(
                            12.dp
                        )
                )


                Spacer(
                    modifier =
                        Modifier.width(
                            4.dp
                        )
                )


                Text(

                    text =
                        "10분 뒤에 다시",

                    fontSize =
                        11.sp
                )
            }
        }
    }
}
