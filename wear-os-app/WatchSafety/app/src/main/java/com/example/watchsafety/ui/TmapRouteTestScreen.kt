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
            "한성대학교까지 경로 검색 중..."

        try {

            routeResult =
                routeClient
                    .getPedestrianRoute(

                        startLongitude =
                            location.longitude,

                        startLatitude =
                            location.latitude
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

    Column(

        modifier =
            Modifier.fillMaxSize(),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        BasicText(
            text =
                "한성대학교 길찾기",
            style =
                titleStyle
        )

        BasicText(
            text =
                statusText,
            style =
                textStyle
        )

        val location =
            currentLocation

        if (
            location != null
        ) {

            BasicText(
                text =
                    "현재 위치",
                style =
                    textStyle
            )

            BasicText(
                text =
                    "위도: %.5f"
                        .format(
                            location.latitude
                        ),
                style =
                    textStyle
            )

            BasicText(
                text =
                    "경도: %.5f"
                        .format(
                            location.longitude
                        ),
                style =
                    textStyle
            )

            BasicText(
                text =
                    "GPS 정확도: %.0fm"
                        .format(
                            location
                                .accuracyMeters
                        ),
                style =
                    textStyle
            )
        }

        val route =
            routeResult

        if (
            route != null
        ) {

            BasicText(
                text = "",
                style =
                    textStyle
            )

            BasicText(
                text =
                    "총 거리: " +
                            formatDistance(
                                route
                                    .totalDistanceMeters
                            ),
                style =
                    textStyle
            )

            BasicText(
                text =
                    "예상 시간: " +
                            formatTime(
                                route
                                    .totalTimeSeconds
                            ),
                style =
                    textStyle
            )

            BasicText(
                text =
                    "안내 지점: " +
                            route.steps.size +
                            "개",
                style =
                    textStyle
            )

            /*
             * turnType 200은 출발점이라 제외
             */
            val firstGuide =
                route.steps
                    .firstOrNull {

                        it.turnType != 200
                    }

            if (
                firstGuide != null
            ) {

                BasicText(
                    text = "",
                    style =
                        textStyle
                )

                BasicText(
                    text =
                        "첫 안내",
                    style =
                        textStyle
                )

                BasicText(
                    text =
                        turnTypeToText(
                            firstGuide.turnType
                        ),
                    style =
                        titleStyle
                )

                BasicText(
                    text =
                        firstGuide.description,
                    style =
                        textStyle
                )
            }
        }
    }
}

private fun formatDistance(
    meters: Int
): String {

    return if (
        meters >= 1000
    ) {

        "%.1f km".format(
            meters / 1000.0
        )

    } else {

        "${meters}m"
    }
}

private fun formatTime(
    seconds: Int
): String {

    val minutes =
        seconds / 60

    return "${minutes}분"
}

private fun turnTypeToText(
    turnType: Int
): String {

    return when (
        turnType
    ) {

        11 ->
            "↑ 직진"

        12 ->
            "← 좌회전"

        13 ->
            "→ 우회전"

        14 ->
            "↶ 유턴"

        16,
        17 ->
            "↙ 왼쪽 방향"

        18,
        19 ->
            "↗ 오른쪽 방향"

        125 ->
            "육교"

        126 ->
            "지하보도"

        127 ->
            "계단"

        128 ->
            "경사로"

        129 ->
            "계단 또는 경사로"

        211 ->
            "횡단보도"

        212 ->
            "← 좌측 횡단보도"

        213 ->
            "→ 우측 횡단보도"

        214,
        215 ->
            "↙ 왼쪽 횡단보도"

        216,
        217 ->
            "↗ 오른쪽 횡단보도"

        218 ->
            "엘리베이터"

        201 ->
            "🏠 목적지 도착"

        else ->
            "경로 안내"
    }
}