package com.watchsafety.guardian.data

import com.watchsafety.guardian.domain.model.GuardianSnapshot
import com.watchsafety.guardian.domain.model.NotificationSettings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow


interface GuardianRepository {

    /*
     * =====================================================
     * 전체 보호자 앱 상태
     * =====================================================
     */

    val snapshot:
            StateFlow<GuardianSnapshot>


    /*
     * =====================================================
     * 새 SOS 실시간 이벤트
     * =====================================================
     *
     * Supabase Repository에서는
     * 워치에서 새로운 SOS가 발생했을 때 emit한다.
     *
     * Mock Repository는 별도 구현하지 않아도
     * 기본 emptyFlow()를 사용한다.
     */

    val newSosEvent:
            Flow<Unit>
        get() = emptyFlow()


    /*
     * =====================================================
     * 상태 새로고침
     * =====================================================
     */

    suspend fun refreshStatus()


    /*
     * =====================================================
     * 귀가 요청
     * =====================================================
     */

    suspend fun sendReturnHomeRequest()


    /*
     * =====================================================
     * 안전구역 활성화
     * =====================================================
     */

    suspend fun setSafeZoneEnabled(
        zoneId: String,
        enabled: Boolean,
    )


    /*
     * =====================================================
     * 안전구역 추가
     * =====================================================
     */

    suspend fun addSafeZone(
        name: String,
        radiusMeters: Int,
    )


    /*
     * =====================================================
     * 알림 설정
     * =====================================================
     */

    suspend fun updateNotificationSettings(
        settings: NotificationSettings,
    )


    /*
     * =====================================================
     * 워치 연결
     * =====================================================
     *
     * 워치에 표시된 6자리 코드를 사용해
     * 현재 보호자와 워치를 연결
     */

    suspend fun redeemPairingCode(
        code: String,
    )
}