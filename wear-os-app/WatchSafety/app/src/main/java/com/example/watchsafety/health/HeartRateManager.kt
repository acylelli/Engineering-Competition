package com.example.watchsafety.health

import android.content.Context
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
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

    private var activeCallback: MeasureCallback? = null

    private fun createCallback(): MeasureCallback =
        object : MeasureCallback {

            override fun onRegistrationFailed(throwable: Throwable) {
                if (activeCallback !== this) return
                activeCallback = null
                _heartRate.value = null
                _isAvailable.value = false
                Log.w("WatchHeartRate", "심박수 센서 등록 실패", throwable)
            }

            override fun onAvailabilityChanged(
                dataType: DeltaDataType<*, *>,
                availability: Availability
            ) {

                if (activeCallback !== this) return
                if (dataType == DataType.HEART_RATE_BPM) {
                    _isAvailable.value = availability == DataTypeAvailability.AVAILABLE
                    if (!_isAvailable.value) {
                        _heartRate.value = null
                    }
                }
            }

            override fun onDataReceived(
                data: DataPointContainer
            ) {
                if (activeCallback !== this) return

                val heartRatePoints =
                    data.getData(
                        DataType.HEART_RATE_BPM
                    )

                val latest =
                    heartRatePoints.lastOrNull()

                if (latest != null && latest.value.isFinite() && latest.value > 0.0) {

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
        if (activeCallback != null) return
        val callback = createCallback()
        activeCallback = callback
        try {
            measureClient.registerMeasureCallback(
                DataType.HEART_RATE_BPM,
                callback
            )
        } catch (error: Exception) {
            activeCallback = null
            _heartRate.value = null
            _isAvailable.value = false
            throw error
        }
    }

    fun stop() {
        val callback = activeCallback
        activeCallback = null
        _heartRate.value = null
        _isAvailable.value = false
        if (callback != null) {
            measureClient
                .unregisterMeasureCallbackAsync(
                    DataType.HEART_RATE_BPM,
                    callback
                )
        }
    }
}
