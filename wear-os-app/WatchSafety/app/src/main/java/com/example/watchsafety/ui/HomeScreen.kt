package com.example.watchsafety.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.curvedText
import androidx.wear.compose.material.rememberScalingLazyListState
import com.example.watchsafety.ui.components.rememberBatteryLevel

/*
 * ---------------------------------------------------------
 * 홈 화면 색상
 * ---------------------------------------------------------
 */

private val HomeBackground = Color(0xFF090909)

private val HomeCardBackground = Color(0xFF1C1C1E)

private val HomeGreen = Color(0xFF69B95E)

private val HomeRed = Color(0xFFFF6868)

private val HomeBlue = Color(0xFF5575E7)

private val HomeSecondaryText = Color(0xFF9B9BA1)


@Composable
fun HomeScreen(
    onGoHomeClick: () -> Unit
) {

    val batteryLevel by rememberBatteryLevel()

    val listState = rememberScalingLazyListState()

    /*
     * TODO
     * 실제 HeartRateManager StateFlow 연결 전 임시 값
     */
    val heartRate = "72"

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground),

        /*
         * -------------------------------------------------
         * 원형 워치 상단
         *
         * 배경 박스 없이
         * 워치의 둥근 곡선을 따라 시간 + 배터리 표시
         * -------------------------------------------------
         */
        timeText = {

            val batteryColor =
                if (batteryLevel <= 20) {
                    HomeRed
                } else {
                    HomeGreen
                }

            TimeText(

                /*
                 * 원형 워치에서 표시되는 부분
                 */
                endCurvedContent = {

                    curvedText(
                        text = "  ▰ $batteryLevel%",
                        color = batteryColor,
                        fontSize = 9.sp
                    )
                },

                /*
                 * Preview나 사각 화면 fallback
                 */
                endLinearContent = {

                    Text(
                        text = "  ▰ $batteryLevel%",
                        style = TextStyle(
                            color = batteryColor,
                            fontSize = 9.sp
                        )
                    )
                }
            )
        },

        vignette = {

            Vignette(
                vignettePosition =
                    VignettePosition.TopAndBottom
            )
        }
    ) {

        /*
         * =================================================
         * 홈 콘텐츠
         *
         * ScalingLazyColumn이므로
         * 위/아래 스와이프로 스크롤 가능
         * =================================================
         */
        ScalingLazyColumn(

            modifier = Modifier
                .fillMaxSize(),

            state = listState,

            /*
             * 자동 중앙 정렬을 끄면
             * 맨 아래 버튼까지 자연스럽게 스크롤 가능
             */
            autoCentering = null,

            contentPadding = PaddingValues(
                top = 38.dp,
                bottom = 34.dp,
                start = 10.dp,
                end = 10.dp
            ),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.spacedBy(8.dp)

        ) {

            /*
             * =================================================
             * 1. 안전 체크 아이콘
             * =================================================
             */

            item {

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            HomeGreen.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    SafetyCheckIcon(
                        modifier = Modifier.size(27.dp)
                    )
                }
            }


            /*
             * =================================================
             * 2. 안전 텍스트
             * =================================================
             */

            item {

                Text(
                    text = "안전",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }


            /*
             * =================================================
             * 3. 낙상 / 심박 / 위치
             * =================================================
             */

            item {

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.Center,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    /*
                     * -----------------------------
                     * 낙상
                     * 넘어지는 사람 아이콘
                     * -----------------------------
                     */
                    HomeStatusCard(

                        icon = {

                            FallingPersonIcon(
                                modifier =
                                    Modifier.size(22.dp)
                            )
                        },

                        value = "켜짐"
                    )


                    Spacer(
                        modifier =
                            Modifier.width(7.dp)
                    )


                    /*
                     * -----------------------------
                     * 심박수
                     *
                     * 채워지지 않은
                     * 빨간 하트 테두리
                     * -----------------------------
                     */
                    HomeStatusCard(

                        icon = {

                            OutlineHeartIcon(
                                modifier =
                                    Modifier.size(22.dp)
                            )
                        },

                        value = heartRate
                    )


                    Spacer(
                        modifier =
                            Modifier.width(7.dp)
                    )


                    /*
                     * -----------------------------
                     * 위치
                     *
                     * 초록색 테두리 위치 핀
                     * -----------------------------
                     */
                    HomeStatusCard(

                        icon = {

                            OutlineLocationIcon(
                                modifier =
                                    Modifier.size(22.dp)
                            )
                        },

                        value = "확인"
                    )
                }
            }


            /*
             * 약간의 공간
             */
            item {

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )
            }


            /*
             * =================================================
             * 4. 집으로 가기
             *
             * 처음 화면에서 안 보여도
             * 아래로 스크롤하면 나타남
             * =================================================
             */

            item {

                Button(
                    onClick =
                        onGoHomeClick,

                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .height(42.dp),

                    colors =
                        ButtonDefaults.buttonColors(
                            backgroundColor =
                                HomeBlue
                        )
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,

                        horizontalArrangement =
                            Arrangement.Center
                    ) {

                        /*
                         * 집 아이콘
                         */
                        HomeOutlineIcon(
                            modifier =
                                Modifier.size(18.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(6.dp)
                        )

                        Text(
                            text = "집으로 가기",

                            style = TextStyle(
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        )
                    }
                }
            }


            /*
             * 버튼 아래 설명
             */
            item {

                Text(
                    text = "안전 귀가 안내",

                    style = TextStyle(
                        color =
                            HomeSecondaryText,

                        fontSize = 8.sp
                    )
                )
            }
        }
    }
}


/*
 * =========================================================
 *
 * 안전 체크 아이콘
 *
 * 배경 원 없이 초록 체크만 표시
 *
 * =========================================================
 */

@Composable
private fun SafetyCheckIcon(
    modifier: Modifier = Modifier
) {

    Canvas(
        modifier = modifier
    ) {

        val strokeWidth =
            3.5.dp.toPx()

        /*
         * 체크 왼쪽
         */
        drawLine(

            color = HomeGreen,

            start = Offset(
                x = size.width * 0.18f,
                y = size.height * 0.52f
            ),

            end = Offset(
                x = size.width * 0.42f,
                y = size.height * 0.75f
            ),

            strokeWidth = strokeWidth,

            cap = StrokeCap.Round
        )


        /*
         * 체크 오른쪽
         */
        drawLine(

            color = HomeGreen,

            start = Offset(
                x = size.width * 0.42f,
                y = size.height * 0.75f
            ),

            end = Offset(
                x = size.width * 0.82f,
                y = size.height * 0.28f
            ),

            strokeWidth = strokeWidth,

            cap = StrokeCap.Round
        )
    }
}


/*
 * =========================================================
 *
 * 낙상 아이콘
 *
 * 사람이 앞으로 넘어지는 모습을 단순화한 아이콘
 *
 * =========================================================
 */

@Composable
private fun FallingPersonIcon(
    modifier: Modifier = Modifier
) {

    Canvas(
        modifier = modifier
    ) {

        val color =
            HomeGreen

        val strokeWidth =
            2.dp.toPx()

        /*
         * 머리
         */
        drawCircle(

            color = color,

            radius =
                size.minDimension * 0.10f,

            center = Offset(
                x = size.width * 0.69f,
                y = size.height * 0.22f
            ),

            style = Stroke(
                width = strokeWidth
            )
        )


        /*
         * 몸통
         *
         * 오른쪽 아래 방향으로 기울어진 모습
         */
        drawLine(

            color = color,

            start = Offset(
                x = size.width * 0.60f,
                y = size.height * 0.35f
            ),

            end = Offset(
                x = size.width * 0.43f,
                y = size.height * 0.61f
            ),

            strokeWidth = strokeWidth,

            cap = StrokeCap.Round
        )


        /*
         * 왼팔
         */
        drawLine(

            color = color,

            start = Offset(
                x = size.width * 0.55f,
                y = size.height * 0.42f
            ),

            end = Offset(
                x = size.width * 0.77f,
                y = size.height * 0.51f
            ),

            strokeWidth = strokeWidth,

            cap = StrokeCap.Round
        )


        /*
         * 오른팔
         */
        drawLine(

            color = color,

            start = Offset(
                x = size.width * 0.52f,
                y = size.height * 0.45f
            ),

            end = Offset(
                x = size.width * 0.35f,
                y = size.height * 0.34f
            ),

            strokeWidth = strokeWidth,

            cap = StrokeCap.Round
        )


        /*
         * 왼쪽 다리
         */
        drawLine(

            color = color,

            start = Offset(
                x = size.width * 0.43f,
                y = size.height * 0.61f
            ),

            end = Offset(
                x = size.width * 0.20f,
                y = size.height * 0.77f
            ),

            strokeWidth = strokeWidth,

            cap = StrokeCap.Round
        )


        /*
         * 오른쪽 다리
         */
        drawLine(

            color = color,

            start = Offset(
                x = size.width * 0.43f,
                y = size.height * 0.61f
            ),

            end = Offset(
                x = size.width * 0.62f,
                y = size.height * 0.83f
            ),

            strokeWidth = strokeWidth,

            cap = StrokeCap.Round
        )


        /*
         * 바닥 / 낙상 느낌을 주는 작은 선
         */
        drawLine(

            color = color.copy(
                alpha = 0.7f
            ),

            start = Offset(
                x = size.width * 0.15f,
                y = size.height * 0.88f
            ),

            end = Offset(
                x = size.width * 0.82f,
                y = size.height * 0.88f
            ),

            strokeWidth =
                1.4.dp.toPx(),

            cap = StrokeCap.Round
        )
    }
}


/*
 * =========================================================
 *
 * 심박수 아이콘
 *
 * 채우지 않은 하트 테두리
 *
 * =========================================================
 */

@Composable
private fun OutlineHeartIcon(
    modifier: Modifier = Modifier
) {

    Canvas(
        modifier = modifier
    ) {

        val path =
            Path().apply {

                /*
                 * 아래 중앙부터 시작
                 */
                moveTo(
                    size.width * 0.50f,
                    size.height * 0.88f
                )


                /*
                 * 왼쪽 아래 → 왼쪽 위
                 */
                cubicTo(

                    size.width * 0.40f,
                    size.height * 0.77f,

                    size.width * 0.10f,
                    size.height * 0.58f,

                    size.width * 0.10f,
                    size.height * 0.34f
                )


                /*
                 * 왼쪽 하트 둥근 부분
                 */
                cubicTo(

                    size.width * 0.10f,
                    size.height * 0.13f,

                    size.width * 0.36f,
                    size.height * 0.08f,

                    size.width * 0.50f,
                    size.height * 0.28f
                )


                /*
                 * 오른쪽 하트 둥근 부분
                 */
                cubicTo(

                    size.width * 0.64f,
                    size.height * 0.08f,

                    size.width * 0.90f,
                    size.height * 0.13f,

                    size.width * 0.90f,
                    size.height * 0.34f
                )


                /*
                 * 오른쪽 → 아래 중앙
                 */
                cubicTo(

                    size.width * 0.90f,
                    size.height * 0.58f,

                    size.width * 0.60f,
                    size.height * 0.77f,

                    size.width * 0.50f,
                    size.height * 0.88f
                )

                close()
            }


        drawPath(

            path = path,

            color = HomeRed,

            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}


/*
 * =========================================================
 *
 * 위치 아이콘
 *
 * 사진과 비슷한
 * 테두리형 위치 핀
 *
 * =========================================================
 */

@Composable
private fun OutlineLocationIcon(
    modifier: Modifier = Modifier
) {

    Canvas(
        modifier = modifier
    ) {

        val strokeWidth =
            2.dp.toPx()

        /*
         * 위치 핀 외곽
         */
        val pinPath =
            Path().apply {

                moveTo(
                    size.width * 0.50f,
                    size.height * 0.91f
                )


                cubicTo(

                    size.width * 0.41f,
                    size.height * 0.80f,

                    size.width * 0.18f,
                    size.height * 0.58f,

                    size.width * 0.18f,
                    size.height * 0.38f
                )


                cubicTo(

                    size.width * 0.18f,
                    size.height * 0.18f,

                    size.width * 0.32f,
                    size.height * 0.08f,

                    size.width * 0.50f,
                    size.height * 0.08f
                )


                cubicTo(

                    size.width * 0.68f,
                    size.height * 0.08f,

                    size.width * 0.82f,
                    size.height * 0.18f,

                    size.width * 0.82f,
                    size.height * 0.38f
                )


                cubicTo(

                    size.width * 0.82f,
                    size.height * 0.58f,

                    size.width * 0.59f,
                    size.height * 0.80f,

                    size.width * 0.50f,
                    size.height * 0.91f
                )

                close()
            }


        drawPath(

            path = pinPath,

            color = HomeGreen,

            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )


        /*
         * 위치 핀 가운데 동그라미
         */
        drawCircle(

            color = HomeGreen,

            radius =
                size.minDimension * 0.105f,

            center = Offset(
                x = size.width * 0.50f,
                y = size.height * 0.37f
            ),

            style = Stroke(
                width = strokeWidth
            )
        )
    }
}


/*
 * =========================================================
 *
 * 집 아이콘
 *
 * 버튼용 심플한 흰색 Outline Home
 *
 * =========================================================
 */

@Composable
private fun HomeOutlineIcon(
    modifier: Modifier = Modifier
) {

    Canvas(
        modifier = modifier
    ) {

        val color =
            Color.White

        val stroke =
            1.8.dp.toPx()


        /*
         * 지붕 왼쪽
         */
        drawLine(

            color = color,

            start = Offset(
                size.width * 0.14f,
                size.height * 0.47f
            ),

            end = Offset(
                size.width * 0.50f,
                size.height * 0.16f
            ),

            strokeWidth = stroke,

            cap = StrokeCap.Round
        )


        /*
         * 지붕 오른쪽
         */
        drawLine(

            color = color,

            start = Offset(
                size.width * 0.50f,
                size.height * 0.16f
            ),

            end = Offset(
                size.width * 0.86f,
                size.height * 0.47f
            ),

            strokeWidth = stroke,

            cap = StrokeCap.Round
        )


        /*
         * 왼쪽 벽
         */
        drawLine(

            color = color,

            start = Offset(
                size.width * 0.24f,
                size.height * 0.40f
            ),

            end = Offset(
                size.width * 0.24f,
                size.height * 0.84f
            ),

            strokeWidth = stroke,

            cap = StrokeCap.Round
        )


        /*
         * 오른쪽 벽
         */
        drawLine(

            color = color,

            start = Offset(
                size.width * 0.76f,
                size.height * 0.40f
            ),

            end = Offset(
                size.width * 0.76f,
                size.height * 0.84f
            ),

            strokeWidth = stroke,

            cap = StrokeCap.Round
        )


        /*
         * 바닥
         */
        drawLine(

            color = color,

            start = Offset(
                size.width * 0.24f,
                size.height * 0.84f
            ),

            end = Offset(
                size.width * 0.76f,
                size.height * 0.84f
            ),

            strokeWidth = stroke,

            cap = StrokeCap.Round
        )


        /*
         * 문 왼쪽
         */
        drawLine(

            color = color,

            start = Offset(
                size.width * 0.43f,
                size.height * 0.84f
            ),

            end = Offset(
                size.width * 0.43f,
                size.height * 0.61f
            ),

            strokeWidth = stroke,

            cap = StrokeCap.Round
        )


        /*
         * 문 오른쪽
         */
        drawLine(

            color = color,

            start = Offset(
                size.width * 0.57f,
                size.height * 0.84f
            ),

            end = Offset(
                size.width * 0.57f,
                size.height * 0.61f
            ),

            strokeWidth = stroke,

            cap = StrokeCap.Round
        )


        /*
         * 문 위
         */
        drawLine(

            color = color,

            start = Offset(
                size.width * 0.43f,
                size.height * 0.61f
            ),

            end = Offset(
                size.width * 0.57f,
                size.height * 0.61f
            ),

            strokeWidth = stroke,

            cap = StrokeCap.Round
        )
    }
}


/*
 * =========================================================
 *
 * 낙상 / 심박수 / 위치 공통 카드
 *
 * =========================================================
 */

@Composable
private fun HomeStatusCard(

    icon: @Composable () -> Unit,

    value: String

) {

    Column(

        modifier = Modifier
            .width(54.dp)
            .height(54.dp)
            .clip(
                RoundedCornerShape(14.dp)
            )
            .background(
                HomeCardBackground
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        icon()


        Spacer(
            modifier =
                Modifier.height(4.dp)
        )


        Text(
            text = value,

            style = TextStyle(

                color = Color.White,

                fontSize = 11.sp,

                fontWeight =
                    FontWeight.Bold
            )
        )
    }
}