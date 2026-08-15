package com.example.watchsafety.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Text
import com.example.watchsafety.location.WatchLocationManager
import com.example.watchsafety.ui.components.SafetyColors
import com.example.watchsafety.ui.components.ScreenTitle
import com.example.watchsafety.ui.components.StatusCard
import com.example.watchsafety.ui.components.StatusLevel
import com.example.watchsafety.ui.components.StatusMessageText

@Composable
fun LocationTestScreen() {

    val context = LocalContext.current

    val locationManager = remember {
        WatchLocationManager(context.applicationContext)
    }

    val location by locationManager.location.collectAsState()
    val isRunning by locationManager.isRunning.collectAsState()

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var statusText by remember { mutableStateOf("위치 기능 확인 중...") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        statusText = if (granted) {
            "위치 권한이 허용되었습니다."
        } else {
            "정확한 위치 권한이 필요합니다."
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        permissionGranted = granted

        if (!granted) {
            statusText = "위치 권한 요청 중..."
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(permissionGranted) {
        if (!permissionGranted) {
            return@LaunchedEffect
        }

        statusText = "GPS 위치 확인 중..."
        locationManager.start()
    }

    DisposableEffect(Unit) {
        onDispose {
            locationManager.stop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        ScreenTitle("GPS 테스트")

        Spacer(modifier = Modifier.height(10.dp))

        StatusCard(
            label = "권한",
            value = if (permissionGranted) "허용됨" else "허용 안 됨",
            level = if (permissionGranted) StatusLevel.SUCCESS else StatusLevel.ERROR
        )

        Spacer(modifier = Modifier.height(6.dp))

        StatusCard(
            label = "GPS 상태",
            value = if (isRunning) "측정 중" else "대기",
            level = if (isRunning) StatusLevel.SUCCESS else StatusLevel.NEUTRAL
        )

        Spacer(modifier = Modifier.height(8.dp))

        StatusMessageText(statusText)

        Spacer(modifier = Modifier.height(12.dp))

        /*
         * 좌표 정보
         * 텍스트를 낱개로 나열하던 방식 대신 하나의 카드로 묶어
         * 위도/경도/정확도가 한 그룹임을 시각적으로 보여준다.
         */
        val loc = location
        if (loc == null) {
            StatusMessageText("현재 위치를 찾고 있습니다...")
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SafetyColors.CardBackground)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "위도 %.6f".format(loc.latitude),
                    style = TextStyle(color = Color.White, fontSize = 12.sp)
                )
                Text(
                    text = "경도 %.6f".format(loc.longitude),
                    style = TextStyle(color = Color.White, fontSize = 12.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "정확도 %.1f m".format(loc.accuracyMeters),
                    style = TextStyle(color = SafetyColors.TextSecondary, fontSize = 11.sp)
                )
            }
        }
    }
}