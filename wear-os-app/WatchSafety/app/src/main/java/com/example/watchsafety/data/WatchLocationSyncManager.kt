package com.example.watchsafety.data

import android.location.Location
import android.os.SystemClock
import android.util.Log
import com.example.watchsafety.location.WatchLocation
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class WatchLocationSyncManager {

    private val supabase =
        SupabaseClientProvider.client

    private var lastSyncedLocation:
            WatchLocation? = null

    private var lastSyncedAtMillis:
            Long? = null

    companion object {
        private const val MIN_DISTANCE_METERS = 30f
        private const val MAX_SYNC_INTERVAL_MILLIS = 60_000L
        private const val RPC_TIMEOUT_MILLIS = 15_000L
        private const val TAG = "WatchLocationSync"
    }

    suspend fun syncIfNeeded(
        currentLocation: WatchLocation
    ) {
        Log.d(
            TAG,
            "syncIfNeeded 호출 " +
                    "lat=${currentLocation.latitude}, " +
                    "lng=${currentLocation.longitude}, " +
                    "accuracy=${currentLocation.accuracyMeters}"
        )

        if (currentLocation.latitude !in -90.0..90.0) {
            Log.w(
                TAG,
                "동기화 중단 - 잘못된 위도: ${currentLocation.latitude}"
            )
            return
        }

        if (currentLocation.longitude !in -180.0..180.0) {
            Log.w(
                TAG,
                "동기화 중단 - 잘못된 경도: ${currentLocation.longitude}"
            )
            return
        }

        if (!shouldSync(currentLocation)) {
            Log.d(
                TAG,
                "동기화 생략 - 30m 미만 이동 + 1분 미경과"
            )
            return
        }

        sendLocation(currentLocation)

        lastSyncedLocation =
            currentLocation

        lastSyncedAtMillis =
            SystemClock.elapsedRealtime()

        Log.d(
            TAG,
            "위치 동기화 완료 " +
                    "lat=${currentLocation.latitude}, " +
                    "lng=${currentLocation.longitude}"
        )
    }

    private fun shouldSync(
        currentLocation: WatchLocation
    ): Boolean {

        val previousLocation =
            lastSyncedLocation

        val previousTime =
            lastSyncedAtMillis

        if (
            previousLocation == null ||
            previousTime == null
        ) {
            Log.d(
                TAG,
                "저장 조건 충족 - 앱/서비스 시작 후 첫 위치"
            )
            return true
        }

        val distance =
            calculateDistanceMeters(
                previousLocation,
                currentLocation
            )

        val elapsedMillis =
            SystemClock.elapsedRealtime() -
                    previousTime

        Log.d(
            TAG,
            "저장 조건 검사 " +
                    "distance=${"%.1f".format(distance)}m, " +
                    "elapsed=${elapsedMillis}ms"
        )

        if (distance >= MIN_DISTANCE_METERS) {
            Log.d(
                TAG,
                "저장 조건 충족 - ${distance}m 이동"
            )
            return true
        }

        if (elapsedMillis >= MAX_SYNC_INTERVAL_MILLIS) {
            Log.d(
                TAG,
                "저장 조건 충족 - 1분 경과"
            )
            return true
        }

        return false
    }

    private fun calculateDistanceMeters(
        previous: WatchLocation,
        current: WatchLocation
    ): Float {

        val results =
            FloatArray(1)

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
     * 중요:
     * 이 계층에서는 더 이상 awaitInitialization()이나
     * signInAnonymously()를 호출하지 않는다.
     *
     * WatchTrackingService가 PairingManager.getPairingInfo()로
     * 현재 워치 Auth/페어링을 먼저 확인한 뒤에만
     * syncIfNeeded()를 실행하므로 현재 세션만 사용한다.
     */
    private suspend fun sendLocation(
        location: WatchLocation
    ) {

        Log.d(
            TAG,
            "sendLocation 진입"
        )

        val currentUser =
            supabase
                .auth
                .currentUserOrNull()

        if (currentUser == null) {
            Log.e(
                TAG,
                "위치 저장 실패 - 현재 Supabase Auth 세션 없음"
            )

            error(
                "현재 워치 Supabase Auth 세션이 없습니다."
            )
        }

        val authUserId =
            currentUser.id

        Log.d(
            TAG,
            "현재 워치 Auth 확인 uid=$authUserId"
        )

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

        Log.d(
            TAG,
            "record_watch_location RPC 시작 " +
                    "uid=$authUserId, " +
                    "lat=${location.latitude}, " +
                    "lng=${location.longitude}"
        )

        withTimeout(
            RPC_TIMEOUT_MILLIS
        ) {
            supabase
                .postgrest
                .rpc(
                    function =
                        "record_watch_location",
                    parameters =
                        parameters
                )
        }

        Log.d(
            TAG,
            "record_watch_location RPC 응답 성공 uid=$authUserId"
        )
    }

    suspend fun forceSync(
        location: WatchLocation
    ) {

        Log.d(
            TAG,
            "forceSync 호출 " +
                    "lat=${location.latitude}, " +
                    "lng=${location.longitude}"
        )

        sendLocation(
            location
        )

        lastSyncedLocation =
            location

        lastSyncedAtMillis =
            SystemClock.elapsedRealtime()

        Log.d(
            TAG,
            "위치 강제 동기화 완료"
        )
    }

    fun reset() {

        lastSyncedLocation =
            null

        lastSyncedAtMillis =
            null

        Log.d(
            TAG,
            "위치 동기화 상태 초기화"
        )
    }
}