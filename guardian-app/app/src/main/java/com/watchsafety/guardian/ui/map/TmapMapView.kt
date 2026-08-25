package com.watchsafety.guardian.ui.map

import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapCircle
import com.watchsafety.guardian.BuildConfig
import com.watchsafety.guardian.domain.model.SafeZone
import kotlinx.coroutines.delay

private const val TAG =
    "GuardianTmap"

private const val MAP_REVEAL_DELAY_MILLIS =
    700L

/*
 * 안전구역 추가 화면에서 지도를 움직일 때
 * 미리보기 원의 중심을 따라가기 위한 주기.
 *
 * 중심점이 실제로 바뀐 경우에만 다시 그린다.
 */
private const val PREVIEW_CENTER_POLL_MILLIS =
    120L


@Composable
fun TmapMapView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
    zoomLevel: Int = 16,
    showLocationMarker: Boolean = false,
    followLocation: Boolean = false,
    safeZones: List<SafeZone> = emptyList(),

    /*
     * null이면 안전구역 미리보기 없음.
     *
     * 안전구역 추가 화면에서는 현재 선택한 반경을 넘겨주면
     * 지도의 현재 중심점을 기준으로 파란 원을 표시한다.
     */
    previewSafeZoneRadiusMeters: Int? = null,

    onMapViewReady: ((TMapView) -> Unit)? = null,
) {

    val isPreview =
        LocalInspectionMode.current

    val lifecycleOwner =
        LocalLifecycleOwner.current


    val latestLatitude by
    rememberUpdatedState(
        latitude
    )

    val latestLongitude by
    rememberUpdatedState(
        longitude
    )

    val latestZoomLevel by
    rememberUpdatedState(
        zoomLevel
    )

    val latestShowLocationMarker by
    rememberUpdatedState(
        showLocationMarker
    )

    val latestSafeZones by
    rememberUpdatedState(
        safeZones
    )

    val latestPreviewRadius by
    rememberUpdatedState(
        previewSafeZoneRadiusMeters
    )

    val latestOnMapViewReady by
    rememberUpdatedState(
        onMapViewReady
    )


    var mapView by
    remember {
        mutableStateOf<TMapView?>(
            null
        )
    }


    var isMapReady by
    remember {
        mutableStateOf(
            false
        )
    }


    if (
        isPreview ||
        BuildConfig.TMAP_APP_KEY.isBlank()
    ) {

        Box(
            modifier =
                modifier
                    .background(
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    ),

            contentAlignment =
                Alignment.Center,
        ) {

            Text(
                text =
                    if (
                        isPreview
                    ) {
                        "TMAP 지도"
                    } else {
                        "TMAP_APP_KEY가 설정되지 않았습니다."
                    },

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
            )
        }

        return
    }


    /*
     * =====================================================
     * 안전구역 미리보기 중심 추적
     * =====================================================
     *
     * 안전구역 추가 화면은 중앙에 고정 핀이 있고
     * 사용자가 지도 자체를 움직여 중심좌표를 선택한다.
     *
     * TMAP 중심점을 주기적으로 확인해서 실제로 중심이 바뀐 경우에만
     * 원을 다시 그려 핀을 따라오게 한다.
     */
    LaunchedEffect(
        mapView,
        isMapReady,
        previewSafeZoneRadiusMeters,
    ) {

        val view =
            mapView
                ?: return@LaunchedEffect

        if (
            !isMapReady ||
            previewSafeZoneRadiusMeters == null
        ) {

            return@LaunchedEffect
        }


        var previousLatitude =
            Double.NaN

        var previousLongitude =
            Double.NaN

        var previousRadius =
            -1


        while (
            true
        ) {

            val center =
                view
                    .getCenterPoint()


            val centerLatitude =
                center
                    .getLatitude()


            val centerLongitude =
                center
                    .getLongitude()


            val currentRadius =
                latestPreviewRadius
                    ?: break


            val centerChanged =
                previousLatitude.isNaN() ||
                        previousLongitude.isNaN() ||
                        kotlin.math.abs(
                            centerLatitude -
                                    previousLatitude
                        ) > 0.0000001 ||
                        kotlin.math.abs(
                            centerLongitude -
                                    previousLongitude
                        ) > 0.0000001


            val radiusChanged =
                previousRadius !=
                        currentRadius


            if (
                centerChanged ||
                radiusChanged
            ) {

                redrawMapCircles(
                    mapView =
                        view,

                    safeZones =
                        latestSafeZones,

                    showWearerLocation =
                        latestShowLocationMarker,

                    wearerLatitude =
                        latestLatitude,

                    wearerLongitude =
                        latestLongitude,

                    previewSafeZoneRadiusMeters =
                        currentRadius,

                    previewCenterLatitude =
                        centerLatitude,

                    previewCenterLongitude =
                        centerLongitude,
                )


                previousLatitude =
                    centerLatitude

                previousLongitude =
                    centerLongitude

                previousRadius =
                    currentRadius
            }


            delay(
                PREVIEW_CENTER_POLL_MILLIS
            )
        }
    }


    /*
     * =====================================================
     * Lifecycle
     * =====================================================
     */
    DisposableEffect(
        lifecycleOwner
    ) {

        val observer =
            LifecycleEventObserver {
                    _,
                    event ->

                when (
                    event
                ) {

                    Lifecycle.Event.ON_RESUME -> {

                        mapView
                            ?.onResume()
                    }


                    Lifecycle.Event.ON_PAUSE -> {

                        mapView
                            ?.onPause()
                    }


                    else ->
                        Unit
                }
            }


        lifecycleOwner
            .lifecycle
            .addObserver(
                observer
            )


        onDispose {

            lifecycleOwner
                .lifecycle
                .removeObserver(
                    observer
                )


            isMapReady =
                false


            mapView
                ?.onDestroy()


            mapView =
                null


            Log.d(
                TAG,
                "TMapView 종료"
            )
        }
    }


    /*
     * =====================================================
     * AndroidView
     * =====================================================
     *
     * TMapView와 네이티브 로딩 덮개를 같은 FrameLayout 안에 둬서
     * TMAP 초기 타일/Surface 깜빡임을 사용자에게 숨긴다.
     */
    AndroidView(
        modifier =
            modifier,

        factory = {
                context ->

            val container =
                FrameLayout(
                    context
                )


            val tMapView =
                TMapView(
                    context
                )


            container
                .addView(
                    tMapView,

                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                )


            val loadingContainer =
                LinearLayout(
                    context
                ).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    gravity =
                        Gravity.CENTER

                    setBackgroundColor(
                        Color.rgb(
                            247,
                            248,
                            250
                        )
                    )

                    isClickable =
                        true

                    isFocusable =
                        true
                }


            val progressBar =
                ProgressBar(
                    context
                )


            loadingContainer
                .addView(
                    progressBar,

                    LinearLayout.LayoutParams(
                        dpToPx(
                            context,
                            42
                        ),
                        dpToPx(
                            context,
                            42
                        ),
                    )
                )


            val loadingText =
                TextView(
                    context
                ).apply {

                    text =
                        "지도를 불러오는 중..."

                    textSize =
                        14f

                    setTextColor(
                        Color.rgb(
                            70,
                            77,
                            87
                        )
                    )

                    gravity =
                        Gravity.CENTER

                    setTypeface(
                        typeface,
                        Typeface.BOLD
                    )
                }


            val textParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {

                    topMargin =
                        dpToPx(
                            context,
                            14
                        )
                }


            loadingContainer
                .addView(
                    loadingText,
                    textParams
                )


            container
                .addView(
                    loadingContainer,

                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                )


            loadingContainer
                .bringToFront()


            Log.d(
                TAG,
                "TMapView 생성 / 초기화 시작"
            )


            tMapView
                .setOnMapReadyListener {

                    Log.d(
                        TAG,
                        "onMapReady 수신"
                    )


                    tMapView
                        .setZoomLevel(
                            latestZoomLevel
                        )


                    tMapView
                        .setCenterPoint(
                            latestLatitude,
                            latestLongitude
                        )


                    val initialCenter =
                        tMapView
                            .getCenterPoint()


                    redrawMapCircles(
                        mapView =
                            tMapView,

                        safeZones =
                            latestSafeZones,

                        showWearerLocation =
                            latestShowLocationMarker,

                        wearerLatitude =
                            latestLatitude,

                        wearerLongitude =
                            latestLongitude,

                        previewSafeZoneRadiusMeters =
                            latestPreviewRadius,

                        previewCenterLatitude =
                            initialCenter
                                .getLatitude(),

                        previewCenterLongitude =
                            initialCenter
                                .getLongitude(),
                    )


                    latestOnMapViewReady
                        ?.invoke(
                            tMapView
                        )


                    isMapReady =
                        true


                    loadingContainer
                        .postDelayed(
                            {

                                loadingContainer
                                    .visibility =
                                    View.GONE


                                Log.d(
                                    TAG,
                                    "로딩 덮개 제거 / 지도 공개"
                                )

                            },

                            MAP_REVEAL_DELAY_MILLIS
                        )
                }


            tMapView
                .setSKTMapApiKey(
                    BuildConfig
                        .TMAP_APP_KEY
                )


            mapView =
                tMapView


            container
        },


        update = {
                container ->

            val view =
                container
                    .getChildAt(
                        0
                    ) as? TMapView
                    ?: return@AndroidView


            if (
                !isMapReady
            ) {

                return@AndroidView
            }


            /*
             * 반경 변경에 따라 AddSafeZoneScreen이 계산한
             * zoomLevel을 실제 지도에 즉시 반영한다.
             */
            view
                .setZoomLevel(
                    zoomLevel
                )


            val previewCenter =
                if (
                    previewSafeZoneRadiusMeters != null
                ) {
                    view
                        .getCenterPoint()
                } else {
                    null
                }


            redrawMapCircles(
                mapView =
                    view,

                safeZones =
                    safeZones,

                showWearerLocation =
                    showLocationMarker,

                wearerLatitude =
                    latitude,

                wearerLongitude =
                    longitude,

                previewSafeZoneRadiusMeters =
                    previewSafeZoneRadiusMeters,

                previewCenterLatitude =
                    previewCenter
                        ?.getLatitude(),

                previewCenterLongitude =
                    previewCenter
                        ?.getLongitude(),
            )


            if (
                followLocation
            ) {

                view
                    .setCenterPoint(
                        latitude,
                        longitude
                    )
            }
        },
    )
}


/*
 * =========================================================
 * 지도 원 다시 그리기
 * =========================================================
 */
private fun redrawMapCircles(
    mapView: TMapView,
    safeZones: List<SafeZone>,
    showWearerLocation: Boolean,
    wearerLatitude: Double,
    wearerLongitude: Double,
    previewSafeZoneRadiusMeters: Int? = null,
    previewCenterLatitude: Double? = null,
    previewCenterLongitude: Double? = null,
) {

    mapView
        .removeAllTMapCircle()


    /*
     * =====================================================
     * 저장된 활성 안전구역
     * =====================================================
     */
    safeZones
        .asSequence()
        .filter {
                zone ->

            zone.enabled &&
                    isValidCoordinate(
                        zone.centerLatitude,
                        zone.centerLongitude,
                    )
        }
        .forEach {
                zone ->

            val safeZoneCircle =
                TMapCircle()
                    .apply {

                        setId(
                            "safe_zone_${zone.id}"
                        )


                        setCenterPoint(
                            TMapPoint(
                                zone.centerLatitude,
                                zone.centerLongitude,
                            )
                        )


                        setRadius(
                            zone
                                .radiusMeters
                                .toDouble()
                        )


                        setAreaColor(
                            Color.rgb(
                                47,
                                95,
                                227,
                            )
                        )


                        setAreaAlpha(
                            35
                        )


                        setLineColor(
                            Color.rgb(
                                47,
                                95,
                                227,
                            )
                        )


                        setLineAlpha(
                            190
                        )


                        setCircleWidth(
                            2.5f
                        )


                        setRadiusVisible(
                            false
                        )
                    }


            mapView
                .addTMapCircle(
                    safeZoneCircle
                )
        }


    /*
     * =====================================================
     * 착용자 현재 위치
     * =====================================================
     */
    if (
        showWearerLocation &&
        isValidCoordinate(
            wearerLatitude,
            wearerLongitude,
        )
    ) {

        val point =
            TMapPoint(
                wearerLatitude,
                wearerLongitude,
            )


        val outerCircle =
            TMapCircle()
                .apply {

                    setId(
                        "wearer_location_outer"
                    )


                    setCenterPoint(
                        point
                    )


                    setRadius(
                        30.0
                    )


                    setAreaColor(
                        Color.WHITE
                    )


                    setAreaAlpha(
                        245
                    )


                    setLineColor(
                        Color.rgb(
                            47,
                            95,
                            227,
                        )
                    )


                    setLineAlpha(
                        255
                    )


                    setCircleWidth(
                        3.5f
                    )


                    setRadiusVisible(
                        false
                    )
                }


        val innerCircle =
            TMapCircle()
                .apply {

                    setId(
                        "wearer_location_inner"
                    )


                    setCenterPoint(
                        point
                    )


                    setRadius(
                        18.0
                    )


                    setAreaColor(
                        Color.rgb(
                            47,
                            95,
                            227,
                        )
                    )


                    setAreaAlpha(
                        255
                    )


                    setLineColor(
                        Color.rgb(
                            47,
                            95,
                            227,
                        )
                    )


                    setLineAlpha(
                        255
                    )


                    setCircleWidth(
                        1.0f
                    )


                    setRadiusVisible(
                        false
                    )
                }


        mapView
            .addTMapCircle(
                outerCircle
            )


        mapView
            .addTMapCircle(
                innerCircle
            )
    }


    /*
     * =====================================================
     * 안전구역 추가 미리보기
     * =====================================================
     *
     * 중앙 선택 핀을 중심으로 현재 선택 반경을 실제 meter 단위로 표시.
     */
    if (
        previewSafeZoneRadiusMeters != null &&
        previewSafeZoneRadiusMeters > 0 &&
        previewCenterLatitude != null &&
        previewCenterLongitude != null &&
        isValidCoordinate(
            previewCenterLatitude,
            previewCenterLongitude,
        )
    ) {

        val previewCircle =
            TMapCircle()
                .apply {

                    setId(
                        "safe_zone_preview"
                    )


                    setCenterPoint(
                        TMapPoint(
                            previewCenterLatitude,
                            previewCenterLongitude,
                        )
                    )


                    setRadius(
                        previewSafeZoneRadiusMeters
                            .toDouble()
                    )


                    /*
                     * 저장된 안전구역보다 조금 더 진하게 표시해서
                     * 현재 편집 중인 범위임을 구분한다.
                     */
                    setAreaColor(
                        Color.rgb(
                            47,
                            95,
                            227,
                        )
                    )


                    setAreaAlpha(
                        55
                    )


                    setLineColor(
                        Color.rgb(
                            47,
                            95,
                            227,
                        )
                    )


                    setLineAlpha(
                        235
                    )


                    setCircleWidth(
                        3.0f
                    )


                    setRadiusVisible(
                        false
                    )
                }


        mapView
            .addTMapCircle(
                previewCircle
            )
    }
}


private fun isValidCoordinate(
    latitude: Double,
    longitude: Double,
): Boolean =

    latitude in
            -90.0..90.0 &&

            longitude in
            -180.0..180.0 &&

            (
                    latitude != 0.0 ||
                            longitude != 0.0
                    )


private fun dpToPx(
    context: android.content.Context,
    dp: Int,
): Int =

    (
            dp *
                    context
                        .resources
                        .displayMetrics
                        .density
            )
        .toInt()