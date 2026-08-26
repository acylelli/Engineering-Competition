package com.example.watchsafety.navigation


/*
 * =========================================================
 * TMAP 경로 전체 결과
 * =========================================================
 *
 * steps:
 *   방향 안내용 Point 노드
 *
 * routePoints:
 *   지도에 실제 보행 경로선을 그리기 위한 LineString 좌표
 */
data class TmapRouteResult(

    val totalDistanceMeters: Int,

    val totalTimeSeconds: Int,

    val steps: List<NavigationStep>,

    val routePoints: List<RoutePoint> =
        emptyList()
)


/*
 * =========================================================
 * TMAP 방향 안내 Point
 * =========================================================
 */
data class NavigationStep(

    val index: Int,

    val turnType: Int,

    val description: String,

    val longitude: Double,

    val latitude: Double
)


/*
 * =========================================================
 * 지도 경로선용 좌표
 *
 * TMAP GeoJSON LineString의
 * [longitude, latitude] 좌표를 보존한다.
 * =========================================================
 */
data class RoutePoint(

    val longitude: Double,

    val latitude: Double
)
