package com.example.watchsafety.safety

import android.util.Log
import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.HealthEvent

class PassiveHealthEventService :
    PassiveListenerService() {

    override fun onHealthEventReceived(
        event: HealthEvent
    ) {

        super.onHealthEventReceived(event)

        Log.d(
            "WatchSafety",
            "Health event: ${event.type}"
        )

        if (
            event.type ==
            HealthEvent.Type.FALL_DETECTED
        ) {

            Log.d(
                "WatchSafety",
                "낙상 감지됨!"
            )

            FallEventState.onFallDetected()
        }
    }
}