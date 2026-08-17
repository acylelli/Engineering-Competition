package com.watchsafety.guardian.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.watchsafety.guardian.navigation.BottomDestination
import com.watchsafety.guardian.navigation.GuardianBottomBar
import com.watchsafety.guardian.navigation.GuardianNavGraph

@Composable
fun GuardianApp() {
    val navController = rememberNavController()
    val guardianViewModel: GuardianViewModel = viewModel(factory = GuardianViewModel.factory())
    val uiState by guardianViewModel.uiState.collectAsStateWithLifecycle()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val bottomBarRoute = when (currentRoute) {
        com.watchsafety.guardian.navigation.GuardianRoute.SAFE_ZONES -> {
            com.watchsafety.guardian.navigation.GuardianRoute.HOME
        }
        else -> currentRoute
    }
    val showBottomBar = currentRoute in setOf(
        com.watchsafety.guardian.navigation.GuardianRoute.HOME,
        com.watchsafety.guardian.navigation.GuardianRoute.HISTORY,
        com.watchsafety.guardian.navigation.GuardianRoute.SETTINGS,
        com.watchsafety.guardian.navigation.GuardianRoute.SAFE_ZONES,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                GuardianBottomBar(
                    currentRoute = bottomBarRoute,
                    onDestinationSelected = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        GuardianNavGraph(
            navController = navController,
            snapshot = uiState.snapshot,
            isRefreshing = uiState.isRefreshing,
            onRefreshStatus = guardianViewModel::refreshStatus,
            onReturnHomeRequest = guardianViewModel::sendReturnHomeRequest,
            onSafeZoneEnabledChange = guardianViewModel::setSafeZoneEnabled,
            onAddSafeZone = guardianViewModel::addSafeZone,
            onNotificationSettingsChange = guardianViewModel::updateNotificationSettings,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
