package com.example.watchsafety.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

import com.example.watchsafety.R
import com.example.watchsafety.data.ReturnHomeRequestStore
import com.example.watchsafety.data.WatchFcmTokenManager
import com.example.watchsafety.ui.MainActivity

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


class WatchFirebaseMessagingService :
    FirebaseMessagingService() {


    companion object {

        private const val TAG =
            "WatchFCM"


        private const val CHANNEL_ID =
            "return_home_request"


        private const val CHANNEL_NAME =
            "귀가 요청"


        const val EXTRA_EMERGENCY_TYPE =
            "EMERGENCY_TYPE"

        const val EXTRA_REQUEST_ID =
            "RETURN_HOME_REQUEST_ID"

        const val EXTRA_GUARDIAN_ID =
            "RETURN_HOME_GUARDIAN_ID"

        const val EXTRA_WEARER_ID =
            "RETURN_HOME_WEARER_ID"


        const val TYPE_RETURN_HOME_REQUEST =
            "RETURN_HOME_REQUEST"


        /*
         * 같은 request_id로
         * 항상 같은 Notification ID 사용
         */
        fun notificationId(
            requestId: String
        ): Int {

            return requestId
                .hashCode()
        }
    }


    private val serviceScope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.IO
        )


    /*
     * =====================================================
     * Firebase Token 변경
     * =====================================================
     */

    override fun onNewToken(
        token: String
    ) {

        super.onNewToken(
            token
        )


        serviceScope.launch {

            runCatching {

                WatchFcmTokenManager()
                    .updateToken(
                        token
                    )

            }.onFailure { error ->

                Log.e(
                    TAG,
                    "FCM 토큰 저장 실패",
                    error
                )
            }
        }
    }


    /*
     * =====================================================
     * FCM 메시지
     * =====================================================
     */

    override fun onMessageReceived(
        message: RemoteMessage
    ) {

        super.onMessageReceived(
            message
        )


        val type =
            message.data[
                "type"
            ]


        if (
            type !=
            TYPE_RETURN_HOME_REQUEST
        ) {

            return
        }


        val requestId =
            message.data[
                "request_id"
            ]


        val guardianId =
            message.data[
                "guardian_id"
            ]


        val wearerId =
            message.data[
                "wearer_id"
            ]


        if (
            requestId.isNullOrBlank() ||
            guardianId.isNullOrBlank() ||
            wearerId.isNullOrBlank()
        ) {

            Log.e(
                TAG,
                "귀가 요청 데이터 누락"
            )

            return
        }


        /*
         * =================================================
         * 중복 방지 Store
         * =================================================
         */

        val requestStore =
            ReturnHomeRequestStore(
                applicationContext
            )


        /*
         * 이미 사용자가 수락한 요청
         */
        if (
            requestStore
                .isHandled(
                    requestId
                )
        ) {

            Log.d(
                TAG,
                "이미 처리 완료한 귀가 요청 무시: $requestId"
            )

            return
        }


        /*
         * 이미 FCM 또는 Realtime에서
         * 알림/화면 처리를 시작한 요청
         */
        if (
            requestStore
                .wasNotified(
                    requestId
                )
        ) {

            Log.d(
                TAG,
                "중복 귀가 요청 FCM 무시: $requestId"
            )

            return
        }


        /*
         * 먼저 기록해두고
         * 그 다음 알림을 표시한다.
         */
        requestStore
            .markNotified(
                requestId
            )


        Log.d(
            TAG,
            "신규 귀가 요청 FCM 수신: $requestId"
        )


        createReturnHomeChannel()


        vibrateReturnHomeRequest()


        showReturnHomeNotification(

            requestId =
                requestId,

            guardianId =
                guardianId,

            wearerId =
                wearerId
        )
    }


    /*
     * =====================================================
     * Notification Channel
     * =====================================================
     */

    private fun createReturnHomeChannel() {


        val manager =

            getSystemService(
                NotificationManager::class.java
            )


        if (
            manager
                .getNotificationChannel(
                    CHANNEL_ID
                ) != null
        ) {

            return
        }


        val channel =
            NotificationChannel(

                CHANNEL_ID,

                CHANNEL_NAME,

                NotificationManager
                    .IMPORTANCE_HIGH

            ).apply {

                description =
                    "보호자의 귀가 요청 알림"

                enableVibration(
                    true
                )

                vibrationPattern =
                    longArrayOf(
                        0,
                        350,
                        150,
                        350,
                        150,
                        600
                    )

                lockscreenVisibility =
                    Notification
                        .VISIBILITY_PUBLIC
            }


        manager
            .createNotificationChannel(
                channel
            )
    }


    /*
     * =====================================================
     * 진동
     * =====================================================
     */

    private fun vibrateReturnHomeRequest() {


        val vibrator =

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
            ) {

                val manager =

                    getSystemService(
                        Context.VIBRATOR_MANAGER_SERVICE
                    ) as VibratorManager


                manager
                    .defaultVibrator

            } else {

                @Suppress("DEPRECATION")

                getSystemService(
                    Context.VIBRATOR_SERVICE
                ) as Vibrator
            }


        if (
            !vibrator.hasVibrator()
        ) {

            return
        }


        val pattern =
            longArrayOf(
                0,
                350,
                150,
                350,
                150,
                600
            )


        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            vibrator.vibrate(

                VibrationEffect
                    .createWaveform(
                        pattern,
                        -1
                    )
            )

        } else {

            @Suppress("DEPRECATION")

            vibrator.vibrate(
                pattern,
                -1
            )
        }
    }


    /*
     * =====================================================
     * Notification
     * =====================================================
     */

    private fun showReturnHomeNotification(

        requestId: String,

        guardianId: String,

        wearerId: String

    ) {


        val intent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =

                    Intent.FLAG_ACTIVITY_NEW_TASK or

                            Intent.FLAG_ACTIVITY_CLEAR_TOP or

                            Intent.FLAG_ACTIVITY_SINGLE_TOP


                putExtra(
                    EXTRA_EMERGENCY_TYPE,
                    TYPE_RETURN_HOME_REQUEST
                )


                putExtra(
                    EXTRA_REQUEST_ID,
                    requestId
                )


                putExtra(
                    EXTRA_GUARDIAN_ID,
                    guardianId
                )


                putExtra(
                    EXTRA_WEARER_ID,
                    wearerId
                )
            }


        val pendingIntent =
            PendingIntent
                .getActivity(

                    this,

                    notificationId(
                        requestId
                    ),

                    intent,

                    PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                )


        val notification =
            Notification
                .Builder(
                    this,
                    CHANNEL_ID
                )

                .setSmallIcon(
                    R.drawable.ic_home_notification
                )

                .setContentTitle(
                    "보호자가 귀가를 요청했어요"
                )

                .setContentText(
                    "눌러서 집으로 안내를 시작하세요."
                )

                .setCategory(
                    Notification.CATEGORY_REMINDER
                )

                .setVisibility(
                    Notification.VISIBILITY_PUBLIC
                )

                .setAutoCancel(
                    true
                )

                .setContentIntent(
                    pendingIntent
                )

                .build()


        val manager =
            getSystemService(
                NotificationManager::class.java
            )


        manager
            .notify(

                notificationId(
                    requestId
                ),

                notification
            )
    }


    override fun onDestroy() {

        serviceScope
            .cancel()

        super.onDestroy()
    }
}