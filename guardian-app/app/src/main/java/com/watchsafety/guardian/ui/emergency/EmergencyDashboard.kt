package com.watchsafety.guardian.ui.emergency

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.watchsafety.guardian.data.MockGuardianRepository
import com.watchsafety.guardian.domain.model.EmergencyDetail
import com.watchsafety.guardian.ui.components.MockMap
import com.watchsafety.guardian.ui.theme.DividerColor
import com.watchsafety.guardian.ui.theme.EmergencyRed
import com.watchsafety.guardian.ui.theme.EmergencyRedContainer
import com.watchsafety.guardian.ui.theme.WatchSafetyTheme

@Composable
fun EmergencyScreen(
    detail: EmergencyDetail,
    onBack: () -> Unit,
    onMapClick: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        EmergencyHero(detail = detail, onBack = onBack)
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            EmergencyTimeline(detail = detail)
            EmergencyLocationCard(locationLabel = detail.locationLabel)
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${detail.phoneNumber}")),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(imageVector = Icons.Rounded.Call, contentDescription = null)
                Text(
                    text = "${detail.userName} 님에게 전화 걸기",
                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            OutlinedButton(
                onClick = onMapClick,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, DividerColor),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = "지도에서 위치 자세히 보기",
                    modifier = Modifier.padding(vertical = 7.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "상황이 해결되었어요 · 알림 종료",
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun EmergencyHero(
    detail: EmergencyDetail,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(EmergencyRedContainer)
            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 28.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(Color.White, CircleShape),
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "뒤로 가기")
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .background(EmergencyRed, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = "긴급 상황",
                    modifier = Modifier.padding(start = 6.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 22.dp)
                    .size(104.dp)
                    .background(EmergencyRed.copy(alpha = .12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .background(EmergencyRed.copy(alpha = .22f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(EmergencyRed, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
            }
            Text(
                text = detail.title,
                modifier = Modifier.padding(top = 18.dp),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "${detail.description} · ${detail.occurredAtLabel}",
                modifier = Modifier.padding(top = 6.dp),
                color = Color(0xFFB43B32),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun EmergencyTimeline(detail: EmergencyDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, DividerColor),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            detail.timeline.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(EmergencyRed, CircleShape),
                    )
                    Text(
                        text = item.label,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = item.timeLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmergencyLocationCard(locationLabel: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, DividerColor),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column {
            MockMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                showZoneLabel = false,
                showUserLabel = false,
                showControls = false,
            )
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = EmergencyRed,
                )
                Text(
                    text = locationLabel,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "1분 전",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EmergencyDashboardPreview() {
    val preview = MockGuardianRepository().snapshot.value
    WatchSafetyTheme {
        EmergencyScreen(
            detail = preview.emergency,
            onBack = {},
            onMapClick = {},
        )
    }
}
