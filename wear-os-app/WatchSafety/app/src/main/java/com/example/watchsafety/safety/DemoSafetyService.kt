package com.example.watchsafety.safety

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
// 🚨 수정됨: MainActivity가 위치한 올바른 주소로 연결!
import com.example.watchsafety.ui.MainActivity
import kotlin.math.sqrt

class DemoSafetyService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastAlertTime: Long = 0
    private var impactTimestamp: Long? = null
    private var stillSince: Long? = null
    private var lastSampleTimestamp: Long = 0
    private var stillX = 0f
    private var stillY = 0f
    private var stillZ = 0f

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // 가속도 벡터 크기 계산
            val gForce = sqrt((x * x + y * y + z * z).toDouble()).toFloat() / SensorManager.GRAVITY_EARTH

            // 기존 충격 임계값은 유지하고, 이후 연속 2초간 정지해야 확정한다.
            if (gForce > 10.0f) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastAlertTime > 10000) { // 10초 쿨타임
                    impactTimestamp = event.timestamp
                    stillSince = null
                }
                return
            }

            val impact = impactTimestamp ?: return
            // 충격과 관계없는 나중의 정지가 낙상으로 처리되지 않도록 관찰을 제한한다.
            if (event.timestamp - impact > 10_000_000_000L) {
                impactTimestamp = null
                stillSince = null
                return
            }

            // 중력을 포함한 정지 상태와 자세 변화를 함께 확인한다 (노이즈 허용 0.15g).
            val nearGravity = kotlin.math.abs(gForce - 1f) <= 0.15f
            val dx = x - stillX
            val dy = y - stillY
            val dz = z - stillZ
            val movement = sqrt(dx * dx + dy * dy + dz * dz) / SensorManager.GRAVITY_EARTH
            val sampleGap = event.timestamp - lastSampleTimestamp > 500_000_000L
            lastSampleTimestamp = event.timestamp

            if (!nearGravity) {
                stillSince = null
                return
            }
            if (stillSince == null || movement > 0.15f || sampleGap) {
                stillSince = event.timestamp
                stillX = x
                stillY = y
                stillZ = z
                return
            }
            if (event.timestamp - (stillSince ?: return) >= 2_000_000_000L) {
                impactTimestamp = null
                stillSince = null
                lastAlertTime = System.currentTimeMillis()
                Log.d("WatchSafety", "충격 후 2초간 움직임 없음: 낙상 감지됨!")
                FallEventState.onFallDetected()
                triggerEmergencyScreen()
            }
        }
    }

    private fun triggerEmergencyScreen() {
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

        val uniqueId = (System.currentTimeMillis() % 10000).toInt()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {}

        val pendingIntent = android.app.PendingIntent.getActivity(
            this, uniqueId, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 🚨 추가됨: 안드로이드 8.0 이상 필수 알림 채널 생성 (이게 없으면 시연할 때 앱이 튕깁니다!)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "SafetyChannel",
                "긴급 알림",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "SafetyChannel")
            .setContentTitle("위급 상황!")
            .setContentText("낙상이 감지되었습니다!")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(uniqueId, notification)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
