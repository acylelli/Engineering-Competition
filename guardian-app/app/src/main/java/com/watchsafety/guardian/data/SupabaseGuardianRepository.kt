package com.watchsafety.guardian.data

import android.util.Log
import com.watchsafety.guardian.domain.model.EmergencyDetail
import com.watchsafety.guardian.domain.model.EmergencyTimelineItem
import com.watchsafety.guardian.domain.model.EventDayGroup
import com.watchsafety.guardian.domain.model.GuardianSnapshot
import com.watchsafety.guardian.domain.model.GuardianUser
import com.watchsafety.guardian.domain.model.LocationInfo
import com.watchsafety.guardian.domain.model.NotificationSettings
import com.watchsafety.guardian.domain.model.ReturnHomeStatus
import com.watchsafety.guardian.domain.model.SafeZone
import com.watchsafety.guardian.domain.model.SafeZoneKind
import com.watchsafety.guardian.domain.model.SafetyEvent
import com.watchsafety.guardian.domain.model.SafetyEventType
import com.watchsafety.guardian.domain.model.WatchStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresListDataFlow
import io.github.jan.supabase.realtime.realtime
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseGuardianRepository(

    private val supabase:
    SupabaseClient,

    private val scope:
    CoroutineScope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.IO
        ),

    ) : GuardianRepository {

    private val fallbackSnapshot =
        MockGuardianRepository()
            .snapshot
            .value

    private val _snapshot =
        MutableStateFlow(
            fallbackSnapshot
        )

    override val snapshot:
            StateFlow<GuardianSnapshot> =
        _snapshot.asStateFlow()

    /*
     * =====================================================
     * 새 SOS 이벤트
     * =====================================================
     */

    private val newSosChannel =
        Channel<Unit>(
            capacity =
                Channel.BUFFERED
        )

    override val newSosEvent:
            Flow<Unit> =
        newSosChannel
            .receiveAsFlow()

    private val refreshMutex =
        Mutex()

    private var guardianId:
            String? = null

    private var wearerId:
            String? = null

    private var realtimeStarted =
        false

    private var lastRealtimeSosId:
            String? = null

    init {

        scope.launch {

            runCatching {

                guardianId =
                    ensureAuthenticatedSession()

                Log.d(
                    TAG,
                    "Guardian Auth 완료: $guardianId"
                )

                supabase
                    .postgrest
                    .rpc(
                        "bootstrap_guardian_demo"
                    )

                loadSnapshot()

                initializeSosBaseline()

                startRealtimeUpdates()

            }.onFailure { error ->

                Log.e(
                    TAG,
                    "Repository 초기화 실패: ${error.message}",
                    error
                )
            }
        }
    }

    /*
     * =====================================================
     * 새로고침
     * =====================================================
     */

    override suspend fun refreshStatus() {

        loadSnapshot()
    }

    /*
     * =====================================================
     * 귀가 요청
     * =====================================================
     */

    override suspend fun sendReturnHomeRequest() {

        val guardian =
            requireGuardianId()

        val wearer =
            requireWearerId()

        /*
         * 이미 진행 중인 최신 요청이 있다면
         * 중복 요청을 만들지 않는다.
         */
        if (
            _snapshot
                .value
                .returnHomeStatus
                .isActive
        ) {

            Log.d(
                TAG,
                "진행 중인 귀가 요청이 있어 새 요청을 생략합니다."
            )

            return
        }

        supabase
            .from(
                "return_home_requests"
            )
            .insert(
                ReturnHomeRequestInsertDto(
                    guardianId =
                        guardian,
                    wearerId =
                        wearer,
                )
            )

        supabase
            .from(
                "safety_events"
            )
            .insert(
                SafetyEventInsertDto(
                    guardianId =
                        guardian,
                    wearerId =
                        wearer,
                    type =
                        "RETURN_HOME_REQUESTED",
                    title =
                        "귀가 안내 요청",
                    description =
                        "보호자가 집으로 안내를 요청했어요",
                )
            )

        loadSnapshot()
    }

    /*
     * =====================================================
     * 안전구역
     * =====================================================
     */

    override suspend fun setSafeZoneEnabled(
        zoneId: String,
        enabled: Boolean,
    ) {

        supabase
            .from(
                "safe_zones"
            )
            .update(
                {
                    set(
                        "enabled",
                        enabled
                    )

                    set(
                        "updated_at",
                        OffsetDateTime
                            .now()
                            .toString()
                    )
                },
            ) {

                filter {
                    eq(
                        "id",
                        zoneId
                    )
                }
            }

        loadSnapshot()
    }

    override suspend fun addSafeZone(
        name: String,
        radiusMeters: Int,
    ) {

        val current =
            snapshot.value

        if (
            current.safeZones.size >= 5
        ) {

            return
        }

        supabase
            .from(
                "safe_zones"
            )
            .insert(
                SafeZoneInsertDto(
                    guardianId =
                        requireGuardianId(),
                    wearerId =
                        requireWearerId(),
                    name =
                        name,
                    address =
                        "지도에서 선택한 위치",
                    centerLatitude =
                        current.location.latitude,
                    centerLongitude =
                        current.location.longitude,
                    radiusMeters =
                        radiusMeters.coerceIn(
                            100,
                            1_000
                        ),
                    enabled =
                        true,
                    kind =
                        "OTHER",
                )
            )

        loadSnapshot()
    }

    /*
     * =====================================================
     * 알림 설정
     * =====================================================
     */

    override suspend fun updateNotificationSettings(
        settings: NotificationSettings,
    ) {

        supabase
            .from(
                "notification_settings"
            )
            .update(
                {
                    set(
                        "sos_alert",
                        settings.sosAlert
                    )
                    set(
                        "safe_zone_exit_alert",
                        settings.safeZoneExitAlert
                    )
                    set(
                        "arrival_alert",
                        settings.arrivalAlert
                    )
                    set(
                        "battery_low_alert",
                        settings.batteryLowAlert
                    )
                    set(
                        "updated_at",
                        OffsetDateTime
                            .now()
                            .toString()
                    )
                },
            ) {

                filter {
                    eq(
                        "guardian_id",
                        requireGuardianId()
                    )
                }
            }

        loadSnapshot()
    }

    /*
     * =====================================================
     * 워치 페어링
     * =====================================================
     */

    override suspend fun redeemPairingCode(
        code: String,
    ) {

        if (
            guardianId == null
        ) {

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
                        parameters,
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

    /*
     * =====================================================
     * Auth
     * =====================================================
     */

    private suspend fun ensureAuthenticatedSession():
            String {

        supabase
            .auth
            .awaitInitialization()

        val existingSession =
            supabase
                .auth
                .currentSessionOrNull()

        if (
            existingSession != null
        ) {

            val existingUserId =
                requireNotNull(
                    existingSession.user?.id
                ) {

                    "Existing Supabase session has no user id."
                }

            Log.d(
                TAG,
                "기존 Guardian Auth 세션 사용: $existingUserId"
            )

            return existingUserId
        }

        supabase
            .auth
            .signInAnonymously()

        val newSession =
            requireNotNull(
                supabase
                    .auth
                    .currentSessionOrNull()
            ) {

                "Supabase authentication session was not created."
            }

        val newUserId =
            requireNotNull(
                newSession.user?.id
            ) {

                "New Supabase session has no user id."
            }

        Log.d(
            TAG,
            "새 Guardian Auth 생성: $newUserId"
        )

        return newUserId
    }

    /*
     * =====================================================
     * 전체 Snapshot
     * =====================================================
     */

    private suspend fun loadSnapshot() =
        refreshMutex.withLock {

            if (
                guardianId == null
            ) {

                guardianId =
                    ensureAuthenticatedSession()
            }

            val profile =
                supabase
                    .from(
                        "guardian_profiles"
                    )
                    .select()
                    .decodeSingle<
                            GuardianProfileDto
                            >()

            val wearer =
                supabase
                    .from(
                        "wearers"
                    )
                    .select()
                    .decodeSingle<
                            WearerDto
                            >()

            wearerId =
                wearer.id

            val devices =
                supabase
                    .from(
                        "devices"
                    )
                    .select()
                    .decodeList<
                            DeviceDto
                            >()

            val locations =
                supabase
                    .from(
                        "locations"
                    )
                    .select()
                    .decodeList<
                            LocationDto
                            >()

            val zones =
                supabase
                    .from(
                        "safe_zones"
                    )
                    .select()
                    .decodeList<
                            SafeZoneDto
                            >()

            val events =
                supabase
                    .from(
                        "safety_events"
                    )
                    .select()
                    .decodeList<
                            SafetyEventDto
                            >()

            val requests =
                supabase
                    .from(
                        "return_home_requests"
                    )
                    .select()
                    .decodeList<
                            ReturnHomeRequestDto
                            >()

            val notification =
                supabase
                    .from(
                        "notification_settings"
                    )
                    .select()
                    .decodeSingle<
                            NotificationSettingsDto
                            >()

            val device =
                devices
                    .maxByOrNull {
                        parseTime(
                            it.lastConnectedAt
                        )
                    }
                    ?: error(
                        "No watch device is paired."
                    )

            val location =
                locations
                    .maxByOrNull {
                        parseTime(
                            it.recordedAt
                        )
                    }
                    ?: error(
                        "No watch location is available."
                    )

            val sortedEvents =
                events
                    .sortedByDescending {
                        parseTime(
                            it.occurredAt
                        )
                    }

            /*
             * 과거 요청 전체에서 active가 하나라도 있는지 보지 않고
             * 가장 최근 귀가 요청 하나의 상태만 사용한다.
             */
            val latestReturnHomeRequest =
                requests
                    .filter {
                        it.wearerId ==
                                wearer.id
                    }
                    .maxByOrNull {
                        parseTime(
                            it.requestedAt
                        )
                    }

            val returnHomeStatus =
                ReturnHomeStatus
                    .fromDb(
                        latestReturnHomeRequest
                            ?.status
                    )

            _snapshot.value =
                GuardianSnapshot(
                    user =
                        GuardianUser(
                            id =
                                wearer.id,
                            name =
                                wearer.name,
                            guardianName =
                                profile.displayName,
                            guardianRelationship =
                                profile.relationship,
                        ),
                    watchStatus =
                        WatchStatus(
                            deviceId =
                                device.id,
                            deviceName =
                                device.deviceName,
                            batteryPercent =
                                device.batteryPercent,
                            isConnected =
                                device.isConnected,
                            isWearing =
                                device.isWearing,
                            lastConnectedLabel =
                                relativeTimeLabel(
                                    device.lastConnectedAt
                                ),
                        ),
                    location =
                        LocationInfo(
                            latitude =
                                location.latitude,
                            longitude =
                                location.longitude,
                            address =
                                location.address,
                            shortAddress =
                                location.shortAddress,
                            lastUpdatedLabel =
                                relativeTimeLabel(
                                    location.recordedAt
                                ),
                            safeZoneName =
                                location
                                    .safeZoneName
                                    .orEmpty(),
                            isInsideSafeZone =
                                location.isInsideSafeZone,
                        ),
                    safeZones =
                        zones.map {
                            it.toDomain()
                        },
                    events =
                        sortedEvents
                            .mapNotNull {
                                it.toDomain()
                            },
                    emergency =
                        buildEmergency(
                            wearer,
                            sortedEvents
                        ),
                    notificationSettings =
                        NotificationSettings(
                            sosAlert =
                                notification.sosAlert,
                            safeZoneExitAlert =
                                notification.safeZoneExitAlert,
                            arrivalAlert =
                                notification.arrivalAlert,
                            batteryLowAlert =
                                notification.batteryLowAlert,
                        ),
                    returnHomeRequested =
                        returnHomeStatus.isActive,
                    returnHomeStatus =
                        returnHomeStatus,
                )

            Log.d(
                TAG,
                "Snapshot 갱신 완료 / events=${sortedEvents.size} / returnHome=${returnHomeStatus.name}"
            )
        }

    /*
     * =====================================================
     * Realtime
     * =====================================================
     */

    private fun initializeSosBaseline() {

        val latestSos =
            _snapshot
                .value
                .events
                .firstOrNull { event ->
                    event.type ==
                            SafetyEventType.SOS_MANUAL ||
                            event.type ==
                            SafetyEventType.SOS_AUTOMATIC
                }

        lastRealtimeSosId =
            latestSos?.id

        Log.d(
            REALTIME_TAG,
            "SOS 초기 기준값 설정 id=$lastRealtimeSosId"
        )
    }

    private suspend fun startRealtimeUpdates() {

        if (
            realtimeStarted
        ) {

            return
        }

        val session =
            supabase
                .auth
                .currentSessionOrNull()
                ?: error(
                    "Guardian Auth session is missing."
                )

        Log.d(
            REALTIME_TAG,
            "Realtime 시작 / guardian=$guardianId"
        )

        supabase
            .realtime
            .setAuth(
                session.accessToken
            )

        Log.d(
            REALTIME_TAG,
            "Realtime JWT 적용 완료"
        )

        val safetyChannel =
            supabase
                .channel(
                    "guardian-safety-events"
                )

        val locationChannel =
            supabase
                .channel(
                    "guardian-locations"
                )

        /*
         * 귀가 상태 전용 Realtime Channel
         */
        val returnHomeChannel =
            supabase
                .channel(
                    "guardian-return-home"
                )

        val safetyEventsFlow:
                Flow<List<SafetyEventDto>> =
            safetyChannel
                .postgresListDataFlow(
                    schema =
                        "public",
                    table =
                        "safety_events",
                    primaryKey =
                        SafetyEventDto::id,
                )

        val locationsFlow:
                Flow<List<LocationDto>> =
            locationChannel
                .postgresListDataFlow(
                    schema =
                        "public",
                    table =
                        "locations",
                    primaryKey =
                        LocationDto::id,
                )

        val returnHomeFlow:
                Flow<List<ReturnHomeRequestDto>> =
            returnHomeChannel
                .postgresListDataFlow(
                    schema =
                        "public",
                    table =
                        "return_home_requests",
                    primaryKey =
                        ReturnHomeRequestDto::id,
                )

        val safetyJob =
            safetyEventsFlow
                .onEach { events ->

                    Log.d(
                        REALTIME_TAG,
                        "🔥 safety_events 변경 수신 / 총 ${events.size}개"
                    )

                    val latestSos =
                        events
                            .filter { event ->
                                event.type ==
                                        "SOS_MANUAL" ||
                                        event.type ==
                                        "SOS_AUTOMATIC"
                            }
                            .maxByOrNull { event ->
                                parseTime(
                                    event.occurredAt
                                )
                            }

                    val isNewSos =
                        latestSos != null &&
                                latestSos.id !=
                                lastRealtimeSosId

                    if (
                        isNewSos
                    ) {

                        lastRealtimeSosId =
                            latestSos!!.id

                        Log.d(
                            REALTIME_TAG,
                            "🚨 새로운 SOS 감지 id=${latestSos.id}, type=${latestSos.type}"
                        )

                        val snapshotResult =
                            runCatching {
                                loadSnapshot()
                            }

                        if (
                            snapshotResult.isSuccess &&
                            _snapshot
                                .value
                                .notificationSettings
                                .sosAlert
                        ) {

                            val sendResult =
                                newSosChannel
                                    .trySend(
                                        Unit
                                    )

                            Log.d(
                                REALTIME_TAG,
                                "🚨 newSosEvent 전송 결과=${sendResult.isSuccess}"
                            )
                        }

                    } else {

                        runCatching {
                            loadSnapshot()
                        }.onFailure { error ->

                            Log.e(
                                REALTIME_TAG,
                                "Snapshot 갱신 실패",
                                error
                            )
                        }
                    }
                }
                .launchIn(
                    scope
                )

        val locationJob =
            locationsFlow
                .onEach { locations ->

                    Log.d(
                        REALTIME_TAG,
                        "📍 locations 변경 수신 / 총 ${locations.size}개"
                    )

                    runCatching {
                        updateLatestLocation(
                            locations
                        )
                    }.onFailure { error ->

                        Log.e(
                            REALTIME_TAG,
                            "실시간 위치 반영 실패",
                            error
                        )
                    }
                }
                .launchIn(
                    scope
                )

        /*
         * REQUESTED / ACCEPTED / NAVIGATING /
         * ARRIVED / COMPLETED / CANCELLED를 즉시 반영한다.
         */
        val returnHomeJob =
            returnHomeFlow
                .onEach { requests ->

                    val currentWearerId =
                        wearerId

                    val latest =
                        requests
                            .filter { request ->
                                currentWearerId == null ||
                                        request.wearerId ==
                                        currentWearerId
                            }
                            .maxByOrNull { request ->
                                parseTime(
                                    request.requestedAt
                                )
                            }

                    val status =
                        ReturnHomeStatus
                            .fromDb(
                                latest?.status
                            )

                    _snapshot.update {
                            currentSnapshot ->

                        currentSnapshot.copy(
                            returnHomeRequested =
                                status.isActive,
                            returnHomeStatus =
                                status,
                        )
                    }

                    Log.d(
                        REALTIME_TAG,
                        "🏠 귀가 상태 변경 id=${latest?.id}, status=${status.name}"
                    )
                }
                .launchIn(
                    scope
                )

        try {

            safetyChannel
                .subscribe(
                    blockUntilSubscribed =
                        true
                )

            Log.d(
                REALTIME_TAG,
                "✅ safety_events Realtime 구독 완료"
            )

            locationChannel
                .subscribe(
                    blockUntilSubscribed =
                        true
                )

            Log.d(
                REALTIME_TAG,
                "✅ locations Realtime 구독 완료"
            )

            returnHomeChannel
                .subscribe(
                    blockUntilSubscribed =
                        true
                )

            Log.d(
                REALTIME_TAG,
                "✅ return_home_requests Realtime 구독 완료"
            )

            realtimeStarted =
                true

        } catch (
            error: Throwable
        ) {

            safetyJob.cancel()
            locationJob.cancel()
            returnHomeJob.cancel()

            realtimeStarted =
                false

            Log.e(
                REALTIME_TAG,
                "Realtime 구독 실패: ${error.message}",
                error
            )

            throw error
        }
    }

    /*
     * =====================================================
     * 최신 위치
     * =====================================================
     */

    private fun updateLatestLocation(
        locations: List<LocationDto>,
    ) {

        val currentWearerId =
            wearerId

        val latestLocation =
            locations
                .filter { location ->
                    currentWearerId == null ||
                            location.wearerId ==
                            currentWearerId
                }
                .maxByOrNull { location ->
                    parseTime(
                        location.recordedAt
                    )
                }
                ?: return

        _snapshot.update {
                currentSnapshot ->

            currentSnapshot.copy(
                location =
                    LocationInfo(
                        latitude =
                            latestLocation.latitude,
                        longitude =
                            latestLocation.longitude,
                        address =
                            latestLocation.address,
                        shortAddress =
                            latestLocation.shortAddress,
                        lastUpdatedLabel =
                            relativeTimeLabel(
                                latestLocation.recordedAt
                            ),
                        safeZoneName =
                            latestLocation
                                .safeZoneName
                                .orEmpty(),
                        isInsideSafeZone =
                            latestLocation
                                .isInsideSafeZone,
                    ),
            )
        }

        Log.d(
            REALTIME_TAG,
            "📍 실시간 위치 반영 id=${latestLocation.id}, lat=${latestLocation.latitude}, lng=${latestLocation.longitude}"
        )
    }

    private fun requireGuardianId():
            String =
        requireNotNull(
            guardianId
        ) {

            "Supabase repository is not initialized."
        }

    private fun requireWearerId():
            String =
        requireNotNull(
            wearerId
        ) {

            "Supabase wearer is not initialized."
        }

    private fun SafeZoneDto.toDomain():
            SafeZone =
        SafeZone(
            id =
                id,
            name =
                name,
            address =
                address,
            radiusMeters =
                radiusMeters,
            enabled =
                enabled,
            kind =
                runCatching {
                    SafeZoneKind
                        .valueOf(
                            kind
                        )
                }.getOrDefault(
                    SafeZoneKind.OTHER
                ),
        )

    private fun SafetyEventDto.toDomain():
            SafetyEvent? {

        val domainType =
            when (type) {
                "ARRIVED_HOME",
                "SAFE_ZONE_ENTERED" ->
                    SafetyEventType.ARRIVED_HOME

                "RETURN_HOME_REQUESTED",
                "NAVIGATION_STARTED" ->
                    SafetyEventType.RETURN_HOME_REQUESTED

                "SAFE_ZONE_EXITED" ->
                    SafetyEventType.SAFE_ZONE_EXITED

                "FALL_CONFIRMED_SAFE",
                "FALL_SUSPECTED" ->
                    SafetyEventType.FALL_CONFIRMED_SAFE

                "SOS_MANUAL" ->
                    SafetyEventType.SOS_MANUAL

                "SOS_AUTOMATIC" ->
                    SafetyEventType.SOS_AUTOMATIC

                "BATTERY_LOW" ->
                    SafetyEventType.BATTERY_LOW

                else ->
                    return null
            }

        val occurred =
            parseTime(
                occurredAt
            )

        val today =
            OffsetDateTime
                .now()
                .atZoneSameInstant(
                    KOREA_ZONE
                )
                .toLocalDate()

        return SafetyEvent(
            id =
                id,
            title =
                title,
            description =
                description,
            timeLabel =
                occurred
                    .atZoneSameInstant(
                        KOREA_ZONE
                    )
                    .format(
                        TIME_FORMATTER
                    ),
            dayGroup =
                if (
                    occurred
                        .atZoneSameInstant(
                            KOREA_ZONE
                        )
                        .toLocalDate() ==
                    today
                ) {
                    EventDayGroup.TODAY
                } else {
                    EventDayGroup.YESTERDAY
                },
            type =
                domainType,
        )
    }

    private fun buildEmergency(
        wearer: WearerDto,
        events: List<SafetyEventDto>,
    ): EmergencyDetail {

        val emergency =
            events
                .firstOrNull { event ->
                    event.type ==
                            "SOS_AUTOMATIC" ||
                            event.type ==
                            "SOS_MANUAL"
                }
                ?: return fallbackSnapshot
                    .emergency
                    .copy(
                        userName =
                            wearer.name,
                        phoneNumber =
                            wearer
                                .phoneNumber
                                .orEmpty(),
                    )

        val time =
            parseTime(
                emergency.occurredAt
            )
                .atZoneSameInstant(
                    KOREA_ZONE
                )
                .format(
                    TIME_FORMATTER
                )

        return EmergencyDetail(
            userName =
                wearer.name,
            phoneNumber =
                wearer
                    .phoneNumber
                    .orEmpty(),
            title =
                emergency.title,
            description =
                emergency.description,
            occurredAtLabel =
                time,
            locationLabel =
                emergency.address
                    ?: "최근 전송 위치",
            timeline =
                listOf(
                    EmergencyTimelineItem(
                        "위험 이벤트 감지",
                        time
                    ),
                    EmergencyTimelineItem(
                        "워치 위치 전송",
                        time
                    ),
                    EmergencyTimelineItem(
                        "보호자 알림 기록",
                        time
                    ),
                ),
        )
    }

    private fun relativeTimeLabel(
        value: String,
    ): String {

        val minutes =
            Duration
                .between(
                    parseTime(
                        value
                    ),
                    OffsetDateTime
                        .now()
                )
                .toMinutes()
                .coerceAtLeast(
                    0
                )

        return when {
            minutes < 1 ->
                "방금 전"

            minutes < 60 ->
                "${minutes}분 전"

            minutes < 1_440 ->
                "${minutes / 60}시간 전"

            else ->
                "${minutes / 1_440}일 전"
        }
    }

    private fun parseTime(
        value: String,
    ): OffsetDateTime =
        OffsetDateTime.parse(
            value
        )

    private companion object {

        const val TAG =
            "SupabaseGuardianRepo"

        const val REALTIME_TAG =
            "GuardianRealtime"

        val KOREA_ZONE:
                ZoneId =
            ZoneId.of(
                "Asia/Seoul"
            )

        val TIME_FORMATTER:
                DateTimeFormatter =
            DateTimeFormatter
                .ofPattern(
                    "a h:mm",
                    Locale.KOREAN
                )
    }
}
