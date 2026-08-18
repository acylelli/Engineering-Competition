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
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseGuardianRepository(
    private val supabase: SupabaseClient,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : GuardianRepository {
    private val fallbackSnapshot = MockGuardianRepository().snapshot.value
    private val _snapshot = MutableStateFlow(fallbackSnapshot)
    override val snapshot: StateFlow<GuardianSnapshot> = _snapshot.asStateFlow()

    private val refreshMutex = Mutex()
    private var guardianId: String? = null
    private var wearerId: String? = null
    private var realtimeStarted = false

    init {
        scope.launch {
            runCatching {
                guardianId = ensureAuthenticatedSession()
                supabase.postgrest.rpc("bootstrap_guardian_demo")
                loadSnapshot()
                startRealtimeUpdates()
            }
        }
    }

    override suspend fun refreshStatus() {
        loadSnapshot()
    }

    override suspend fun sendReturnHomeRequest() {
        val guardian = requireGuardianId()
        val wearer = requireWearerId()
        supabase.from("return_home_requests").insert(
            ReturnHomeRequestInsertDto(
                guardianId = guardian,
                wearerId = wearer,
            ),
        )
        supabase.from("safety_events").insert(
            SafetyEventInsertDto(
                guardianId = guardian,
                wearerId = wearer,
                type = "RETURN_HOME_REQUESTED",
                title = "귀가 안내 요청",
                description = "보호자가 집으로 안내를 요청했어요",
            ),
        )
        loadSnapshot()
    }

    override suspend fun setSafeZoneEnabled(zoneId: String, enabled: Boolean) {
        supabase.from("safe_zones").update(
            {
                set("enabled", enabled)
                set("updated_at", OffsetDateTime.now().toString())
            },
        ) {
            filter { eq("id", zoneId) }
        }
        loadSnapshot()
    }

    override suspend fun addSafeZone(name: String, radiusMeters: Int) {
        val current = snapshot.value
        if (current.safeZones.size >= 5) return

        supabase.from("safe_zones").insert(
            SafeZoneInsertDto(
                guardianId = requireGuardianId(),
                wearerId = requireWearerId(),
                name = name,
                address = "지도에서 선택한 위치",
                centerLatitude = current.location.latitude,
                centerLongitude = current.location.longitude,
                radiusMeters = radiusMeters.coerceIn(100, 1_000),
                enabled = true,
                kind = "OTHER",
            ),
        )
        loadSnapshot()
    }

    override suspend fun updateNotificationSettings(settings: NotificationSettings) {
        supabase.from("notification_settings").update(
            {
                set("sos_alert", settings.sosAlert)
                set("safe_zone_exit_alert", settings.safeZoneExitAlert)
                set("arrival_alert", settings.arrivalAlert)
                set("battery_low_alert", settings.batteryLowAlert)
                set("updated_at", OffsetDateTime.now().toString())
            },
        ) {
            filter { eq("guardian_id", requireGuardianId()) }
        }
        loadSnapshot()
    }

    override suspend fun redeemPairingCode(
        code: String,
    ) {

        if (guardianId == null) {
            guardianId =
                ensureAuthenticatedSession()
        }

        val normalizedCode =
            code.filter {
                it.isDigit()
            }

        require(
            normalizedCode.length == 6
        ) {
            "6자리 연결 코드를 입력해주세요."
        }

        val parameters =
            buildJsonObject {

                put(
                    "p_code",
                    normalizedCode
                )
            }

        val response =
            supabase
                .postgrest
                .rpc(
                    function =
                        "redeem_pairing_code",

                    parameters =
                        parameters
                )
                .decodeList<
                        RedeemPairingCodeResponse
                        >()
                .firstOrNull()
                ?: error(
                    "연결 결과를 확인할 수 없습니다."
                )

        if (
            !response.success
        ) {

            error(
                "워치 연결에 실패했습니다."
            )
        }

        loadSnapshot()
    }
    private suspend fun ensureAuthenticatedSession(): String {
        if (supabase.auth.currentSessionOrNull() == null) {
            supabase.auth.signInAnonymously()
        }
        return requireNotNull(supabase.auth.currentSessionOrNull()?.user?.id) {
            "Supabase authentication session was not created."
        }
    }

    private suspend fun loadSnapshot() = refreshMutex.withLock {
        if (guardianId == null) guardianId = ensureAuthenticatedSession()

        val profile = supabase.from("guardian_profiles")
            .select()
            .decodeSingle<GuardianProfileDto>()
        val wearer = supabase.from("wearers")
            .select()
            .decodeSingle<WearerDto>()
        wearerId = wearer.id

        val devices = supabase.from("devices").select().decodeList<DeviceDto>()
        val locations = supabase.from("locations").select().decodeList<LocationDto>()
        val zones = supabase.from("safe_zones").select().decodeList<SafeZoneDto>()
        val events = supabase.from("safety_events").select().decodeList<SafetyEventDto>()
        val requests = supabase.from("return_home_requests")
            .select()
            .decodeList<ReturnHomeRequestDto>()
        val notification = supabase.from("notification_settings")
            .select()
            .decodeSingle<NotificationSettingsDto>()

        val device = devices.maxByOrNull { parseTime(it.lastConnectedAt) }
            ?: error("No watch device is paired.")
        val location = locations.maxByOrNull { parseTime(it.recordedAt) }
            ?: error("No watch location is available.")
        val sortedEvents = events.sortedByDescending { parseTime(it.occurredAt) }

        _snapshot.value = GuardianSnapshot(
            user = GuardianUser(
                id = wearer.id,
                name = wearer.name,
                guardianName = profile.displayName,
                guardianRelationship = profile.relationship,
            ),
            watchStatus = WatchStatus(
                deviceId = device.id,
                deviceName = device.deviceName,
                batteryPercent = device.batteryPercent,
                isConnected = device.isConnected,
                isWearing = device.isWearing,
                lastConnectedLabel = relativeTimeLabel(device.lastConnectedAt),
            ),
            location = LocationInfo(
                latitude = location.latitude,
                longitude = location.longitude,
                address = location.address,
                shortAddress = location.shortAddress,
                lastUpdatedLabel = relativeTimeLabel(location.recordedAt),
                safeZoneName = location.safeZoneName.orEmpty(),
                isInsideSafeZone = location.isInsideSafeZone,
            ),
            safeZones = zones.map { it.toDomain() },
            events = sortedEvents.mapNotNull { it.toDomain() },
            emergency = buildEmergency(wearer, sortedEvents),
            notificationSettings = NotificationSettings(
                sosAlert = notification.sosAlert,
                safeZoneExitAlert = notification.safeZoneExitAlert,
                arrivalAlert = notification.arrivalAlert,
                batteryLowAlert = notification.batteryLowAlert,
            ),
            returnHomeRequested = requests.any {
                it.status in setOf("REQUESTED", "ACCEPTED", "NAVIGATING")
            },
        )
    }

    private suspend fun startRealtimeUpdates() {
        if (realtimeStarted) return
        realtimeStarted = true

        val channel = supabase.channel("guardian-data")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            filter("guardian_id", FilterOperator.EQ, requireGuardianId())
        }
        scope.launch {
            changes.collect {
                runCatching { loadSnapshot() }
            }
        }
        supabase.realtime.connect()
        channel.subscribe()
    }

    private fun requireGuardianId(): String =
        requireNotNull(guardianId) { "Supabase repository is not initialized." }

    private fun requireWearerId(): String =
        requireNotNull(wearerId) { "Supabase wearer is not initialized." }

    private fun SafeZoneDto.toDomain(): SafeZone = SafeZone(
        id = id,
        name = name,
        address = address,
        radiusMeters = radiusMeters,
        enabled = enabled,
        kind = runCatching { SafeZoneKind.valueOf(kind) }.getOrDefault(SafeZoneKind.OTHER),
    )

    private fun SafetyEventDto.toDomain(): SafetyEvent? {
        val domainType = when (type) {
            "ARRIVED_HOME", "SAFE_ZONE_ENTERED" -> SafetyEventType.ARRIVED_HOME
            "RETURN_HOME_REQUESTED", "NAVIGATION_STARTED" -> SafetyEventType.RETURN_HOME_REQUESTED
            "SAFE_ZONE_EXITED" -> SafetyEventType.SAFE_ZONE_EXITED
            "FALL_CONFIRMED_SAFE", "FALL_SUSPECTED" -> SafetyEventType.FALL_CONFIRMED_SAFE
            "SOS_MANUAL" -> SafetyEventType.SOS_MANUAL
            "SOS_AUTOMATIC" -> SafetyEventType.SOS_AUTOMATIC
            "BATTERY_LOW" -> SafetyEventType.BATTERY_LOW
            else -> return null
        }
        val occurred = parseTime(occurredAt)
        val today = OffsetDateTime.now().atZoneSameInstant(KOREA_ZONE).toLocalDate()
        return SafetyEvent(
            id = id,
            title = title,
            description = description,
            timeLabel = occurred.atZoneSameInstant(KOREA_ZONE).format(TIME_FORMATTER),
            dayGroup = if (occurred.atZoneSameInstant(KOREA_ZONE).toLocalDate() == today) {
                EventDayGroup.TODAY
            } else {
                EventDayGroup.YESTERDAY
            },
            type = domainType,
        )
    }

    private fun buildEmergency(
        wearer: WearerDto,
        events: List<SafetyEventDto>,
    ): EmergencyDetail {
        val emergency = events.firstOrNull { it.type == "SOS_AUTOMATIC" || it.type == "SOS_MANUAL" }
            ?: return fallbackSnapshot.emergency.copy(
                userName = wearer.name,
                phoneNumber = wearer.phoneNumber.orEmpty(),
            )
        val time = parseTime(emergency.occurredAt)
            .atZoneSameInstant(KOREA_ZONE)
            .format(TIME_FORMATTER)
        return EmergencyDetail(
            userName = wearer.name,
            phoneNumber = wearer.phoneNumber.orEmpty(),
            title = emergency.title,
            description = emergency.description,
            occurredAtLabel = time,
            locationLabel = emergency.address ?: "최근 전송 위치",
            timeline = listOf(
                EmergencyTimelineItem("위험 이벤트 감지", time),
                EmergencyTimelineItem("워치 위치 전송", time),
                EmergencyTimelineItem("보호자 알림 기록", time),
            ),
        )
    }

    private fun relativeTimeLabel(value: String): String {
        val minutes = Duration.between(parseTime(value), OffsetDateTime.now()).toMinutes()
            .coerceAtLeast(0)
        return when {
            minutes < 1 -> "방금 전"
            minutes < 60 -> "${minutes}분 전"
            minutes < 1_440 -> "${minutes / 60}시간 전"
            else -> "${minutes / 1_440}일 전"
        }
    }

    private fun parseTime(value: String): OffsetDateTime = OffsetDateTime.parse(value)

    private companion object {
        val KOREA_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        val TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
    }
}
