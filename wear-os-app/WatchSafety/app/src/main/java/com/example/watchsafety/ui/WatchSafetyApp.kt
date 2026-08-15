package com.example.watchsafety.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private enum class WatchScreen {
    HOME,
    NAVIGATION
}

@Composable
fun WatchSafetyApp() {

    var currentScreen by remember {
        mutableStateOf(WatchScreen.HOME)
    }

    BackHandler(
        enabled = currentScreen != WatchScreen.HOME
    ) {
        currentScreen = WatchScreen.HOME
    }

    when (currentScreen) {

        WatchScreen.HOME -> {

            HomeScreen(
                onGoHomeClick = {
                    currentScreen = WatchScreen.NAVIGATION
                }
            )
        }

        WatchScreen.NAVIGATION -> {

            TmapRouteTestScreen()
        }
    }
}