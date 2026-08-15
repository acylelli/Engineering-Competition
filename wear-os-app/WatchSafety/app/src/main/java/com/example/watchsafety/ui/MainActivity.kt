package com.example.watchsafety.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.watchsafety.ui.theme.WatchSafetyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContent {

            WatchSafetyTheme {

                WatchSafetyApp()
            }
        }
    }
}