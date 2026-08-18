package com.example.watchsafety.ui // 본인의 패키지명에 맞게 수정하세요

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MedicationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            // 🚨 CLEAR_TOP 뒤에 'or Intent.FLAG_ACTIVITY_SINGLE_TOP' 을 꼭 추가해 주세요!
            // 이렇게 하면 기존 화면을 부수지 않고 그 위에 부드럽게 알림 화면만 띄웁니다.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EMERGENCY_TYPE", "MEDICATION_ALERT")
        }
        context.startActivity(launchIntent)
    }
}