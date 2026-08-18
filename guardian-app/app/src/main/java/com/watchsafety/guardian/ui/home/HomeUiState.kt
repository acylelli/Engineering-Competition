package com.watchsafety.guardian.ui.home

import com.watchsafety.guardian.domain.model.EventDayGroup
import com.watchsafety.guardian.domain.model.GuardianSnapshot
import com.watchsafety.guardian.domain.model.SafetyEventType

data class HomeUiState(
    val userName: String,
    val safetyStatus: String,
    val safeZoneName: String,
    val safeZoneDescription: String,
    val lastUpdatedText: String,
    val lastLocationText: String,
    val batteryPercent: Int,
    val isWearingWatch: Boolean,
    val safeZoneCount: Int,
    val todayEventCount: Int,
    val recentEvents: List<HomeEventUiModel>,
) {
    companion object {
        val Preview = HomeUiState(
            userName = "김순자",
            safetyStatus = "안전",
            safeZoneName = "안전구역 안",
            safeZoneDescription = "집 안전구역 안에서 활동 중이에요",
            lastUpdatedText = "2분 전 업데이트",
            lastLocationText = "행복동 자택 근처",
            batteryPercent = 78,
            isWearingWatch = true,
            safeZoneCount = 3,
            todayEventCount = 4,
            recentEvents = listOf(
                HomeEventUiModel(
                    title = "안전구역 이탈 후 재진입",
                    time = "오후 5:48",
                    type = HomeEventType.WARNING,
                ),
                HomeEventUiModel(
                    title = "낙상 의심 → 정상 확인",
                    time = "오후 2:31",
                    type = HomeEventType.SAFE,
                ),
            ),
        )
    }
}

data class HomeEventUiModel(
    val title: String,
    val time: String,
    val type: HomeEventType,
)

enum class HomeEventType {
    SAFE,
    WARNING,
}

fun GuardianSnapshot.toHomeUiState(): HomeUiState {
    return HomeUiState(
        userName = user.name,
        safetyStatus = if (location.isInsideSafeZone) "안전" else "주의",
        safeZoneName = if (location.isInsideSafeZone) "안전구역 안" else "안전구역 밖",
        safeZoneDescription = if (location.isInsideSafeZone) {
            "${location.safeZoneName} 안전구역 안에서 활동 중이에요"
        } else {
            "안전구역을 벗어난 상태예요"
        },
        lastUpdatedText = "${location.lastUpdatedLabel} 업데이트",
        lastLocationText = location.shortAddress,
        batteryPercent = watchStatus.batteryPercent,
        isWearingWatch = watchStatus.isWearing,
        safeZoneCount = safeZones.size,
        todayEventCount = events.count { it.dayGroup == EventDayGroup.TODAY },
        recentEvents = events
            .filter {
                it.type == SafetyEventType.SAFE_ZONE_EXITED ||
                    it.type == SafetyEventType.FALL_CONFIRMED_SAFE
            }
            .take(2)
            .map { event ->
                HomeEventUiModel(
                    title = event.title,
                    time = event.timeLabel,
                    type = if (event.type == SafetyEventType.SAFE_ZONE_EXITED) {
                        HomeEventType.WARNING
                    } else {
                        HomeEventType.SAFE
                    },
                )
            },
    )
}
