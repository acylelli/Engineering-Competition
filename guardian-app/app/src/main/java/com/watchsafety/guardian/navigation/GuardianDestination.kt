package com.watchsafety.guardian.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector


object GuardianRoute {

    const val HOME =
        "home"

    const val MAP =
        "map"

    const val HISTORY =
        "history"

    const val SETTINGS =
        "settings"

    const val SAFE_ZONES =
        "safe-zones"

    const val SAFE_ZONE_ADD =
        "safe-zones/add"

    const val EMERGENCY =
        "emergency"

    const val USER_STATUS =
        "user-status"


    /*
     * 워치 6자리 연결 코드 입력 화면
     */
    const val WATCH_PAIRING =
        "watch-pairing"
}


enum class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {

    HOME(
        GuardianRoute.HOME,
        "홈",
        Icons.Rounded.Home,
    ),

    MAP(
        GuardianRoute.MAP,
        "지도",
        Icons.Rounded.Map,
    ),

    HISTORY(
        GuardianRoute.HISTORY,
        "이벤트",
        Icons.Rounded.History,
    ),

    SETTINGS(
        GuardianRoute.SETTINGS,
        "설정",
        Icons.Rounded.Settings,
    ),
}