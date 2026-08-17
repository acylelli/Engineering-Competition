package com.watchsafety.guardian.ui.status

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.watchsafety.guardian.data.MockGuardianRepository
import com.watchsafety.guardian.domain.model.GuardianUser
import com.watchsafety.guardian.domain.model.LocationInfo
import com.watchsafety.guardian.domain.model.SafetyState
import com.watchsafety.guardian.domain.model.WatchStatus
import com.watchsafety.guardian.ui.components.GuardianTopBar
import com.watchsafety.guardian.ui.components.StatusBadge
import com.watchsafety.guardian.ui.theme.DividerColor
import com.watchsafety.guardian.ui.theme.SafeGreen
import com.watchsafety.guardian.ui.theme.SafeGreenContainer
import com.watchsafety.guardian.ui.theme.TextSecondary
import com.watchsafety.guardian.ui.theme.TrustBlue
import com.watchsafety.guardian.ui.theme.TrustBlueContainer
import com.watchsafety.guardian.ui.theme.WatchSafetyTheme

@Composable
fun UserStatusScreen(
    user: GuardianUser,
    watchStatus: WatchStatus,
    location: LocationInfo,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { GuardianTopBar(title = "사용자 및 워치 상태", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, DividerColor),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(TrustBlueContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = TrustBlue,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                    ) {
                        Text(
                            text = "${user.name} 님",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = watchStatus.deviceName,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    StatusBadge(
                        state = if (watchStatus.isConnected) SafetyState.SAFE else SafetyState.WARNING,
                        text = if (watchStatus.isConnected) "연결됨" else "연결 끊김",
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatusTile(
                    label = "배터리",
                    value = "${watchStatus.batteryPercent}%",
                    icon = Icons.Outlined.BatteryFull,
                    modifier = Modifier.weight(1f),
                )
                StatusTile(
                    label = "착용 상태",
                    value = if (watchStatus.isWearing) "착용 중" else "미착용",
                    icon = Icons.Outlined.Watch,
                    modifier = Modifier.weight(1f),
                )
            }
            StatusDetailCard(watchStatus = watchStatus, location = location)
            Button(
                onClick = onRefreshClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRefreshing,
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Text(
                    text = if (isRefreshing) "상태 갱신 중" else "상태 새로고침",
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SafeGreenContainer),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = SafeGreen)
            Text(
                text = label,
                modifier = Modifier.padding(top = 10.dp),
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = value,
                color = SafeGreen,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun StatusDetailCard(
    watchStatus: WatchStatus,
    location: LocationInfo,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, DividerColor),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "최근 상태",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            DetailRow(Icons.Rounded.CheckCircle, "마지막 연결", watchStatus.lastConnectedLabel)
            DetailRow(Icons.Rounded.LocationOn, "마지막 위치", location.shortAddress)
            DetailRow(
                Icons.Outlined.Watch,
                "워치 상태",
                if (watchStatus.isConnected) "정상 작동 중" else "연결 확인 필요",
            )
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = TrustBlue)
        Text(
            text = label,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            color = TextSecondary,
        )
        Text(text = value, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun UserWatchStatusPreview() {
    val preview = MockGuardianRepository().snapshot.value
    WatchSafetyTheme {
        UserStatusScreen(
            user = preview.user,
            watchStatus = preview.watchStatus,
            location = preview.location,
            isRefreshing = false,
            onRefreshClick = {},
            onBack = {},
        )
    }
}
