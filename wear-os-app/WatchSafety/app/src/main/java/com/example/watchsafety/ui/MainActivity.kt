package com.example.watchsafety.ui

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
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

import com.example.watchsafety.health.HeartRateManager
import com.example.watchsafety.location.WatchLocationManager
import com.example.watchsafety.navigation.TmapRouteClient
import com.example.watchsafety.navigation.TmapRouteResult
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

    /*
     * 보호자 연결 코드 화면
     */
    PAIRING,

    /*
     * 보호자 연결 성공 화면
     */
    PAIRING_SUCCESS
}


/*
 * =========================================================
 * 앱 설정
 * =========================================================
 */

object AppConfig {

    /*
     * 현재 테스트용 목적지
     * 한성대학교 근처
     */
    const val DEST_LAT = 37.5884

    const val DEST_LON = 127.0062
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


    private lateinit var fusedLocationClient:
            FusedLocationProviderClient


    private val myLocationState =
        mutableStateOf<Location?>(null)


    private val currentScreenState =
        mutableStateOf(
            AppScreen.HOME
        )


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        /*
         * -------------------------------------------------
         * 잠금 화면에서도 긴급 화면 표시
         * -------------------------------------------------
         */

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O_MR1
        ) {

            setShowWhenLocked(true)

            setTurnScreenOn(true)

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
         * -------------------------------------------------
         * Manager 초기화
         * -------------------------------------------------
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


        fusedLocationClient =
            LocationServices
                .getFusedLocationProviderClient(
                    this
                )


        /*
         * 알림 등을 통해 앱이 실행되었을 때 처리
         */
        handleIntent(
            intent
        )


        /*
         * -------------------------------------------------
         * Compose
         * -------------------------------------------------
         */

        setContent {

            val heartRate by
            heartRateManager
                .heartRate
                .collectAsState()


            val location by
            locationManager
                .location
                .collectAsState()


            val isFallDetected by
            FallEventState
                .fallDetected
                .collectAsState()


            /*
             * -------------------------------------------------
             * 낙상 발생 감시
             * -------------------------------------------------
             */

            LaunchedEffect(
                isFallDetected
            ) {

                if (
                    isFallDetected
                ) {

                    currentScreenState.value =
                        AppScreen.FALL_DETECTED
                }
            }


            /*
             * -------------------------------------------------
             * 권한 요청
             * -------------------------------------------------
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
                         * 위치
                         */
                        locationManager
                            .start()


                        /*
                         * 현재 위치 1회 조회
                         */
                        getLocation()


                        /*
                         * 낙상 감지 등록
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
                         * 데모 안전 서비스
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
             * 앱 시작 시 권한 요청
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
             * -------------------------------------------------
             * UI
             * -------------------------------------------------
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

                    myLocation =
                        myLocationState.value,

                    heartRate =
                        heartRate
                            ?.toString()
                            ?.toFloatOrNull()
                )
            }
        }
    }


    /*
     * =====================================================
     * 알림으로 앱이 이미 실행 중일 때
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
                    "EMERGENCY_TYPE"
                )


        when (
            emergencyType
        ) {

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
     * 현재 GPS 위치
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
     * 종료
     * =====================================================
     */

    override fun onDestroy() {

        super.onDestroy()

        heartRateManager
            .stop()

        locationManager
            .stop()
    }
}


/*
 * =========================================================
 *
 * 화면 관리자
 *
 * =========================================================
 */

@Composable
fun EmergencyManager(

    currentScreen: AppScreen,

    onScreenChange:
        (AppScreen) -> Unit,

    myLocation:
    Location?,

    heartRate:
    Float?

) {

    val context =
        LocalContext.current


    /*
     * -----------------------------------------------------
     * 진동
     * -----------------------------------------------------
     */

    val vibrator =
        remember {

            context
                .getSystemService(
                    Context.VIBRATOR_SERVICE
                ) as android.os.Vibrator
        }


    /*
     * -----------------------------------------------------
     * 보호자 연결 Manager
     * -----------------------------------------------------
     */

    val pairingManager =
        remember {

            PairingManager()
        }


    /*
     * 연결 성공 여부
     *
     * 현재는 앱 실행 중 연결 성공 시 true.
     * 이후 Supabase에서 앱 시작 시 연결 상태도
     * 조회하도록 개선 예정.
     */
    var guardianConnected by
    remember {

        mutableStateOf(
            false
        )
    }


    /*
     * -----------------------------------------------------
     * 집 위치
     * -----------------------------------------------------
     */

    val homeLocation =
        remember {

            Location(
                ""
            ).apply {

                latitude =
                    AppConfig.DEST_LAT

                longitude =
                    AppConfig.DEST_LON
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
        myLocation
    ) {

        if (
            myLocation != null
        ) {

            val distance =
                myLocation
                    .distanceTo(
                        homeLocation
                    )


            /*
             * 테스트 기준
             * 집에서 1km 이상 벗어나면 경고
             */
            if (
                distance > 1000f &&
                !hasTriggeredSafeZoneAlert
            ) {

                hasTriggeredSafeZoneAlert =
                    true

                onScreenChange(
                    AppScreen.OUT_OF_SAFE_ZONE
                )

            } else if (
                distance <= 1000f
            ) {

                hasTriggeredSafeZoneAlert =
                    false
            }
        }
    }


    /*
     * =====================================================
     * 심박수 이상 감시
     * =====================================================
     */
/*
var hasTriggeredHeartRateAlert by
remember {

    mutableStateOf(
        false
    )
}


LaunchedEffect(
    heartRate
) {

    if (
        heartRate != null
    ) {

        /*
         * 테스트용 기준
         *
         * 50 미만
         * 또는
         * 90 초과
         */
        if (
            (
                    heartRate < 50f ||
                            heartRate > 90f
                    ) &&
            !hasTriggeredHeartRateAlert
        ) {

            hasTriggeredHeartRateAlert =
                true


            /*
             * 진동
             */
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                vibrator.vibrate(

                    VibrationEffect
                        .createOneShot(
                            1000,
                            VibrationEffect
                                .DEFAULT_AMPLITUDE
                        )
                )

            } else {

                @Suppress("DEPRECATION")

                vibrator.vibrate(
                    1000
                )
            }


            /*
             * 현재는 낙상 확인 화면 사용
             */
            onScreenChange(
                AppScreen.FALL_DETECTED
            )

        } else if (
            heartRate in 50f..90f
        ) {

            hasTriggeredHeartRateAlert =
                false
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
     * -------------------------------------------------
     * 홈
     *
     * 여기서 기존 MainScreen이 아니라
     * 새 HomeScreen.kt를 사용
     * -------------------------------------------------
     */

    AppScreen.HOME -> {

        HomeScreen(

            guardianConnected =
                guardianConnected,

            onGoHomeClick = {

                onScreenChange(
                    AppScreen.COMPASS
                )
            },

            onGuardianConnectClick = {

                onScreenChange(
                    AppScreen.PAIRING
                )
            }
        )
    }


    /*
     * -------------------------------------------------
     * 보호자 연결
     * -------------------------------------------------
     */

    AppScreen.PAIRING -> {

        PairingScreen(

            pairingManager =
                pairingManager,

            onConnected = {

                guardianConnected =
                    true

                onScreenChange(
                    AppScreen.PAIRING_SUCCESS
                )
            }
        )
    }


    /*
     * -------------------------------------------------
     * 연결 완료
     * -------------------------------------------------
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
     * -------------------------------------------------
     * 낙상 감지
     * -------------------------------------------------
     */

    AppScreen.FALL_DETECTED -> {

        FallDetectScreen(

            onOkayClick = {

                FallEventState
                    .reset()

                onScreenChange(
                    AppScreen.HOME
                )
            },

            onHelpClick = {

                onScreenChange(
                    AppScreen.SOS_SENT
                )
            },

            onTimeout = {

                onScreenChange(
                    AppScreen.SOS_SENT
                )
            }
        )
    }


    /*
     * -------------------------------------------------
     * SOS 전송
     * -------------------------------------------------
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
     * -------------------------------------------------
     * 집으로 가기
     * -------------------------------------------------
     */

    AppScreen.COMPASS -> {

        TmapRouteTestScreen()
    }


    /*
     * -------------------------------------------------
     * 안전구역 이탈
     * -------------------------------------------------
     */

    AppScreen.OUT_OF_SAFE_ZONE -> {

        OutOfSafeZoneScreen(

            onGoHomeClick = {

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
     * -------------------------------------------------
     * 복약 알림
     * -------------------------------------------------
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
*
* 안전구역 이탈 화면
*
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

    modifier = Modifier
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

        modifier = Modifier
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

        modifier = Modifier
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
*
* 낙상 감지 화면
*
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

    modifier = Modifier
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

        modifier = Modifier
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

        modifier = Modifier
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
*
* SOS 전송 완료
*
* =========================================================
*/

@Composable
fun SosSentScreen(

onReturnHome:
    () -> Unit

) {

Column(

    modifier = Modifier
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
*
* 기존 Compass 화면
*
* 현재 AppScreen.COMPASS에서는 TmapRouteTestScreen을 사용하지만
* 필요해서 기존 코드는 유지.
*
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

    mutableStateOf<
            TmapRouteResult?
            >(
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

    modifier = Modifier
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
*
* 복약 알림 화면
*
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


/*
 * 화면 표시 직후 진동
 */
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

    modifier = Modifier
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


    /*
     * 먹었어요
     */

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

        modifier = Modifier
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


    /*
     * 10분 뒤
     */

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

        modifier = Modifier
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