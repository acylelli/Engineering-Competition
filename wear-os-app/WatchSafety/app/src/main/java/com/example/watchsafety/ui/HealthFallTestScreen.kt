package com.example.watchsafety.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Text
import com.example.watchsafety.safety.FallEventState
import com.example.watchsafety.safety.FallHealthServiceManager
import com.example.watchsafety.ui.components.SafetyColors
import com.example.watchsafety.ui.components.ScreenTitle
import com.example.watchsafety.ui.components.StatusCard
import com.example.watchsafety.ui.components.StatusLevel
import com.example.watchsafety.ui.components.StatusMessageText

@Composable
fun HealthFallTestScreen() {

    val context = LocalContext.current

    val manager = remember {
        FallHealthServiceManager(context.applicationContext)
    }

    val fallDetected by FallEventState.fallDetected.collectAsState()

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var fallSupported by remember { mutableStateOf<Boolean?>(null) }

    var statusText by remember { mutableStateOf("Health Services 확인 중...") }

    /*
     * ACTIVITY_RECOGNITION 권한 요청
     */
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        permissionGranted = granted
        statusText = if (granted) {
            "활동 인식 권한이 허용되었습니다."
        } else {
            "활동 인식 권한이 필요합니다."
        }
    }

    /*
     * 화면 최초 진입 시 권한 상태 확인
     */
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED

        permissionGranted = granted

        if (!granted) {
            statusText = "활동 인식 권한 요청 중..."
            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    /*
     * 권한이 허용되면 낙상 이벤트 지원 여부 확인 후 등록
     */
    LaunchedEffect(permissionGranted) {
        if (!permissionGranted) {
            return@LaunchedEffect
        }

        statusText = "낙상 감지 지원 여부 확인 중..."

        try {
            val supported = manager.isFallDetectionSupported()
            fallSupported = supported

            if (!supported) {
                statusText = "이 워치는 낙상 이벤트를 지원하지 않습니다."
                return@LaunchedEffect
            }

            statusText = "낙상 감지 등록 중..."
            manager.registerFallDetection()
            statusText = "낙상 감지 준비 완료"

        } catch (e: Exception) {
            fallSupported = false
            statusText = "Health Services 등록 실패\n" + (e.message ?: "알 수 없는 오류")
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        ScreenTitle("낙상 감지 테스트")

        Spacer(modifier = Modifier.height(10.dp))

        StatusCard(
            label = "권한",
            value = if (permissionGranted) "허용됨" else "허용 안 됨",
            level = if (permissionGranted) StatusLevel.SUCCESS else StatusLevel.ERROR
        )

        Spacer(modifier = Modifier.height(6.dp))

        StatusCard(
            label = "낙상 이벤트",
            value = when (fallSupported) {
                true -> "지원"
                false -> "미지원"
                null -> "확인 중"
            },
            level = when (fallSupported) {
                true -> StatusLevel.SUCCESS
                false -> StatusLevel.ERROR
                null -> StatusLevel.LOADING
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        StatusMessageText(statusText)

        Spacer(modifier = Modifier.height(12.dp))

        /*
         * 낙상 감지됨 배너
         * 배경색이 있는 카드로 눈에 띄게 하여 디버그 중에도 놓치지 않도록 구성.
         */
        if (fallDetected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SafetyColors.Error.copy(alpha = 0.25f))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚠ 낙상 감지됨!",
                    style = TextStyle(
                        color = SafetyColors.Error,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        } else {
            StatusMessageText("낙상 이벤트 대기 중")
        }
    }
}