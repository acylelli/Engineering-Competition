package com.example.watchsafety.safety

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.HealthEvent
import com.example.watchsafety.ui.MainActivity

class PassiveHealthEventService : PassiveListenerService() {

    override fun onHealthEventReceived(event: HealthEvent) {
        super.onHealthEventReceived(event)

        Log.d("WatchSafety", "Health event: ${event.type}")

        if (event.type == HealthEvent.Type.FALL_DETECTED) {
            Log.d("WatchSafety", "낙상 감지됨!")

            // 1. 상태값 변경 (UI 업데이트용)
            FallEventState.onFallDetected()

            // 2. 화면이 꺼져있어도 무조건 깨우는 강제 기동 로직 (우리의 필살기!)
            triggerEmergencyScreen()
        }
    }

    private fun triggerEmergencyScreen() {
        // 진동 울리기
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1000)
        }

        // 캐싱 방지용 고유 ID 생성
        val uniqueId = (System.currentTimeMillis() % 10000).toInt()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra("EMERGENCY_TYPE", "FALL_DETECTED")
            putExtra("TIMESTAMP", System.currentTimeMillis())
        }

        // 1차: 포그라운드 띄우기 시도
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("WatchSafety", "startActivity 실패: ${e.message}")
        }

        // 2차: 백그라운드/절전 상태일 때 FullScreenIntent로 뚫기
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, uniqueId, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val emergencyNotification = NotificationCompat.Builder(this, "SafetyChannel")
            .setContentTitle("위급 상황 발생!")
            .setContentText("낙상이 감지되었습니다!")
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // 적절한 아이콘으로 변경 가능
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(uniqueId, emergencyNotification)
    }
}