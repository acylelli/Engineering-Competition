package com.watchsafety.guardian.domain.model

data class GuardianUser(
    val id: String,
    val name: String,
    val guardianName: String,
    val guardianRelationship: String,
)

data class WatchStatus(
    val deviceId: String,
    val deviceName: String,
    val batteryPercent: Int,
    val isConnected: Boolean,
    val isWearing: Boolean,
    val lastConnectedLabel: String,
)

data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val shortAddress: String,
    val lastUpdatedLabel: String,
    val safeZoneName: String,
    val isInsideSafeZone: Boolean,
)

data class SafeZone(
    val id: String,
    val name: String,
    val address: String,
    val radiusMeters: Int,
    val enabled: Boolean,
    val kind: SafeZoneKind,
)

enum class SafeZoneKind {
    HOME,
    CARE_CENTER,
    HOSPITAL,
    OTHER,
}

data class SafetyEvent(
    val id: String,
    val title: String,
    val description: String,
    val timeLabel: String,
    val dayGroup: EventDayGroup,
    val type: SafetyEventType,
)

enum class EventDayGroup {
    TODAY,
    YESTERDAY,
}

enum class SafetyEventType {
    ARRIVED_HOME,
    RETURN_HOME_REQUESTED,
    SAFE_ZONE_EXITED,
    FALL_CONFIRMED_SAFE,
    SOS_MANUAL,
    SOS_AUTOMATIC,
    BATTERY_LOW,
}

data class EmergencyDetail(
    val userName: String,
    val phoneNumber: String,
    val title: String,
    val description: String,
    val occurredAtLabel: String,
    val locationLabel: String,
    val timeline: List<EmergencyTimelineItem>,
)

data class EmergencyTimelineItem(
    val label: String,
    val timeLabel: String,
)

data class NotificationSettings(
    val sosAlert: Boolean,
    val safeZoneExitAlert: Boolean,
    val arrivalAlert: Boolean,
    val batteryLowAlert: Boolean,
)

enum class ReturnHomeStatus {
    NONE,
    REQUESTED,
    ACCEPTED,
    NAVIGATING,
    ARRIVED,
    COMPLETED,
    CANCELLED;

    val isActive: Boolean
        get() =
            this == REQUESTED ||
                    this == ACCEPTED ||
                    this == NAVIGATING ||
                    this == ARRIVED

    companion object {
        fun fromDb(status: String?): ReturnHomeStatus =
            when (status) {
                "REQUESTED" -> REQUESTED
                "ACCEPTED" -> ACCEPTED
                "NAVIGATING" -> NAVIGATING
                "ARRIVED" -> ARRIVED
                "COMPLETED" -> COMPLETED
                "CANCELLED" -> CANCELLED
                else -> NONE
            }
    }
}

data class GuardianSnapshot(
    val user: GuardianUser,
    val watchStatus: WatchStatus,
    val location: LocationInfo,
    val safeZones: List<SafeZone>,
    val events: List<SafetyEvent>,
    val emergency: EmergencyDetail,
    val notificationSettings: NotificationSettings,
    val returnHomeRequested: Boolean,
    val returnHomeStatus: ReturnHomeStatus,
)