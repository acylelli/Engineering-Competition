package com.watchsafety.guardian.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.watchsafety.guardian.data.MockGuardianRepository
import com.watchsafety.guardian.domain.model.EventDayGroup
import com.watchsafety.guardian.domain.model.SafetyEvent
import com.watchsafety.guardian.domain.model.SafetyEventType
import com.watchsafety.guardian.ui.components.GuardianTopBar
import com.watchsafety.guardian.ui.theme.DividerColor
import com.watchsafety.guardian.ui.theme.EmergencyRed
import com.watchsafety.guardian.ui.theme.EmergencyRedContainer
import com.watchsafety.guardian.ui.theme.SafeGreen
import com.watchsafety.guardian.ui.theme.SafeGreenContainer
import com.watchsafety.guardian.ui.theme.TextSecondary
import com.watchsafety.guardian.ui.theme.TrustBlue
import com.watchsafety.guardian.ui.theme.TrustBlueContainer
import com.watchsafety.guardian.ui.theme.WarningAmber
import com.watchsafety.guardian.ui.theme.WarningAmberContainer
import com.watchsafety.guardian.ui.theme.WatchSafetyTheme

@Composable
fun HistoryScreen(
    events: List<SafetyEvent>,
    onEmergencyClick: () -> Unit,
) {
    var selectedFilter by rememberSaveable { mutableStateOf(EventFilter.ALL) }
    val todayEvents = events.filter {
        it.dayGroup == EventDayGroup.TODAY && selectedFilter.matches(it.type)
    }
    val yesterdayEvents = events.filter {
        it.dayGroup == EventDayGroup.YESTERDAY && selectedFilter.matches(it.type)
    }

    Scaffold(
        topBar = { GuardianTopBar(title = "안전 이벤트") },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EventFilter.entries.size) { index ->
                        val filter = EventFilter.entries[index]
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF243142),
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }
            }
            if (todayEvents.isNotEmpty()) {
                item {
                    Text(
                        text = "오늘 · 8월 14일",
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                item {
                    EventGroupCard(
                        events = todayEvents,
                        onEmergencyClick = onEmergencyClick,
                    )
                }
            }
            if (yesterdayEvents.isNotEmpty()) {
                item {
                    Text(
                        text = "어제 · 8월 13일",
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                item {
                    EventGroupCard(
                        events = yesterdayEvents,
                        onEmergencyClick = onEmergencyClick,
                    )
                }
            }
            if (todayEvents.isEmpty() && yesterdayEvents.isEmpty()) {
                item {
                    Text(
                        text = "선택한 유형의 이벤트가 없습니다.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun EventGroupCard(
    events: List<SafetyEvent>,
    onEmergencyClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DividerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            events.forEachIndexed { index, event ->
                EventRow(
                    event = event,
                    onClick = if (
                        event.type == SafetyEventType.SOS_MANUAL ||
                        event.type == SafetyEventType.SOS_AUTOMATIC
                    ) {
                        onEmergencyClick
                    } else {
                        null
                    },
                )
                if (index < events.lastIndex) {
                    HorizontalDivider(color = DividerColor)
                }
            }
        }
    }
}

@Composable
private fun EventRow(
    event: SafetyEvent,
    onClick: (() -> Unit)?,
) {
    val visuals = event.type.visuals
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .then(Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = visuals.container),
                shape = CircleShape,
            ) {
                Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = visuals.icon,
                        contentDescription = null,
                        tint = visuals.tint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            Text(
                text = event.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = event.description,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = event.timeLabel,
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private enum class EventFilter(val label: String) {
    ALL("전체"),
    SOS("SOS"),
    ZONE("이탈"),
    FALL("낙상"),
    BATTERY("배터리");

    fun matches(type: SafetyEventType): Boolean = when (this) {
        ALL -> true
        SOS -> type == SafetyEventType.SOS_MANUAL || type == SafetyEventType.SOS_AUTOMATIC
        ZONE -> type == SafetyEventType.SAFE_ZONE_EXITED
        FALL -> type == SafetyEventType.FALL_CONFIRMED_SAFE
        BATTERY -> type == SafetyEventType.BATTERY_LOW
    }
}

private data class EventVisuals(
    val icon: ImageVector,
    val container: Color,
    val tint: Color,
)

private val SafetyEventType.visuals: EventVisuals
    get() = when (this) {
        SafetyEventType.ARRIVED_HOME -> EventVisuals(Icons.Rounded.Home, SafeGreenContainer, SafeGreen)
        SafetyEventType.RETURN_HOME_REQUESTED -> EventVisuals(
            Icons.Outlined.Home,
            TrustBlueContainer,
            TrustBlue,
        )
        SafetyEventType.SAFE_ZONE_EXITED -> EventVisuals(
            Icons.Rounded.WarningAmber,
            WarningAmberContainer,
            WarningAmber,
        )
        SafetyEventType.FALL_CONFIRMED_SAFE -> EventVisuals(
            Icons.Rounded.Check,
            Color(0xFFDFF5F2),
            Color(0xFF0F9F91),
        )
        SafetyEventType.SOS_MANUAL, SafetyEventType.SOS_AUTOMATIC -> EventVisuals(
            Icons.Rounded.WarningAmber,
            EmergencyRedContainer,
            EmergencyRed,
        )
        SafetyEventType.BATTERY_LOW -> EventVisuals(
            Icons.Outlined.BatteryAlert,
            MaterialThemeNeutral,
            TextSecondary,
        )
    }

private val MaterialThemeNeutral = Color(0xFFF1F4F8)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EventHistoryPreview() {
    val preview = MockGuardianRepository().snapshot.value
    WatchSafetyTheme {
        HistoryScreen(events = preview.events, onEmergencyClick = {})
    }
}
