package com.example.watchsafety.pairing

import android.util.Log
import com.example.watchsafety.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PairingCodeResponse(
    val code: String,
    @SerialName("expires_at")
    val expiresAt: String
)

@Serializable
data class PairingStatusResponse(
    @SerialName("is_paired")
    val isPaired: Boolean
)

@Serializable
data class PairingInfoResponse(
    @SerialName("is_paired")
    val isPaired: Boolean,
    @SerialName("guardian_id")
    val guardianId: String? = null,
    @SerialName("wearer_id")
    val wearerId: String? = null
)

class PairingManager {

    private val supabase =
        SupabaseClientProvider.client


    companion object {
        private const val TAG =
            "PairingManager"
    }


    /*
     * =====================================================
     * 워치 Auth 세션 보장
     * =====================================================
     *
     * 저장된 익명 세션 복원이 끝난 뒤 확인해야
     * 기존 페어링의 watch_auth_id가 유지된다.
     */
    suspend fun ensureAuthenticated() {

        supabase
            .auth
            .awaitInitialization()


        val existingSession =
            supabase
                .auth
                .currentSessionOrNull()


        if (
            existingSession != null
        ) {

            Log.d(
                TAG,
                "기존 워치 Auth 세션 사용 uid=${existingSession.user?.id}"
            )

            return
        }


        Log.w(
            TAG,
            "기존 워치 Auth 세션 없음 → 새 익명 Auth 생성"
        )


        supabase
            .auth
            .signInAnonymously()


        Log.d(
            TAG,
            "새 워치 Auth 생성 uid=${supabase.auth.currentUserOrNull()?.id}"
        )
    }


    suspend fun createPairingCode():
            PairingCodeResponse {

        ensureAuthenticated()


        Log.d(
            TAG,
            "페어링 코드 생성 요청 uid=${getWatchAuthId()}"
        )


        return supabase
            .postgrest
            .rpc(
                function =
                    "create_pairing_code"
            )
            .decodeSingle<
                    PairingCodeResponse
                    >()
    }


    suspend fun isPaired():
            Boolean {

        ensureAuthenticated()


        val response =
            supabase
                .postgrest
                .rpc(
                    function =
                        "get_watch_pairing_status"
                )
                .decodeSingle<
                        PairingStatusResponse
                        >()


        Log.d(
            TAG,
            "페어링 상태 uid=${getWatchAuthId()}, isPaired=${response.isPaired}"
        )


        return response
            .isPaired
    }


    suspend fun getPairingInfo():
            PairingInfoResponse {

        ensureAuthenticated()


        val response =
            supabase
                .postgrest
                .rpc(
                    function =
                        "get_watch_pairing_info"
                )
                .decodeSingle<
                        PairingInfoResponse
                        >()


        Log.d(
            TAG,
            "페어링 정보 uid=${getWatchAuthId()}, " +
                    "isPaired=${response.isPaired}, " +
                    "guardianId=${response.guardianId}, " +
                    "wearerId=${response.wearerId}"
        )


        return response
    }


    suspend fun getWatchAuthId():
            String? {

        supabase
            .auth
            .awaitInitialization()


        return supabase
            .auth
            .currentUserOrNull()
            ?.id
    }
}