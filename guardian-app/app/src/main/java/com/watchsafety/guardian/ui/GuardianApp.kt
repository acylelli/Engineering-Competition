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

import com.watchsafety.guardian.navigation.GuardianBottomBar
import com.watchsafety.guardian.navigation.GuardianNavGraph
import com.watchsafety.guardian.navigation.GuardianRoute


@Composable
fun GuardianApp() {


    /*
     * =====================================================
     * Navigation
     * =====================================================
     */

    val navController =
        rememberNavController()


    /*
     * =====================================================
     * ViewModel
     * =====================================================
     */

    val guardianViewModel:
            GuardianViewModel =

        viewModel(
            factory =
                GuardianViewModel
                    .factory()
        )


    /*
     * 기존 전체 상태
     */
    val uiState by
    guardianViewModel
        .uiState
        .collectAsStateWithLifecycle()


    /*
     * 워치 연결 상태
     */
    val pairingState by
    guardianViewModel
        .pairingUiState
        .collectAsStateWithLifecycle()


    /*
     * =====================================================
     * 현재 화면
     * =====================================================
     */

    val currentRoute =
        navController
            .currentBackStackEntryAsState()
            .value
            ?.destination
            ?.route


    /*
     * 안전구역 화면에서는
     * 기존처럼 홈 탭 선택 상태 유지
     */
    val bottomBarRoute =

        when (
            currentRoute
        ) {

            GuardianRoute.SAFE_ZONES -> {

                GuardianRoute.HOME
            }

            else -> {

                currentRoute
            }
        }


    /*
     * 워치 연결 화면에서는
     * BottomBar를 숨긴다.
     */
    val showBottomBar =

        currentRoute in
                setOf(

                    GuardianRoute.HOME,

                    GuardianRoute.HISTORY,

                    GuardianRoute.SETTINGS,

                    GuardianRoute.SAFE_ZONES,
                )


    /*
     * =====================================================
     * 앱 Scaffold
     * =====================================================
     */

    Scaffold(

        containerColor =
            MaterialTheme
                .colorScheme
                .background,


        bottomBar = {

            if (
                showBottomBar
            ) {

                GuardianBottomBar(

                    currentRoute =
                        bottomBarRoute,

                    onDestinationSelected = {
                            destination ->


                        navController
                            .navigate(
                                destination.route
                            ) {


                                popUpTo(
                                    navController
                                        .graph
                                        .startDestinationId
                                ) {

                                    saveState =
                                        true
                                }


                                launchSingleTop =
                                    true


                                restoreState =
                                    true
                            }
                    },
                )
            }
        },

        ) { innerPadding ->


        /*
         * =================================================
         * Navigation Graph
         * =================================================
         */

        GuardianNavGraph(

            navController =
                navController,


            /*
             * 메인 상태
             */
            snapshot =
                uiState.snapshot,

            isRefreshing =
                uiState.isRefreshing,


            /*
             * 기존 기능
             */
            onRefreshStatus =
                guardianViewModel::
                refreshStatus,

            onReturnHomeRequest =
                guardianViewModel::
                sendReturnHomeRequest,

            onSafeZoneEnabledChange =
                guardianViewModel::
                setSafeZoneEnabled,

            onAddSafeZone =
                guardianViewModel::
                addSafeZone,

            onNotificationSettingsChange =
                guardianViewModel::
                updateNotificationSettings,


            /*
             * -------------------------------------------------
             * 워치 페어링
             * -------------------------------------------------
             */

            pairingState =
                pairingState,

            onPairingCodeSubmit =
                guardianViewModel::
                redeemPairingCode,

            onResetPairingState =
                guardianViewModel::
                resetPairingState,


            modifier =
                Modifier.padding(
                    innerPadding
                ),
        )
    }
}