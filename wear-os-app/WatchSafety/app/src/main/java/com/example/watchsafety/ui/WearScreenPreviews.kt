package com.example.watchsafety.ui

import androidx.compose.runtime.Composable
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import com.example.watchsafety.ui.theme.WatchSafetyTheme

@WearPreviewDevices
@Composable
private fun HomeSafePreview() {
    WatchSafetyTheme {
        HomeScreen(
            guardianConnected = true,
            safeZoneStatus = SafeZoneStatus.SAFE,
            onGoHomeClick = {},
            onSosClick = {},
            onGuardianConnectClick = {},
            previewBatteryLevel = 82
        )
    }
}

@WearPreviewLargeRound
@Composable
private fun HomeCheckingLocationPreview() {
    WatchSafetyTheme {
        HomeScreen(
            guardianConnected = false,
            safeZoneStatus = SafeZoneStatus.CHECKING,
            onGoHomeClick = {},
            onSosClick = {},
            onGuardianConnectClick = {},
            previewBatteryLevel = 18
        )
    }
}

@WearPreviewDevices
@Composable
private fun HomeOutsideSafeZonePreview() {
    WatchSafetyTheme {
        HomeScreen(
            guardianConnected = true,
            safeZoneStatus = SafeZoneStatus.OUTSIDE,
            onGoHomeClick = {},
            onSosClick = {},
            onGuardianConnectClick = {},
            previewBatteryLevel = 64
        )
    }
}

@WearPreviewDevices
@Composable
private fun PairingCodePreview() {
    WatchSafetyTheme {
        PairingScreen(
            pairingManager = null,
            onConnected = {},
            previewPairingCode = "123456",
            previewRemainingSeconds = 487
        )
    }
}

@WearPreviewLargeRound
@Composable
private fun PairingErrorPreview() {
    WatchSafetyTheme {
        PairingScreen(
            pairingManager = null,
            onConnected = {},
            previewErrorMessage = "연결 코드를 가져오지 못했습니다."
        )
    }
}

@WearPreviewDevices
@Composable
private fun PairingSuccessPreview() {
    WatchSafetyTheme {
        PairingSuccessScreen(onFinished = {})
    }
}

@WearPreviewDevices
@Composable
private fun FallDetectedPreview() {
    WatchSafetyTheme {
        FallDetectScreen(
            onOkayClick = {},
            onHelpClick = {},
            onTimeout = {}
        )
    }
}

@WearPreviewDevices
@Composable
private fun SosSentPreview() {
    WatchSafetyTheme {
        SosSentScreen(
            callStatus =
                com.example.watchsafety.data
                    .EmergencyCallStatus
                    .CALL_STARTED,
            onReturnHome = {},
        )
    }
}
