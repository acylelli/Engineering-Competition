package com.example.watchsafety.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
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

import androidx.lifecycle.lifecycleScope

import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

import com.example.watchsafety.data.HomeSafeZoneManager
import com.example.watchsafety.data.ReturnHomeRequestStore
import com.example.watchsafety.data.ReturnHomeRealtimeManager
import com.example.watchsafety.data.WatchFcmTokenManager
import com.example.watchsafety.data.WatchLocationSyncManager
import com.example.watchsafety.data.WatchSafetyEventManager
import com.example.watchsafety.data.WatchStatusManager
import com.example.watchsafety.health.HeartRateManager
import com.example.watchsafety.location.WatchLocation
import com.example.watchsafety.location.WatchLocationManager
import com.example.watchsafety.navigation.TmapRouteClient
import com.example.watchsafety.navigation.TmapRouteResult
import com.example.watchsafety.pairing.PairingManager
import com.example.watchsafety.notification.WatchFirebaseMessagingService
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

class MainActivity : ComponentActivity() {


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
     * Realtime 수신 시 현재 Activity가 실제 화면에 보이는지 확인
     */
    private var isActivityResumed:
            Boolean = false


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
        mutableStateOf<String?>(null)


    /*
     * 실제 페어링 ID
     */
    private val guardianIdState =
        mutableStateOf<String?>(null)

    private val wearerIdState =
        mutableStateOf<String?>(null)


    /*
     * DB의 실제 집 안전구역
     */
    private val homeLatitudeState =
        mutableStateOf<Double?>(null)

    private val homeLongitudeState =
        mutableStateOf<Double?>(null)

    private val homeRadiusMetersState =
        mutableStateOf<Double?>(null)


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
         * 잠금 화면에서도 긴급화면 표시
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
         * 배터리 상태 감시
         */
        watchStatusManager
            .start(
                lifecycleScope
            )


        /*
         * 실제 페어링 정보 조회 후
         * 귀가 요청 Realtime 시작
         */
        refreshPairingAndStartRealtime()


        /*
         * 알림 Intent 처리
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


                    val allGranted =
                        permissions
                            .values
                            .all {
                                it
                            }


                    if (
                        allGranted
                    ) {


                        /*
                         * 심박수
                         */
                        heartRateManager
                            .start()


                        /*
                         * GPS
                         */
                        locationManager
                            .start()


                        /*
                         * 안전구역 계산용
                         * 위치 1회 조회
                         */
                        getLocation()


                        /*
                         * Health Services 낙상 감지
                         */
                        lifecycleScope.launch {

                            if (
                                fallManager
                                    .isFallDetectionSupported()
                            ) {

                                fallManager
                                    .registerFallDetection()
                            }
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


                    } else {

                        Toast
                            .makeText(
                                this@MainActivity,
                                "권한이 필요합니다!",
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
                                .POST_NOTIFICATIONS
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
                        currentReturnHomeRequestIdState.value = null
                    },


                    onReturnHomeRequestAccepted = { requestId ->

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


                    /*
                     * 실제 지속 GPS
                     */
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


                    /*
                     * 안전 이벤트 Manager
                     */
                    watchSafetyEventManager =
                        watchSafetyEventManager
                )
            }
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
                pairingManager.getPairingInfo()
            }.onSuccess { info ->

                if (
                    !info.isPaired ||
                    info.guardianId.isNullOrBlank() ||
                    info.wearerId.isNullOrBlank()
                ) {
                    guardianIdState.value = null
                    wearerIdState.value = null
                    homeLatitudeState.value = null
                    homeLongitudeState.value = null
                    homeRadiusMetersState.value = null
                    returnHomeRealtimeManager.stop()
                    Log.d("PairingInfo", "현재 페어링 정보 없음")
                    return@onSuccess
                }

                val guardianId =
                    info.guardianId
                        ?: return@onSuccess

                val wearerId =
                    info.wearerId
                        ?: return@onSuccess

                guardianIdState.value = guardianId
                wearerIdState.value = wearerId

                Log.d(
                    "PairingInfo",
                    "실제 페어링 정보 조회 성공 guardianId=$guardianId wearerId=$wearerId"
                )

                /*
                 * 현재 워치 FCM Token → devices.watch_fcm_token 동기화
                 */
                runCatching {
                    watchFcmTokenManager.syncCurrentToken()
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

                refreshHomeSafeZone()

                returnHomeRealtimeManager.stop()
                returnHomeRealtimeManager.start(
                    guardianId = guardianId,
                    wearerId = wearerId
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
                     * Activity가 백그라운드여도 요청 ID는 보관한다.
                     * 이 경우 FCM 알림은 막지 않는다.
                     */
                    currentReturnHomeRequestIdState.value =
                        requestId

                    currentScreenState.value =
                        AppScreen.RETURN_HOME_REQUEST

                    if (
                        isActivityResumed
                    ) {
                        /*
                         * 화면에 앱이 보이는 경우 Realtime이 직접 화면을 표시하므로
                         * 같은 request_id의 FCM 알림이 중복 표시되지 않게 기록한다.
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
    private fun refreshHomeSafeZone() {

        lifecycleScope.launch {

            runCatching {
                homeSafeZoneManager.getHomeSafeZone()
            }.onSuccess { home ->

                if (
                    !home.isConfigured ||
                    home.centerLatitude == null ||
                    home.centerLongitude == null
                ) {
                    homeLatitudeState.value = null
                    homeLongitudeState.value = null
                    homeRadiusMetersState.value = null
                    Log.d("HomeSafeZone", "등록된 집 안전구역이 없습니다.")
                    return@onSuccess
                }

                homeLatitudeState.value = home.centerLatitude
                homeLongitudeState.value = home.centerLongitude
                homeRadiusMetersState.value = home.radiusMeters

                Log.d(
                    "HomeSafeZone",
                    "집 안전구역 조회 성공 latitude=${home.centerLatitude} " +
                            "longitude=${home.centerLongitude} radius=${home.radiusMeters}"
                )

            }.onFailure { error ->
                Log.e("HomeSafeZone", "집 안전구역 조회 실패", error)
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
     * Intent 처리
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

                if (
                    requestId.isNullOrBlank()
                ) {
                    Log.w(
                        "ReturnHomeFCM",
                        "귀가 요청 request_id가 없습니다."
                    )
                    return
                }

                if (
                    returnHomeRequestStore
                        .isHandled(
                            requestId
                        )
                ) {
                    Log.d(
                        "ReturnHomeFCM",
                        "이미 처리된 귀가 요청 알림 클릭 무시: $requestId"
                    )

                    cancelReturnHomeNotification(
                        requestId
                    )

                    return
                }

                returnHomeRequestStore
                    .markNotified(
                        requestId
                    )

                cancelReturnHomeNotification(
                    requestId
                )

                currentReturnHomeRequestIdState.value =
                    requestId

                if (
                    !pushGuardianId.isNullOrBlank()
                ) {
                    guardianIdState.value =
                        pushGuardianId
                }

                if (
                    !pushWearerId.isNullOrBlank()
                ) {
                    wearerIdState.value =
                        pushWearerId
                }

                currentScreenState.value =
                    AppScreen.RETURN_HOME_REQUEST

                Log.d(
                    "ReturnHomeFCM",
                    "귀가 요청 Notification 클릭 requestId=$requestId"
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

        manager.cancel(
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
                    location: Location? ->


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

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
    }


    override fun onPause() {
        isActivityResumed = false
        super.onPause()
    }


    /*
     * =====================================================
     * 종료
     * =====================================================
     */

    override fun onDestroy() {


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


    onReturnHomeRequestAccepted:
        (String) -> Unit,


    /*
     * 기존 안전구역 계산용
     */
    myLocation:
    Location?,


    /*
     * 지속 GPS
     */
    watchLocation:
    WatchLocation?,


    heartRate:
    Float?,


    watchStatusManager:
    WatchStatusManager,


    watchLocationSyncManager:
    WatchLocationSyncManager,


    watchSafetyEventManager:
    WatchSafetyEventManager

) {


    val context =
        LocalContext.current


    /*
     * Composable 버튼에서
     * suspend 함수를 실행하기 위한 Scope
     */
    val eventScope =
        rememberCoroutineScope()


    /*
     * =====================================================
     * 실제 페어링 여부
     * =====================================================
     */

    val guardianConnected =
        guardianId != null && wearerId != null


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
     * GPS → Supabase
     * =====================================================
     *
     * 첫 위치
     * 또는
     * 30m 이동
     * 또는
     * 1분 경과
     */

    LaunchedEffect(
        guardianConnected,
        watchLocation
    ) {


        if (
            !guardianConnected
        ) {

            return@LaunchedEffect
        }


        val currentLocation =
            watchLocation
                ?: return@LaunchedEffect


        runCatching {

            watchLocationSyncManager
                .syncIfNeeded(
                    currentLocation
                )

        }.onFailure { error ->

            Log.w(
                "WatchLocationSync",
                "위치 동기화 실패: ${error.message}",
                error
            )
        }
    }


    /*
     * =====================================================
     * DB에서 조회한 실제 집 위치
     * =====================================================
     */

    val homeLocation =
        remember(
            homeLatitude,
            homeLongitude
        ) {
            if (homeLatitude != null && homeLongitude != null) {
                Location("").apply {
                    latitude = homeLatitude
                    longitude = homeLongitude
                }
            } else {
                null
            }
        }


    /*
     * =====================================================
     * 안전구역 감시
     * =====================================================
     */

    var hasTriggeredSafeZoneAlert by
    remember {
        mutableStateOf(false)
    }


    LaunchedEffect(
        myLocation,
        homeLocation,
        homeRadiusMeters
    ) {
        val currentLocation = myLocation ?: return@LaunchedEffect
        val currentHome = homeLocation ?: return@LaunchedEffect
        val radius = homeRadiusMeters ?: return@LaunchedEffect

        val distance =
            currentLocation.distanceTo(currentHome)

        if (
            distance > radius.toFloat() &&
            !hasTriggeredSafeZoneAlert
        ) {
            hasTriggeredSafeZoneAlert = true
            onScreenChange(AppScreen.OUT_OF_SAFE_ZONE)
        } else if (distance <= radius.toFloat()) {
            hasTriggeredSafeZoneAlert = false
        }
    }


    /*
     * =====================================================
     * 심박수 이상 → 구조요청
     *
     * 현재 비활성화
     * =====================================================
     */

    /*
    var hasTriggeredHeartRateAlert by
        remember {
            mutableStateOf(false)
        }

    LaunchedEffect(heartRate) {

        if (heartRate != null) {

            if (
                (heartRate < 50f || heartRate > 90f) &&
                !hasTriggeredHeartRateAlert
            ) {

                hasTriggeredHeartRateAlert = true

                onScreenChange(
                    AppScreen.FALL_DETECTED
                )

            } else if (
                heartRate in 50f..90f
            ) {

                hasTriggeredHeartRateAlert = false
            }
        }
    }
    */


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

                guardianConnected =
                    guardianConnected,

                onGoHomeClick = {

                    onClearReturnHomeRequest()

                    onScreenChange(
                        AppScreen.COMPASS
                    )
                },

                /*
                 * 홈 SOS 버튼
                 */
                onSosClick = {

                    val locationSnapshot =
                        watchLocation


                    /*
                     * 우선 화면 즉시 전환
                     */
                    onScreenChange(
                        AppScreen.SOS_SENT
                    )


                    eventScope.launch {

                        /*
                         * SOS 발생 순간 위치 강제 저장
                         */
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


                        /*
                         * safety_events에
                         * SOS_MANUAL 저장
                         */
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

                onGoHomeClick = {
                    val requestId = returnHomeRequestId
                    val currentGuardianId = guardianId
                    val currentWearerId = wearerId

                    if (
                        requestId == null ||
                        currentGuardianId == null ||
                        currentWearerId == null
                    ) {
                        Toast.makeText(
                            context,
                            "귀가 요청 정보를 확인할 수 없습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        eventScope.launch {
                            runCatching {
                                returnHomeRealtimeManager.acceptRequest(
                                    requestId = requestId,
                                    guardianId = currentGuardianId,
                                    wearerId = currentWearerId
                                )
                            }.onSuccess {

                                onReturnHomeRequestAccepted(
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
                                Log.e("ReturnHome", "귀가 요청 수락 실패", error)
                                Toast.makeText(
                                    context,
                                    "귀가 요청 수락에 실패했습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                },

                onDismissClick = {
                    onScreenChange(AppScreen.HOME)
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


                /*
                 * -----------------------------------------
                 * 괜찮아요
                 * -----------------------------------------
                 */
                onOkayClick = {

                    val locationSnapshot =
                        watchLocation


                    /*
                     * 화면은 바로 홈으로
                     */
                    FallEventState
                        .reset()


                    onScreenChange(
                        AppScreen.HOME
                    )


                    /*
                     * 안전 확인 이벤트 저장
                     */
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


                /*
                 * -----------------------------------------
                 * 도와주세요
                 * -----------------------------------------
                 */
                onHelpClick = {

                    val locationSnapshot =
                        watchLocation


                    /*
                     * 화면은 바로 SOS 완료로 이동
                     */
                    onScreenChange(
                        AppScreen.SOS_SENT
                    )


                    eventScope.launch {


                        /*
                         * SOS 순간 GPS 강제 저장
                         */
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


                        /*
                         * 수동 SOS 이벤트
                         */
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


                /*
                 * -----------------------------------------
                 * 10초 무응답
                 * -----------------------------------------
                 */
                onTimeout = {

                    val locationSnapshot =
                        watchLocation


                    onScreenChange(
                        AppScreen.SOS_SENT
                    )


                    eventScope.launch {


                        /*
                         * 자동 SOS 순간 GPS 강제 저장
                         */
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


                        /*
                         * 자동 SOS 이벤트 저장
                         */
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
                returnHomeRequestId = returnHomeRequestId,
                returnHomeRealtimeManager = returnHomeRealtimeManager,
                guardianId = guardianId,
                wearerId = wearerId,
                homeLatitude = homeLatitude,
                homeLongitude = homeLongitude
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
            context.getSystemService(
                Context.VIBRATOR_SERVICE
            ) as android.os.Vibrator
        }

    LaunchedEffect(Unit) {
        val pattern =
            longArrayOf(0, 350, 150, 350, 150, 600)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(pattern, -1)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1976D2))
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = "귀가 요청",
            modifier = Modifier.size(36.dp),
            tint = Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "보호자가 귀가를\n요청했어요",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onGoHomeClick,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.White,
                contentColor = Color(0xFF1976D2)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "집으로 가기",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "집으로 가기",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = onDismissClick,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.DarkGray,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        ) {
            Text(
                text = "나중에",
                fontSize = 12.sp
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
