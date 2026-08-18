package com.example.watchsafety.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.watchsafety.pairing.PairingManager


private enum class WatchScreen {

    HOME,

    NAVIGATION,

    PAIRING,

    PAIRING_SUCCESS
}


@Composable
fun WatchSafetyApp() {

    var currentScreen by
    remember {
        mutableStateOf(
            WatchScreen.HOME
        )
    }


    var guardianConnected by
    remember {
        mutableStateOf(
            false
        )
    }


    val pairingManager =
        remember {
            PairingManager()
        }


    BackHandler(
        enabled =
            currentScreen !=
                    WatchScreen.HOME
    ) {

        currentScreen =
            WatchScreen.HOME
    }


    when (
        currentScreen
    ) {

        WatchScreen.HOME -> {

            HomeScreen(

                guardianConnected =
                    guardianConnected,

                onGoHomeClick = {

                    currentScreen =
                        WatchScreen.NAVIGATION
                },

                onGuardianConnectClick = {

                    currentScreen =
                        WatchScreen.PAIRING
                }
            )
        }


        WatchScreen.NAVIGATION -> {

            TmapRouteTestScreen()
        }


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