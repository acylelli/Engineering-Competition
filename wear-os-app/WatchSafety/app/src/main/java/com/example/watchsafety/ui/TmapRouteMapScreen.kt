package com.example.watchsafety.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

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
 * TMAP VectorMap 귀가 경로 지도
 * =========================================================
 *
 * 동작
 *
 * 1. 지도 버튼을 누르면 TMAP 지도를 연다.
 * 2. 지도 준비 완료 후 실제 보행 경로선을 그린다.
 * 3. 집 마커를 표시한다.
 * 4. 처음에는 전체 경로를 약 1.7초 보여준다.
 * 5. 이후 현재 위치 중심 + 줌 17로 이동한다.
 * 6. GPS 위치가 갱신될 때마다 현재 위치를 따라간다.
 *
 * 중요
 *
 * TMAP VectorMap은 지도 준비 전 API를 호출하면
 * 정상적으로 적용되지 않을 수 있으므로
 * setOnMapReadyListener 이후에만 경로/마커/카메라를 조작한다.
 */
@Composable
fun TmapRouteMapScreen(

    routeResult: TmapRouteResult,

    watchLocation: WatchLocation?,

    homeLatitude: Double?,

    homeLongitude: Double?,

    /*
     * 기본 화살표 화면과 동일한 함수 시그니처를 유지하기 위해
     * 전달받는다.
     *
     * 현재 지도 화면은 "위쪽=북쪽" 고정형으로 두고
     * 위치 추적만 수행한다.
     */
    @Suppress("UNUSED_PARAMETER")
    headingDegrees: Float?,

    onClose: () -> Unit

) {

    val context =
        LocalContext.current


    var isMapReady by
    remember {

        mutableStateOf(
            false
        )
    }


    var initialRouteShown by
    remember(
        routeResult
    ) {

        mutableStateOf(
            false
        )
    }


    /*
     * =====================================================
     * TMapView 생성
     * =====================================================
     *
     * 공식 TMAP Compose 관련 사례처럼:
     *
     * 1. API Key 설정
     * 2. MapReadyListener 설정
     * 3. AndroidView에 전달
     */
    val tMapView =
        remember(
            context
        ) {

            TMapView(
                context
            )
                .apply {

                    setSKTMapApiKey(
                        BuildConfig.TMAP_APP_KEY
                    )


                    setOnMapReadyListener {

                        Log.d(
                            TAG,
                            "TMAP VectorMap 준비 완료"
                        )


                        isMapReady =
                            true
                    }


                    /*
                     * 현재 위치 표시는 직접 WatchLocation을 넣는다.
                     */
                    setIconVisibility(
                        true
                    )


                    /*
                     * 지도 자체를 워치 방향으로 회전시키지 않는다.
                     * 고령 사용자에게 화면 방향이 계속 회전하는 것보다
                     * 북쪽 고정 지도가 더 안정적이다.
                     */
                    setCompassMode(
                        false
                    )


                    setTrackingMode(
                        false
                    )


                    /*
                     * 사용자가 필요하면 직접 움직이거나 확대할 수 있다.
                     */
                    setUserScrollMoveEnable(
                        true
                    )


                    setUserScrollZoomEnable(
                        true
                    )
                }
        }


    /*
     * =====================================================
     * TMapView Lifecycle
     * =====================================================
     *
     * VectorMap 공식 API:
     * onResume()
     * onPause()
     * onDestroy()
     */
    DisposableEffect(
        tMapView
    ) {

        tMapView.onResume()


        onDispose {

            isMapReady =
                false


            try {

                tMapView.onPause()

            } catch (
                error: Throwable
            ) {

                Log.w(
                    TAG,
                    "TMapView onPause 실패",
                    error
                )
            }


            try {

                tMapView.onDestroy()

            } catch (
                error: Throwable
            ) {

                Log.w(
                    TAG,
                    "TMapView onDestroy 실패",
                    error
                )
            }
        }
    }


    /*
     * =====================================================
     * 경로선 표시
     * =====================================================
     *
     * 현재 VectorMap API:
     *
     * polyLine.setID(...)
     * tMapView.addTMapPolyLine(polyLine)
     *
     * 예전 Raster SDK처럼
     *
     * addTMapPolyLine(id, polyLine)
     *
     * 형태로 호출하지 않는다.
     */
    DisposableEffect(
        isMapReady,
        routeResult.routePoints,
        tMapView
    ) {

        if (
            !isMapReady
        ) {

            onDispose { }

        } else {

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


            var routeAdded =
                false


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
                                AndroidColor.rgb(
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
                                AndroidColor.WHITE
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


                /*
                 * VectorMap v3.x:
                 * ID는 PolyLine 객체 내부에 설정하고
                 * 객체 하나만 add한다.
                 */
                tMapView.addTMapPolyLine(
                    polyLine
                )


                routeAdded =
                    true


                Log.d(
                    TAG,
                    "TMAP 경로선 표시 points=${validRoutePoints.size}"
                )
            }


            onDispose {

                if (
                    routeAdded
                ) {

                    try {

                        tMapView.removeTMapPolyLine(
                            ROUTE_LINE_ID
                        )

                    } catch (
                        error: Throwable
                    ) {

                        Log.w(
                            TAG,
                            "경로선 제거 실패",
                            error
                        )
                    }
                }
            }
        }
    }


    /*
     * =====================================================
     * 집 마커
     * =====================================================
     *
     * VectorMap v3.x:
     *
     * marker.setId(...)
     * map.addMarkerItem(marker)
     */
    DisposableEffect(
        isMapReady,
        homeLatitude,
        homeLongitude,
        tMapView
    ) {

        if (
            !isMapReady ||
            homeLatitude == null ||
            homeLongitude == null ||
            !isValidCoordinate(
                latitude =
                    homeLatitude,
                longitude =
                    homeLongitude
            )
        ) {

            onDispose { }

        } else {

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


                        setIcon(
                            createHomeMarkerBitmap()
                        )
                    }


            tMapView.addTMapMarkerItem(
                homeMarker
            )


            Log.d(
                TAG,
                "집 마커 표시 lat=$homeLatitude lng=$homeLongitude"
            )


            onDispose {

                try {

                    tMapView.removeTMapMarkerItem(
                        HOME_MARKER_ID
                    )

                } catch (
                    error: Throwable
                ) {

                    Log.w(
                        TAG,
                        "집 마커 제거 실패",
                        error
                    )
                }
            }
        }
    }


    /*
     * =====================================================
     * 처음 전체 경로 표시
     * =====================================================
     *
     * VectorMap 공식 API:
     *
     * zoomToTMapPoint(
     *     leftTop,
     *     rightBottom
     * )
     *
     * leftTop:
     * 북서쪽
     *
     * rightBottom:
     * 남동쪽
     *
     * 전체 경로를 1.7초 보여준 뒤
     * 현재 위치 중심으로 이동한다.
     */
    LaunchedEffect(
        isMapReady,
        routeResult.routePoints,
        tMapView
    ) {

        if (
            !isMapReady ||
            initialRouteShown
        ) {

            return@LaunchedEffect
        }


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
                    .maxOf {

                        it.latitude
                    }


            val minLatitude =
                validRoutePoints
                    .minOf {

                        it.latitude
                    }


            val minLongitude =
                validRoutePoints
                    .minOf {

                        it.longitude
                    }


            val maxLongitude =
                validRoutePoints
                    .maxOf {

                        it.longitude
                    }


            val leftTop =
                TMapPoint(

                    maxLatitude,

                    minLongitude
                )


            val rightBottom =
                TMapPoint(

                    minLatitude,

                    maxLongitude
                )


            tMapView.zoomToTMapPoint(

                leftTop,

                rightBottom
            )


            initialRouteShown =
                true


            Log.d(
                TAG,
                "전체 귀가 경로 표시"
            )


            delay(
                WHOLE_ROUTE_HOLD_MILLIS
            )
        } else {

            initialRouteShown =
                true
        }


        val currentLocation =
            watchLocation
                ?: return@LaunchedEffect


        if (
            isValidCoordinate(

                latitude =
                    currentLocation.latitude,

                longitude =
                    currentLocation.longitude
            )
        ) {

            moveToCurrentLocation(

                tMapView =
                    tMapView,

                watchLocation =
                    currentLocation
            )
        }
    }


    /*
     * =====================================================
     * GPS 변경 → 현재 위치 표시
     * =====================================================
     *
     * 첫 전체경로 화면을 보여주는 동안에는
     * GPS 업데이트 때문에 즉시 현재 위치로 줌이 돌아가지 않도록
     * initialRouteShown 이후에만 추적한다.
     */
    LaunchedEffect(
        isMapReady,
        initialRouteShown,
        watchLocation,
        tMapView
    ) {

        if (
            !isMapReady ||
            !initialRouteShown
        ) {

            return@LaunchedEffect
        }


        val currentLocation =
            watchLocation
                ?: return@LaunchedEffect


        if (
            !isValidCoordinate(

                latitude =
                    currentLocation.latitude,

                longitude =
                    currentLocation.longitude
            )
        ) {

            return@LaunchedEffect
        }


        moveToCurrentLocation(

            tMapView =
                tMapView,

            watchLocation =
                currentLocation
        )
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

        AndroidView(

            factory = {

                tMapView
            },

            modifier =
                Modifier.fillMaxSize()
        )


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
         * 사용자가 지도를 직접 움직인 후
         * 현재 위치로 돌아오기
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

                val currentLocation =
                    watchLocation


                if (
                    isMapReady &&
                    currentLocation != null &&
                    isValidCoordinate(

                        latitude =
                            currentLocation.latitude,

                        longitude =
                            currentLocation.longitude
                    )
                ) {

                    moveToCurrentLocation(

                        tMapView =
                            tMapView,

                        watchLocation =
                            currentLocation
                    )
                }
            }
        )
    }
}


/*
 * =========================================================
 * 현재 위치로 지도 이동
 * =========================================================
 *
 * VectorMap v3.x 공식 좌표 순서:
 *
 * setLocationPoint(
 *     latitude,
 *     longitude
 * )
 *
 * setCenterPoint(
 *     latitude,
 *     longitude
 * )
 *
 * 예전 Raster SDK의 longitude → latitude 순서와 다르므로
 * 절대 바꾸면 안 된다.
 */
private fun moveToCurrentLocation(

    tMapView: TMapView,

    watchLocation: WatchLocation

) {

    tMapView.setLocationPoint(

        watchLocation.latitude,

        watchLocation.longitude
    )


    tMapView.setIconVisibility(
        true
    )


    /*
     * 워치에서 주변 1~2블록 정도가 보이도록 설정.
     */
    tMapView.setZoomLevel(
        FOLLOW_ZOOM_LEVEL
    )


    tMapView.setCenterPoint(

        watchLocation.latitude,

        watchLocation.longitude
    )


    Log.v(
        TAG,
        "지도 현재 위치 이동 " +
                "lat=${watchLocation.latitude} " +
                "lng=${watchLocation.longitude}"
    )
}


/*
 * =========================================================
 * 동그란 지도 버튼
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
                    MAP_BUTTON_SIZE_DP.dp
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
 * 집 마커 Bitmap
 * =========================================================
 */
private fun createHomeMarkerBitmap(): Bitmap {

    val size =
        HOME_MARKER_SIZE_PX


    val bitmap =
        Bitmap.createBitmap(

            size,

            size,

            Bitmap.Config.ARGB_8888
        )


    val canvas =
        Canvas(
            bitmap
        )


    val circlePaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        )
            .apply {

                color =
                    AndroidColor.rgb(
                        46,
                        160,
                        67
                    )
            }


    canvas.drawCircle(

        size /
                2f,

        size /
                2f,

        size *
                0.43f,

        circlePaint
    )


    val textPaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        )
            .apply {

                color =
                    AndroidColor.WHITE


                textSize =
                    28f


                typeface =
                    Typeface.DEFAULT_BOLD


                textAlign =
                    Paint.Align.CENTER
            }


    val textY =
        size /
                2f -
                (
                        textPaint.ascent() +
                                textPaint.descent()
                        ) /
                2f


    canvas.drawText(

        "집",

        size /
                2f,

        textY,

        textPaint
    )


    return bitmap
}


/*
 * =========================================================
 * GPS 좌표 검증
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
 * TMAP VectorMap:
 * 워치 화면에서 주변 1~2블록 정도.
 *
 * 너무 확대되면 16,
 * 조금 더 가까이 보고 싶으면 18.
 */
private const val FOLLOW_ZOOM_LEVEL =
    17


/*
 * 처음 전체 경로를 보여주는 시간.
 */
private const val WHOLE_ROUTE_HOLD_MILLIS =
    1_700L


private const val ROUTE_LINE_ID =
    "return_home_route"


private const val HOME_MARKER_ID =
    "return_home_home"


private const val ROUTE_LINE_WIDTH =
    8f


private const val ROUTE_OUTLINE_WIDTH =
    2f


private const val HOME_MARKER_SIZE_PX =
    72


private const val MAP_BUTTON_SIZE_DP =
    42
