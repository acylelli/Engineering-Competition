package com.watchsafety.guardian.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import com.watchsafety.guardian.domain.model.GuardianSnapshot
import com.watchsafety.guardian.domain.model.NotificationSettings
import com.watchsafety.guardian.domain.model.SafeZoneKind

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

    /*
     * =====================================================
     * 안전구역 추가
     * =====================================================
     *
     * 마지막 Boolean
     *
     * true  -> HOME
     * false -> OTHER
     */
    onAddSafeZone:
        (
        String,
        Int,
        Double,
        Double,
        Boolean,
    ) -> Unit,

    onUpdateSafeZone:
        (
        String,
        String,
        Int,
        Double,
        Double,
        Boolean,
    ) -> Unit,

    onDeleteSafeZone:
        (String) -> Unit,

    onWearerNameChange:
        (String) -> Unit,

    onNotificationSettingsChange:
        (NotificationSettings) -> Unit,

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
         * =====================================================
         * 홈
         * =====================================================
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
         * =====================================================
         * 현재 위치 / 귀가 요청
         * =====================================================
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

                safeZones =
                    snapshot.safeZones,

                returnHomeRequested =
                    snapshot.returnHomeRequested,

                returnHomeStatus =
                    snapshot.returnHomeStatus,

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
         * =====================================================
         * 이벤트 기록
         * =====================================================
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
         * =====================================================
         * 설정
         * =====================================================
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
                    snapshot.notificationSettings,

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

                onWatchPairingClick = {

                    onResetPairingState()

                    navController
                        .navigate(
                            GuardianRoute.WATCH_PAIRING
                        )
                },
            )
        }


        /*
         * =====================================================
         * 워치 페어링
         * =====================================================
         */

        composable(
            GuardianRoute.WATCH_PAIRING
        ) {

            WatchPairingScreen(

                state =
                    pairingState,

                onPairingClick =
                    onPairingCodeSubmit,

                onBack = {

                    onResetPairingState()

                    navController
                        .popBackStack()
                },

                onSuccessFinished = {

                    onResetPairingState()

                    navController
                        .popBackStack()
                },
            )
        }


        /*
         * =====================================================
         * 안전구역 목록
         * =====================================================
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

                onEditClick = { zoneId ->
                    navController.navigate(
                        GuardianRoute.safeZoneEdit(zoneId)
                    )
                },

                onDelete =
                    onDeleteSafeZone,
            )
        }


        /*
         * =====================================================
         * 안전구역 추가
         * =====================================================
         */

        composable(
            GuardianRoute.SAFE_ZONE_ADD
        ) {

            /*
             * 현재 HOME으로 지정되어 있는
             * 안전구역 이름을 찾는다.
             *
             * HOME이 없다면 null.
             */
            val existingHomeName =

                snapshot
                    .safeZones
                    .firstOrNull { zone ->

                        zone.kind ==
                                SafeZoneKind.HOME
                    }
                    ?.name


            AddSafeZoneScreen(

                initialLatitude =
                    snapshot.location.latitude,

                initialLongitude =
                    snapshot.location.longitude,

                /*
                 * 기존 HOME 이름 전달
                 *
                 * 새 안전구역에서
                 * "집으로 지정"을 켜면
                 * 기존 집이 교체된다는 안내에 사용.
                 */
                existingHomeName =
                    existingHomeName,

                onBack =
                    navController::
                    popBackStack,

                onSave = {
                        name,
                        radius,
                        latitude,
                        longitude,
                        isHome ->


                    /*
                     * ViewModel / Repository까지
                     * isHome 전달
                     */
                    onAddSafeZone(

                        name,

                        radius,

                        latitude,

                        longitude,

                        isHome,
                    )


                    navController
                        .popBackStack()
                },
            )
        }

        composable(
            "${GuardianRoute.SAFE_ZONE_EDIT}/{zoneId}"
        ) { backStackEntry ->
            val zoneId = backStackEntry.arguments?.getString("zoneId")
            val zone = snapshot.safeZones.firstOrNull { it.id == zoneId }

            if (zone == null) {
                LaunchedEffect(zoneId) {
                    navController.popBackStack()
                }
            } else {
                val existingHomeName = snapshot.safeZones
                    .firstOrNull {
                        it.kind == SafeZoneKind.HOME && it.id != zone.id
                    }
                    ?.name

                AddSafeZoneScreen(
                    initialLatitude = zone.centerLatitude,
                    initialLongitude = zone.centerLongitude,
                    existingHomeName = existingHomeName,
                    initialName = zone.name,
                    initialRadiusMeters = zone.radiusMeters,
                    initialIsHome = zone.kind == SafeZoneKind.HOME,
                    screenTitle = "안전구역 수정",
                    saveButtonText = "변경사항 저장",
                    onBack = navController::popBackStack,
                    onSave = { name, radius, latitude, longitude, isHome ->
                        onUpdateSafeZone(
                            zone.id,
                            name,
                            radius,
                            latitude,
                            longitude,
                            isHome,
                        )
                        navController.popBackStack()
                    },
                )
            }
        }


        /*
         * =====================================================
         * 긴급 상황
         * =====================================================
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
         * =====================================================
         * 착용자 상태
         * =====================================================
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

                onWearerNameChange =
                    onWearerNameChange,

                onBack =
                    navController::
                    popBackStack,
            )
        }
    }
}
