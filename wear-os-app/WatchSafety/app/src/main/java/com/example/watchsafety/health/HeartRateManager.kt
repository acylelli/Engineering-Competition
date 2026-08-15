package com.example.watchsafety.health

import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.guava.await

class HeartRateManager(
    context: Context
) {

    private val measureClient =
        HealthServices
            .getClient(context)
            .measureClient

    private val _heartRate =
        MutableStateFlow<Double?>(null)

    val heartRate: StateFlow<Double?> =
        _heartRate

    private val _isAvailable =
        MutableStateFlow(false)

    val isAvailable: StateFlow<Boolean> =
        _isAvailable

    private val callback =
        object : MeasureCallback {

            override fun onAvailabilityChanged(
                dataType: DeltaDataType<*, *>,
                availability: Availability
            ) {

                if (dataType == DataType.HEART_RATE_BPM) {

                    /*
                     * Availability 클래스의 세부 상태에 의존하지 않고,
                     * callback이 들어왔다는 것만 기록한다.
                     *
                     * 실제 심박수 데이터가 들어오면 아래
                     * onDataReceived()에서 true로 변경된다.
                     */
                }
            }

            override fun onDataReceived(
                data: DataPointContainer
            ) {

                val heartRatePoints =
                    data.getData(
                        DataType.HEART_RATE_BPM
                    )

                val latest =
                    heartRatePoints.lastOrNull()

                if (latest != null) {

                    _heartRate.value =
                        latest.value

                    _isAvailable.value =
                        true
                }
            }
        }

    suspend fun isHeartRateSupported(): Boolean {

        val capabilities =
            measureClient
                .getCapabilitiesAsync()
                .await()

        return DataType.HEART_RATE_BPM in
                capabilities.supportedDataTypesMeasure
    }

    fun start() {

        measureClient.registerMeasureCallback(
            DataType.HEART_RATE_BPM,
            callback
        )
    }

    fun stop() {

        measureClient
            .unregisterMeasureCallbackAsync(
                DataType.HEART_RATE_BPM,
                callback
            )
    }
}