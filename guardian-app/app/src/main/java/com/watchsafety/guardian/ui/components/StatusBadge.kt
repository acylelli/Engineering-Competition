package com.watchsafety.guardian.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.watchsafety.guardian.domain.model.SafetyState
import com.watchsafety.guardian.ui.theme.EmergencyRed
import com.watchsafety.guardian.ui.theme.EmergencyRedContainer
import com.watchsafety.guardian.ui.theme.SafeGreen
import com.watchsafety.guardian.ui.theme.SafeGreenContainer
import com.watchsafety.guardian.ui.theme.WarningAmber
import com.watchsafety.guardian.ui.theme.WarningAmberContainer

@Composable
fun StatusBadge(
    state: SafetyState,
    modifier: Modifier = Modifier,
    text: String = state.defaultLabel,
) {
    val colors = state.badgeColors
    Box(
        modifier = modifier
            .background(colors.container, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = colors.content,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private val SafetyState.defaultLabel: String
    get() = when (this) {
        SafetyState.SAFE -> "안전"
        SafetyState.WARNING -> "주의"
        SafetyState.EMERGENCY -> "긴급"
    }

private data class BadgeColors(
    val container: Color,
    val content: Color,
)

private val SafetyState.badgeColors: BadgeColors
    get() = when (this) {
        SafetyState.SAFE -> BadgeColors(SafeGreenContainer, SafeGreen)
        SafetyState.WARNING -> BadgeColors(WarningAmberContainer, WarningAmber)
        SafetyState.EMERGENCY -> BadgeColors(EmergencyRedContainer, EmergencyRed)
    }
