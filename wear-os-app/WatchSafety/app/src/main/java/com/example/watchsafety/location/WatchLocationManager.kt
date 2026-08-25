package com.example.watchsafety.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
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

    companion object {
        private const val TAG =
            "WatchGPS"
    }


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

    val location:
            StateFlow<WatchLocation?> =
        _location


    private val _isRunning =
        MutableStateFlow(
            false
        )

    val isRunning:
            StateFlow<Boolean> =
        _isRunning


    /*
     * =====================================================
     * 위치 요청 설정
     * =====================================================
     *
     * 5초마다 위치 갱신 요청.
     * 최소 2초 간격.
     */
    private val locationRequest =
        LocationRequest
            .Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5_000L
            )
            .setMinUpdateIntervalMillis(
                2_000L
            )
            .build()


    /*
     * =====================================================
     * GPS 결과
     * =====================================================
     */
    private val locationCallback =
        object :
            LocationCallback() {

            override fun onLocationResult(
                result: LocationResult
            ) {

                val androidLocation =
                    result
                        .lastLocation
                        ?: run {

                            Log.w(
                                TAG,
                                "GPS 결과 수신했지만 lastLocation=null"
                            )

                            return
                        }


                Log.d(
                    TAG,
                    "GPS 수신 " +
                            "lat=${androidLocation.latitude}, " +
                            "lng=${androidLocation.longitude}, " +
                            "accuracy=${androidLocation.accuracy}m, " +
                            "provider=${androidLocation.provider}"
                )


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


    /*
     * =====================================================
     * GPS 시작
     * =====================================================
     */
    fun start() {

        val fineLocationGranted =
            ContextCompat
                .checkSelfPermission(
                    context,
                    Manifest.permission
                        .ACCESS_FINE_LOCATION
                ) ==
                    PackageManager
                        .PERMISSION_GRANTED


        if (
            !fineLocationGranted
        ) {

            Log.w(
                TAG,
                "GPS 시작 실패 - ACCESS_FINE_LOCATION 권한 없음"
            )

            return
        }


        if (
            _isRunning.value
        ) {

            Log.d(
                TAG,
                "GPS는 이미 실행 중"
            )

            return
        }


        Log.d(
            TAG,
            "GPS 위치 업데이트 요청 시작"
        )


        try {

            fusedLocationClient
                .requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
                .addOnSuccessListener {

                    _isRunning.value =
                        true

                    Log.d(
                        TAG,
                        "GPS 위치 업데이트 등록 성공"
                    )
                }
                .addOnFailureListener { error ->

                    _isRunning.value =
                        false

                    Log.e(
                        TAG,
                        "GPS 위치 업데이트 등록 실패",
                        error
                    )
                }

        } catch (
            securityException:
            SecurityException
        ) {

            _isRunning.value =
                false

            Log.e(
                TAG,
                "GPS 요청 중 SecurityException",
                securityException
            )
        }
    }


    /*
     * =====================================================
     * GPS 종료
     * =====================================================
     */
    fun stop() {

        if (
            !_isRunning.value
        ) {

            Log.d(
                TAG,
                "GPS가 실행 중이 아니므로 stop 생략"
            )

            return
        }


        fusedLocationClient
            .removeLocationUpdates(
                locationCallback
            )
            .addOnCompleteListener {

                Log.d(
                    TAG,
                    "GPS 위치 업데이트 종료"
                )
            }


        _isRunning.value =
            false
    }
}
