package com.example.watchsafety.navigation

import com.example.watchsafety.BuildConfig

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.json.JSONArray
import org.json.JSONObject

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


class TmapRouteClient {


    suspend fun getPedestrianRoute(

        startLongitude: Double,

        startLatitude: Double,

        endLongitude: Double,

        endLatitude: Double

    ): TmapRouteResult =

        withContext(
            Dispatchers.IO
        ) {

            require(
                BuildConfig
                    .TMAP_APP_KEY
                    .isNotBlank()
            ) {

                "TMAP AppKey가 없습니다. " +
                        "local.properties의 TMAP_APP_KEY를 확인하세요."
            }


            val url =
                URL(
                    ENDPOINT
                )


            val connection =
                url
                    .openConnection()
                        as HttpURLConnection


            try {

                connection.requestMethod =
                    "POST"


                connection.doOutput =
                    true


                connection.connectTimeout =
                    CONNECT_TIMEOUT_MILLIS


                connection.readTimeout =
                    READ_TIMEOUT_MILLIS


                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )


                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )


                connection.setRequestProperty(
                    "appKey",
                    BuildConfig.TMAP_APP_KEY
                )


                val requestBody =
                    createRequestBody(

                        startLongitude =
                            startLongitude,

                        startLatitude =
                            startLatitude,

                        endLongitude =
                            endLongitude,

                        endLatitude =
                            endLatitude
                    )


                connection
                    .outputStream
                    .bufferedWriter(
                        Charsets.UTF_8
                    )
                    .use { writer ->

                        writer.write(
                            requestBody.toString()
                        )


                        writer.flush()
                    }


                val responseCode =
                    connection.responseCode


                val responseText =

                    if (
                        responseCode in
                        200..299
                    ) {

                        connection
                            .inputStream
                            .bufferedReader(
                                Charsets.UTF_8
                            )
                            .use {

                                it.readText()
                            }

                    } else {

                        connection
                            .errorStream
                            ?.bufferedReader(
                                Charsets.UTF_8
                            )
                            ?.use {

                                it.readText()
                            }
                            ?: "응답 내용 없음"
                    }


                if (
                    responseCode !in
                    200..299
                ) {

                    throw IllegalStateException(

                        "TMAP API 요청 실패 " +
                                "(HTTP $responseCode)\n" +
                                responseText
                                    .take(
                                        500
                                    )
                    )
                }


                parseRoute(
                    responseText
                )

            } finally {

                connection.disconnect()
            }
        }


    private fun createRequestBody(

        startLongitude: Double,

        startLatitude: Double,

        endLongitude: Double,

        endLatitude: Double

    ): JSONObject {

        return JSONObject()
            .apply {

                /*
                 * TMAP
                 *
                 * X = 경도
                 * Y = 위도
                 */
                put(
                    "startX",
                    startLongitude
                )


                put(
                    "startY",
                    startLatitude
                )


                put(
                    "endX",
                    endLongitude
                )


                put(
                    "endY",
                    endLatitude
                )


                put(
                    "startName",
                    encodeName(
                        "현재 위치"
                    )
                )


                put(
                    "endName",
                    encodeName(
                        "집"
                    )
                )


                put(
                    "reqCoordType",
                    "WGS84GEO"
                )


                put(
                    "resCoordType",
                    "WGS84GEO"
                )


                /*
                 * 기존 프로젝트에서 사용하던 옵션 유지.
                 */
                put(
                    "searchOption",
                    "30"
                )


                put(
                    "sort",
                    "index"
                )
            }
    }


    private fun encodeName(
        value: String
    ): String {

        return URLEncoder.encode(
            value,
            Charsets.UTF_8.name()
        )
    }


    /*
     * =====================================================
     * TMAP GeoJSON 파싱
     * =====================================================
     *
     * Point:
     *   회전/안내 지점 → NavigationStep
     *
     * LineString:
     *   실제 도로 경로 → RoutePoint
     *
     * 기존에는 LineString을 버렸기 때문에
     * 지도에 실제 경로선을 그릴 수 없었다.
     * =====================================================
     */
    private fun parseRoute(
        json: String
    ): TmapRouteResult {

        val root =
            JSONObject(
                json
            )


        val features =
            root.getJSONArray(
                "features"
            )


        var totalDistance =
            0


        var totalTime =
            0


        val navigationSteps =
            mutableListOf<NavigationStep>()


        val routePoints =
            mutableListOf<RoutePoint>()


        for (
        i in
        0 until features.length()
        ) {

            val feature =
                features.getJSONObject(
                    i
                )


            val geometry =
                feature.getJSONObject(
                    "geometry"
                )


            val geometryType =
                geometry.optString(
                    "type"
                )


            val properties =
                feature.optJSONObject(
                    "properties"
                )
                    ?: JSONObject()


            /*
             * 전체 거리/시간은 출발 Point에 주로 들어오지만
             * 응답 형태 변화에 대비해 값이 있으면 확보한다.
             */
            if (
                totalDistance <=
                0
            ) {

                totalDistance =
                    properties.optInt(
                        "totalDistance",
                        0
                    )
            }


            if (
                totalTime <=
                0
            ) {

                totalTime =
                    properties.optInt(
                        "totalTime",
                        0
                    )
            }


            when (
                geometryType
            ) {

                "Point" -> {

                    parseNavigationPoint(
                        featureIndex =
                            i,

                        geometry =
                            geometry,

                        properties =
                            properties,

                        target =
                            navigationSteps
                    )
                }


                "LineString" -> {

                    parseLineString(
                        geometry =
                            geometry,

                        target =
                            routePoints
                    )
                }
            }
        }


        /*
         * 일부 응답에서 LineString이 누락되더라도
         * 최소한 Point들을 이용해 지도에 대략적인 경로를
         * 표시할 수 있도록 fallback.
         */
        if (
            routePoints.size <
            2
        ) {

            navigationSteps
                .sortedBy {

                    it.index
                }
                .forEach { step ->

                    addRoutePointIfNeeded(

                        target =
                            routePoints,

                        point =
                            RoutePoint(

                                longitude =
                                    step.longitude,

                                latitude =
                                    step.latitude
                            )
                    )
                }
        }


        return TmapRouteResult(

            totalDistanceMeters =
                totalDistance,

            totalTimeSeconds =
                totalTime,

            steps =
                navigationSteps
                    .sortedBy {

                        it.index
                    },

            routePoints =
                routePoints
        )
    }


    private fun parseNavigationPoint(

        featureIndex: Int,

        geometry: JSONObject,

        properties: JSONObject,

        target: MutableList<NavigationStep>

    ) {

        val coordinates =
            geometry.optJSONArray(
                "coordinates"
            )
                ?: return


        if (
            coordinates.length() <
            2
        ) {

            return
        }


        val longitude =
            coordinates.optDouble(
                0,
                Double.NaN
            )


        val latitude =
            coordinates.optDouble(
                1,
                Double.NaN
            )


        if (
            !longitude.isFinite() ||
            !latitude.isFinite()
        ) {

            return
        }


        val index =
            properties.optInt(
                "index",
                featureIndex
            )


        val turnType =
            properties.optInt(
                "turnType",
                0
            )


        val description =

            properties.optString(
                "description",
                ""
            )
                .ifBlank {

                    properties.optString(
                        "name",
                        ""
                    )
                }


        target.add(

            NavigationStep(

                index =
                    index,

                turnType =
                    turnType,

                description =
                    description,

                longitude =
                    longitude,

                latitude =
                    latitude
            )
        )
    }


    private fun parseLineString(

        geometry: JSONObject,

        target: MutableList<RoutePoint>

    ) {

        val coordinates =
            geometry.optJSONArray(
                "coordinates"
            )
                ?: return


        for (
        index in
        0 until coordinates.length()
        ) {

            val coordinate =
                coordinates.optJSONArray(
                    index
                )
                    ?: continue


            if (
                coordinate.length() <
                2
            ) {

                continue
            }


            val longitude =
                coordinate.optDouble(
                    0,
                    Double.NaN
                )


            val latitude =
                coordinate.optDouble(
                    1,
                    Double.NaN
                )


            if (
                !longitude.isFinite() ||
                !latitude.isFinite()
            ) {

                continue
            }


            addRoutePointIfNeeded(

                target =
                    target,

                point =
                    RoutePoint(

                        longitude =
                            longitude,

                        latitude =
                            latitude
                    )
            )
        }
    }


    /*
     * LineString Feature 사이의 경계 좌표가
     * 같은 경우가 있으므로 연속 중복만 제거한다.
     */
    private fun addRoutePointIfNeeded(

        target: MutableList<RoutePoint>,

        point: RoutePoint

    ) {

        val last =
            target.lastOrNull()


        if (
            last != null &&
            last.latitude ==
            point.latitude &&
            last.longitude ==
            point.longitude
        ) {

            return
        }


        target.add(
            point
        )
    }


    companion object {

        private const val ENDPOINT =
            "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1"


        private const val CONNECT_TIMEOUT_MILLIS =
            10_000


        private const val READ_TIMEOUT_MILLIS =
            10_000
    }
}
