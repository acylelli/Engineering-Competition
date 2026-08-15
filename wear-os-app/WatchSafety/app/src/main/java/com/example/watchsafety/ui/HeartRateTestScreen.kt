package com.example.watchsafety.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Text
import com.example.watchsafety.health.HeartRateManager
import com.example.watchsafety.ui.components.SafetyColors
import com.example.watchsafety.ui.components.ScreenTitle
import com.example.watchsafety.ui.components.StatusCard
import com.example.watchsafety.ui.components.StatusLevel
import com.example.watchsafety.ui.components.StatusMessageText

@Composable
fun HeartRateTestScreen() {

    val context = LocalContext.current

    // Galaxy Watch 4 / API 33
    val heartRatePermission = Manifest.permission.BODY_SENSORS

    val heartRateManager = remember {
        HeartRateManager(context.applicationContext)
    }

    val heartRate by heartRateManager.heartRate.collectAsState()
    val isAvailable by heartRateManager.isAvailable.collectAsState()

    var isSupported by remember { mutableStateOf<Boolean?>(null) }

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                heartRatePermission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var statusText by remember { mutableStateOf("심박수 기능 확인 중...") }

    // ----------------------------------------
    // BODY_SENSORS 권한 요청
    // ----------------------------------------
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        statusText = if (granted) {
            "심박수 권한이 허용되었습니다."
        } else {
            "심박수 권한이 거부되었습니다."
        }
    }

    // ----------------------------------------
    // 처음 실행할 때 권한 확인
    // ----------------------------------------
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            heartRatePermission
        ) == PackageManager.PERMISSION_GRANTED

        permissionGranted = granted

        if (!granted) {
            statusText = "심박수 권한 요청 중..."
            permissionLauncher.launch(heartRatePermission)
        } else {
            statusText = "심박수 권한 확인 완료"
        }
    }

    // ----------------------------------------
    // 권한 허용 후 Health Services 확인
    // ----------------------------------------
    LaunchedEffect(permissionGranted) {
        if (!permissionGranted) {
            return@LaunchedEffect
        }

        statusText = "심박수 지원 여부 확인 중..."

        try {
            val supported = heartRateManager.isHeartRateSupported()
            isSupported = supported

            if (supported) {
                statusText = "심박수 측정 중..."
                heartRateManager.start()
            } else {
                statusText = "이 워치는 심박수 측정을 지원하지 않습니다."
            }

        } catch (e: Exception) {
            isSupported = false
            statusText = "심박수 기능 오류\n" + (e.message ?: "알 수 없는 오류")
        }
    }

    // ----------------------------------------
    // 화면 종료 시 측정 중지
    // ----------------------------------------
    DisposableEffect(Unit) {
        onDispose {
            heartRateManager.stop()
        }
    }

    // ----------------------------------------
    // UI
    // ----------------------------------------
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        ScreenTitle("심박수 테스트")

        Spacer(modifier = Modifier.height(8.dp))

        /*
         * BPM을 화면에서 가장 큰 요소로 배치해
         * 테스트 중에도 값 변화를 한눈에 확인할 수 있게 한다.
         */
        Text(
            text = heartRate?.let { "%.0f".format(it) } ?: "--",
            style = TextStyle(
                color = SafetyColors.Error,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Text(
            text = "BPM",
            style = TextStyle(
                color = SafetyColors.TextSecondary,
                fontSize = 11.sp
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        StatusCard(
            label = "권한",
            value = if (permissionGranted) "허용됨" else "허용 안 됨",
            level = if (permissionGranted) StatusLevel.SUCCESS else StatusLevel.ERROR
        )

        Spacer(modifier = Modifier.height(6.dp))

        StatusCard(
            label = "심박수 지원",
            value = when (isSupported) {
                true -> "지원"
                false -> "미지원"
                null -> "확인 중"
            },
            level = when (isSupported) {
                true -> StatusLevel.SUCCESS
                false -> StatusLevel.ERROR
                null -> StatusLevel.LOADING
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        StatusCard(
            label = "센서 상태",
            value = if (isAvailable) "사용 가능" else "준비 중",
            level = if (isAvailable) StatusLevel.SUCCESS else StatusLevel.WARNING
        )

        Spacer(modifier = Modifier.height(8.dp))

        StatusMessageText(statusText)
    }
}