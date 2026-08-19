package com.example.watchsafety.pairing

import com.example.watchsafety.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PairingCodeResponse(
    val code: String,
    @SerialName("expires_at") val expiresAt: String
)

@Serializable
data class PairingStatusResponse(
    @SerialName("is_paired") val isPaired: Boolean
)

@Serializable
data class PairingInfoResponse(
    @SerialName("is_paired") val isPaired: Boolean,
    @SerialName("guardian_id") val guardianId: String? = null,
    @SerialName("wearer_id") val wearerId: String? = null
)

class PairingManager {
    private val supabase = SupabaseClientProvider.client

    suspend fun ensureAuthenticated() {
        if (supabase.auth.currentSessionOrNull() == null) {
            supabase.auth.signInAnonymously()
        }
    }

    suspend fun createPairingCode(): PairingCodeResponse {
        ensureAuthenticated()
        return supabase.postgrest
            .rpc(function = "create_pairing_code")
            .decodeSingle<PairingCodeResponse>()
    }

    suspend fun isPaired(): Boolean {
        ensureAuthenticated()
        return supabase.postgrest
            .rpc(function = "get_watch_pairing_status")
            .decodeSingle<PairingStatusResponse>()
            .isPaired
    }

    suspend fun getPairingInfo(): PairingInfoResponse {
        ensureAuthenticated()
        return supabase.postgrest
            .rpc(function = "get_watch_pairing_info")
            .decodeSingle<PairingInfoResponse>()
    }

    suspend fun getWatchAuthId(): String? {
        ensureAuthenticated()
        return supabase.auth.currentUserOrNull()?.id
    }
}
