package com.watchsafety.guardian.ui.safezone

import com.watchsafety.guardian.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

/*
 * =========================================================
 * TMAP POI 검색 결과
 * =========================================================
 */
data class TmapPoiSearchResult(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
)

/*
 * =========================================================
 * TMAP 장소(POI) 통합 검색
 * =========================================================
 *
 * GET https://apis.openapi.sk.com/tmap/pois
 *
 * 검색 결과는 안전구역 중심을 고르기 위해 일시적으로만 사용한다.
 */
class TmapPoiSearchClient {

    companion object {

        private const val ENDPOINT =
            "https://apis.openapi.sk.com/tmap/pois"

        private const val CONNECT_TIMEOUT_MILLIS =
            8_000

        private const val READ_TIMEOUT_MILLIS =
            8_000

        private const val RESULT_COUNT =
            5
    }

    /*
     * =====================================================
     * 장소 검색
     * =====================================================
     */
    suspend fun search(
        keyword: String
    ): List<TmapPoiSearchResult> =
        withContext(
            Dispatchers.IO
        ) {

            val trimmedKeyword =
                keyword.trim()

            if (
                trimmedKeyword.isBlank()
            ) {
                return@withContext emptyList<TmapPoiSearchResult>()
            }

            require(
                BuildConfig.TMAP_APP_KEY.isNotBlank()
            ) {
                "TMAP_APP_KEY가 설정되지 않았습니다."
            }

            val encodedKeyword =
                URLEncoder.encode(
                    trimmedKeyword,
                    Charsets.UTF_8.name()
                )

            val requestUrl =
                "$ENDPOINT" +
                        "?version=1" +
                        "&searchKeyword=$encodedKeyword" +
                        "&searchType=all" +
                        "&searchtypCd=A" +
                        "&reqCoordType=WGS84GEO" +
                        "&resCoordType=WGS84GEO" +
                        "&page=1" +
                        "&count=$RESULT_COUNT" +
                        "&multiPoint=Y" +
                        "&poiGroupYn=N"

            val connection =
                (
                        URL(requestUrl)
                            .openConnection()
                                as HttpURLConnection
                        )
                    .apply {

                        requestMethod =
                            "GET"

                        connectTimeout =
                            CONNECT_TIMEOUT_MILLIS

                        readTimeout =
                            READ_TIMEOUT_MILLIS

                        setRequestProperty(
                            "Accept",
                            "application/json"
                        )

                        setRequestProperty(
                            "appKey",
                            BuildConfig.TMAP_APP_KEY
                        )

                        doInput =
                            true
                    }

            val results =
                try {

                    val responseCode =
                        connection.responseCode

                    val inputStream =
                        if (
                            responseCode in 200..299
                        ) {
                            connection.inputStream
                        } else {
                            connection.errorStream
                        }

                    val body =
                        inputStream
                            ?.bufferedReader(
                                Charsets.UTF_8
                            )
                            ?.use(
                                BufferedReader::readText
                            )
                            .orEmpty()

                    if (
                        responseCode !in 200..299
                    ) {

                        val shortBody =
                            body
                                .replace(
                                    "\n",
                                    " "
                                )
                                .take(
                                    300
                                )

                        throw IllegalStateException(
                            "TMAP 장소 검색 실패 " +
                                    "(HTTP $responseCode) " +
                                    shortBody
                        )
                    }

                    parseResults(
                        body
                    )

                } finally {

                    connection.disconnect()
                }

            return@withContext results
        }

    /*
     * =====================================================
     * JSON 파싱
     * =====================================================
     */
    private fun parseResults(
        body: String
    ): List<TmapPoiSearchResult> {

        if (
            body.isBlank()
        ) {
            return emptyList()
        }

        val root =
            JSONObject(
                body
            )

        val searchPoiInfo =
            root.optJSONObject(
                "searchPoiInfo"
            )
                ?: return emptyList()

        val pois =
            searchPoiInfo
                .optJSONObject(
                    "pois"
                )
                ?.optJSONArray(
                    "poi"
                )
                ?: return emptyList()

        val results =
            mutableListOf<TmapPoiSearchResult>()

        for (
        index in 0 until pois.length()
        ) {

            val poi =
                pois.optJSONObject(
                    index
                )
                    ?: continue

            val name =
                poi
                    .optString(
                        "name"
                    )
                    .trim()

            if (
                name.isBlank()
            ) {
                continue
            }

            /*
             * 보행자 출입구 좌표를 가장 먼저 사용하고,
             * 없으면 POI 중심/정면 좌표 순서로 사용.
             */
            val coordinate =
                firstValidCoordinate(
                    poi = poi,
                    candidates =
                        listOf(
                            "pnsLat" to "pnsLon",
                            "noorLat" to "noorLon",
                            "frontLat" to "frontLon",
                        )
                )
                    ?: continue

            results +=
                TmapPoiSearchResult(
                    name = name,
                    address =
                        buildAddress(
                            poi
                        ),
                    latitude =
                        coordinate.first,
                    longitude =
                        coordinate.second,
                )
        }

        return results
            .distinctBy {
                Triple(
                    it.name,
                    it.latitude,
                    it.longitude
                )
            }
            .take(
                RESULT_COUNT
            )
    }

    /*
     * =====================================================
     * 사용 가능한 좌표 선택
     * =====================================================
     *
     * forEach 내부 non-local return을 사용하지 않고
     * 일반 for 문으로 작성해서 반환 타입 추론 문제를 피한다.
     */
    private fun firstValidCoordinate(
        poi: JSONObject,
        candidates: List<Pair<String, String>>,
    ): Pair<Double, Double>? {

        for (
        candidate in candidates
        ) {

            val latitudeKey =
                candidate.first

            val longitudeKey =
                candidate.second

            val latitude =
                poi
                    .optString(
                        latitudeKey
                    )
                    .toDoubleOrNull()

            val longitude =
                poi
                    .optString(
                        longitudeKey
                    )
                    .toDoubleOrNull()

            if (
                latitude != null &&
                longitude != null &&
                latitude in -90.0..90.0 &&
                longitude in -180.0..180.0 &&
                (
                        latitude != 0.0 ||
                                longitude != 0.0
                        )
            ) {
                return latitude to longitude
            }
        }

        return null
    }

    /*
     * =====================================================
     * 주소 만들기
     * =====================================================
     *
     * 도로명 주소가 있으면 우선 사용하고,
     * 없으면 지번/행정구역 주소를 사용한다.
     */
    private fun buildAddress(
        poi: JSONObject
    ): String {

        val upper =
            poi
                .optString(
                    "upperAddrName"
                )
                .trim()

        val middle =
            poi
                .optString(
                    "middleAddrName"
                )
                .trim()

        val lower =
            poi
                .optString(
                    "lowerAddrName"
                )
                .trim()

        val road =
            poi
                .optString(
                    "roadName"
                )
                .trim()

        val firstBuildingNo =
            poi
                .optString(
                    "firstBuildNo"
                )
                .trim()

        val secondBuildingNo =
            poi
                .optString(
                    "secondBuildNo"
                )
                .trim()

        val detail =
            poi
                .optString(
                    "detailAddrName"
                )
                .trim()

        val roadBuildingNumber =
            when {

                firstBuildingNo.isBlank() ->
                    ""

                secondBuildingNo.isBlank() ||
                        secondBuildingNo == "0" ->
                    firstBuildingNo

                else ->
                    "$firstBuildingNo-$secondBuildingNo"
            }

        val roadAddress =
            listOf(
                upper,
                middle,
                road,
                roadBuildingNumber,
            )
                .filter {
                    it.isNotBlank()
                }
                .joinToString(
                    " "
                )

        if (
            road.isNotBlank() &&
            roadAddress.isNotBlank()
        ) {
            return roadAddress
        }

        val lotAddress =
            listOf(
                upper,
                middle,
                lower,
                detail,
            )
                .filter {
                    it.isNotBlank()
                }
                .joinToString(
                    " "
                )

        return lotAddress
    }
}