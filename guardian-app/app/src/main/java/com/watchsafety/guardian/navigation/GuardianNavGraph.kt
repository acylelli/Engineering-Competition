package com.watchsafety.guardian.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.watchsafety.guardian.domain.model.GuardianSnapshot
import com.watchsafety.guardian.domain.model.NotificationSettings
import com.watchsafety.guardian.ui.emergency.EmergencyScreen
import com.watchsafety.guardian.ui.history.HistoryScreen
import com.watchsafety.guardian.ui.home.HomeScreen
import com.watchsafety.guardian.ui.home.toHomeUiState
import com.watchsafety.guardian.ui.map.CurrentLocationScreen
import com.watchsafety.guardian.ui.safezone.AddSafeZoneScreen
import com.watchsafety.guardian.ui.safezone.SafeZoneListScreen
import com.watchsafety.guardian.ui.settings.SettingsScreen
import com.watchsafety.guardian.ui.status.UserStatusScreen

@Composable
fun GuardianNavGraph(
    navController: NavHostController,
    snapshot: GuardianSnapshot,
    isRefreshing: Boolean,
    onRefreshStatus: () -> Unit,
    onReturnHomeRequest: () -> Unit,
    onSafeZoneEnabledChange: (String, Boolean) -> Unit,
    onAddSafeZone: (String, Int) -> Unit,
    onNotificationSettingsChange: (NotificationSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = GuardianRoute.HOME,
        modifier = modifier,
    ) {
        composable(GuardianRoute.HOME) {
            HomeScreen(
                state = snapshot.toHomeUiState(),
                onMapClick = { navController.navigate(GuardianRoute.MAP) },
                onSafeZonesClick = { navController.navigate(GuardianRoute.SAFE_ZONES) },
                onReturnHomeClick = { navController.navigate(GuardianRoute.MAP) },
                onHistoryClick = { navController.navigate(GuardianRoute.HISTORY) },
                onNotificationsClick = { navController.navigate(GuardianRoute.HISTORY) },
            )
        }
        composable(GuardianRoute.MAP) {
            CurrentLocationScreen(
                user = snapshot.user,
                watchStatus = snapshot.watchStatus,
                location = snapshot.location,
                returnHomeRequested = snapshot.returnHomeRequested,
                isRefreshing = isRefreshing,
                onReturnHomeClick = onReturnHomeRequest,
                onRefreshClick = onRefreshStatus,
                onBack = navController::popBackStack,
            )
        }
        composable(GuardianRoute.HISTORY) {
            HistoryScreen(
                events = snapshot.events,
                onEmergencyClick = { navController.navigate(GuardianRoute.EMERGENCY) },
            )
        }
        composable(GuardianRoute.SETTINGS) {
            SettingsScreen(
                user = snapshot.user,
                watchStatus = snapshot.watchStatus,
                settings = snapshot.notificationSettings,
                onSettingsChange = onNotificationSettingsChange,
                onUserStatusClick = { navController.navigate(GuardianRoute.USER_STATUS) },
                onSafeZonesClick = { navController.navigate(GuardianRoute.SAFE_ZONES) },
            )
        }
        composable(GuardianRoute.SAFE_ZONES) {
            SafeZoneListScreen(
                zones = snapshot.safeZones,
                onEnabledChange = onSafeZoneEnabledChange,
                onBack = navController::popBackStack,
                onAddClick = { navController.navigate(GuardianRoute.SAFE_ZONE_ADD) },
            )
        }
        composable(GuardianRoute.SAFE_ZONE_ADD) {
            AddSafeZoneScreen(
                onBack = navController::popBackStack,
                onSave = { name, radius ->
                    onAddSafeZone(name, radius)
                    navController.popBackStack()
                },
            )
        }
        composable(GuardianRoute.EMERGENCY) {
            EmergencyScreen(
                detail = snapshot.emergency,
                onBack = navController::popBackStack,
                onMapClick = { navController.navigate(GuardianRoute.MAP) },
            )
        }
        composable(GuardianRoute.USER_STATUS) {
            UserStatusScreen(
                user = snapshot.user,
                watchStatus = snapshot.watchStatus,
                location = snapshot.location,
                isRefreshing = isRefreshing,
                onRefreshClick = onRefreshStatus,
                onBack = navController::popBackStack,
            )
        }
    }
}
