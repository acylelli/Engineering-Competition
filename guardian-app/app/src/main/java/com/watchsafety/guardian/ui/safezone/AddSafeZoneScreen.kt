package com.watchsafety.guardian.ui.safezone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.watchsafety.guardian.ui.components.GuardianTopBar
import com.watchsafety.guardian.ui.components.MockMap
import com.watchsafety.guardian.ui.theme.TrustBlue
import com.watchsafety.guardian.ui.theme.WatchSafetyTheme
import kotlin.math.roundToInt

@Composable
fun AddSafeZoneScreen(
    onBack: () -> Unit,
    onSave: (String, Int) -> Unit,
) {
    var zoneName by rememberSaveable { mutableStateOf("행복 복지관") }
    var radius by rememberSaveable { mutableFloatStateOf(300f) }

    Scaffold(
        topBar = { GuardianTopBar(title = "안전구역 추가", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
            ) {
                MockMap(
                    modifier = Modifier.fillMaxSize(),
                    selectionMode = true,
                    showZoneLabel = false,
                    showUserLabel = false,
                )
                Text(
                    text = "지도를 움직여 중심 위치를 정하세요",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp)
                        .background(Color(0xFF243142), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "구역 이름",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = zoneName,
                    onValueChange = { zoneName = it },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = TrustBlue,
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "반경", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${radius.roundToInt()}m",
                        color = TrustBlue,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 100f..1000f,
                    steps = 8,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(100, 300, 500, 1000).forEach { preset ->
                        RadiusPresetButton(
                            radius = preset,
                            selected = radius.roundToInt() == preset,
                            onClick = { radius = preset.toFloat() },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Button(
                    onClick = { onSave(zoneName, radius.roundToInt()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = zoneName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = "안전구역 저장하기",
                        modifier = Modifier.padding(vertical = 7.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun RadiusPresetButton(
    radius: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (radius == 1000) "1km" else "${radius}m"
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
        ) {
            Text(text = label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
        ) {
            Text(text = label)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AddSafeZonePreview() {
    WatchSafetyTheme {
        AddSafeZoneScreen(onBack = {}, onSave = { _, _ -> })
    }
}
