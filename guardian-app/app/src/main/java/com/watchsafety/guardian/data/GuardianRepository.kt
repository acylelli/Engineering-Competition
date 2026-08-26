package com.watchsafety.guardian.data

import com.watchsafety.guardian.domain.model.GuardianSnapshot
import com.watchsafety.guardian.domain.model.NotificationSettings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


interface GuardianRepository {

    val snapshot:
            StateFlow<GuardianSnapshot>


    val newSosEvent:
            Flow<Unit>


    suspend fun refreshStatus()


    suspend fun sendReturnHomeRequest()


    suspend fun setSafeZoneEnabled(
        zoneId: String,
        enabled: Boolean,
    )


    /*
     * =====================================================
     * 안전구역 추가
     * =====================================================
     *
     * isHome = true
     * → HOME
     *
     * isHome = false
     * → OTHER
     */
    suspend fun addSafeZone(
        name: String,
        radiusMeters: Int,
        latitude: Double,
        longitude: Double,
        isHome: Boolean,
    )

    suspend fun updateSafeZone(
        zoneId: String,
        name: String,
        radiusMeters: Int,
        latitude: Double,
        longitude: Double,
        isHome: Boolean,
    )

    suspend fun deleteSafeZone(
        zoneId: String,
    )


    suspend fun updateWearerName(
        name: String,
    )


    suspend fun updateNotificationSettings(
        settings: NotificationSettings,
    )


    suspend fun redeemPairingCode(
        code: String,
    )
}
