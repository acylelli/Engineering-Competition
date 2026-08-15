package com.example.watchsafety.safety

import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.HealthEvent
import androidx.health.services.client.data.PassiveListenerConfig
import kotlinx.coroutines.guava.await

class FallHealthServiceManager(
    context: Context
) {

    private val passiveMonitoringClient =
        HealthServices
            .getClient(context)
            .passiveMonitoringClient

    /**
     * 현재 워치의 Health Services가
     * FALL_DETECTED 이벤트를 지원하는지 확인
     */
    suspend fun isFallDetectionSupported(): Boolean {

        val capabilities =
            passiveMonitoringClient
                .getCapabilitiesAsync()
                .await()

        return capabilities
            .supportedHealthEventTypes
            .contains(
                HealthEvent.Type.FALL_DETECTED
            )
    }

    /**
     * Health Services에 낙상 이벤트 등록
     */
    suspend fun registerFallDetection() {

        val config =
            PassiveListenerConfig.builder()
                .setHealthEventTypes(
                    setOf(
                        HealthEvent.Type.FALL_DETECTED
                    )
                )
                .build()

        passiveMonitoringClient
            .setPassiveListenerServiceAsync(
                PassiveHealthEventService::class.java,
                config
            )
            .await()
    }

    /**
     * 낙상 이벤트 등록 해제
     */
    suspend fun unregisterFallDetection() {

        passiveMonitoringClient
            .clearPassiveListenerServiceAsync()
            .await()
    }
}