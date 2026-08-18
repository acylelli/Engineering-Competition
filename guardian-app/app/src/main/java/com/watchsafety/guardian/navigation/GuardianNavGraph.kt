package com.watchsafety.guardian.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import com.watchsafety.guardian.domain.model.GuardianSnapshot
import com.watchsafety.guardian.domain.model.NotificationSettings

import com.watchsafety.guardian.ui.PairingUiState

import com.watchsafety.guardian.ui.emergency.EmergencyScreen
import com.watchsafety.guardian.ui.history.HistoryScreen
import com.watchsafety.guardian.ui.home.HomeScreen
import com.watchsafety.guardian.ui.home.toHomeUiState
import com.watchsafety.guardian.ui.map.CurrentLocationScreen
import com.watchsafety.guardian.ui.pairing.WatchPairingScreen
import com.watchsafety.guardian.ui.safezone.AddSafeZoneScreen
import com.watchsafety.guardian.ui.safezone.SafeZoneListScreen
import com.watchsafety.guardian.ui.settings.SettingsScreen
import com.watchsafety.guardian.ui.status.UserStatusScreen


@Composable
fun GuardianNavGraph(

    navController:
    NavHostController,

    snapshot:
    GuardianSnapshot,

    isRefreshing:
    Boolean,

    onRefreshStatus:
        () -> Unit,

    onReturnHomeRequest:
        () -> Unit,

    onSafeZoneEnabledChange:
        (String, Boolean) -> Unit,

    onAddSafeZone:
        (String, Int) -> Unit,

    onNotificationSettingsChange:
        (NotificationSettings) -> Unit,


    /*
     * -----------------------------------------------------
     * 워치 페어링
     * -----------------------------------------------------
     */

    pairingState:
    PairingUiState,

    onPairingCodeSubmit:
        (String) -> Unit,

    onResetPairingState:
        () -> Unit,


    modifier:
    Modifier = Modifier,

    ) {


    NavHost(

        navController =
            navController,

        startDestination =
            GuardianRoute.HOME,

        modifier =
            modifier,

        ) {


        /*
         * =================================================
         * 홈
         * =================================================
         */

        composable(
            GuardianRoute.HOME
        ) {

            HomeScreen(

                state =
                    snapshot
                        .toHomeUiState(),

                onMapClick = {

                    navController
                        .navigate(
                            GuardianRoute.MAP
                        )
                },

                onSafeZonesClick = {

                    navController
                        .navigate(
                            GuardianRoute.SAFE_ZONES
                        )
                },

                onReturnHomeClick = {

                    navController
                        .navigate(
                            GuardianRoute.MAP
                        )
                },

                onHistoryClick = {

                    navController
                        .navigate(
                            GuardianRoute.HISTORY
                        )
                },

                onNotificationsClick = {

                    navController
                        .navigate(
                            GuardianRoute.HISTORY
                        )
                },
            )
        }


        /*
         * =================================================
         * 지도
         * =================================================
         */

        composable(
            GuardianRoute.MAP
        ) {

            CurrentLocationScreen(

                user =
                    snapshot.user,

                watchStatus =
                    snapshot.watchStatus,

                location =
                    snapshot.location,

                returnHomeRequested =
                    snapshot
                        .returnHomeRequested,

                isRefreshing =
                    isRefreshing,

                onReturnHomeClick =
                    onReturnHomeRequest,

                onRefreshClick =
                    onRefreshStatus,

                onBack =
                    navController::
                    popBackStack,
            )
        }


        /*
         * =================================================
         * 이벤트
         * =================================================
         */

        composable(
            GuardianRoute.HISTORY
        ) {

            HistoryScreen(

                events =
                    snapshot.events,

                onEmergencyClick = {

                    navController
                        .navigate(
                            GuardianRoute.EMERGENCY
                        )
                },
            )
        }


        /*
         * =================================================
         * 설정
         * =================================================
         */

        composable(
            GuardianRoute.SETTINGS
        ) {

            SettingsScreen(

                user =
                    snapshot.user,

                watchStatus =
                    snapshot.watchStatus,

                settings =
                    snapshot
                        .notificationSettings,

                onSettingsChange =
                    onNotificationSettingsChange,

                onUserStatusClick = {

                    navController
                        .navigate(
                            GuardianRoute.USER_STATUS
                        )
                },

                onSafeZonesClick = {

                    navController
                        .navigate(
                            GuardianRoute.SAFE_ZONES
                        )
                },


                /*
                 * 워치 연결 메뉴
                 */
                onWatchPairingClick = {

                    /*
                     * 이전 성공/실패 메시지 제거
                     */
                    onResetPairingState()

                    navController
                        .navigate(
                            GuardianRoute.WATCH_PAIRING
                        )
                },
            )
        }


        /*
         * =================================================
         * 워치 연결
         * =================================================
         */

        composable(
            GuardianRoute.WATCH_PAIRING
        ) {

            WatchPairingScreen(

                state =
                    pairingState,

                /*
                 * 연결 버튼
                 */
                onPairingClick =
                    onPairingCodeSubmit,


                /*
                 * 뒤로가기
                 */
                onBack = {

                    onResetPairingState()

                    navController
                        .popBackStack()
                },


                /*
                 * 연결 완료 화면을 잠깐 보여준 뒤
                 * 설정 화면으로 복귀
                 */
                onSuccessFinished = {

                    onResetPairingState()

                    navController
                        .popBackStack()
                },
            )
        }


        /*
         * =================================================
         * 안전구역 목록
         * =================================================
         */

        composable(
            GuardianRoute.SAFE_ZONES
        ) {

            SafeZoneListScreen(

                zones =
                    snapshot.safeZones,

                onEnabledChange =
                    onSafeZoneEnabledChange,

                onBack =
                    navController::
                    popBackStack,

                onAddClick = {

                    navController
                        .navigate(
                            GuardianRoute.SAFE_ZONE_ADD
                        )
                },
            )
        }


        /*
         * =================================================
         * 안전구역 추가
         * =================================================
         */

        composable(
            GuardianRoute.SAFE_ZONE_ADD
        ) {

            AddSafeZoneScreen(

                onBack =
                    navController::
                    popBackStack,

                onSave = {
                        name,
                        radius ->

                    onAddSafeZone(
                        name,
                        radius
                    )

                    navController
                        .popBackStack()
                },
            )
        }


        /*
         * =================================================
         * 긴급
         * =================================================
         */

        composable(
            GuardianRoute.EMERGENCY
        ) {

            EmergencyScreen(

                detail =
                    snapshot.emergency,

                onBack =
                    navController::
                    popBackStack,

                onMapClick = {

                    navController
                        .navigate(
                            GuardianRoute.MAP
                        )
                },
            )
        }


        /*
         * =================================================
         * 사용자 상태
         * =================================================
         */

        composable(
            GuardianRoute.USER_STATUS
        ) {

            UserStatusScreen(

                user =
                    snapshot.user,

                watchStatus =
                    snapshot.watchStatus,

                location =
                    snapshot.location,

                isRefreshing =
                    isRefreshing,

                onRefreshClick =
                    onRefreshStatus,

                onBack =
                    navController::
                    popBackStack,
            )
        }
    }
}