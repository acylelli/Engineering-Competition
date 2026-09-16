package com.example.watchsafety.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.watchsafety.safety.FallEventState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class WatchLocationManager(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    private val _location = MutableStateFlow<WatchLocation?>(null)
    val location: StateFlow<WatchLocation?> = _location
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning
    private var modeScope: CoroutineScope? = null
    private var modeJob: Job? = null
    private var activeCallback: LocationCallback? = null

    fun start() {
        if (modeJob?.isActive == true) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "GPS 시작 실패 - 위치 권한 없음")
            return
        }
        val scope = CoroutineScope(Dispatchers.Main.immediate)
        modeScope = scope
        modeJob = scope.launch {
            combine(LocationTrackingState.mode, FallEventState.fallDetected) { mode, fall ->
                LocationTrackingMode.effective(mode, fall)
            }.distinctUntilChanged().collect { mode ->
                requestUpdates(mode)
            }
        }
    }

    private fun requestUpdates(mode: LocationTrackingMode) {
        activeCallback?.let { client.removeLocationUpdates(it) }
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (activeCallback !== this) return
                val location = result.lastLocation ?: return
                _location.value = WatchLocation(
                    location.latitude, location.longitude, location.accuracy
                )
            }
        }
        activeCallback = callback
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, mode.intervalMillis
        ).setMinUpdateIntervalMillis(mode.minIntervalMillis).build()
        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnSuccessListener {
                    if (activeCallback === callback) {
                        _isRunning.value = true
                        Log.d(TAG, "GPS 주기 적용: $mode ${mode.intervalMillis}/${mode.minIntervalMillis}ms")
                    } else {
                        // 등록 도중 종료되거나 모드가 바뀐 요청도 반드시 해제한다.
                        client.removeLocationUpdates(callback)
                    }
                }
                .addOnFailureListener { error ->
                    if (activeCallback === callback) {
                        activeCallback = null
                        _isRunning.value = false
                        modeJob?.cancel()
                    }
                    Log.e(TAG, "GPS 등록 실패", error)
                }
        } catch (error: SecurityException) {
            activeCallback = null
            _isRunning.value = false
            modeScope?.cancel()
            Log.e(TAG, "GPS 권한 오류", error)
        }
    }

    fun stop() {
        modeScope?.cancel()
        modeScope = null
        modeJob = null
        val callback = activeCallback
        activeCallback = null
        if (callback != null) client.removeLocationUpdates(callback)
        _isRunning.value = false
    }

    private companion object {
        const val TAG = "WatchGPS"
    }
}
