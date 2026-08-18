package com.example.watchsafety.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.watchsafety.location.WatchLocationManager
import com.example.watchsafety.navigation.TmapRouteClient
import com.example.watchsafety.navigation.TmapRouteResult
import androidx.wear.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun TmapRouteTestScreen() {

    val context =
        LocalContext.current

    val locationManager =
        remember {

            WatchLocationManager(
                context.applicationContext
            )
        }

    val routeClient =
        remember {
            TmapRouteClient()
        }

    val currentLocation by
    locationManager
        .location
        .collectAsState()

    var permissionGranted by
    remember {

        mutableStateOf(

            ContextCompat
                .checkSelfPermission(
                    context,
                    Manifest.permission
                        .ACCESS_FINE_LOCATION
                ) ==
                    PackageManager
                        .PERMISSION_GRANTED
        )
    }

    var routeResult by
    remember {

        mutableStateOf<
                TmapRouteResult?
                >(null)
    }

    var routeRequested by
    remember {

        mutableStateOf(
            false
        )
    }

    var statusText by
    remember {

        mutableStateOf(
            "GPS 위치 확인 중..."
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .RequestPermission()
        ) { granted ->

            permissionGranted =
                granted

            statusText =

                if (granted) {

                    "GPS 위치 확인 중..."

                } else {

                    "위치 권한이 필요합니다."
                }
        }

    /*
     * 위치 권한 확인
     */
    LaunchedEffect(Unit) {

        if (
            !permissionGranted
        ) {

            statusText =
                "위치 권한 요청 중..."

            permissionLauncher.launch(
                Manifest.permission
                    .ACCESS_FINE_LOCATION
            )
        }
    }

    /*
     * 권한이 생기면 GPS 시작
     */
    LaunchedEffect(
        permissionGranted
    ) {

        if (
            permissionGranted
        ) {

            locationManager.start()

            statusText =
                "현재 위치를 찾고 있습니다..."
        }
    }

    /*
     * 처음 GPS가 들어온 순간
     * TMAP 경로 API 1회 호출
     */
    LaunchedEffect(
        currentLocation
    ) {

        val location =
            currentLocation
                ?: return@LaunchedEffect

        if (
            routeRequested
        ) {
            return@LaunchedEffect
        }

        routeRequested =
            true

        statusText =
            "집까지 경로 검색 중..."

        try {

            routeResult =
                routeClient
                    .getPedestrianRoute(

                        startLongitude =
                            location.longitude,

                        startLatitude =
                            location.latitude,
                        endLongitude = AppConfig.DEST_LON,
                        endLatitude = AppConfig.DEST_LAT
                    )

            statusText =
                "경로 검색 완료"

        } catch (
            e: Exception
        ) {

            statusText =
                "경로 검색 실패\n" +
                        (
                                e.message
                                    ?: "알 수 없는 오류"
                                )
        }
    }

    DisposableEffect(Unit) {

        onDispose {

            locationManager.stop()
        }
    }

    val titleStyle =
        TextStyle(
            color = Color.White,
            fontSize = 16.sp
        )

    val textStyle =
        TextStyle(
            color = Color.White,
            fontSize = 12.sp
        )

    // 여기서부터 복사해서 기존 화면 코드와 교체하세요!
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)) // 깔끔한 다크 배경
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val route = routeResult

        if (route == null) {
            // 경로를 불러오기 전 로딩 화면
            BasicText(
                text = statusText,
                style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
        } else {
            // 남은 거리와 시간 강조
            // 🚨 1. 실시간 GPS 위치로 목적지까지의 거리를 직접 계산합니다!
            val realTimeDistance = currentLocation?.let { current ->
                val results = FloatArray(1)
                android.location.Location.distanceBetween(
                    current.latitude,
                    current.longitude,
                    AppConfig.DEST_LAT,
                    AppConfig.DEST_LON,
                    results
                )
                results[0].toInt()
            } ?: route.totalDistanceMeters

            // 🚨 2. 실시간으로 줄어드는 거리를 화면에 띄웁니다!
            BasicText(
                text = "${realTimeDistance}m 남음",
                style = TextStyle(color = Color(0xFF4CAF50), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            )
            BasicText(
                text = "예상 소요시간: ${route.totalTimeSeconds / 60}분",
                style = TextStyle(color = Color.LightGray, fontSize = 12.sp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 첫 번째 안내 지점 찾기 (turnType 200: 출발점 제외)
            val firstGuide = route.steps.firstOrNull { it.turnType != 200 }

            if (firstGuide != null) {
                // TMAP 방향 코드(turnType)에 따라 화살표 각도 계산
                val rotationDegree = when (firstGuide.turnType) {
                    11 -> 0f      // 직진
                    12 -> -90f    // 좌회전
                    13 -> 90f     // 우회전
                    14 -> 180f    // 유턴
                    16, 17, 214, 215 -> -45f // 약간 왼쪽 (11시 방향)
                    18, 19, 216, 217 -> 45f  // 약간 오른쪽 (1시 방향)
                    else -> 0f    // 기본 직진
                }

                // 휙휙 돌아가는 큼지막한 노란색 화살표
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "방향 화살표",
                    tint = Color(0xFFFFEB3B),
                    modifier = Modifier
                        .size(60.dp)
                        .rotate(rotationDegree)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 안내 메시지 (예: "50m 앞 우회전")
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
                // 안내가 끝났을 때
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "도착",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                BasicText(
                    text = "목적지 부근입니다",
                    style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}