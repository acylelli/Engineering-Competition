package com.watchsafety.guardian.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.watchsafety.guardian.data.MockGuardianRepository
import com.watchsafety.guardian.domain.model.GuardianUser
import com.watchsafety.guardian.domain.model.LocationInfo
import com.watchsafety.guardian.domain.model.SafetyState
import com.watchsafety.guardian.domain.model.WatchStatus
import com.watchsafety.guardian.ui.components.GuardianTopBar
import com.watchsafety.guardian.ui.components.MockMap
import com.watchsafety.guardian.ui.components.StatusBadge
import com.watchsafety.guardian.ui.theme.DividerColor
import com.watchsafety.guardian.ui.theme.SafeGreen
import com.watchsafety.guardian.ui.theme.TrustBlue
import com.watchsafety.guardian.ui.theme.WatchSafetyTheme

@Composable
fun CurrentLocationScreen(
    user: GuardianUser,
    watchStatus: WatchStatus,
    location: LocationInfo,
    returnHomeRequested: Boolean,
    isRefreshing: Boolean,
    onReturnHomeClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { GuardianTopBar(title = "현재 위치", onBack = onBack) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            MockMap(
                modifier = Modifier.fillMaxSize(),
                userLabel = "${user.name} 님",
            )
            LocationBottomSheet(
                user = user,
                watchStatus = watchStatus,
                location = location,
                returnHomeRequested = returnHomeRequested,
                isRefreshing = isRefreshing,
                onReturnHomeClick = onReturnHomeClick,
                onRefreshClick = onRefreshClick,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun LocationBottomSheet(
    user: GuardianUser,
    watchStatus: WatchStatus,
    location: LocationInfo,
    returnHomeRequested: Boolean,
    isRefreshing: Boolean,
    onReturnHomeClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${user.name} 님의 현재 위치",
                    style = MaterialTheme.typography.titleLarge,
                )
                StatusBadge(
                    state = if (location.isInsideSafeZone) SafetyState.SAFE else SafetyState.WARNING,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = location.address,
                    modifier = Modifier.padding(start = 6.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(vertical = 12.dp),
            ) {
                LocationStat(
                    label = "위치 갱신",
                    value = location.lastUpdatedLabel,
                    modifier = Modifier.weight(1f),
                )
                LocationStat(
                    label = "배터리",
                    value = "${watchStatus.batteryPercent}%",
                    modifier = Modifier.weight(1f),
                )
                LocationStat(
                    label = "안전구역",
                    value = if (location.isInsideSafeZone) "안 · ${location.safeZoneName}" else "구역 밖",
                    valueColor = if (location.isInsideSafeZone) SafeGreen else MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onReturnHomeClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (returnHomeRequested) SafeGreen else TrustBlue,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(imageVector = Icons.Rounded.Home, contentDescription = null)
                    Text(
                        text = if (returnHomeRequested) "귀가 요청 전송됨" else "집으로 귀가 요청",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                OutlinedButton(
                    onClick = onRefreshClick,
                    enabled = !isRefreshing,
                    border = BorderStroke(1.dp, DividerColor),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "갱신")
                    Text(
                        text = if (isRefreshing) "갱신 중" else "갱신",
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = value,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CurrentLocationPreview() {
    val preview = MockGuardianRepository().snapshot.value
    WatchSafetyTheme {
        CurrentLocationScreen(
            user = preview.user,
            watchStatus = preview.watchStatus,
            location = preview.location,
            returnHomeRequested = preview.returnHomeRequested,
            isRefreshing = false,
            onReturnHomeClick = {},
            onRefreshClick = {},
            onBack = {},
        )
    }
}
