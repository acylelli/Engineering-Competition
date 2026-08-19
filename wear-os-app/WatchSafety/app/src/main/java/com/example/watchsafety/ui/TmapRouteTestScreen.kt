package com.example.watchsafety.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Icon
import com.example.watchsafety.data.ReturnHomeRealtimeManager
import com.example.watchsafety.location.WatchLocationManager
import com.example.watchsafety.navigation.TmapRouteClient
import com.example.watchsafety.navigation.TmapRouteResult

@Composable
fun TmapRouteTestScreen(
    returnHomeRequestId: String?,
    returnHomeRealtimeManager: ReturnHomeRealtimeManager,
    guardianId: String?,
    wearerId: String?,
    homeLatitude: Double?,
    homeLongitude: Double?
) {
    val context = LocalContext.current

    val locationManager = remember {
        WatchLocationManager(context.applicationContext)
    }
    val routeClient = remember { TmapRouteClient() }
    val currentLocation by locationManager.location.collectAsState()

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var routeResult by remember { mutableStateOf<TmapRouteResult?>(null) }
    var routeRequested by remember { mutableStateOf(false) }
    var navigatingUpdated by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("GPS 위치 확인 중...") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        statusText = if (granted) {
            "GPS 위치 확인 중..."
        } else {
            "위치 권한이 필요합니다."
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            statusText = "위치 권한 요청 중..."
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            locationManager.start()
            statusText = "현재 위치를 찾고 있습니다..."
        }
    }

    LaunchedEffect(currentLocation, homeLatitude, homeLongitude) {
        val location = currentLocation ?: return@LaunchedEffect

        val destinationLatitude = homeLatitude
        val destinationLongitude = homeLongitude

        if (destinationLatitude == null || destinationLongitude == null) {
            statusText = "집 안전구역이 등록되지 않았습니다."
            return@LaunchedEffect
        }

        if (routeRequested) return@LaunchedEffect
        routeRequested = true
        statusText = "집까지 경로 검색 중..."

        try {
            val result = routeClient.getPedestrianRoute(
                startLongitude = location.longitude,
                startLatitude = location.latitude,
                endLongitude = destinationLongitude,
                endLatitude = destinationLatitude
            )

            routeResult = result
            statusText = "경로 검색 완료"
            Log.d("TmapRoute", "TMAP 경로 검색 성공")

            val requestId = returnHomeRequestId
            val currentGuardianId = guardianId
            val currentWearerId = wearerId

            if (
                requestId != null &&
                currentGuardianId != null &&
                currentWearerId != null &&
                !navigatingUpdated
            ) {
                try {
                    returnHomeRealtimeManager.startNavigation(
                        requestId = requestId,
                        guardianId = currentGuardianId,
                        wearerId = currentWearerId
                    )
                    navigatingUpdated = true
                    Log.d("ReturnHome", "NAVIGATING 변경 성공: $requestId")
                } catch (e: Exception) {
                    Log.e("ReturnHome", "NAVIGATING 변경 실패", e)
                }
            }
        } catch (e: Exception) {
            Log.e("TmapRoute", "TMAP 경로 검색 실패", e)
            statusText = "경로 검색 실패\n${e.message ?: "알 수 없는 오류"}"
        }
    }

    DisposableEffect(Unit) {
        onDispose { locationManager.stop() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val route = routeResult

        if (route == null) {
            BasicText(
                text = statusText,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        } else {
            val realTimeDistance = currentLocation?.let { current ->
                if (homeLatitude != null && homeLongitude != null) {
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(
                        current.latitude,
                        current.longitude,
                        homeLatitude,
                        homeLongitude,
                        results
                    )
                    results[0].toInt()
                } else {
                    route.totalDistanceMeters
                }
            } ?: route.totalDistanceMeters

            BasicText(
                text = "${realTimeDistance}m 남음",
                style = TextStyle(
                    color = Color(0xFF4CAF50),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            BasicText(
                text = "예상 소요시간: ${route.totalTimeSeconds / 60}분",
                style = TextStyle(
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            val firstGuide = route.steps.firstOrNull { it.turnType != 200 }

            if (firstGuide != null) {
                val rotationDegree = when (firstGuide.turnType) {
                    11 -> 0f
                    12 -> -90f
                    13 -> 90f
                    14 -> 180f
                    16, 17, 214, 215 -> -45f
                    18, 19, 216, 217 -> 45f
                    else -> 0f
                }

                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "방향 화살표",
                    tint = Color(0xFFFFEB3B),
                    modifier = Modifier
                        .size(60.dp)
                        .rotate(rotationDegree)
                )

                Spacer(modifier = Modifier.height(12.dp))

                BasicText(
                    text = firstGuide.description,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "도착",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(60.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                BasicText(
                    text = "목적지 부근입니다",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
