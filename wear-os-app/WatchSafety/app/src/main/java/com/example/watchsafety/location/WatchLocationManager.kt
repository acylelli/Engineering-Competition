package com.example.watchsafety.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WatchLocationManager(
    private val context: Context
) {

    private val fusedLocationClient:
            FusedLocationProviderClient =
        LocationServices
            .getFusedLocationProviderClient(
                context
            )

    private val _location =
        MutableStateFlow<WatchLocation?>(
            null
        )

    val location: StateFlow<WatchLocation?> =
        _location

    private val _isRunning =
        MutableStateFlow(false)

    val isRunning: StateFlow<Boolean> =
        _isRunning

    /*
     * 내비게이션 테스트용.
     *
     * 5초마다 위치 갱신 요청.
     * 최소 2초 간격.
     */
    private val locationRequest =
        LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5_000L
        )
            .setMinUpdateIntervalMillis(
                2_000L
            )
            .build()

    private val locationCallback =
        object : LocationCallback() {

            override fun onLocationResult(
                result: LocationResult
            ) {

                val androidLocation =
                    result.lastLocation
                        ?: return

                _location.value =
                    WatchLocation(
                        latitude =
                            androidLocation.latitude,

                        longitude =
                            androidLocation.longitude,

                        accuracyMeters =
                            androidLocation.accuracy
                    )
            }
        }

    fun start() {

        val fineLocationGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) ==
                    PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted) {
            return
        }

        if (_isRunning.value) {
            return
        }

        fusedLocationClient
            .requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

        _isRunning.value =
            true
    }

    fun stop() {

        fusedLocationClient
            .removeLocationUpdates(
                locationCallback
            )

        _isRunning.value =
            false
    }
}