package com.example.watchsafety.navigation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlin.math.PI


class WatchHeadingManager(
    context: Context
) : SensorEventListener {

    private val sensorManager =
        context.applicationContext
            .getSystemService(
                Context.SENSOR_SERVICE
            ) as SensorManager


    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(
            Sensor.TYPE_ROTATION_VECTOR
        )
            ?: sensorManager.getDefaultSensor(
                Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR
            )


    private val _headingDegrees =
        MutableStateFlow<Float?>(
            null
        )


    val headingDegrees: StateFlow<Float?> =
        _headingDegrees.asStateFlow()


    private var previousHeading: Float? =
        null


    fun start(): Boolean {

        val sensor =
            rotationSensor


        if (sensor == null) {

            Log.e(
                TAG,
                "방향 센서 없음: TYPE_ROTATION_VECTOR / TYPE_GEOMAGNETIC_ROTATION_VECTOR"
            )

            return false
        }


        val registered =
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )


        Log.d(
            TAG,
            "방향 센서 시작 " +
                    "name=${sensor.name}, " +
                    "type=${sensor.type}, " +
                    "registered=$registered"
        )


        return registered
    }


    fun stop() {

        sensorManager.unregisterListener(
            this
        )


        Log.d(
            TAG,
            "방향 센서 종료"
        )
    }


    override fun onSensorChanged(
        event: SensorEvent?
    ) {

        if (event == null) {
            return
        }


        if (
            event.sensor.type != Sensor.TYPE_ROTATION_VECTOR &&
            event.sensor.type != Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR
        ) {
            return
        }


        val rotationMatrix =
            FloatArray(
                9
            )


        val orientation =
            FloatArray(
                3
            )


        SensorManager.getRotationMatrixFromVector(
            rotationMatrix,
            event.values
        )


        SensorManager.getOrientation(
            rotationMatrix,
            orientation
        )


        var heading =
            orientation[0] *
                    180f /
                    PI.toFloat()


        heading =
            normalize360(
                heading
            )


        val previous =
            previousHeading


        val smoothedHeading =
            if (previous == null) {

                heading

            } else {

                smoothAngle(
                    current = previous,
                    target = heading,
                    factor = 0.25f
                )
            }


        previousHeading =
            smoothedHeading


        _headingDegrees.value =
            smoothedHeading


        Log.v(
            TAG,
            "heading=${smoothedHeading.toInt()}°"
        )
    }


    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {

        Log.d(
            TAG,
            "센서 정확도 변경 accuracy=$accuracy"
        )
    }


    private fun smoothAngle(
        current: Float,
        target: Float,
        factor: Float
    ): Float {

        val difference =
            (
                    (
                            target -
                                    current +
                                    540f
                            ) %
                            360f
                    ) -
                    180f


        return normalize360(
            current +
                    difference *
                    factor
        )
    }


    private fun normalize360(
        angle: Float
    ): Float {

        return (
                (
                        angle %
                                360f
                        ) +
                        360f
                ) %
                360f
    }


    companion object {

        private const val TAG =
            "WatchHeading"
    }
}
