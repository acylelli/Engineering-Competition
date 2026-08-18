package com.watchsafety.guardian.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun GuardianBottomBar(
    currentRoute: String?,
    onDestinationSelected: (BottomDestination) -> Unit,
) {
    NavigationBar {
        BottomDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                    )
                },
                label = { Text(text = destination.label) },
                colors = NavigationBarItemDefaults.colors(),
            )
        }
    }
}
