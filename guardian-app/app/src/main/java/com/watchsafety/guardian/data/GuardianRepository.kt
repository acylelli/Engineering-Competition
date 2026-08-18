package com.watchsafety.guardian.data

import com.watchsafety.guardian.domain.model.GuardianSnapshot
import com.watchsafety.guardian.domain.model.NotificationSettings
import kotlinx.coroutines.flow.StateFlow

interface GuardianRepository {

    val snapshot: StateFlow<GuardianSnapshot>

    suspend fun refreshStatus()

    suspend fun sendReturnHomeRequest()

    suspend fun setSafeZoneEnabled(
        zoneId: String,
        enabled: Boolean,
    )

    suspend fun addSafeZone(
        name: String,
        radiusMeters: Int,
    )

    suspend fun updateNotificationSettings(
        settings: NotificationSettings,
    )

    /*
     * 워치에 표시된 6자리 코드를 사용해
     * 현재 보호자와 워치를 연결
     */
    suspend fun redeemPairingCode(
        code: String,
    )
}