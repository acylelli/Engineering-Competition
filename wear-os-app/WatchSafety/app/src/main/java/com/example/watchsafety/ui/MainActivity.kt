package com.example.watchsafety.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import androidx.compose.material.icons.filled.*
import com.example.watchsafety.health.HeartRateManager
import com.example.watchsafety.location.WatchLocationManager
import com.example.watchsafety.safety.DemoSafetyService
import com.example.watchsafety.safety.FallEventState
import com.example.watchsafety.safety.FallHealthServiceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.example.watchsafety.navigation.TmapRouteClient
import com.example.watchsafety.navigation.TmapRouteResult
enum class AppScreen { HOME, FALL_DETECTED, SOS_SENT, COMPASS, OUT_OF_SAFE_ZONE }

class MainActivity : ComponentActivity() {
    private lateinit var heartRateManager: HeartRateManager
    private lateinit var locationManager: WatchLocationManager
    private lateinit var fallManager: FallHealthServiceManager

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val myLocationState = mutableStateOf<Location?>(null)
    private val currentScreenState = mutableStateOf(AppScreen.HOME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        heartRateManager = HeartRateManager(this)
        locationManager = WatchLocationManager(this)
        fallManager = FallHealthServiceManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            val heartRate by heartRateManager.heartRate.collectAsState()
            val location by locationManager.location.collectAsState()
            val isFallDetected by FallEventState.fallDetected.collectAsState()

            LaunchedEffect(isFallDetected) {
                if (isFallDetected) {
                    currentScreenState.value = AppScreen.FALL_DETECTED
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val allGranted = permissions.values.all { it }
                if (allGranted) {
                    heartRateManager.start()
                    locationManager.start()
                    getLocation()

                    lifecycleScope.launch {
                        if (fallManager.isFallDetectionSupported()) {
                            fallManager.registerFallDetection()
                        }
                    }
                    startService(Intent(this@MainActivity, DemoSafetyService::class.java))
                } else {
                    Toast.makeText(this@MainActivity, "권한이 필요합니다!", Toast.LENGTH_SHORT).show()
                }
            }

            LaunchedEffect(Unit) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BODY_SENSORS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACTIVITY_RECOGNITION,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                )
            }

            // 테마 오류 방지를 위해 기본 MaterialTheme을 사용합니다.
            MaterialTheme {
                EmergencyManager(
                    currentScreen = currentScreenState.value,
                    onScreenChange = { newScreen -> currentScreenState.value = newScreen },
                    myLocation = myLocationState.value,
                    heartRate = heartRate?.toString()?.toFloatOrNull()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val emergencyType = intent?.getStringExtra("EMERGENCY_TYPE")
        if (emergencyType == "FALL_DETECTED") {
            currentScreenState.value = AppScreen.FALL_DETECTED
        } else if (emergencyType == "OUT_OF_SAFE_ZONE") {
            currentScreenState.value = AppScreen.OUT_OF_SAFE_ZONE
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun getLocation() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) myLocationState.value = location
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        heartRateManager.stop()
        locationManager.stop()
    }
}

@Composable
fun EmergencyManager(
    currentScreen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    myLocation: Location?,
    heartRate: Float?
) {
    val homeLocation = remember {
        Location("").apply {
            latitude = 37.6833
            longitude = 126.7722
        }
    }
    var hasTriggeredSafeZoneAlert by remember { mutableStateOf(false) }

    LaunchedEffect(myLocation) {
        if (myLocation != null) {
            val distance = myLocation.distanceTo(homeLocation)

            // 👉 이 부분의 숫자 '1000f'가 바로 안전구역 반경(1000m = 1km)입니다!
            if (distance > 1000f && !hasTriggeredSafeZoneAlert) {
                hasTriggeredSafeZoneAlert = true
                onScreenChange(AppScreen.OUT_OF_SAFE_ZONE) // 이탈 시 경고 화면 띄우기
            } else if (distance <= 1000f) {
                hasTriggeredSafeZoneAlert = false
            }
        }
    }

    when (currentScreen) {
        AppScreen.HOME -> MainScreen(
            heartRate = heartRate,
            onManualSosClick = { onScreenChange(AppScreen.SOS_SENT) },
            onGoHomeClick = { onScreenChange(AppScreen.COMPASS) }
        )
        AppScreen.FALL_DETECTED -> FallDetectScreen(
            onOkayClick = {
                FallEventState.reset()
                onScreenChange(AppScreen.HOME)
            },
            onHelpClick = { onScreenChange(AppScreen.SOS_SENT) },
            onTimeout = { onScreenChange(AppScreen.SOS_SENT) }
        )
        AppScreen.SOS_SENT -> SosSentScreen(
            onReturnHome = {
                FallEventState.reset()
                onScreenChange(AppScreen.HOME)
            }
        )
        AppScreen.COMPASS -> TmapRouteTestScreen()
        AppScreen.OUT_OF_SAFE_ZONE -> OutOfSafeZoneScreen(
            onGoHomeClick = { onScreenChange(AppScreen.COMPASS) },
            onDismissClick = { onScreenChange(AppScreen.HOME) }
        )
    }
}

@Composable
fun MainScreen(heartRate: Float?, onManualSosClick: () -> Unit, onGoHomeClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Favorite, contentDescription = "BPM", modifier = Modifier.size(20.dp), tint = Color.Red)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (heartRate != null) "${heartRate.toInt()} BPM" else "측정 중...",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onManualSosClick,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F), contentColor = Color.White),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Warning, contentDescription = "긴급 SOS", modifier = Modifier.size(20.dp))
                    Text(text = "SOS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = onGoHomeClick,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1976D2), contentColor = Color.White),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Home, contentDescription = "집으로", modifier = Modifier.size(20.dp))
                    Text(text = "집으로", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun OutOfSafeZoneScreen(onGoHomeClick: () -> Unit, onDismissClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFE64A19)).padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.LocationOff, contentDescription = "경로 이탈", modifier = Modifier.size(36.dp), tint = Color.White)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "안전구역을 벗어났습니다!", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onGoHomeClick,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White, contentColor = Color(0xFFE64A19)),
            modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Home, contentDescription = "집으로", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("집으로 안내받기", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onDismissClick,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(32.dp)
        ) {
            Text("괜찮아요 (알림 닫기)", fontSize = 12.sp)
        }
    }
}

@Composable
fun FallDetectScreen(onOkayClick: () -> Unit, onHelpClick: () -> Unit, onTimeout: () -> Unit) {
    var timeLeft by remember { mutableIntStateOf(10) }
    LaunchedEffect(key1 = timeLeft) {
        if (timeLeft > 0) { delay(1000L); timeLeft-- } else { onTimeout() }
    }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFF9800)).padding(horizontal = 16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "${timeLeft}초 후 구조 요청", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "괜찮으신가요?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onOkayClick, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50), contentColor = Color.White), modifier = Modifier.fillMaxWidth().height(40.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Check, contentDescription = "괜찮아요", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "괜찮아요", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = onHelpClick, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F), contentColor = Color.White), modifier = Modifier.fillMaxWidth().height(40.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Warning, contentDescription = "도와주세요", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "도와주세요!", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SosSentScreen(onReturnHome: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFD32F2F)).padding(horizontal = 16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Call, contentDescription = "전송 완료", modifier = Modifier.size(40.dp), tint = Color.White)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "보호자에게 구조를\n요청했습니다.", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onReturnHome, colors = ButtonDefaults.buttonColors(backgroundColor = Color.White, contentColor = Color.Black), modifier = Modifier.height(32.dp)) {
            Text("확인", fontSize = 12.sp)
        }
    }
}

@Composable
fun CompassScreen(myLocation: Location?, homeLocation: Location, onCloseClick: () -> Unit) {
    val context = LocalContext.current
    var routeResult by remember { mutableStateOf<TmapRouteResult?>(null) }
    var isRouting by remember { mutableStateOf(false) }

    // T맵 클라이언트 준비
    val tmapClient = remember { TmapRouteClient() }

    // 내 위치가 잡히면 자동으로 T맵 경로를 탐색합니다.
    LaunchedEffect(myLocation) {
        if (myLocation != null && routeResult == null && !isRouting) {
            isRouting = true
            try {
                routeResult = tmapClient.getPedestrianRoute(
                    startLongitude = myLocation.longitude,
                    startLatitude = myLocation.latitude,
                    endLongitude = homeLocation.longitude,
                    endLatitude = homeLocation.latitude
                )
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "경로 탐색 실패", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                isRouting = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1976D2)).padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "T맵 도보 길안내", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (myLocation == null) {
            Text(text = "GPS 위치 탐색중...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        } else if (isRouting) {
            Text(text = "경로 탐색 중...", color = Color.Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        } else if (routeResult != null) {
            val result = routeResult!!
            Text(text = "남은 거리: ${result.totalDistanceMeters}m", color = Color.Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "예상 시간: ${result.totalTimeSeconds / 60}분", color = Color.White, fontSize = 12.sp)

            // 다음 턴(방향 전환) 안내 메시지 표시
            val nextStep = result.steps.firstOrNull { it.description.isNotBlank() }
            if (nextStep != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "▶ ${nextStep.description}",
                    color = Color.White,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(text = "경로를 찾을 수 없습니다.", color = Color.White, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onCloseClick,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White, contentColor = Color(0xFF1976D2)),
            modifier = Modifier.height(28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Close, contentDescription = "안내 종료", modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("안내 종료", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}