package com.example.watchsafety.location

import org.junit.Assert.*
import org.junit.Test

class TrackingPolicyTest {
    @Test fun normalNavigationAndEmergencyUseRequestedIntervals() {
        assertEquals(30_000L, LocationTrackingMode.NORMAL.intervalMillis)
        assertEquals(30_000L, LocationTrackingMode.NORMAL.minIntervalMillis)
        for (mode in listOf(LocationTrackingMode.NAVIGATION, LocationTrackingMode.EMERGENCY)) {
            assertEquals(5_000L, mode.intervalMillis)
            assertEquals(2_000L, mode.minIntervalMillis)
        }
    }

    @Test fun fallKeepsFastTrackingUntilClearedAndNormalScreenRestoresSlowTracking() {
        assertEquals(LocationTrackingMode.EMERGENCY,
            LocationTrackingMode.effective(LocationTrackingMode.NORMAL, true))
        assertEquals(LocationTrackingMode.NAVIGATION,
            LocationTrackingMode.effective(LocationTrackingMode.NAVIGATION, false))
        assertEquals(LocationTrackingMode.NORMAL,
            LocationTrackingMode.effective(LocationTrackingMode.NORMAL, false))
    }

    @Test fun startupWaitsForConfirmationAndPairingCanStartTransmissionImmediately() {
        val state = TrackingConnectionState()
        assertFalse(state.canSend(0))
        state.onPairingConfirmed(false)
        assertFalse(state.canSend(60_000))
        state.onPairingConfirmed(true)
        assertTrue(state.canSend(60_000))
    }

    @Test fun unlinkStopsTransmissionAndRePairingResumesWithoutPolling() {
        val state = TrackingConnectionState()
        state.onPairingConfirmed(true)
        state.onPairingConfirmed(false)
        assertFalse(state.canSend(60_000))
        state.onNetworkAvailable()
        assertFalse(state.canSend(60_000))
        state.onPairingConfirmed(true)
        assertTrue(state.canSend(60_000))
    }

    @Test fun networkFailurePreservesPairingButBacksOffRequests() {
        val state = TrackingConnectionState()
        state.onPairingConfirmed(true)
        state.onSyncFailed(1_000)
        assertFalse(state.canSend(30_999))
        assertTrue(state.canSend(31_000))
        state.onSyncFailed(31_000)
        assertFalse(state.canSend(90_999))
        assertTrue(state.canSend(91_000))
        state.onNetworkAvailable()
        assertTrue(state.canSend(40_000))
    }

    @Test fun successResetsFailureBackoff() {
        val state = TrackingConnectionState()
        state.onPairingConfirmed(true)
        state.onSyncFailed(0)
        state.onSyncFailed(30_000)
        state.onSyncSucceeded()
        state.onSyncFailed(100_000)
        assertTrue(state.canSend(130_000))
    }
}
