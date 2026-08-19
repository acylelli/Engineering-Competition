package com.example.watchsafety.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HomeSafeZoneResponse(
    @SerialName("is_configured")
    val isConfigured: Boolean,

    @SerialName("center_latitude")
    val centerLatitude: Double? = null,

    @SerialName("center_longitude")
    val centerLongitude: Double? = null,

    @SerialName("radius_meters")
    val radiusMeters: Double? = null
)

class HomeSafeZoneManager {
    private val supabase = SupabaseClientProvider.client

    suspend fun getHomeSafeZone(): HomeSafeZoneResponse {
        return supabase.postgrest
            .rpc(function = "get_watch_home_safe_zone")
            .decodeSingle<HomeSafeZoneResponse>()
    }
}
