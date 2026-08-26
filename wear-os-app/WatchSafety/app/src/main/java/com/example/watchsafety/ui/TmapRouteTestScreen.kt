package com.example.watchsafety.ui

import android.util.Log

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.wear.compose.material.Icon

import com.example.watchsafety.data.ReturnHomeRealtimeManager
import com.example.watchsafety.location.WatchLocation
import com.example.watchsafety.navigation.TmapRouteClient
import com.example.watchsafety.navigation.TmapRouteResult


@Composable
fun TmapRouteTestScreen(

    returnHomeRequestId: String?,

    returnHomeRealtimeManager: ReturnHomeRealtimeManager,

    guardianId: String?,

    wearerId: String?,

    homeLatitude: Double?,

    homeLongitude: Double?,

    homeRadiusMeters: Double?,

    /*
     * MainActivity의 WatchLocationManager가 이미 받고 있는
     * 실제 GPS 위치를 그대로 사용한다.
     *
     * 화면 내부에서 별도의 WatchLocationManager를 만들지 않는다.
     */
    watchLocation: WatchLocation?,

    onReturnHomeCompleted: (String) -> Unit,
) {

    val routeClient =
        remember {

            TmapRouteClient()
        }


    var routeResult by
    remember {

        mutableStateOf<TmapRouteResult?>(
            null
        )
    }


    /*
     * 한 화면 진입 동안 같은 경로를
     * 반복해서 요청하지 않기 위한 상태.
     */
    var routeRequested by
    remember {

        mutableStateOf(
            false
        )
    }


    /*
     * ACCEPTED → NAVIGATING 반영 여부
     */
    var navigatingUpdated by
    remember {

        mutableStateOf(
            false
        )
    }


    /*
     * HOME 반경 진입 후
     * COMPLETED 중복 전송 방지.
     */
    var completedUpdated by
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


    /*
     * =====================================================
     * 화면 진입 상태 로그
     * =====================================================
     */
    LaunchedEffect(
        returnHomeRequestId,
        guardianId,
        wearerId,
        homeLatitude,
        homeLongitude,
        homeRadiusMeters
    ) {

        Log.d(
            TAG,
            "TmapRouteTestScreen 진입 " +
                    "requestId=$returnHomeRequestId " +
                    "guardianId=$guardianId " +
                    "wearerId=$wearerId"
        )


        Log.d(
            TAG,
            "HOME 정보 " +
                    "lat=$homeLatitude " +
                    "lng=$homeLongitude " +
                    "radius=$homeRadiusMeters"
        )
    }


    /*
     * =====================================================
     * GPS 상태 로그
     * =====================================================
     */
    LaunchedEffect(
        watchLocation
    ) {

        if (
            watchLocation == null
        ) {

            statusText =
                "현재 위치를 찾고 있습니다..."


            Log.d(
                TAG,
                "MainActivity GPS 아직 없음"
            )

        } else {

            Log.d(
                TAG,
                "MainActivity GPS 수신 " +
                        "lat=${watchLocation.latitude} " +
                        "lng=${watchLocation.longitude}"
            )
        }
    }


    /*
     * =====================================================
     * 집까지 경로 검색
     *
     * MainActivity가 이미 받고 있는 watchLocation을 사용한다.
     *
     * 경로 검색 성공 후:
     * ACCEPTED → NAVIGATING
     * =====================================================
     */
    LaunchedEffect(
        watchLocation,
        homeLatitude,
        homeLongitude,
        returnHomeRequestId
    ) {

        val location =
            watchLocation


        if (
            location == null
        ) {

            statusText =
                "현재 위치를 찾고 있습니다..."


            Log.d(
                TAG,
                "경로 검색 대기: GPS 위치 없음"
            )


            return@LaunchedEffect
        }


        val destinationLatitude =
            homeLatitude


        val destinationLongitude =
            homeLongitude


        if (
            destinationLatitude == null ||
            destinationLongitude == null
        ) {

            statusText =
                "집 안전구역이 등록되지 않았습니다."


            Log.e(
                TAG,
                "경로 검색 중단: HOME 좌표 없음 " +
                        "lat=$homeLatitude lng=$homeLongitude"
            )


            return@LaunchedEffect
        }


        if (
            routeRequested
        ) {

            return@LaunchedEffect
        }


        routeRequested =
            true


        statusText =
            "집까지 경로 검색 중..."


        Log.d(
            TAG,
            "TMAP 경로 검색 시작 " +
                    "startLat=${location.latitude} " +
                    "startLng=${location.longitude} " +
                    "endLat=$destinationLatitude " +
                    "endLng=$destinationLongitude"
        )


        try {

            val result =
                routeClient
                    .getPedestrianRoute(

                        startLongitude =
                            location.longitude,

                        startLatitude =
                            location.latitude,

                        endLongitude =
                            destinationLongitude,

                        endLatitude =
                            destinationLatitude
                    )


            routeResult =
                result


            statusText =
                "경로 검색 완료"


            Log.d(
                TAG,
                "TMAP 경로 검색 성공 " +
                        "distance=${result.totalDistanceMeters}m " +
                        "time=${result.totalTimeSeconds}s"
            )


            val requestId =
                returnHomeRequestId


            val currentGuardianId =
                guardianId


            val currentWearerId =
                wearerId


            /*
             * 보호자가 보낸 귀가 요청으로 시작된 경우에만
             * DB 상태를 NAVIGATING으로 변경한다.
             *
             * 워치 홈 화면에서 직접 집으로 가기를 누른 경우에는
             * requestId가 null이므로 길안내만 사용한다.
             */
            if (
                requestId != null &&
                currentGuardianId != null &&
                currentWearerId != null &&
                !navigatingUpdated
            ) {

                Log.d(
                    RETURN_HOME_TAG,
                    "NAVIGATING 변경 요청: $requestId"
                )


                try {

                    returnHomeRealtimeManager
                        .startNavigation(

                            requestId =
                                requestId,

                            guardianId =
                                currentGuardianId,

                            wearerId =
                                currentWearerId
                        )


                    navigatingUpdated =
                        true


                    Log.d(
                        RETURN_HOME_TAG,
                        "NAVIGATING 변경 성공: $requestId"
                    )

                } catch (
                    error: Exception
                ) {

                    Log.e(
                        RETURN_HOME_TAG,
                        "NAVIGATING 변경 실패: ${error.message}",
                        error
                    )


                    statusText =
                        "길안내 상태 저장 실패"
                }

            } else {

                Log.d(
                    RETURN_HOME_TAG,
                    "NAVIGATING 변경 생략 " +
                            "requestId=$requestId " +
                            "guardianId=$currentGuardianId " +
                            "wearerId=$currentWearerId"
                )
            }

        } catch (
            error: Exception
        ) {

            Log.e(
                TAG,
                "TMAP 경로 검색 실패: ${error.message}",
                error
            )


            /*
             * 다음 GPS 업데이트에서 재시도할 수 있도록 한다.
             */
            routeRequested =
                false


            statusText =
                "경로 검색 실패\n" +
                        (
                                error.message
                                    ?: "알 수 없는 오류"
                                )
        }
    }


    /*
     * =====================================================
     * HOME 안전구역 도착 감지
     *
     * NAVIGATING이 된 이후
     * 현재 GPS가 HOME 중심 반경 안으로 들어오면:
     *
     * NAVIGATING → COMPLETED
     * =====================================================
     */
    LaunchedEffect(
        watchLocation,
        homeLatitude,
        homeLongitude,
        homeRadiusMeters,
        navigatingUpdated,
        completedUpdated
    ) {

        if (
            !navigatingUpdated
        ) {

            return@LaunchedEffect
        }


        if (
            completedUpdated
        ) {

            return@LaunchedEffect
        }


        val location =
            watchLocation
                ?: return@LaunchedEffect


        val destinationLatitude =
            homeLatitude
                ?: return@LaunchedEffect


        val destinationLongitude =
            homeLongitude
                ?: return@LaunchedEffect


        val radiusMeters =
            homeRadiusMeters
                ?: return@LaunchedEffect


        if (
            radiusMeters <= 0.0
        ) {

            Log.e(
                RETURN_HOME_TAG,
                "HOME 반경 값 오류: $radiusMeters"
            )


            return@LaunchedEffect
        }


        val distanceResult =
            FloatArray(
                1
            )


        android.location.Location
            .distanceBetween(

                location.latitude,
                location.longitude,

                destinationLatitude,
                destinationLongitude,

                distanceResult
            )


        val distanceMeters =
            distanceResult[0]
                .toDouble()


        Log.d(
            RETURN_HOME_TAG,
            "HOME 거리=${distanceMeters.toInt()}m / " +
                    "반경=${radiusMeters.toInt()}m"
        )


        /*
         * 아직 HOME 안전구역 밖
         */
        if (
            distanceMeters >
            radiusMeters
        ) {

            return@LaunchedEffect
        }


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

            Log.e(
                RETURN_HOME_TAG,
                "COMPLETED 처리 정보 부족 " +
                        "requestId=$requestId " +
                        "guardianId=$currentGuardianId " +
                        "wearerId=$currentWearerId"
            )


            return@LaunchedEffect
        }


        /*
         * GPS가 연속으로 들어오는 동안
         * 같은 COMPLETED RPC를 동시에 여러 번 호출하지 않는다.
         */
        completedUpdated =
            true


        statusText =
            "집에 도착했습니다."


        Log.d(
            RETURN_HOME_TAG,
            "HOME 반경 진입 → COMPLETED 변경 요청: $requestId"
        )


        try {

            returnHomeRealtimeManager
                .completeRequest(

                    requestId =
                        requestId,

                    guardianId =
                        currentGuardianId,

                    wearerId =
                        currentWearerId
                )


            Log.d(
                RETURN_HOME_TAG,
                "HOME 도착 → COMPLETED 성공: $requestId"
            )


            onReturnHomeCompleted(
                requestId
            )

        } catch (
            error: Exception
        ) {

            completedUpdated =
                false


            Log.e(
                RETURN_HOME_TAG,
                "COMPLETED 변경 실패: ${error.message}",
                error
            )


            statusText =
                "귀가 완료 상태 저장 실패"
        }
    }


    /*
     * =====================================================
     * UI
     * =====================================================
     */
    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(
                        0xFF1E1E1E
                    )
                )
                .padding(
                    8.dp
                ),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        val route =
            routeResult


        if (
            route == null
        ) {

            BasicText(

                text =
                    statusText,

                style =
                    TextStyle(

                        color =
                            Color.White,

                        fontSize =
                            14.sp,

                        fontWeight =
                            FontWeight.Bold,

                        textAlign =
                            TextAlign.Center
                    )
            )

        } else {


            val realTimeDistance =

                watchLocation
                    ?.let { current ->

                        if (
                            homeLatitude != null &&
                            homeLongitude != null
                        ) {

                            val results =
                                FloatArray(
                                    1
                                )


                            android.location.Location
                                .distanceBetween(

                                    current.latitude,
                                    current.longitude,

                                    homeLatitude,
                                    homeLongitude,

                                    results
                                )


                            results[0]
                                .toInt()

                        } else {

                            route.totalDistanceMeters
                        }

                    }
                    ?: route.totalDistanceMeters


            if (
                completedUpdated
            ) {

                BasicText(

                    text =
                        "집에 도착했습니다",

                    style =
                        TextStyle(

                            color =
                                Color(
                                    0xFF4CAF50
                                ),

                            fontSize =
                                20.sp,

                            fontWeight =
                                FontWeight.Bold,

                            textAlign =
                                TextAlign.Center
                        )
                )

            } else {

                BasicText(

                    text =
                        "${realTimeDistance}m 남음",

                    style =
                        TextStyle(

                            color =
                                Color(
                                    0xFF4CAF50
                                ),

                            fontSize =
                                22.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                )
            }


            BasicText(

                text =
                    "예상 소요시간: " +
                            "${route.totalTimeSeconds / 60}분",

                style =
                    TextStyle(

                        color =
                            Color.LightGray,

                        fontSize =
                            12.sp
                    )
            )


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            val firstGuide =
                route
                    .steps
                    .firstOrNull {

                        it.turnType !=
                                200
                    }


            if (
                firstGuide != null &&
                !completedUpdated
            ) {

                val rotationDegree =

                    when (
                        firstGuide.turnType
                    ) {

                        11 ->
                            0f

                        12 ->
                            -90f

                        13 ->
                            90f

                        14 ->
                            180f

                        16,
                        17,
                        214,
                        215 ->
                            -45f

                        18,
                        19,
                        216,
                        217 ->
                            45f

                        else ->
                            0f
                    }


                Icon(

                    imageVector =
                        Icons.Default.ArrowUpward,

                    contentDescription =
                        "방향 화살표",

                    tint =
                        Color(
                            0xFFFFEB3B
                        ),

                    modifier =
                        Modifier
                            .size(
                                60.dp
                            )
                            .rotate(
                                rotationDegree
                            )
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )


                BasicText(

                    text =
                        firstGuide.description,

                    style =
                        TextStyle(

                            color =
                                Color.White,

                            fontSize =
                                16.sp,

                            fontWeight =
                                FontWeight.Bold,

                            textAlign =
                                TextAlign.Center
                        )
                )

            } else {

                Icon(

                    imageVector =
                        Icons.Default.ArrowUpward,

                    contentDescription =
                        "도착",

                    tint =
                        Color(
                            0xFF4CAF50
                        ),

                    modifier =
                        Modifier.size(
                            60.dp
                        )
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )


                BasicText(

                    text =
                        if (
                            completedUpdated
                        ) {

                            "안전하게 집에 도착했어요"

                        } else {

                            "목적지 부근입니다"
                        },

                    style =
                        TextStyle(

                            color =
                                Color.White,

                            fontSize =
                                16.sp,

                            fontWeight =
                                FontWeight.Bold,

                            textAlign =
                                TextAlign.Center
                        )
                )
            }
        }
    }
}


private const val TAG =
    "TmapRoute"


private const val RETURN_HOME_TAG =
    "ReturnHome"
