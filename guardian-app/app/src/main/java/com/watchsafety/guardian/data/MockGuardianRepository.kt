package com.watchsafety.guardian.data

import com.watchsafety.guardian.domain.model.EmergencyDetail
import com.watchsafety.guardian.domain.model.EmergencyTimelineItem
import com.watchsafety.guardian.domain.model.EventDayGroup
import com.watchsafety.guardian.domain.model.GuardianSnapshot
import com.watchsafety.guardian.domain.model.GuardianUser
import com.watchsafety.guardian.domain.model.LocationInfo
import com.watchsafety.guardian.domain.model.NotificationSettings
import com.watchsafety.guardian.domain.model.SafeZone
import com.watchsafety.guardian.domain.model.SafeZoneKind
import com.watchsafety.guardian.domain.model.SafetyEvent
import com.watchsafety.guardian.domain.model.SafetyEventType
import com.watchsafety.guardian.domain.model.WatchStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockGuardianRepository : GuardianRepository {
    private val _snapshot = MutableStateFlow(createInitialSnapshot())
    override val snapshot: StateFlow<GuardianSnapshot> = _snapshot.asStateFlow()

    override suspend fun refreshStatus() {
        delay(350)
        _snapshot.value = _snapshot.value.copy(
            location = _snapshot.value.location.copy(lastUpdatedLabel = "방금 전"),
            watchStatus = _snapshot.value.watchStatus.copy(lastConnectedLabel = "방금 전"),
        )
    }

    override suspend fun sendReturnHomeRequest() {
        val current = _snapshot.value
        if (current.returnHomeRequested) return
        _snapshot.value = current.copy(
            returnHomeRequested = true,
            events = listOf(
                SafetyEvent(
                    id = "return-home-new",
                    title = "귀가 안내 요청",
                    description = "보호자가 집으로 안내를 요청했어요",
                    timeLabel = "방금 전",
                    dayGroup = EventDayGroup.TODAY,
                    type = SafetyEventType.RETURN_HOME_REQUESTED,
                ),
            ) + current.events,
        )
    }

    override suspend fun setSafeZoneEnabled(zoneId: String, enabled: Boolean) {
        _snapshot.value = _snapshot.value.copy(
            safeZones = _snapshot.value.safeZones.map { zone ->
                if (zone.id == zoneId) zone.copy(enabled = enabled) else zone
            },
        )
    }

    override suspend fun addSafeZone(name: String, radiusMeters: Int) {
        val current = _snapshot.value
        if (current.safeZones.size >= 5) return
        val newZone = SafeZone(
            id = "zone-${current.safeZones.size + 1}",
            name = name,
            address = "지도에서 선택한 위치",
            radiusMeters = radiusMeters,
            enabled = true,
            kind = SafeZoneKind.OTHER,
        )
        _snapshot.value = current.copy(safeZones = current.safeZones + newZone)
    }

    override suspend fun updateNotificationSettings(settings: NotificationSettings) {
        _snapshot.value = _snapshot.value.copy(notificationSettings = settings)
    }

    override suspend fun redeemPairingCode(
        code: String,
    ) {

        delay(
            500
        )

        require(
            code.length == 6
        ) {
            "6자리 연결 코드를 입력해주세요."
        }

        _snapshot.value =
            _snapshot.value.copy(

                watchStatus =
                    _snapshot
                        .value
                        .watchStatus
                        .copy(
                            isConnected =
                                true,

                            lastConnectedLabel =
                                "방금 전"
                        )
            )
    }

    private fun createInitialSnapshot(): GuardianSnapshot = GuardianSnapshot(
        user = GuardianUser(
            id = "user-1",
            name = "김순자",
            guardianName = "황현정",
            guardianRelationship = "딸",
        ),
        watchStatus = WatchStatus(
            deviceId = "watch-1",
            deviceName = "Galaxy Watch7",
            batteryPercent = 78,
            isConnected = true,
            isWearing = true,
            lastConnectedLabel = "방금 전",
        ),
        location = LocationInfo(
            latitude = 37.5665,
            longitude = 126.9780,
            address = "서울시 행복구 행복동 123-4 인근",
            shortAddress = "행복동 자택 근처",
            lastUpdatedLabel = "2분 전",
            safeZoneName = "집",
            isInsideSafeZone = true,
        ),
        safeZones = listOf(
            SafeZone("zone-home", "집", "서울시 행복구 행복동 123-4", 500, true, SafeZoneKind.HOME),
            SafeZone("zone-care", "행복 복지관", "행복구 복지로 22", 300, true, SafeZoneKind.CARE_CENTER),
            SafeZone("zone-hospital", "행복 병원", "행복구 건강길 8", 300, false, SafeZoneKind.HOSPITAL),
        ),
        events = listOf(
            SafetyEvent("e1", "집 도착", "집 안전구역에 진입했어요", "오후 6:12", EventDayGroup.TODAY, SafetyEventType.ARRIVED_HOME),
            SafetyEvent("e2", "귀가 안내 시작", "보호자 요청으로 안내 시작", "오후 5:52", EventDayGroup.TODAY, SafetyEventType.RETURN_HOME_REQUESTED),
            SafetyEvent("e3", "안전구역 이탈", "집 구역에서 120m 벗어남", "오후 5:40", EventDayGroup.TODAY, SafetyEventType.SAFE_ZONE_EXITED),
            SafetyEvent("e4", "낙상 의심 → 정상 확인", "사용자가 괜찮음을 선택", "오후 2:31", EventDayGroup.TODAY, SafetyEventType.FALL_CONFIRMED_SAFE),
            SafetyEvent("e5", "수동 SOS", "사용자가 SOS 버튼을 눌렀어요", "오후 3:05", EventDayGroup.YESTERDAY, SafetyEventType.SOS_MANUAL),
            SafetyEvent("e6", "배터리 부족 20%", "충전 안내가 전송되었어요", "오전 11:42", EventDayGroup.YESTERDAY, SafetyEventType.BATTERY_LOW),
        ),
        emergency = EmergencyDetail(
            userName = "김순자",
            phoneNumber = "01012345678",
            title = "낙상 감지 후 응답이 없어요",
            description = "자동 SOS가 발송되었습니다",
            occurredAtLabel = "오후 2:35",
            locationLabel = "행복구 소망공원 북측 산책로 인근",
            timeline = listOf(
                EmergencyTimelineItem("낙상 의심 감지", "오후 2:34"),
                EmergencyTimelineItem("워치에서 상태 확인 요청", "오후 2:34"),
                EmergencyTimelineItem("60초 동안 응답 없음", "오후 2:35"),
                EmergencyTimelineItem("자동 SOS 발송 완료", "오후 2:35"),
            ),
        ),
        notificationSettings = NotificationSettings(
            sosAlert = true,
            safeZoneExitAlert = true,
            arrivalAlert = true,
            batteryLowAlert = false,
        ),
        returnHomeRequested = false,
    )
}
