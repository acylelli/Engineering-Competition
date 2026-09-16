package com.example.watchsafety.location

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LocationTrackingMode(val intervalMillis: Long, val minIntervalMillis: Long) {
    NORMAL(30_000L, 30_000L),
    NAVIGATION(5_000L, 2_000L),
    EMERGENCY(5_000L, 2_000L);

    companion object {
        fun effective(screenMode: LocationTrackingMode, fallDetected: Boolean) =
            if (fallDetected) EMERGENCY else screenMode
    }
}

/** 화면과 백그라운드 서비스가 같은 위치 주기를 사용한다. */
object LocationTrackingState {
    private val _mode = MutableStateFlow(LocationTrackingMode.NORMAL)
    val mode = _mode.asStateFlow()

    fun setMode(mode: LocationTrackingMode) {
        _mode.value = mode
    }
}
