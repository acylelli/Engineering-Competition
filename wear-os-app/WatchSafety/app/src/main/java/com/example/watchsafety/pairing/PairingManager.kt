package com.example.watchsafety.pairing

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

class PairingManager {

    private val supabase =
        SupabaseClientProvider.client

    /*
     * 워치 내부 익명 인증.
     *
     * 기존 세션이 있으면 그대로 사용하고,
     * 없을 때만 새로운 익명 계정을 만든다.
     */
    suspend fun ensureAuthenticated() {

        if (
            supabase.auth
                .currentSessionOrNull() == null
        ) {

            supabase.auth
                .signInAnonymously()
        }
    }

    /*
     * 10분짜리 6자리 코드 요청
     */
    suspend fun createPairingCode():
            PairingCodeResponse {

        ensureAuthenticated()

        return supabase
            .postgrest
            .rpc(
                function = "create_pairing_code"
            )
            .decodeSingle<
                    PairingCodeResponse
                    >()
    }

    /*
     * 보호자가 코드 입력을 끝냈는지 확인
     */
    suspend fun isPaired(): Boolean {

        ensureAuthenticated()

        return supabase
            .postgrest
            .rpc(
                function =
                    "get_watch_pairing_status"
            )
            .decodeSingle<
                    PairingStatusResponse
                    >()
            .isPaired
    }

    /*
     * 디버그용.
     *
     * 지금 워치에 발급된 Supabase Auth UUID 확인 가능.
     */
    suspend fun getWatchAuthId(): String? {

        ensureAuthenticated()

        return supabase.auth
            .currentUserOrNull()
            ?.id
    }
}