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


        /*
         * =================================================
         * MainActivity에 직접 전달할 Broadcast
         * =================================================
         */

        const val ACTION_RETURN_HOME_REQUEST_RECEIVED =
            "com.example.watchsafety.action.RETURN_HOME_REQUEST_RECEIVED"


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
         * 중복 방지
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
         * Realtime에서 이미 처리한 요청이라면
         * FCM 중복 처리하지 않는다.
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


        requestStore
            .markNotified(
                requestId
            )


        Log.d(
            TAG,
            "신규 귀가 요청 FCM 수신: $requestId"
        )


        /*
         * =================================================
         * 앱이 현재 화면에 떠 있는 경우
         * =================================================
         *
         * Notification을 띄우지 않고
         * MainActivity로 즉시 전달한다.
         */

        if (
            MainActivity.isInForeground
        ) {

            Log.d(
                TAG,
                "워치 앱 Foreground → 귀가 요청 화면 즉시 표시"
            )


            sendReturnHomeForegroundBroadcast(

                requestId =
                    requestId,

                guardianId =
                    guardianId,

                wearerId =
                    wearerId,
            )


            return
        }


        /*
         * =================================================
         * 앱이 Background인 경우
         * =================================================
         */

        Log.d(
            TAG,
            "워치 앱 Background → 귀가 요청 Notification 표시"
        )


        createReturnHomeChannel()


        vibrateReturnHomeRequest()


        showReturnHomeNotification(

            requestId =
                requestId,

            guardianId =
                guardianId,

            wearerId =
                wearerId,
        )
    }


    /*
     * =====================================================
     * Foreground Activity 전달
     * =====================================================
     */

    private fun sendReturnHomeForegroundBroadcast(

        requestId: String,

        guardianId: String,

        wearerId: String,
    ) {

        val intent =
            Intent(
                ACTION_RETURN_HOME_REQUEST_RECEIVED
            ).apply {

                setPackage(
                    packageName
                )


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


        sendBroadcast(
            intent
        )


        Log.d(
            TAG,
            "Foreground 귀가 요청 Broadcast 전송 requestId=$requestId"
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

        wearerId: String,
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