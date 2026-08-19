package com.watchsafety.guardian.ui

import android.util.Log

import androidx.compose.foundation.layout.padding

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
fun GuardianApp(

    /*
     * MainActivity에서 FCM 알림 클릭을 감지하면
     * 값이 증가한다.
     */

    emergencyRequestVersion:
    Long = 0L,
) {


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
     * =====================================================
     * 메인 상태
     * =====================================================
     */

    val uiState by
    guardianViewModel
        .uiState
        .collectAsStateWithLifecycle()


    /*
     * =====================================================
     * 워치 페어링 상태
     * =====================================================
     */

    val pairingState by
    guardianViewModel
        .pairingUiState
        .collectAsStateWithLifecycle()


    /*
     * =====================================================
     * Realtime SOS
     * =====================================================
     *
     * 보호자 앱 프로세스가 살아있는 상태에서
     * Supabase Realtime으로 SOS를 받으면
     * 즉시 긴급화면으로 이동한다.
     */

    LaunchedEffect(
        guardianViewModel
    ) {

        guardianViewModel
            .newSosEvent
            .collect {


                Log.d(
                    NAVIGATION_TAG,
                    "🚨 Realtime SOS 이벤트 수신"
                )


                val currentDestination =

                    navController
                        .currentBackStackEntry
                        ?.destination
                        ?.route


                Log.d(
                    NAVIGATION_TAG,
                    "현재 화면=$currentDestination"
                )


                /*
                 * 이미 긴급 화면이면
                 * 다시 이동하지 않음
                 */
                if (
                    currentDestination ==
                    GuardianRoute.EMERGENCY
                ) {

                    return@collect
                }


                navController
                    .navigate(
                        GuardianRoute.EMERGENCY
                    ) {

                        launchSingleTop =
                            true
                    }


                Log.d(
                    NAVIGATION_TAG,
                    "🚨 Realtime → EMERGENCY 이동 완료"
                )
            }
    }


    /*
     * =====================================================
     * FCM Push 클릭
     * =====================================================
     *
     * 앱 백그라운드 또는 종료 상태에서
     * FCM 알림을 사용자가 누르면
     *
     * MainActivity
     *     ↓
     * emergencyRequestVersion 증가
     *     ↓
     * 여기서 감지
     *     ↓
     * EMERGENCY
     */

    LaunchedEffect(
        emergencyRequestVersion
    ) {


        /*
         * 0은 일반 앱 실행
         */
        if (
            emergencyRequestVersion <= 0L
        ) {

            return@LaunchedEffect
        }


        Log.d(
            NAVIGATION_TAG,
            "🚨 Push 클릭 화면전환 요청 version=$emergencyRequestVersion"
        )


        val currentDestination =

            navController
                .currentBackStackEntry
                ?.destination
                ?.route


        Log.d(
            NAVIGATION_TAG,
            "Push 클릭 당시 화면=$currentDestination"
        )


        /*
         * 이미 Emergency면 중복 이동 방지
         */
        if (
            currentDestination ==
            GuardianRoute.EMERGENCY
        ) {

            Log.d(
                NAVIGATION_TAG,
                "이미 EMERGENCY 화면"
            )


            return@LaunchedEffect
        }


        /*
         * 긴급화면 이동
         */

        navController
            .navigate(
                GuardianRoute.EMERGENCY
            ) {

                launchSingleTop =
                    true
            }


        Log.d(
            NAVIGATION_TAG,
            "🚨 Push 클릭 → EMERGENCY 이동 완료"
        )
    }


    /*
     * =====================================================
     * 현재 Route
     * =====================================================
     */

    val currentRoute =

        navController
            .currentBackStackEntryAsState()
            .value
            ?.destination
            ?.route


    /*
     * =====================================================
     * BottomBar 선택 상태
     * =====================================================
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
     * =====================================================
     * BottomBar 표시 여부
     * =====================================================
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
     * Scaffold
     * =====================================================
     */

    Scaffold(


        containerColor =
            MaterialTheme
                .colorScheme
                .background,


        /*
         * =================================================
         * BottomBar
         * =================================================
         */

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


            snapshot =
                uiState.snapshot,


            isRefreshing =
                uiState.isRefreshing,


            /*
             * 상태 새로고침
             */

            onRefreshStatus =
                guardianViewModel::
                refreshStatus,


            /*
             * 귀가 요청
             */

            onReturnHomeRequest =
                guardianViewModel::
                sendReturnHomeRequest,


            /*
             * 안전구역
             */

            onSafeZoneEnabledChange =
                guardianViewModel::
                setSafeZoneEnabled,


            onAddSafeZone =
                guardianViewModel::
                addSafeZone,


            /*
             * 알림 설정
             */

            onNotificationSettingsChange =
                guardianViewModel::
                updateNotificationSettings,


            /*
             * 워치 페어링
             */

            pairingState =
                pairingState,


            onPairingCodeSubmit =
                guardianViewModel::
                redeemPairingCode,


            onResetPairingState =
                guardianViewModel::
                resetPairingState,


            /*
             * Padding
             */

            modifier =
                Modifier.padding(
                    innerPadding
                ),
        )
    }
}


/*
 * =========================================================
 * Log TAG
 * =========================================================
 */

private const val NAVIGATION_TAG =
    "GuardianNavigation"