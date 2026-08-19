package com.watchsafety.guardian.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class GuardianFirebaseMessagingService :
    FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d(
            "GuardianFCM",
            "새 FCM TOKEN = $token"
        )
    }

    override fun onMessageReceived(
        remoteMessage: RemoteMessage
    ) {
        super.onMessageReceived(remoteMessage)

        Log.d(
            "GuardianFCM",
            "FCM 메시지 수신"
        )

        Log.d(
            "GuardianFCM",
            "data = ${remoteMessage.data}"
        )

        Log.d(
            "GuardianFCM",
            "title = ${remoteMessage.notification?.title}"
        )

        Log.d(
            "GuardianFCM",
            "body = ${remoteMessage.notification?.body}"
        )
    }
}