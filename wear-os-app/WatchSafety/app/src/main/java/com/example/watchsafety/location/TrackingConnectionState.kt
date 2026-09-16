package com.example.watchsafety.location

/** 서버에서 확인한 연결 여부와 일시적 통신 실패를 구분한다. */
internal class TrackingConnectionState {
    private var paired = false
    private var retryAtMillis = 0L
    private var retryDelayMillis = 30_000L

    fun onPairingConfirmed(isPaired: Boolean) {
        paired = isPaired
        onSyncSucceeded()
    }

    fun canSend(nowMillis: Long) = paired && nowMillis >= retryAtMillis

    fun onSyncSucceeded() {
        retryAtMillis = 0L
        retryDelayMillis = 30_000L
    }

    fun onSyncFailed(nowMillis: Long) {
        retryAtMillis = nowMillis + retryDelayMillis
        retryDelayMillis = (retryDelayMillis * 2).coerceAtMost(300_000L)
    }

    fun onNetworkAvailable() = onSyncSucceeded()
}
