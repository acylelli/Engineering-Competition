package com.watchsafety.guardian.ui.home

import androidx.compose.runtime.Composable

@Composable
fun HomeScreen(
    state: HomeUiState = HomeUiState.Preview,
    onMapClick: () -> Unit = {},
    onSafeZonesClick: () -> Unit = {},
    onReturnHomeClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
) {
    HomeDashboard(
        state = state,
        onMapClick = onMapClick,
        onSafeZonesClick = onSafeZonesClick,
        onReturnHomeClick = onReturnHomeClick,
        onHistoryClick = onHistoryClick,
        onNotificationsClick = onNotificationsClick,
    )
}
