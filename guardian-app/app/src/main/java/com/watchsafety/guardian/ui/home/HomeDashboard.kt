package com.watchsafety.guardian.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.watchsafety.guardian.domain.model.SafetyState
import com.watchsafety.guardian.ui.components.StatusBadge
import com.watchsafety.guardian.ui.theme.DividerColor
import com.watchsafety.guardian.ui.theme.SafeGreen
import com.watchsafety.guardian.ui.theme.SafeGreenContainer
import com.watchsafety.guardian.ui.theme.TextSecondary
import com.watchsafety.guardian.ui.theme.TrustBlue
import com.watchsafety.guardian.ui.theme.TrustBlueContainer
import com.watchsafety.guardian.ui.theme.WarningAmber
import com.watchsafety.guardian.ui.theme.WarningAmberContainer
import com.watchsafety.guardian.ui.theme.WatchSafetyTheme

@Composable
internal fun HomeDashboard(
    state: HomeUiState,
    onMapClick: () -> Unit,
    onSafeZonesClick: () -> Unit,
    onReturnHomeClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onNotificationsClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 20.dp,
            end = 16.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HomeHeader(
                userName = state.userName,
                onNotificationsClick = onNotificationsClick,
            )
        }
        item { SafetySummaryCard(state = state) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatusInfoCard(
                    label = "워치 배터리",
                    value = "${state.batteryPercent}%",
                    icon = Icons.Outlined.BatteryFull,
                    iconTint = SafeGreen,
                    modifier = Modifier.weight(1f),
                )
                StatusInfoCard(
                    label = "워치 착용",
                    value = if (state.isWearingWatch) "착용 중" else "미착용",
                    icon = Icons.Outlined.Watch,
                    iconTint = TrustBlue,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Text(text = "빠른 메뉴", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuickMenuCard(
                        title = "현재 위치",
                        subtitle = "지도에서 확인",
                        icon = Icons.Outlined.LocationOn,
                        onClick = onMapClick,
                        modifier = Modifier.weight(1f),
                    )
                    QuickMenuCard(
                        title = "안전구역",
                        subtitle = "${state.safeZoneCount}개 설정됨",
                        icon = Icons.Outlined.Security,
                        onClick = onSafeZonesClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuickMenuCard(
                        title = "귀가 요청",
                        subtitle = "집으로 안내",
                        icon = Icons.Outlined.Home,
                        onClick = onReturnHomeClick,
                        modifier = Modifier.weight(1f),
                    )
                    QuickMenuCard(
                        title = "이벤트 기록",
                        subtitle = "오늘 ${state.todayEventCount}건",
                        icon = Icons.Outlined.AccessTime,
                        onClick = onHistoryClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "최근 이벤트", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "전체 보기",
                    color = TrustBlue,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        item {
            RecentEventsCard(
                events = state.recentEvents,
                onClick = onHistoryClick,
            )
        }
    }
}

@Composable
private fun HomeHeader(
    userName: String,
    onNotificationsClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "안녕하세요, 보호자님",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "$userName 님의 안전 상태",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        Box(modifier = Modifier.padding(start = 12.dp)) {
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = "알림",
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(11.dp)
                    .background(MaterialTheme.colorScheme.error, CircleShape),
            )
        }
    }
}

@Composable
private fun SafetySummaryCard(state: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SafeGreenContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.safetyStatus,
                            color = SafeGreen,
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        StatusBadge(
                            state = SafetyState.SAFE,
                            text = state.safeZoneName,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                    Text(
                        text = state.safeZoneDescription,
                        color = SafetyTextGreen,
                        maxLines = 2,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = SafeGreen.copy(alpha = 0.18f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(SafeGreen, CircleShape),
                    )
                    Text(
                        text = state.lastUpdatedText,
                        modifier = Modifier.padding(start = 8.dp),
                        color = SafetyTextGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = state.lastLocationText,
                    color = SafetyTextGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun StatusInfoCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(92.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DividerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(25.dp),
                )
                Text(
                    text = value,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun QuickMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(86.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DividerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(TrustBlueContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TrustBlue,
                    modifier = Modifier.size(23.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun RecentEventsCard(
    events: List<HomeEventUiModel>,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DividerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            events.forEachIndexed { index, event ->
                HomeEventRow(event = event)
                if (index < events.lastIndex) {
                    HorizontalDivider(color = DividerColor)
                }
            }
        }
    }
}

@Composable
private fun HomeEventRow(event: HomeEventUiModel) {
    val isWarning = event.type == HomeEventType.WARNING
    val containerColor = if (isWarning) WarningAmberContainer else SafeEventContainer
    val iconColor = if (isWarning) WarningAmber else SafeEventColor
    val icon = if (isWarning) Icons.Rounded.WarningAmber else Icons.Rounded.Check

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(containerColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = event.title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = event.time,
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private val SafetyTextGreen = Color(0xFF31704D)
private val SafeEventContainer = Color(0xFFDFF5F2)
private val SafeEventColor = Color(0xFF0F9F91)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeDashboardPreview() {
    WatchSafetyTheme {
        HomeScreen()
    }
}
