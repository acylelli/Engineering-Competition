package com.watchsafety.guardian.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val GuardianColorScheme = lightColorScheme(
    primary = TrustBlue,
    primaryContainer = TrustBlueContainer,
    onPrimary = CardSurface,
    onPrimaryContainer = TrustBlue,
    secondary = SafeGreen,
    secondaryContainer = SafeGreenContainer,
    onSecondary = CardSurface,
    onSecondaryContainer = SafeGreen,
    error = EmergencyRed,
    errorContainer = EmergencyRedContainer,
    onError = CardSurface,
    onErrorContainer = EmergencyRed,
    background = AppBackground,
    surface = CardSurface,
    surfaceVariant = AppBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
)

@Composable
fun WatchSafetyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GuardianColorScheme,
        typography = GuardianTypography,
        shapes = GuardianShapes,
        content = content,
    )
}
