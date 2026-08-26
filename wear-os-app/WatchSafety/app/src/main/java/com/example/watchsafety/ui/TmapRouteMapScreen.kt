package com.example.watchsafety.ui

import android.util.Log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon

import com.example.watchsafety.BuildConfig
import com.example.watchsafety.location.WatchLocation
import com.example.watchsafety.navigation.TmapRouteResult

import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapMarkerItem
import com.skt.tmap.overlay.TMapPolyLine

import kotlinx.coroutines.delay


/*
 * =========================================================
 * TMAP v3.7 귀가 경로 지도
 * =========================================================
 *
 * 안전한 초기화 순서
 *
 * 1. TMapView 생성
 * 2. Listener 등록
 * 3. API Key 설정
 * 4. onMapReady 대기
 * 5. onMapReady 이후에만:
 *      - 현재 위치 아이콘
 *      - 지도 설정
 *      - 경로선
 *      - 집 마커
 *      - 카메라 이동
 *
 * 초기화 중 예외가 발생해도 앱을 종료시키지 않고
 * 오류 화면을 표시한다.
 */
@Composable
fun TmapRouteMapScreen(

    routeResult: TmapRouteResult,

    watchLocation: WatchLocation?,

    homeLatitude: Double?,

    homeLongitude: Double?,

    @Suppress("UNUSED_PARAMETER")
    headingDegrees: Float?,

    onClose: () -> Unit

) {

    val context =
        LocalContext.current


    /*
     * =====================================================
     * 지도 상태
     * =====================================================
     */

    var isMapReady by
    remember {

        mutableStateOf(
            false
        )
    }


    var isRoutePrepared by
    remember {

        mutableStateOf(
            false
        )
    }


    var followCurrentLocation by
    remember {

        mutableStateOf(
            false
        )
    }


    var mapErrorMessage by
    remember {

        mutableStateOf<String?>(
            null
        )
    }


    var loadingMessage by
    remember {

        mutableStateOf(
            "지도 불러오는 중..."
        )
    }


    /*
     * =====================================================
     * TMapView 생성
     *
     * 생성자 자체에서 문제가 발생하더라도
     * runCatching으로 앱 강제 종료 방지.
     * =====================================================
     */
    val mapViewResult =
        remember(
            context
        ) {

            runCatching {

                TMapView(
                    context
                )
            }
        }


    val tMapView =
        mapViewResult
            .getOrNull()


    /*
     * 생성 자체 실패
     */
    LaunchedEffect(
        mapViewResult
    ) {

        val error =
            mapViewResult
                .exceptionOrNull()


        if (
            error != null
        ) {

            Log.e(
                TAG,
                "TMapView 생성 실패",
                error
            )


            mapErrorMessage =
                "지도를 시작하지 못했습니다."


            loadingMessage =
                "지도 초기화 실패"
        }
    }


    /*
     * =====================================================
     * Listener + API Key + Lifecycle
     * =====================================================
     */
    DisposableEffect(
        tMapView
    ) {

        if (
            tMapView == null
        ) {

            onDispose { }

        } else {

            try {

                /*
                 * API Key 성공/실패를 직접 확인.
                 */
                tMapView
                    .setOnApiKeyListenerCallback(

                        object :
                            TMapView.OnApiKeyListenerCallback {

                            override fun onSKTMapApikeySucceed() {

                                Log.d(
                                    TAG,
                                    "TMAP API Key 인증 성공"
                                )


                                loadingMessage =
                                    "지도 준비 중..."
                            }


                            override fun onSKTMapApikeyFailed(
                                errorMsg: String?
                            ) {

                                Log.e(
                                    TAG,
                                    "TMAP API Key 인증 실패: $errorMsg"
                                )


                                mapErrorMessage =
                                    if (
                                        errorMsg.isNullOrBlank()
                                    ) {

                                        "TMAP 인증에 실패했습니다."

                                    } else {

                                        "TMAP 인증 실패\n$errorMsg"
                                    }
                            }
                        }
                    )


                /*
                 * 지도 엔진 준비 완료.
                 *
                 * 여기까지 오기 전에는 지도 관련 설정을 하지 않는다.
                 */
                tMapView
                    .setOnMapReadyListener(

                        object :
                            TMapView.OnMapReadyListener {

                            override fun onMapReady() {

                                Log.d(
                                    TAG,
                                    "TMAP onMapReady"
                                )


                                try {

                                    /*
                                     * 지도 준비 완료 이후에만 설정.
                                     */
                                    tMapView.setIconVisibility(
                                        true
                                    )


                                    tMapView.setCompassMode(
                                        false
                                    )


                                    tMapView.setTrackingMode(
                                        false
                                    )


                                    tMapView.setUserScrollMoveEnable(
                                        true
                                    )


                                    tMapView.setUserScrollZoomEnable(
                                        true
                                    )


                                    /*
                                     * Wear OS에서는 과도한 FPS가 필요하지 않으므로
                                     * 적당한 값으로 제한.
                                     *
                                     * v3.7에 추가된 공식 API.
                                     */
                                    tMapView.setFPS(
                                        MAP_FPS
                                    )


                                    isMapReady =
                                        true


                                    loadingMessage =
                                        "경로 불러오는 중..."


                                    mapErrorMessage =
                                        null


                                } catch (
                                    error: Throwable
                                ) {

                                    Log.e(
                                        TAG,
                                        "onMapReady 후 지도 설정 실패",
                                        error
                                    )


                                    mapErrorMessage =
                                        "지도 설정 중 오류가 발생했습니다."
                                }
                            }
                        }
                    )


                /*
                 * Listener를 먼저 등록한 다음
                 * 마지막에 API Key 설정.
                 */
                if (
                    BuildConfig.TMAP_APP_KEY.isBlank()
                ) {

                    mapErrorMessage =
                        "TMAP API Key가 없습니다."

                } else {

                    tMapView.setSKTMapApiKey(
                        BuildConfig.TMAP_APP_KEY
                    )
                }


                /*
                 * View lifecycle 시작.
                 */
                tMapView.onResume()


            } catch (
                error: Throwable
            ) {

                Log.e(
                    TAG,
                    "TMAP 초기화 실패",
                    error
                )


                mapErrorMessage =
                    "지도를 불러오는 중 오류가 발생했습니다."
            }


            onDispose {

                isMapReady =
                    false


                isRoutePrepared =
                    false


                followCurrentLocation =
                    false


                /*
                 * Overlay 먼저 제거.
                 */
                runCatching {

                    tMapView.removeTMapPolyLine(
                        ROUTE_LINE_ID
                    )
                }


                runCatching {

                    tMapView.removeTMapMarkerItem(
                        HOME_MARKER_ID
                    )
                }


                runCatching {

                    tMapView.onPause()
                }


                runCatching {

                    tMapView.onDestroy()
                }
            }
        }
    }


    /*
     * =====================================================
     * 지도 로딩 Timeout
     * =====================================================
     *
     * SDK가 콜백을 주지 않고 계속 검은 화면에 머무는 경우
     * 무한 대기하지 않는다.
     */
    LaunchedEffect(
        tMapView,
        isMapReady,
        mapErrorMessage
    ) {

        if (
            tMapView == null ||
            isMapReady ||
            mapErrorMessage != null
        ) {

            return@LaunchedEffect
        }


        delay(
            MAP_READY_TIMEOUT_MILLIS
        )


        if (
            !isMapReady &&
            mapErrorMessage == null
        ) {

            Log.e(
                TAG,
                "TMAP onMapReady timeout"
            )


            mapErrorMessage =
                "지도를 불러오지 못했습니다.\n잠시 후 다시 시도해주세요."
        }
    }


    /*
     * =====================================================
     * 경로선 + 집 마커
     * =====================================================
     *
     * onMapReady 이후에만 실행.
     * =====================================================
     */
    LaunchedEffect(
        isMapReady,
        routeResult.routePoints,
        homeLatitude,
        homeLongitude,
        tMapView
    ) {

        if (
            !isMapReady ||
            tMapView == null
        ) {

            return@LaunchedEffect
        }


        loadingMessage =
            "경로 불러오는 중..."


        try {

            /*
             * 이전 객체 제거.
             */
            runCatching {

                tMapView.removeTMapPolyLine(
                    ROUTE_LINE_ID
                )
            }


            runCatching {

                tMapView.removeTMapMarkerItem(
                    HOME_MARKER_ID
                )
            }


            /*
             * ---------------------------------------------
             * 실제 TMAP 보행 경로선
             * ---------------------------------------------
             */
            val validRoutePoints =
                routeResult
                    .routePoints
                    .filter { point ->

                        isValidCoordinate(

                            latitude =
                                point.latitude,

                            longitude =
                                point.longitude
                        )
                    }


            if (
                validRoutePoints.size >=
                2
            ) {

                val polyLine =
                    TMapPolyLine()
                        .apply {

                            setID(
                                ROUTE_LINE_ID
                            )


                            setLineColor(
                                android.graphics.Color.rgb(
                                    47,
                                    95,
                                    227
                                )
                            )


                            setLineWidth(
                                ROUTE_LINE_WIDTH
                            )


                            setLineAlpha(
                                255
                            )


                            setOutLineColor(
                                android.graphics.Color.WHITE
                            )


                            setOutLineWidth(
                                ROUTE_OUTLINE_WIDTH
                            )


                            setOutLineAlpha(
                                220
                            )


                            validRoutePoints
                                .forEach { point ->

                                    addLinePoint(

                                        TMapPoint(

                                            point.latitude,

                                            point.longitude
                                        )
                                    )
                                }
                        }


                tMapView.addTMapPolyLine(
                    polyLine
                )


                Log.d(
                    TAG,
                    "경로선 추가 완료 points=${validRoutePoints.size}"
                )

            } else {

                Log.w(
                    TAG,
                    "경로선 좌표 부족: ${validRoutePoints.size}"
                )
            }


            /*
             * ---------------------------------------------
             * 집 마커
             * ---------------------------------------------
             *
             * 커스텀 Bitmap을 사용하지 않고
             * SDK 기본 마커를 우선 사용해서
             * 초기 런타임 안정성을 높인다.
             */
            if (
                homeLatitude != null &&
                homeLongitude != null &&
                isValidCoordinate(

                    latitude =
                        homeLatitude,

                    longitude =
                        homeLongitude
                )
            ) {

                val homeMarker =
                    TMapMarkerItem()
                        .apply {

                            setId(
                                HOME_MARKER_ID
                            )


                            setTMapPoint(

                                TMapPoint(

                                    homeLatitude,

                                    homeLongitude
                                )
                            )


                            setName(
                                "집"
                            )


                            setCanShowCallout(
                                false
                            )


                            setPosition(
                                0.5f,
                                1.0f
                            )
                        }


                tMapView.addTMapMarkerItem(
                    homeMarker
                )


                Log.d(
                    TAG,
                    "집 마커 추가 완료"
                )
            }


            isRoutePrepared =
                true


        } catch (
            error: Throwable
        ) {

            Log.e(
                TAG,
                "경로/마커 표시 실패",
                error
            )


            mapErrorMessage =
                "경로를 지도에 표시하지 못했습니다."
        }
    }


    /*
     * =====================================================
     * 최초 지도 카메라
     *
     * 1. 전체 경로
     * 2. 약 1.7초 유지
     * 3. 현재 위치 중심
     * =====================================================
     */
    LaunchedEffect(
        isMapReady,
        isRoutePrepared,
        routeResult.routePoints,
        watchLocation,
        tMapView
    ) {

        if (
            !isMapReady ||
            !isRoutePrepared ||
            tMapView == null
        ) {

            return@LaunchedEffect
        }


        try {

            followCurrentLocation =
                false


            val validRoutePoints =
                routeResult
                    .routePoints
                    .filter { point ->

                        isValidCoordinate(

                            latitude =
                                point.latitude,

                            longitude =
                                point.longitude
                        )
                    }


            if (
                validRoutePoints.size >=
                2
            ) {

                val maxLatitude =
                    validRoutePoints
                        .maxOf { point ->

                            point.latitude
                        }


                val minLatitude =
                    validRoutePoints
                        .minOf { point ->

                            point.latitude
                        }


                val minLongitude =
                    validRoutePoints
                        .minOf { point ->

                            point.longitude
                        }


                val maxLongitude =
                    validRoutePoints
                        .maxOf { point ->

                            point.longitude
                        }


                tMapView.zoomToTMapPoint(

                    TMapPoint(

                        maxLatitude,

                        minLongitude
                    ),

                    TMapPoint(

                        minLatitude,

                        maxLongitude
                    )
                )


                Log.d(
                    TAG,
                    "전체 경로 카메라 표시"
                )


                /*
                 * 여기까지 정상적으로 왔으므로
                 * 로딩 Overlay 제거.
                 */
                loadingMessage =
                    ""


                delay(
                    WHOLE_ROUTE_HOLD_MILLIS
                )
            }


            followCurrentLocation =
                true


            val current =
                watchLocation


            if (
                current != null &&
                isValidCoordinate(

                    latitude =
                        current.latitude,

                    longitude =
                        current.longitude
                )
            ) {

                moveToCurrentLocation(

                    tMapView =
                        tMapView,

                    watchLocation =
                        current
                )
            }


        } catch (
            error: Throwable
        ) {

            Log.e(
                TAG,
                "초기 카메라 설정 실패",
                error
            )


            /*
             * 카메라 조작 실패만으로 앱이나 지도 자체를
             * 종료시키지는 않는다.
             */
            loadingMessage =
                ""
        }
    }


    /*
     * =====================================================
     * GPS 갱신 → 현재 위치 추적
     * =====================================================
     */
    LaunchedEffect(
        isMapReady,
        isRoutePrepared,
        followCurrentLocation,
        watchLocation,
        tMapView
    ) {

        if (
            !isMapReady ||
            !isRoutePrepared ||
            !followCurrentLocation ||
            tMapView == null
        ) {

            return@LaunchedEffect
        }


        val current =
            watchLocation
                ?: return@LaunchedEffect


        if (
            !isValidCoordinate(

                latitude =
                    current.latitude,

                longitude =
                    current.longitude
            )
        ) {

            return@LaunchedEffect
        }


        try {

            moveToCurrentLocation(

                tMapView =
                    tMapView,

                watchLocation =
                    current
            )


        } catch (
            error: Throwable
        ) {

            /*
             * GPS 한 번 갱신 실패했다고 앱을 종료시키지 않는다.
             */
            Log.e(
                TAG,
                "GPS 지도 갱신 실패",
                error
            )
        }
    }


    /*
     * =====================================================
     * UI
     * =====================================================
     */
    Box(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color.Black
                )
    ) {

        /*
         * TMapView가 정상 생성됐을 때만 AndroidView 표시.
         */
        if (
            tMapView != null
        ) {

            AndroidView(

                factory = {

                    tMapView
                },

                modifier =
                    Modifier.fillMaxSize()
            )
        }


        /*
         * =================================================
         * 보호자 앱 느낌의 지도 Loading Overlay
         * =================================================
         */
        if (
            mapErrorMessage == null &&
            (
                    !isMapReady ||
                            !isRoutePrepared ||
                            loadingMessage.isNotBlank()
                    )
        ) {

            MapLoadingOverlay(

                message =
                    loadingMessage
                        .ifBlank {

                            "지도 불러오는 중..."
                        }
            )
        }


        /*
         * =================================================
         * 지도 오류
         * =================================================
         *
         * 앱이 죽지 않고 이 화면에 남는다.
         */
        val errorMessage =
            mapErrorMessage


        if (
            errorMessage != null
        ) {

            MapErrorOverlay(

                message =
                    errorMessage,

                onClose =
                    onClose
            )
        }


        /*
         * 정상 지도 화면에서만 버튼 표시
         */
        if (
            mapErrorMessage == null &&
            isMapReady
        ) {

            /*
             * 지도 닫기
             */
            RoundTmapMapButton(

                modifier =
                    Modifier
                        .align(
                            Alignment.BottomCenter
                        )
                        .padding(
                            bottom =
                                8.dp
                        ),

                type =
                    MapButtonType.CLOSE,

                contentDescription =
                    "지도 닫기",

                onClick =
                    onClose
            )


            /*
             * 현재 위치 복귀
             */
            RoundTmapMapButton(

                modifier =
                    Modifier
                        .align(
                            Alignment.BottomEnd
                        )
                        .padding(
                            end =
                                10.dp,

                            bottom =
                                10.dp
                        ),

                type =
                    MapButtonType.MY_LOCATION,

                contentDescription =
                    "현재 위치",

                onClick = {

                    val current =
                        watchLocation


                    if (
                        current != null &&
                        tMapView != null &&
                        isValidCoordinate(

                            latitude =
                                current.latitude,

                            longitude =
                                current.longitude
                        )
                    ) {

                        try {

                            followCurrentLocation =
                                true


                            moveToCurrentLocation(

                                tMapView =
                                    tMapView,

                                watchLocation =
                                    current
                            )


                        } catch (
                            error: Throwable
                        ) {

                            Log.e(
                                TAG,
                                "현재 위치 버튼 실패",
                                error
                            )
                        }
                    }
                }
            )
        }
    }
}


/*
 * =========================================================
 * Loading Overlay
 * =========================================================
 */
@Composable
private fun MapLoadingOverlay(

    message: String

) {

    Box(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(
                        0xE6000000
                    )
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            CircularProgressIndicator(

                modifier =
                    Modifier.size(
                        32.dp
                    ),

                indicatorColor =
                    Color(
                        0xFF4CAF50
                    ),

                trackColor =
                    Color(
                        0xFF333333
                    )
            )


            Spacer(

                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            BasicText(

                text =
                    message,

                style =
                    TextStyle(

                        color =
                            Color.White,

                        fontSize =
                            13.sp,

                        fontWeight =
                            FontWeight.Bold,

                        textAlign =
                            TextAlign.Center
                    )
            )
        }
    }
}


/*
 * =========================================================
 * Error Overlay
 * =========================================================
 */
@Composable
private fun MapErrorOverlay(

    message: String,

    onClose: () -> Unit

) {

    Box(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color.Black
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(

            modifier =
                Modifier.padding(
                    20.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(

                imageVector =
                    Icons.Default.Home,

                contentDescription =
                    null,

                tint =
                    Color(
                        0xFFFFC107
                    ),

                modifier =
                    Modifier.size(
                        32.dp
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
                    message,

                style =
                    TextStyle(

                        color =
                            Color.White,

                        fontSize =
                            12.sp,

                        fontWeight =
                            FontWeight.Bold,

                        textAlign =
                            TextAlign.Center
                    )
            )


            Spacer(

                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            Box(

                modifier =
                    Modifier
                        .size(
                            42.dp
                        )
                        .clip(
                            CircleShape
                        )
                        .background(
                            Color(
                                0xFF333333
                            )
                        )
                        .clickable(
                            onClick =
                                onClose
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(

                    imageVector =
                        Icons.Default.Close,

                    contentDescription =
                        "닫기",

                    tint =
                        Color.White
                )
            }
        }
    }
}


/*
 * =========================================================
 * 현재 위치 이동
 * =========================================================
 */
private fun moveToCurrentLocation(

    tMapView: TMapView,

    watchLocation: WatchLocation

) {

    /*
     * v3.7:
     * latitude, longitude 순서.
     */
    tMapView.setLocationPoint(

        watchLocation.latitude,

        watchLocation.longitude
    )


    tMapView.setIconVisibility(
        true
    )


    tMapView.setZoomLevel(
        FOLLOW_ZOOM_LEVEL
    )


    tMapView.setCenterPoint(

        watchLocation.latitude,

        watchLocation.longitude,

        true
    )
}


/*
 * =========================================================
 * Round Buttons
 * =========================================================
 */
private enum class MapButtonType {

    CLOSE,

    MY_LOCATION
}


@Composable
private fun RoundTmapMapButton(

    modifier: Modifier,

    type: MapButtonType,

    contentDescription: String,

    onClick: () -> Unit

) {

    Box(

        modifier =
            modifier
                .size(
                    42.dp
                )
                .clip(
                    CircleShape
                )
                .background(
                    Color(
                        0xE6000000
                    )
                )
                .clickable(
                    onClick =
                        onClick
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Icon(

            imageVector =
                when (
                    type
                ) {

                    MapButtonType.CLOSE ->
                        Icons.Default.Close


                    MapButtonType.MY_LOCATION ->
                        Icons.Default.MyLocation
                },

            contentDescription =
                contentDescription,

            tint =
                Color.White,

            modifier =
                Modifier.size(
                    21.dp
                )
        )
    }
}


/*
 * =========================================================
 * 좌표 검증
 * =========================================================
 */
private fun isValidCoordinate(

    latitude: Double,

    longitude: Double

): Boolean {

    return latitude in
            -90.0..90.0 &&
            longitude in
            -180.0..180.0 &&
            (
                    latitude !=
                            0.0 ||
                            longitude !=
                            0.0
                    )
}


/*
 * =========================================================
 * Constants
 * =========================================================
 */

private const val TAG =
    "TmapRouteMap"


/*
 * 주변 약 1~2블록.
 */
private const val FOLLOW_ZOOM_LEVEL =
    17


/*
 * 전체 경로를 먼저 보여주는 시간.
 */
private const val WHOLE_ROUTE_HOLD_MILLIS =
    1_700L


/*
 * 지도 준비 최대 대기시간.
 */
private const val MAP_READY_TIMEOUT_MILLIS =
    12_000L


/*
 * Wear OS에서 충분한 지도 FPS.
 * v3.7부터 제공.
 */
private const val MAP_FPS =
    30


private const val ROUTE_LINE_ID =
    "return_home_route"


private const val HOME_MARKER_ID =
    "return_home_home"


private const val ROUTE_LINE_WIDTH =
    8f


private const val ROUTE_OUTLINE_WIDTH =
    2f
