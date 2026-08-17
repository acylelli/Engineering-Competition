package com.watchsafety.guardian.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.watchsafety.guardian.ui.theme.SafeGreen
import com.watchsafety.guardian.ui.theme.TrustBlue

@Composable
fun MockMap(
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    showZoneLabel: Boolean = true,
    showUserLabel: Boolean = true,
    showControls: Boolean = true,
    userLabel: String = "김순자 님",
) {
    val zoneColor = if (selectionMode) TrustBlue else SafeGreen
    BoxWithConstraints(
        modifier = modifier.background(MapBackground),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRoundRect(
                color = ParkGreen,
                topLeft = Offset(w * 0.02f, h * 0.06f),
                size = Size(w * 0.25f, h * 0.20f),
                cornerRadius = CornerRadius(18f),
            )
            val buildings = listOf(
                floatArrayOf(.31f, .08f, .14f, .12f),
                floatArrayOf(.55f, .05f, .13f, .16f),
                floatArrayOf(.76f, .12f, .17f, .12f),
                floatArrayOf(.08f, .34f, .20f, .13f),
                floatArrayOf(.36f, .31f, .14f, .15f),
                floatArrayOf(.62f, .32f, .18f, .11f),
                floatArrayOf(.82f, .37f, .13f, .16f),
                floatArrayOf(.10f, .68f, .18f, .13f),
                floatArrayOf(.39f, .66f, .17f, .15f),
                floatArrayOf(.67f, .70f, .13f, .12f),
                floatArrayOf(.82f, .65f, .15f, .18f),
            )
            buildings.forEach { item ->
                drawRoundRect(
                    color = BuildingColor,
                    topLeft = Offset(w * item[0], h * item[1]),
                    size = Size(w * item[2], h * item[3]),
                    cornerRadius = CornerRadius(16f),
                )
            }

            val roadStroke = w * 0.025f
            drawLine(Color.White, Offset(w * .30f, 0f), Offset(w * .30f, h), roadStroke)
            drawLine(Color.White, Offset(w * .62f, 0f), Offset(w * .62f, h), roadStroke)
            drawLine(Color.White, Offset(0f, h * .29f), Offset(w, h * .29f), roadStroke)
            drawLine(Color.White, Offset(0f, h * .59f), Offset(w, h * .59f), roadStroke)
            drawLine(
                Color.White,
                Offset(-w * .08f, h * .88f),
                Offset(w * 1.08f, h * .52f),
                roadStroke * .75f,
            )

            val zoneCenter = if (selectionMode) {
                Offset(w * .50f, h * .50f)
            } else {
                Offset(w * .56f, h * .52f)
            }
            val zoneRadius = minOf(w, h) * .25f
            drawCircle(zoneColor.copy(alpha = .13f), zoneRadius, zoneCenter)
            drawCircle(
                color = zoneColor,
                radius = zoneRadius,
                center = zoneCenter,
                style = Stroke(
                    width = 3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                ),
            )
        }

        if (showZoneLabel && !selectionMode) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "안전구역: 집 (500m)",
                        modifier = Modifier.padding(start = 5.dp),
                        color = SafeGreen,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        if (showControls) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 72.dp, end = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "확대",
                            modifier = Modifier.padding(11.dp),
                        )
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = "축소",
                            modifier = Modifier.padding(11.dp),
                        )
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = "현재 위치",
                        tint = TrustBlue,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        if (selectionMode) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = "선택한 위치",
                tint = TrustBlue,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-8).dp)
                    .size(48.dp),
            )
        } else {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = maxWidth * .12f, y = maxHeight * .06f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(TrustBlue.copy(alpha = .22f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .background(TrustBlue, CircleShape),
                    )
                }
                if (showUserLabel) {
                    Text(
                        text = userLabel,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .background(Color(0xFF364152), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

private val MapBackground = Color(0xFFECF1F7)
private val BuildingColor = Color(0xFFDDE5F0)
private val ParkGreen = Color(0xFFDDF4E8)
