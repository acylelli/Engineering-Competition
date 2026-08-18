package com.watchsafety.guardian.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GuardianProfileDto(
    @SerialName("guardian_id") val guardianId: String,
    @SerialName("display_name") val displayName: String,
    val relationship: String,
)

@Serializable
data class WearerDto(
    val id: String,
    @SerialName("guardian_id") val guardianId: String,
    val name: String,
    @SerialName("phone_number") val phoneNumber: String? = null,
)

@Serializable
data class DeviceDto(
    val id: String,
    @SerialName("guardian_id") val guardianId: String,
    @SerialName("wearer_id") val wearerId: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("battery_percent") val batteryPercent: Int,
    @SerialName("is_connected") val isConnected: Boolean,
    @SerialName("is_wearing") val isWearing: Boolean,
    @SerialName("last_connected_at") val lastConnectedAt: String,
)

@Serializable
data class LocationDto(
    val id: Long,
    @SerialName("guardian_id") val guardianId: String,
    @SerialName("wearer_id") val wearerId: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    @SerialName("short_address") val shortAddress: String,
    @SerialName("safe_zone_name") val safeZoneName: String? = null,
    @SerialName("is_inside_safe_zone") val isInsideSafeZone: Boolean,
    @SerialName("recorded_at") val recordedAt: String,
)

@Serializable
data class SafeZoneDto(
    val id: String,
    @SerialName("guardian_id") val guardianId: String,
    @SerialName("wearer_id") val wearerId: String,
    val name: String,
    val address: String,
    @SerialName("center_latitude") val centerLatitude: Double,
    @SerialName("center_longitude") val centerLongitude: Double,
    @SerialName("radius_meters") val radiusMeters: Int,
    val enabled: Boolean,
    val kind: String,
)

@Serializable
data class SafetyEventDto(
    val id: String,
    @SerialName("guardian_id") val guardianId: String,
    @SerialName("wearer_id") val wearerId: String,
    val type: String,
    val title: String,
    val description: String,
    val address: String? = null,
    @SerialName("occurred_at") val occurredAt: String,
)

@Serializable
data class ReturnHomeRequestDto(
    val id: String,
    @SerialName("guardian_id") val guardianId: String,
    @SerialName("wearer_id") val wearerId: String,
    val status: String,
    @SerialName("requested_at") val requestedAt: String,
)

@Serializable
data class NotificationSettingsDto(
    @SerialName("guardian_id") val guardianId: String,
    @SerialName("sos_alert") val sosAlert: Boolean,
    @SerialName("safe_zone_exit_alert") val safeZoneExitAlert: Boolean,
    @SerialName("arrival_alert") val arrivalAlert: Boolean,
    @SerialName("battery_low_alert") val batteryLowAlert: Boolean,
)

@Serializable
data class SafeZoneInsertDto(
    @SerialName("guardian_id") val guardianId: String,
    @SerialName("wearer_id") val wearerId: String,
    val name: String,
    val address: String,
    @SerialName("center_latitude") val centerLatitude: Double,
    @SerialName("center_longitude") val centerLongitude: Double,
    @SerialName("radius_meters") val radiusMeters: Int,
    val enabled: Boolean,
    val kind: String,
)

@Serializable
data class ReturnHomeRequestInsertDto(
    @SerialName("guardian_id") val guardianId: String,
    @SerialName("wearer_id") val wearerId: String,
    val status: String = "REQUESTED",
)

@Serializable
data class SafetyEventInsertDto(
    @SerialName("guardian_id") val guardianId: String,
    @SerialName("wearer_id") val wearerId: String,
    val type: String,
    val title: String,
    val description: String,
)
