package com.watchsafety.guardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.watchsafety.guardian.ui.GuardianApp
import com.watchsafety.guardian.ui.theme.WatchSafetyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WatchSafetyTheme {
                GuardianApp()
            }
        }
    }
}
