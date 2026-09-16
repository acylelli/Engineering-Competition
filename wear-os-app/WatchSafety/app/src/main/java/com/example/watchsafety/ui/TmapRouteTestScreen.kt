package com.example.watchsafety.ui

import android.hardware.GeomagneticField
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.wear.compose.material.Icon

import com.example.watchsafety.data.ReturnHomeRealtimeManager
import com.example.watchsafety.location.WatchLocation
import com.example.watchsafety.navigation.NavigationStep
import com.example.watchsafety.navigation.NavigationVoiceManager
import com.example.watchsafety.navigation.TmapRouteClient
import com.example.watchsafety.navigation.TmapRouteResult
import com.example.watchsafety.navigation.WatchHeadingManager

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException


@Composable
fun TmapRouteTestScreen(

    returnHomeRequestId: String?,

    returnHomeRealtimeManager: ReturnHomeRealtimeManager,

    guardianId: String?,

    wearerId: String?,

    homeLatitude: Double?,

    homeLongitude: Double?,

    homeRadiusMeters: Double?,

    watchLocation: WatchLocation?,

    onReturnHomeCompleted: (String) -> Unit,
    onBack: () -> Unit,
    onNavigationCancelled: (String) -> Unit,
) {

    var completedRequestId by remember { mutableStateOf<String?>(null) }

    val context =
        LocalContext.current

    val exitScope = rememberCoroutineScope()
    var exitInProgress by remember { mutableStateOf(false) }


    val routeClient =
        remember {
            TmapRouteClient()
        }


    /*
     * =====================================================
     * 워치 방향 센서
     * =====================================================
     */
    val headingManager =
        remember {

            WatchHeadingManager(
                context
            )
        }


    val headingDegrees by
    headingManager
        .headingDegrees
        .collectAsState()


    DisposableEffect(
        headingManager
    ) {

        val started =
            headingManager.start()


        Log.d(
            HEADING_TAG,
            "heading manager start=$started"
        )


        onDispose {

            headingManager.stop()
        }
    }


    /*
     * =====================================================
     * 음성 안내
     * =====================================================
     */
    val voiceManager =
        remember {

            NavigationVoiceManager(
                context
            )
        }


    val voiceReady by
    voiceManager
        .isReady
        .collectAsState()


    DisposableEffect(
        voiceManager
    ) {

        onDispose {

            voiceManager.shutdown()
        }
    }


    var routeResult by
    remember {

        mutableStateOf<TmapRouteResult?>(
            null
        )
    }


    var routeRequested by
    remember {

        mutableStateOf(
            false
        )
    }


    var navigatingUpdated by
    remember {

        mutableStateOf(
            false
        )
    }


    var completedUpdated by
    remember {

        mutableStateOf(
            false
        )
    }

    val handleBack: () -> Unit = {
        if (!exitInProgress && !completedUpdated) {
            val requestId = returnHomeRequestId
            if (requestId == null) {
                onBack()
            } else {
                exitInProgress = true
                exitScope.launch {
                    try {
                        returnHomeRealtimeManager.cancelRequest(
                            requestId = requestId,
                            guardianId = requireNotNull(guardianId),
                            wearerId = requireNotNull(wearerId)
                        )
                        onNavigationCancelled(requestId)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        exitInProgress = false
                        Log.e(RETURN_HOME_TAG, "귀가 안내 취소 실패", error)
                        Toast.makeText(context, "취소하지 못했습니다. 연결 확인 후 다시 눌러주세요.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    BackHandler(onBack = handleBack)


    var statusText by
    remember {

        mutableStateOf(
            "GPS 위치 확인 중..."
        )
    }


    /*
     * 현재 보여주고 있는 TMAP 안내 Point.
     */
    var currentGuidePosition by
    remember {

        mutableStateOf(
            0
        )
    }


    /*
     * 귀가 안내 시작 음성을 한 번만 말하기 위한 상태.
     */
    var voiceStarted by
    remember(
        returnHomeRequestId
    ) {

        mutableStateOf(
            false
        )
    }


    /*
     * 다음에 말할 100m 단위 거리.
     *
     * 예:
     * 500 → 400 → 300 → 200 → 100
     */
    var nextVoiceThresholdMeters by
    remember(
        returnHomeRequestId
    ) {

        mutableStateOf<Int?>(
            null
        )
    }


    /*
     * =====================================================
     * 화면 상태 로그
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
     * GPS 상태
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
                "GPS 아직 없음"
            )

        } else {

            Log.d(
                TAG,
                "GPS 수신 " +
                        "lat=${watchLocation.latitude} " +
                        "lng=${watchLocation.longitude}"
            )
        }
    }


    /*
     * =====================================================
     * TMAP 집까지 보행자 경로 검색
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
                "HOME 좌표 없음"
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


            /*
             * TMAP 출발점 turnType=200은 실제 회전 안내가 아니므로
             * 첫 실제 안내점으로 이동한다.
             */
            currentGuidePosition =

                result
                    .steps
                    .indexOfFirst {

                        it.turnType !=
                                TMAP_START_POINT
                    }
                    .let {

                        if (
                            it >= 0
                        ) {
                            it
                        } else {
                            0
                        }
                    }


            statusText =
                "경로 검색 완료"


            Log.d(
                TAG,
                "TMAP 경로 검색 성공 " +
                        "distance=${result.totalDistanceMeters}m " +
                        "time=${result.totalTimeSeconds}s " +
                        "steps=${result.steps.size}"
            )


            /*
             * 보호자의 귀가 요청으로 시작된 경우
             * ACCEPTED → NAVIGATING
             */
            val requestId =
                returnHomeRequestId


            val currentGuardianId =
                guardianId


            val currentWearerId =
                wearerId


            if (
                requestId != null &&
                currentGuardianId != null &&
                currentWearerId != null &&
                !navigatingUpdated
            ) {

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
                        "NAVIGATING 변경 실패",
                        error
                    )


                    statusText =
                        "길안내 상태 저장 실패"
                }
            }

        } catch (
            error: Exception
        ) {

            Log.e(
                TAG,
                "TMAP 경로 검색 실패",
                error
            )


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
     * 귀가 시작 음성
     *
     * 시작할 때 현재 GPS→집 거리를 약 100m 단위로 알려준다.
     *
     * 예:
     * 563m → "집까지 약 600미터입니다."
     * =====================================================
     */
    LaunchedEffect(
        routeResult,
        voiceReady,
        watchLocation,
        homeLatitude,
        homeLongitude,
        completedUpdated
    ) {

        if (
            !voiceReady ||
            voiceStarted ||
            completedUpdated
        ) {

            return@LaunchedEffect
        }


        routeResult
            ?: return@LaunchedEffect


        val current =
            watchLocation
                ?: return@LaunchedEffect


        val destinationLatitude =
            homeLatitude
                ?: return@LaunchedEffect


        val destinationLongitude =
            homeLongitude
                ?: return@LaunchedEffect


        val remainingMeters =
            distanceBetweenMeters(

                startLatitude =
                    current.latitude,

                startLongitude =
                    current.longitude,

                endLatitude =
                    destinationLatitude,

                endLongitude =
                    destinationLongitude
            )
                .toInt()


        val roundedDistance =
            nearestHundred(
                remainingMeters
            )


        voiceManager.speak(

            "집으로 안내를 시작합니다. " +
                    "집까지 약 ${roundedDistance}미터입니다."
        )


        voiceStarted =
            true


        /*
         * 시작 안내 다음에 말할 100m 경계.
         *
         * 실제 현재 거리를 기준으로 잡아
         * 첫 안내와 중복되지 않도록 한다.
         */
        nextVoiceThresholdMeters =
            nextLowerHundred(
                remainingMeters
            )


        Log.d(
            VOICE_TAG,
            "귀가 시작 음성 " +
                    "remaining=$remainingMeters " +
                    "rounded=$roundedDistance " +
                    "next=$nextVoiceThresholdMeters"
        )
    }


    /*
     * =====================================================
     * 100m 단위 음성 안내
     *
     * 각 거리 구간은 한 번만 말한다.
     *
     * 예:
     * 500m → 400m → 300m → 200m → 100m
     *
     * GPS가 크게 건너뛰면
     * 지나간 숫자를 뒤늦게 읽지 않고 현재 위치에 맞춘
     * 가장 가까운 100m 단위를 말한다.
     * =====================================================
     */
    LaunchedEffect(
        watchLocation,
        voiceReady,
        voiceStarted,
        nextVoiceThresholdMeters,
        homeLatitude,
        homeLongitude,
        completedUpdated
    ) {

        if (
            !voiceReady ||
            !voiceStarted ||
            completedUpdated
        ) {

            return@LaunchedEffect
        }


        val threshold =
            nextVoiceThresholdMeters
                ?: return@LaunchedEffect


        val current =
            watchLocation
                ?: return@LaunchedEffect


        val destinationLatitude =
            homeLatitude
                ?: return@LaunchedEffect


        val destinationLongitude =
            homeLongitude
                ?: return@LaunchedEffect


        val remainingMeters =
            distanceBetweenMeters(

                startLatitude =
                    current.latitude,

                startLongitude =
                    current.longitude,

                endLatitude =
                    destinationLatitude,

                endLongitude =
                    destinationLongitude
            )
                .toInt()


        if (
            remainingMeters >
            threshold
        ) {

            return@LaunchedEffect
        }


        val currentHundred =
            ceilHundred(
                remainingMeters
            )


        val announceDistance =
            minOf(
                threshold,
                currentHundred
            )


        if (
            announceDistance <
            100
        ) {

            nextVoiceThresholdMeters =
                null

            return@LaunchedEffect
        }


        voiceManager.speak(
            "집까지 약 ${announceDistance}미터 남았습니다."
        )


        Log.d(
            VOICE_TAG,
            "100m 음성 안내 " +
                    "remaining=$remainingMeters " +
                    "announce=$announceDistance"
        )


        nextVoiceThresholdMeters =
            (
                    announceDistance -
                            100
                    )
                .takeIf {

                    it >=
                            100
                }
    }


    /*
     * =====================================================
     * GPS 위치에 따라 현재 안내 Step 변경
     * =====================================================
     */
    LaunchedEffect(
        watchLocation,
        routeResult,
        completedUpdated
    ) {

        if (
            completedUpdated
        ) {

            return@LaunchedEffect
        }


        val current =
            watchLocation
                ?: return@LaunchedEffect


        val route =
            routeResult
                ?: return@LaunchedEffect


        if (
            route.steps.isEmpty()
        ) {

            return@LaunchedEffect
        }


        var position =
            currentGuidePosition
                .coerceIn(
                    0,
                    route.steps.lastIndex
                )


        while (
            position <
            route.steps.lastIndex &&
            route.steps[position].turnType ==
            TMAP_START_POINT
        ) {

            position++
        }


        val searchEndPosition =
            (
                    position +
                            GUIDE_LOOK_AHEAD_COUNT
                    )
                .coerceAtMost(
                    route.steps.lastIndex
                )


        var nearestPosition =
            position


        var nearestDistance =
            Double.MAX_VALUE


        for (
        index in
        position..searchEndPosition
        ) {

            val step =
                route.steps[index]


            if (
                step.turnType ==
                TMAP_START_POINT
            ) {

                continue
            }


            val distance =
                distanceBetweenMeters(

                    startLatitude =
                        current.latitude,

                    startLongitude =
                        current.longitude,

                    endLatitude =
                        step.latitude,

                    endLongitude =
                        step.longitude
                )


            if (
                distance <
                nearestDistance
            ) {

                nearestDistance =
                    distance


                nearestPosition =
                    index
            }
        }


        if (
            nearestPosition >
            position &&
            nearestDistance <=
            GUIDE_SNAP_DISTANCE_METERS
        ) {

            position =
                nearestPosition
        }


        val currentStep =
            route.steps[position]


        val currentStepDistance =
            distanceBetweenMeters(

                startLatitude =
                    current.latitude,

                startLongitude =
                    current.longitude,

                endLatitude =
                    currentStep.latitude,

                endLongitude =
                    currentStep.longitude
            )


        Log.d(
            NAVIGATION_TAG,
            "현재안내 position=$position " +
                    "turnType=${currentStep.turnType} " +
                    "distance=${currentStepDistance.toInt()}m " +
                    "description=${currentStep.description}"
        )


        if (
            currentStepDistance <=
            GUIDE_REACHED_DISTANCE_METERS
        ) {

            val nextPosition =
                findNextGuidePosition(

                    steps =
                        route.steps,

                    currentPosition =
                        position
                )


            if (
                nextPosition !=
                position
            ) {

                Log.d(
                    NAVIGATION_TAG,
                    "다음 안내로 변경 " +
                            "$position → $nextPosition"
                )


                position =
                    nextPosition
            }
        }


        if (
            currentGuidePosition !=
            position
        ) {

            currentGuidePosition =
                position
        }
    }


    /*
     * =====================================================
     * HOME 안전구역 도착 감지
     * =====================================================
     */
    LaunchedEffect(
        watchLocation,
        homeLatitude,
        homeLongitude,
        homeRadiusMeters,
        navigatingUpdated,
        completedUpdated,
        exitInProgress
    ) {

        /*
         * 보호자 요청 없이 워치에서 직접 귀가 안내를 시작한 경우도
         * 화면상의 도착 처리는 가능하게 한다.
         *
         * DB COMPLETED 처리는 request 정보가 있을 때만 수행한다.
         */
        if (
            completedUpdated || exitInProgress
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

            return@LaunchedEffect
        }


        val distanceMeters =
            distanceBetweenMeters(

                startLatitude =
                    location.latitude,

                startLongitude =
                    location.longitude,

                endLatitude =
                    destinationLatitude,

                endLongitude =
                    destinationLongitude
            )


        Log.d(
            RETURN_HOME_TAG,
            "HOME 거리=${distanceMeters.toInt()}m / " +
                    "반경=${radiusMeters.toInt()}m"
        )


        if (
            distanceMeters >
            radiusMeters
        ) {

            return@LaunchedEffect
        }


        completedUpdated =
            true


        statusText =
            "집에 도착했습니다."


        /*
         * 도착 음성을 먼저 들려준다.
         *
         * 바로 HOME 화면으로 이동하면 TTS manager가 dispose 되면서
         * 음성이 잘릴 수 있어 짧게 유지한다.
         */
        voiceManager.speak(
            "집에 도착했습니다. 안전하게 귀가했습니다."
        )
    }

    // GPS 갱신으로 완료 저장이나 화면 전환 타이머가 취소되지 않도록 분리한다.
    LaunchedEffect(completedUpdated) {
        if (!completedUpdated) return@LaunchedEffect


        val requestId =
            returnHomeRequestId


        val currentGuardianId =
            guardianId


        val currentWearerId =
            wearerId


        if (
            requestId != null &&
            currentGuardianId != null &&
            currentWearerId != null
        ) {

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


                completedRequestId = requestId

            } catch (
                error: Exception
            ) {

                if (error is kotlinx.coroutines.CancellationException) throw error


                Log.e(
                    RETURN_HOME_TAG,
                    "COMPLETED 변경 실패",
                    error
                )


            }
        }
    }

    LaunchedEffect(completedUpdated) {
        if (!completedUpdated) return@LaunchedEffect
        delay(ARRIVAL_VOICE_HOLD_MILLIS)
        val requestId = completedRequestId
        if (requestId != null) {
            onReturnHomeCompleted(requestId)
        } else {
            onBack()
        }
    }

    /*
     * =====================================================
     * 기본 큰 화살표 화면
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


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            Box(
                modifier =
                    Modifier
                        .size(
                            46.dp
                        )
                        .clip(
                            CircleShape
                        )
                        .background(
                            Color(
                                0xFF2F5FE3
                            )
                        )
                        .clickable(enabled = !exitInProgress) {

                            handleBack()
                        },
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription =
                        "뒤로가기",
                    tint =
                        Color.White,
                    modifier =
                        Modifier.size(
                            23.dp
                        )
                )
            }

        } else {

            val realTimeDistance =
                watchLocation
                    ?.let { current ->

                        if (
                            homeLatitude != null &&
                            homeLongitude != null
                        ) {

                            distanceBetweenMeters(

                                startLatitude =
                                    current.latitude,

                                startLongitude =
                                    current.longitude,

                                endLatitude =
                                    homeLatitude,

                                endLongitude =
                                    homeLongitude
                            )
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


                BasicText(

                    text =
                        "예상 ${route.totalTimeSeconds / 60}분",

                    style =
                        TextStyle(

                            color =
                                Color.LightGray,

                            fontSize =
                                11.sp
                        )
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            val currentGuide =
                route
                    .steps
                    .getOrNull(
                        currentGuidePosition
                    )


            if (
                currentGuide != null &&
                currentGuide.turnType !=
                TMAP_START_POINT &&
                !completedUpdated
            ) {

                val guideDistance =
                    watchLocation
                        ?.let { current ->

                            distanceBetweenMeters(

                                startLatitude =
                                    current.latitude,

                                startLongitude =
                                    current.longitude,

                                endLatitude =
                                    currentGuide.latitude,

                                endLongitude =
                                    currentGuide.longitude
                            )
                                .toInt()
                        }


                val currentHeading =
                    headingDegrees


                val arrowRotation =
                    if (
                        watchLocation != null &&
                        currentHeading != null
                    ) {

                        val targetBearing =
                            bearingTo(

                                startLatitude =
                                    watchLocation.latitude,

                                startLongitude =
                                    watchLocation.longitude,

                                endLatitude =
                                    currentGuide.latitude,

                                endLongitude =
                                    currentGuide.longitude
                            )


                        val geomagneticField =
                            GeomagneticField(

                                watchLocation.latitude
                                    .toFloat(),

                                watchLocation.longitude
                                    .toFloat(),

                                0f,

                                System.currentTimeMillis()
                            )


                        val trueHeading =
                            normalize360(

                                currentHeading +
                                        geomagneticField.declination
                            )


                        val relativeBearing =
                            normalize180(

                                targetBearing -
                                        trueHeading
                            )


                        Log.d(
                            HEADING_TAG,
                            "watch=${trueHeading.toInt()}° " +
                                    "target=${targetBearing.toInt()}° " +
                                    "arrow=${relativeBearing.toInt()}°"
                        )


                        relativeBearing

                    } else {

                        0f
                    }


                Icon(

                    imageVector =
                        Icons.Default.ArrowUpward,

                    contentDescription =
                        "진행 방향",

                    tint =
                        Color(
                            0xFFFFEB3B
                        ),

                    modifier =
                        Modifier
                            .size(
                                64.dp
                            )
                            .rotate(
                                arrowRotation
                            )
                )


                if (
                    currentHeading ==
                    null
                ) {

                    BasicText(

                        text =
                            "방향 센서 확인 중...",

                        style =
                            TextStyle(

                                color =
                                    Color.Gray,

                                fontSize =
                                    9.sp,

                                textAlign =
                                    TextAlign.Center
                            )
                    )
                }


                /*
                 * 사용자가 요청한 대로
                 * "우회전 후 ...", 도로명 같은 하단 설명문은
                 * 더 이상 표시하지 않는다.
                 *
                 * 화면에는 다음 안내점까지 거리만 남긴다.
                 */
                if (
                    guideDistance != null &&
                    currentGuide.turnType !=
                    TMAP_DESTINATION_POINT
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(
                                3.dp
                            )
                    )


                    BasicText(

                        text =
                            "${guideDistance}m 후",

                        style =
                            TextStyle(

                                color =
                                    Color(
                                        0xFFFFEB3B
                                    ),

                                fontSize =
                                    13.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                textAlign =
                                    TextAlign.Center
                            )
                    )
                }

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
                            5.dp
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
                                14.sp,

                            fontWeight =
                                FontWeight.Bold,

                            textAlign =
                                TextAlign.Center
                        )
                )
            }


            /*
             * =================================================
             * 동그란 뒤로가기 버튼
             * =================================================
             */
            if (
                !completedUpdated
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )


                Box(

                    modifier =
                        Modifier
                            .size(
                                46.dp
                            )
                            .clip(
                                CircleShape
                            )
                            .background(
                                Color(
                                    0xFF2F5FE3
                                )
                            )
                            .clickable(enabled = !exitInProgress) {

                                handleBack()
                            },

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(

                        imageVector =
                            Icons.AutoMirrored.Filled.ArrowBack,

                        contentDescription =
                            "뒤로가기",

                        tint =
                            Color.White,

                        modifier =
                            Modifier.size(
                                23.dp
                            )
                    )
                }
            }
        }
    }
}


/*
 * =========================================================
 * 다음 안내 Step 찾기
 * =========================================================
 */
private fun findNextGuidePosition(

    steps: List<NavigationStep>,

    currentPosition: Int

): Int {

    if (
        steps.isEmpty()
    ) {

        return 0
    }


    if (
        currentPosition >=
        steps.lastIndex
    ) {

        return steps.lastIndex
    }


    for (
    index in
    currentPosition + 1..steps.lastIndex
    ) {

        if (
            steps[index].turnType !=
            TMAP_START_POINT
        ) {

            return index
        }
    }


    return currentPosition
}


/*
 * =========================================================
 * 두 GPS 좌표 사이 거리
 * =========================================================
 */
private fun distanceBetweenMeters(

    startLatitude: Double,

    startLongitude: Double,

    endLatitude: Double,

    endLongitude: Double

): Double {

    val result =
        FloatArray(
            1
        )


    Location.distanceBetween(

        startLatitude,
        startLongitude,

        endLatitude,
        endLongitude,

        result
    )


    return result[0]
        .toDouble()
}


/*
 * =========================================================
 * 현재 위치 → 다음 안내점 방위각
 *
 * 0°   = 북
 * 90°  = 동
 * 180° = 남
 * 270° = 서
 * =========================================================
 */
private fun bearingTo(

    startLatitude: Double,

    startLongitude: Double,

    endLatitude: Double,

    endLongitude: Double

): Float {

    val start =
        Location(
            "navigation_start"
        ).apply {

            latitude =
                startLatitude

            longitude =
                startLongitude
        }


    val end =
        Location(
            "navigation_end"
        ).apply {

            latitude =
                endLatitude

            longitude =
                endLongitude
        }


    return normalize360(

        start.bearingTo(
            end
        )
    )
}


/*
 * 가장 가까운 100m.
 *
 * 542 → 500
 * 551 → 600
 */
private fun nearestHundred(
    distanceMeters: Int
): Int {

    if (
        distanceMeters <=
        100
    ) {

        return 100
    }


    return (
            (
                    distanceMeters +
                            50
                    ) /
                    100
            ) *
            100
}


/*
 * 현재 거리보다 작은 다음 100m 경계.
 *
 * 563 → 500
 * 500 → 400
 * 399 → 300
 */
private fun nextLowerHundred(
    distanceMeters: Int
): Int? {

    if (
        distanceMeters <=
        100
    ) {

        return null
    }


    val threshold =
        if (
            distanceMeters %
            100 ==
            0
        ) {

            distanceMeters -
                    100

        } else {

            (
                    distanceMeters /
                            100
                    ) *
                    100
        }


    return threshold
        .takeIf {

            it >=
                    100
        }
}


/*
 * 현재 거리보다 크거나 같은 가장 가까운 100m.
 *
 * 278 → 300
 * 399 → 400
 */
private fun ceilHundred(
    distanceMeters: Int
): Int {

    if (
        distanceMeters <=
        100
    ) {

        return 100
    }


    return (
            (
                    distanceMeters +
                            99
                    ) /
                    100
            ) *
            100
}


/*
 * 0 ~ 360°
 */
private fun normalize360(
    angle: Float
): Float {

    return (
            (
                    angle %
                            360f
                    ) +
                    360f
            ) %
            360f
}


/*
 * -180 ~ +180°
 */
private fun normalize180(
    angle: Float
): Float {

    return (
            (
                    angle +
                            540f
                    ) %
                    360f
            ) -
            180f
}


/*
 * =========================================================
 * Constants
 * =========================================================
 */

private const val TAG =
    "TmapRoute"


private const val RETURN_HOME_TAG =
    "ReturnHome"


private const val NAVIGATION_TAG =
    "TmapNavigation"


private const val HEADING_TAG =
    "WatchNavigation"


private const val VOICE_TAG =
    "NavigationVoice"


private const val TMAP_START_POINT =
    200


private const val TMAP_DESTINATION_POINT =
    201


private const val GUIDE_REACHED_DISTANCE_METERS =
    15.0


private const val GUIDE_SNAP_DISTANCE_METERS =
    35.0


private const val GUIDE_LOOK_AHEAD_COUNT =
    4


private const val ARRIVAL_VOICE_HOLD_MILLIS =
    3_000L
