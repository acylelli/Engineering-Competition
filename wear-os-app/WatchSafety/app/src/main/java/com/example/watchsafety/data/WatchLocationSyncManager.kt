package com.example.watchsafety.data

import android.location.Location
import android.os.SystemClock
import android.util.Log

import com.example.watchsafety.location.WatchLocation

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive


class WatchLocationSyncManager {

    private val supabase =
        SupabaseClientProvider.client


    /*
     * =====================================================
     * 마지막으로 DB에 저장한 위치
     * =====================================================
     */

    private var lastSyncedLocation:
            WatchLocation? = null


    /*
     * 마지막 저장 시각
     *
     * System.currentTimeMillis() 대신
     * elapsedRealtime 사용.
     */
    private var lastSyncedAtMillis:
            Long? = null


    /*
     * =====================================================
     * 위치 저장 기준
     * =====================================================
     */

    companion object {

        /*
         * 30m 이상 이동하면 저장
         */
        private const val MIN_DISTANCE_METERS =
            30f


        /*
         * 이동하지 않아도
         * 1분에 한 번 저장
         */
        private const val MAX_SYNC_INTERVAL_MILLIS =
            60_000L


        private const val TAG =
            "WatchLocationSync"
    }


    /*
     * =====================================================
     * 현재 위치를 서버에 저장할지 판단
     * =====================================================
     */

    suspend fun syncIfNeeded(
        currentLocation: WatchLocation
    ) {

        /*
         * 위도 / 경도 자체 검증
         */
        if (
            currentLocation.latitude !in
            -90.0..90.0
        ) {
            return
        }


        if (
            currentLocation.longitude !in
            -180.0..180.0
        ) {
            return
        }


        /*
         * 저장할 필요가 없으면 종료
         */
        if (
            !shouldSync(
                currentLocation
            )
        ) {

            return
        }


        /*
         * Supabase 저장
         */
        sendLocation(
            currentLocation
        )


        /*
         * 실제 RPC 성공 후에만
         * 마지막 위치/시간 업데이트
         */
        lastSyncedLocation =
            currentLocation


        lastSyncedAtMillis =
            SystemClock
                .elapsedRealtime()


        Log.d(
            TAG,
            """
            위치 동기화 성공
            lat=${currentLocation.latitude}
            lon=${currentLocation.longitude}
            accuracy=${currentLocation.accuracyMeters}
            """.trimIndent()
        )
    }


    /*
     * =====================================================
     * 저장 여부 결정
     * =====================================================
     */

    private fun shouldSync(
        currentLocation: WatchLocation
    ): Boolean {

        val previousLocation =
            lastSyncedLocation


        val previousTime =
            lastSyncedAtMillis


        /*
         * -------------------------------------------------
         * 앱 실행 후 첫 위치
         * -------------------------------------------------
         */

        if (
            previousLocation == null ||
            previousTime == null
        ) {

            return true
        }


        /*
         * -------------------------------------------------
         * 이전 저장 위치와 거리 계산
         * -------------------------------------------------
         */

        val distance =
            calculateDistanceMeters(
                previousLocation,
                currentLocation
            )


        /*
         * -------------------------------------------------
         * 마지막 저장 후 시간
         * -------------------------------------------------
         */

        val elapsedMillis =
            SystemClock
                .elapsedRealtime() -
                    previousTime


        /*
         * -------------------------------------------------
         * 30m 이상 이동
         * -------------------------------------------------
         */

        if (
            distance >=
            MIN_DISTANCE_METERS
        ) {

            return true
        }


        /*
         * -------------------------------------------------
         * 많이 이동하지 않았어도
         * 1분 이상 지났으면 저장
         * -------------------------------------------------
         */

        if (
            elapsedMillis >=
            MAX_SYNC_INTERVAL_MILLIS
        ) {

            return true
        }


        return false
    }


    /*
     * =====================================================
     * 두 좌표 사이 거리 계산
     * =====================================================
     */

    private fun calculateDistanceMeters(
        previous: WatchLocation,
        current: WatchLocation
    ): Float {

        val results =
            FloatArray(
                1
            )


        Location.distanceBetween(

            previous.latitude,
            previous.longitude,

            current.latitude,
            current.longitude,

            results
        )


        return results[0]
    }


    /*
     * =====================================================
     * Supabase 전송
     * =====================================================
     */

    private suspend fun sendLocation(
        location: WatchLocation
    ) {

        /*
         * -------------------------------------------------
         * 익명 Auth 세션 확인
         * -------------------------------------------------
         */

        if (
            supabase
                .auth
                .currentUserOrNull() == null
        ) {

            supabase
                .auth
                .signInAnonymously()
        }


        /*
         * -------------------------------------------------
         * RPC 파라미터
         * -------------------------------------------------
         */

        val parameters =
            JsonObject(

                mapOf(

                    "p_latitude" to
                            JsonPrimitive(
                                location.latitude
                            ),

                    "p_longitude" to
                            JsonPrimitive(
                                location.longitude
                            ),

                    "p_accuracy_meters" to
                            JsonPrimitive(
                                location
                                    .accuracyMeters
                                    .toDouble()
                            )
                )
            )


        /*
         * -------------------------------------------------
         * RPC 실행
         * -------------------------------------------------
         */

        supabase
            .postgrest
            .rpc(

                function =
                    "record_watch_location",

                parameters =
                    parameters
            )
    }


    /*
     * =====================================================
     * 강제 위치 저장
     * =====================================================
     *
     * 나중에:
     *
     * - SOS
     * - 낙상
     * - 안전구역 이탈
     *
     * 발생 시 거리/시간 조건과 상관없이 사용.
     */

    suspend fun forceSync(
        location: WatchLocation
    ) {

        sendLocation(
            location
        )


        lastSyncedLocation =
            location


        lastSyncedAtMillis =
            SystemClock
                .elapsedRealtime()


        Log.d(
            TAG,
            "위치 강제 동기화 완료"
        )
    }


    /*
     * =====================================================
     * 상태 초기화
     * =====================================================
     */

    fun reset() {

        lastSyncedLocation =
            null

        lastSyncedAtMillis =
            null
    }
}