package com.example.watchsafety.safety

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object FallEventState {

    private val _fallDetected =
        MutableStateFlow(false)

    val fallDetected: StateFlow<Boolean> =
        _fallDetected

    fun onFallDetected() {
        _fallDetected.value = true
    }

    fun reset() {
        _fallDetected.value = false
    }
}