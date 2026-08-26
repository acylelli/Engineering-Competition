package com.watchsafety.guardian.ui.safezone

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.watchsafety.guardian.data.MockGuardianRepository
import com.watchsafety.guardian.domain.model.SafeZone
import com.watchsafety.guardian.domain.model.SafeZoneKind
import com.watchsafety.guardian.ui.components.GuardianTopBar
import com.watchsafety.guardian.ui.theme.DividerColor
import com.watchsafety.guardian.ui.theme.SafeGreen
import com.watchsafety.guardian.ui.theme.SafeGreenContainer
import com.watchsafety.guardian.ui.theme.TextDisabled
import com.watchsafety.guardian.ui.theme.TrustBlue
import com.watchsafety.guardian.ui.theme.TrustBlueContainer
import com.watchsafety.guardian.ui.theme.WatchSafetyTheme

@Composable
fun SafeZoneListScreen(
    zones: List<SafeZone>,
    onEnabledChange: (String, Boolean) -> Unit,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var zonePendingDeletion by remember { mutableStateOf<SafeZone?>(null) }

    Scaffold(
        topBar = { GuardianTopBar(title = "안전구역 관리", onBack = onBack) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = "구역을 벗어나면 보호자에게 알림이 와요",
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            items(zones, key = { it.id }) { zone ->
                SafeZoneCard(
                    zone = zone,
                    onEnabledChange = { enabled -> onEnabledChange(zone.id, enabled) },
                    onEditClick = { onEditClick(zone.id) },
                    onDeleteClick = { zonePendingDeletion = zone },
                )
            }
            item {
                OutlinedButton(
                    onClick = onAddClick,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, TrustBlue),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                    Text(
                        text = "새 안전구역 추가",
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                    )
                }
            }
        }
    }

    zonePendingDeletion?.let { zone ->
        AlertDialog(
            onDismissRequest = { zonePendingDeletion = null },
            title = { Text("안전구역 삭제") },
            text = {
                Text(
                    if (zone.kind == SafeZoneKind.HOME) {
                        "'${zone.name}'은 집으로 지정된 구역입니다. 삭제하면 워치의 귀가 및 집 안전구역 감지가 중단됩니다. 삭제할까요?"
                    } else {
                        "'${zone.name}' 안전구역을 삭제할까요?"
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        zonePendingDeletion = null
                        onDelete(zone.id)
                    },
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { zonePendingDeletion = null }) {
                    Text("취소")
                }
            },
        )
    }
}

@Composable
private fun SafeZoneCard(
    zone: SafeZone,
    onEnabledChange: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val colors = when (zone.kind) {
        SafeZoneKind.HOME -> SafeGreenContainer to SafeGreen
        SafeZoneKind.CARE_CENTER -> TrustBlueContainer to TrustBlue
        SafeZoneKind.HOSPITAL, SafeZoneKind.OTHER -> Color(0xFFDFF5F2) to Color(0xFF0F9F91)
    }
    val icon = when (zone.kind) {
        SafeZoneKind.HOME -> Icons.Outlined.Home
        SafeZoneKind.CARE_CENTER -> Icons.Outlined.Security
        SafeZoneKind.HOSPITAL, SafeZoneKind.OTHER -> Icons.Outlined.LocationOn
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, DividerColor),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ZoneIcon(icon = icon, container = colors.first, tint = colors.second)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = zone.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "반경 ${zone.radiusMeters}m",
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(12.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Text(
                        text = zone.address,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = zone.enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TrustBlue,
                    ),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DividerColor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(if (zone.enabled) SafeGreen else TextDisabled, CircleShape),
                    )
                    Text(
                        text = if (zone.enabled) "알림 켜짐" else "알림 꺼짐",
                        modifier = Modifier.padding(start = 7.dp),
                        color = if (zone.enabled) SafeGreen else TextDisabled,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Row {
                    TextButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("수정", modifier = Modifier.padding(start = 4.dp))
                    }
                    TextButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "삭제",
                            modifier = Modifier.padding(start = 4.dp),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneIcon(
    icon: ImageVector,
    container: Color,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SafeZoneListPreview() {
    val preview = MockGuardianRepository().snapshot.value
    WatchSafetyTheme {
        SafeZoneListScreen(
            zones = preview.safeZones,
            onEnabledChange = { _, _ -> },
            onBack = {},
            onAddClick = {},
            onEditClick = {},
            onDelete = {},
        )
    }
}
