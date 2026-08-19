package com.example.watchsafety.ui

import androidx.activity.compose.BackHandler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import com.example.watchsafety.pairing.PairingManager


/*
 * =========================================================
 * WatchSafetyApp 내부 화면
 * =========================================================
 */

private enum class WatchScreen {

    HOME,

    NAVIGATION,

    PAIRING,

    PAIRING_SUCCESS
}


/*
 * =========================================================
 * WatchSafetyApp
 * =========================================================
 */

@Composable
fun WatchSafetyApp() {


    /*
     * -----------------------------------------------------
     * 현재 화면
     * -----------------------------------------------------
     */

    var currentScreen by
    remember {

        mutableStateOf(
            WatchScreen.HOME
        )
    }


    /*
     * -----------------------------------------------------
     * 보호자 연결 상태
     * -----------------------------------------------------
     */

    var guardianConnected by
    remember {

        mutableStateOf(
            false
        )
    }


    /*
     * -----------------------------------------------------
     * 페어링 Manager
     * -----------------------------------------------------
     */

    val pairingManager =
        remember {

            PairingManager()
        }


    /*
     * -----------------------------------------------------
     * 뒤로가기
     * -----------------------------------------------------
     *
     * 홈이 아닌 화면에서 뒤로가기를 누르면
     * 홈으로 돌아간다.
     */

    BackHandler(

        enabled =
            currentScreen !=
                    WatchScreen.HOME

    ) {

        currentScreen =
            WatchScreen.HOME
    }


    /*
     * =====================================================
     * 화면 전환
     * =====================================================
     */

    when (
        currentScreen
    ) {


        /*
         * =================================================
         * 홈
         * =================================================
         */

        WatchScreen.HOME -> {

            HomeScreen(

                guardianConnected =
                    guardianConnected,


                /*
                 * 집으로 가기
                 */
                onGoHomeClick = {

                    currentScreen =
                        WatchScreen.NAVIGATION
                },


                /*
                 * SOS
                 *
                 * 실제 SOS 전송은 MainActivity의
                 * EmergencyManager에서 처리한다.
                 *
                 * 이 WatchSafetyApp은 현재 보조/이전
                 * 화면 구조이므로 여기서는 빈 동작으로 둔다.
                 */
                onSosClick = {

                    /*
                     * intentionally empty
                     */
                },


                /*
                 * 보호자 연결
                 */
                onGuardianConnectClick = {

                    /*
                     * 이미 보호자와 연결돼 있다면
                     * 다시 페어링 화면으로 보내지 않는다.
                     */

                    if (
                        !guardianConnected
                    ) {

                        currentScreen =
                            WatchScreen.PAIRING
                    }
                }
            )
        }


        /*
         * =================================================
         * 집으로 가기
         * =================================================
         */

        WatchScreen.NAVIGATION -> {

            TmapRouteTestScreen()
        }


        /*
         * =================================================
         * 보호자 페어링
         * =================================================
         */

        WatchScreen.PAIRING -> {

            PairingScreen(

                pairingManager =
                    pairingManager,


                onConnected = {

                    guardianConnected =
                        true


                    currentScreen =
                        WatchScreen.PAIRING_SUCCESS
                }
            )
        }


        /*
         * =================================================
         * 보호자 연결 성공
         * =================================================
         */

        WatchScreen.PAIRING_SUCCESS -> {

            PairingSuccessScreen(

                onFinished = {

                    currentScreen =
                        WatchScreen.HOME
                }
            )
        }
    }
}