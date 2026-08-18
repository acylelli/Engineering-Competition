package com.example.watchsafety.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberScalingLazyListState
import com.example.watchsafety.pairing.PairingManager
import java.time.Duration
import java.time.OffsetDateTime
import kotlinx.coroutines.delay


private val PairingBackground =
    Color(0xFF090909)

private val PairingCard =
    Color(0xFF1C1C1E)

private val PairingBlue =
    Color(0xFF5575E7)

private val PairingGreen =
    Color(0xFF69B95E)

private val PairingSecondary =
    Color(0xFF9B9BA1)

private val PairingError =
    Color(0xFFFF6868)


@Composable
fun PairingScreen(
    pairingManager: PairingManager,
    onConnected: () -> Unit
) {

    val listState =
        rememberScalingLazyListState()

    var pairingCode by
    remember {
        mutableStateOf<String?>(null)
    }

    var expiresAt by
    remember {
        mutableStateOf<OffsetDateTime?>(null)
    }

    var remainingSeconds by
    remember {
        mutableIntStateOf(0)
    }

    var loading by
    remember {
        mutableStateOf(true)
    }

    var errorMessage by
    remember {
        mutableStateOf<String?>(null)
    }

    var requestVersion by
    remember {
        mutableIntStateOf(0)
    }


    /*
     * ---------------------------------------------------------
     * 코드 발급
     * ---------------------------------------------------------
     */
    LaunchedEffect(requestVersion) {

        loading = true
        errorMessage = null

        try {

            /*
             * 이미 연결된 워치이면
             * 새 코드 만들 필요 없음.
             */
            if (
                pairingManager.isPaired()
            ) {

                onConnected()

                return@LaunchedEffect
            }

            val result =
                pairingManager
                    .createPairingCode()

            pairingCode =
                result.code

            expiresAt =
                OffsetDateTime.parse(
                    result.expiresAt
                )

        } catch (
            e: Exception
        ) {

            errorMessage =
                e.message
                    ?: "연결 코드를 가져오지 못했습니다."

        } finally {

            loading = false
        }
    }


    /*
     * ---------------------------------------------------------
     * 10분 카운트다운
     * ---------------------------------------------------------
     */
    LaunchedEffect(expiresAt) {

        val expiry =
            expiresAt
                ?: return@LaunchedEffect

        while (true) {

            val seconds =
                Duration
                    .between(
                        OffsetDateTime.now(),
                        expiry
                    )
                    .seconds
                    .coerceAtLeast(0)

            remainingSeconds =
                seconds
                    .coerceAtMost(
                        Int.MAX_VALUE.toLong()
                    )
                    .toInt()

            if (
                seconds <= 0
            ) {
                break
            }

            delay(1_000)
        }
    }


    /*
     * ---------------------------------------------------------
     * 보호자가 연결을 완료했는지 확인
     *
     * Pairing 화면에 있을 때만 2초마다 확인.
     * ---------------------------------------------------------
     */
    LaunchedEffect(pairingCode) {

        if (
            pairingCode == null
        ) {
            return@LaunchedEffect
        }

        while (
            remainingSeconds > 0
        ) {

            try {

                if (
                    pairingManager.isPaired()
                ) {

                    onConnected()

                    break
                }

            } catch (
                _: Exception
            ) {
                // 다음 확인에서 다시 시도
            }

            delay(2_000)
        }
    }


    /*
     * ---------------------------------------------------------
     * UI
     * ---------------------------------------------------------
     */

    ScalingLazyColumn(

        modifier = Modifier
            .fillMaxSize()
            .background(
                PairingBackground
            ),

        state = listState,

        autoCentering = null,

        contentPadding =
            PaddingValues(
                top = 28.dp,
                bottom = 28.dp,
                start = 14.dp,
                end = 14.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.spacedBy(8.dp)

    ) {


        item {

            Text(
                text =
                    "보호자와 연결",

                style =
                    TextStyle(
                        color =
                            Color.White,

                        fontSize =
                            18.sp,

                        fontWeight =
                            FontWeight.Bold
                    )
            )
        }


        item {

            Text(
                text =
                    "보호자 앱에서 아래 코드를\n입력해주세요",

                textAlign =
                    TextAlign.Center,

                style =
                    TextStyle(
                        color =
                            PairingSecondary,

                        fontSize =
                            10.sp
                    )
            )
        }


        /*
         * 코드
         */
        item {

            Box(

                modifier =
                    Modifier
                        .fillMaxWidth(0.78f)
                        .clip(
                            RoundedCornerShape(
                                18.dp
                            )
                        )
                        .background(
                            PairingCard
                        ),

                contentAlignment =
                    Alignment.Center

            ) {

                Text(

                    text =
                        when {

                            loading ->
                                "------"

                            pairingCode != null ->
                                formatPairingCode(
                                    pairingCode!!
                                )

                            else ->
                                "------"
                        },

                    modifier =
                        Modifier
                            .fillMaxWidth(),

                    textAlign =
                        TextAlign.Center,

                    style =
                        TextStyle(
                            color =
                                Color.White,

                            fontSize =
                                25.sp,

                            fontWeight =
                                FontWeight.Bold,

                            letterSpacing =
                                2.sp
                        )
                )
            }
        }


        /*
         * 남은 시간
         */
        item {

            val expired =
                pairingCode != null &&
                        remainingSeconds <= 0

            Text(
                text =
                    if (expired) {

                        "코드가 만료되었습니다"

                    } else {

                        "남은 시간  " +
                                formatCountdown(
                                    remainingSeconds
                                )
                    },

                style =
                    TextStyle(
                        color =
                            if (expired) {
                                PairingError
                            } else {
                                PairingGreen
                            },

                        fontSize =
                            10.sp,

                        fontWeight =
                            FontWeight.SemiBold
                    )
            )
        }


        /*
         * 작고 둥근 새 코드 발급 버튼
         */
        item {

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Box(

                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(
                                CircleShape
                            )
                            .background(
                                PairingBlue
                            )
                            .clickable(
                                enabled =
                                    !loading
                            ) {

                                requestVersion++
                            },

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "↻",

                        style =
                            TextStyle(
                                color =
                                    Color.White,

                                fontSize =
                                    19.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )


                Text(
                    text =
                        "새 코드",

                    style =
                        TextStyle(
                            color =
                                PairingSecondary,

                            fontSize =
                                8.sp
                        )
                )
            }
        }


        if (
            errorMessage != null
        ) {

            item {

                Text(
                    text =
                        errorMessage!!,

                    textAlign =
                        TextAlign.Center,

                    style =
                        TextStyle(
                            color =
                                PairingError,

                            fontSize =
                                9.sp
                        )
                )
            }
        }
    }
}


@Composable
fun PairingSuccessScreen(
    onFinished: () -> Unit
) {

    LaunchedEffect(Unit) {

        delay(
            1_500
        )

        onFinished()
    }


    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    PairingBackground
                ),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center

    ) {

        Box(

            modifier =
                Modifier
                    .size(60.dp)
                    .clip(
                        CircleShape
                    )
                    .background(
                        PairingGreen
                            .copy(
                                alpha = 0.15f
                            )
                    ),

            contentAlignment =
                Alignment.Center

        ) {

            Text(
                text = "✓",

                style =
                    TextStyle(
                        color =
                            PairingGreen,

                        fontSize =
                            34.sp,

                        fontWeight =
                            FontWeight.Bold
                    )
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )


        Text(
            text =
                "연결 완료",

            style =
                TextStyle(
                    color =
                        Color.White,

                    fontSize =
                        19.sp,

                    fontWeight =
                        FontWeight.Bold
                )
        )


        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )


        Text(
            text =
                "보호자와 연결되었습니다",

            style =
                TextStyle(
                    color =
                        PairingSecondary,

                    fontSize =
                        10.sp
                )
        )
    }
}


private fun formatPairingCode(
    code: String
): String {

    if (
        code.length != 6
    ) {
        return code
    }

    return code.substring(
        0,
        3
    ) +
            " " +
            code.substring(
                3,
                6
            )
}


private fun formatCountdown(
    totalSeconds: Int
): String {

    val minutes =
        totalSeconds / 60

    val seconds =
        totalSeconds % 60

    return "%02d:%02d"
        .format(
            minutes,
            seconds
        )
}