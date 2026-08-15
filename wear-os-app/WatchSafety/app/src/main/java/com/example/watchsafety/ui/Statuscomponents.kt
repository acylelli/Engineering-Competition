package com.example.watchsafety.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text

/**
 * 앱 전역에서 재사용하는 색상 팔레트.
 * 워치 화면은 어둡고 작기 때문에 상태를 "색"으로 즉시 구분할 수 있게 한다.
 */
object SafetyColors {
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFFB300)
    val Error = Color(0xFFEF5350)
    val Neutral = Color(0xFF8A8A8A)
    val Accent = Color(0xFF2F6FED)
    val CardBackground = Color(0xFF1C1C1E)
    val TextSecondary = Color(0xFFB0B0B0)
}

enum class StatusLevel { SUCCESS, WARNING, ERROR, NEUTRAL, LOADING }

private fun StatusLevel.toColor(): Color = when (this) {
    StatusLevel.SUCCESS -> SafetyColors.Success
    StatusLevel.WARNING -> SafetyColors.Warning
    StatusLevel.ERROR -> SafetyColors.Error
    StatusLevel.NEUTRAL -> SafetyColors.Neutral
    StatusLevel.LOADING -> SafetyColors.Accent
}

/**
 * 상태를 나타내는 작은 점.
 * LOADING 상태일 때는 점 대신 회전 인디케이터를 보여줘 "확인 중"임을 명확히 한다.
 */
@Composable
fun StatusDot(
    level: StatusLevel,
    size: Dp = 8.dp
) {
    if (level == StatusLevel.LOADING) {
        CircularProgressIndicator(
            modifier = Modifier.size(size + 4.dp),
            strokeWidth = 2.dp,
            indicatorColor = SafetyColors.Accent
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(level.toColor())
        )
    }
}

@Composable
fun ScreenTitle(text: String) {
    Text(
        text = text,
        style = TextStyle(
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    )
}

/**
 * "라벨 - 값" 한 줄을 카드 형태로 보여준다.
 * 기존의 텍스트만 나열하던 방식보다 항목 구분이 뚜렷하고, 상태 색으로 한눈에 파악 가능.
 */
@Composable
fun StatusCard(
    label: String,
    value: String,
    level: StatusLevel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(14.dp))
            .background(SafetyColors.CardBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(level)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = TextStyle(color = SafetyColors.TextSecondary, fontSize = 12.sp)
            )
        }
        Text(
            text = value,
            style = TextStyle(
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
fun StatusMessageText(text: String) {
    Text(
        text = text,
        style = TextStyle(color = SafetyColors.TextSecondary, fontSize = 12.sp)
    )
}

/**
 * 홈 화면처럼 공간이 좁은 곳에서 쓰는 압축형 상태 칩.
 * 라벨 대신 아이콘으로 의미를 전달하고, 값은 짧게(1~3자) 표시한다.
 * 상태 색은 아이콘 자체에 입혀서 "정상은 눈에 안 띄고, 이상만 눈에 띄게" 한다.
 */
@Composable
fun QuickStatChip(
    icon: ImageVector,
    value: String,
    level: StatusLevel,
    modifier: Modifier = Modifier
) {
    val tintColor = if (level == StatusLevel.SUCCESS) Color.White else level.toColor()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SafetyColors.CardBackground)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = TextStyle(
                color = tintColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

/**
 * 현재 배터리 잔량(%)을 실시간으로 반환한다.
 * ACTION_BATTERY_CHANGED 브로드캐스트를 구독해 값이 바뀔 때마다 갱신되며,
 * 화면이 사라지면(onDispose) 자동으로 구독을 해제한다.
 */
@Composable
fun rememberBatteryLevel(): State<Int> {
    val context = LocalContext.current

    val batteryLevel = remember {
        mutableStateOf(readBatteryLevel(context))
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    batteryLevel.value = level * 100 / scale
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    return batteryLevel
}

private fun readBatteryLevel(context: Context): Int {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    return batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
}

/**
 * 배터리 잔량에 따른 표시 색상.
 * 평소엔 눈에 안 띄는 회색, 낮아지면 주황 -> 빨강으로 경고한다.
 */
fun batteryLevelColor(level: Int): Color = when {
    level <= 15 -> SafetyColors.Error
    level <= 30 -> SafetyColors.Warning
    else -> SafetyColors.TextSecondary
}