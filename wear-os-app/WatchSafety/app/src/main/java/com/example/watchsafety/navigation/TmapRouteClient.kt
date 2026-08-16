package com.example.watchsafety.navigation

import com.example.watchsafety.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    ): TmapRouteResult {

        return withContext(
            Dispatchers.IO
        ) {

            if (
                BuildConfig.TMAP_APP_KEY
                    .isBlank()
            ) {

                throw IllegalStateException(
                    "TMAP AppKey가 없습니다. " +
                            "local.properties의 " +
                            "TMAP_APP_KEY를 확인하세요."
                )
            }

            val url =
                URL(
                    "https://apis.openapi.sk.com" +
                            "/tmap/routes/pedestrian" +
                            "?version=1"
                )

            val connection =
                url.openConnection()
                        as HttpURLConnection

            try {

                connection.requestMethod =
                    "POST"

                connection.doOutput =
                    true

                connection.connectTimeout =
                    10_000

                connection.readTimeout =
                    10_000

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

                val body =
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
                    .bufferedWriter()
                    .use { writer ->

                        writer.write(
                            body.toString()
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
                            .bufferedReader()
                            .use {
                                it.readText()
                            }

                    } else {

                        connection
                            .errorStream
                            ?.bufferedReader()
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
                        "TMAP API 요청 실패\n" +
                                "HTTP $responseCode\n" +
                                responseText
                    )
                }

                parseRoute(
                    responseText
                )

            } finally {

                connection.disconnect()
            }
        }
    }

    private fun createRequestBody(
        startLongitude: Double,
        startLatitude: Double,
        endLongitude: Double,
        endLatitude: Double
    ): JSONObject {

        return JSONObject().apply {

            /*
             * TMAP:
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
             * 30
             * =
             * 최단거리 + 계단 제외
             */
            put(
                "searchOption",
                "30"
            )

            /*
             * 노드 순서대로 받음
             */
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

    private fun parseRoute(
        json: String
    ): TmapRouteResult {

        val root =
            JSONObject(json)

        val features =
            root.getJSONArray(
                "features"
            )

        var totalDistance =
            0

        var totalTime =
            0

        val navigationSteps =
            mutableListOf<
                    NavigationStep
                    >()

        for (
        i in
        0 until features.length()
        ) {

            val feature =
                features
                    .getJSONObject(i)

            val geometry =
                feature
                    .getJSONObject(
                        "geometry"
                    )

            /*
             * 방향 안내는 Point 노드에 있음.
             */
            if (
                geometry.optString(
                    "type"
                ) != "Point"
            ) {

                continue
            }

            val properties =
                feature
                    .getJSONObject(
                        "properties"
                    )

            /*
             * 출발점(SP)에
             * 전체 거리와 전체 시간이 들어있다.
             */
            if (
                properties.optString(
                    "pointType"
                ) == "SP"
            ) {

                totalDistance =
                    properties.optInt(
                        "totalDistance",
                        0
                    )

                totalTime =
                    properties.optInt(
                        "totalTime",
                        0
                    )
            }

            val coordinates =
                geometry
                    .getJSONArray(
                        "coordinates"
                    )

            if (
                coordinates.length() <
                2
            ) {

                continue
            }

            navigationSteps.add(

                NavigationStep(

                    index =
                        properties.optInt(
                            "index",
                            i
                        ),

                    turnType =
                        properties.optInt(
                            "turnType",
                            0
                        ),

                    description =
                        properties.optString(
                            "description",
                            ""
                        ),

                    longitude =
                        coordinates
                            .getDouble(0),

                    latitude =
                        coordinates
                            .getDouble(1)
                )
            )
        }

        return TmapRouteResult(

            totalDistanceMeters =
                totalDistance,

            totalTimeSeconds =
                totalTime,

            steps =
                navigationSteps
        )
    }
}