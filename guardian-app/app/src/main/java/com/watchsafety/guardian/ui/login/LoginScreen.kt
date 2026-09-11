package com.watchsafety.guardian.ui.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchsafety.guardian.R
import com.watchsafety.guardian.ui.theme.WatchSafetyTheme

private val LoginText = Color(0xFF6B7280)
private val DividerColor = Color(0xFFB8BDC7)
private val KakaoYellow = Color(0xFFFEE500)
private val KakaoBrown = Color(0xFF191919)

@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onKakaoLogin: () -> Unit,
) {
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        BrandSection(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = maxHeight * 0.20f),
        )

        LoginSection(
            isLoading = isLoading,
            errorMessage = errorMessage,
            onKakaoLogin = onKakaoLogin,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 34.dp)
                    .padding(bottom = 72.dp),
        )
    }
}

@Composable
private fun BrandSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.watch_safety_brand_logo),
            contentDescription = "WatchSafety",
            modifier = Modifier.size(width = 202.dp, height = 202.dp),
            contentScale = ContentScale.Fit,
        )

        Text(
            text = "가족의 안전을 이어주는 스마트 케어",
            color = LoginText,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoginSection(
    isLoading: Boolean,
    errorMessage: String?,
    onKakaoLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (errorMessage != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        ProviderButton(
            text = "카카오로 3초 만에 시작하기",
            backgroundColor = KakaoYellow,
            contentColor = KakaoBrown,
            enabled = !isLoading,
            onClick = onKakaoLogin,
            icon = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(19.dp),
                        color = KakaoBrown,
                        strokeWidth = 2.2.dp,
                    )
                } else {
                    KakaoMark()
                }
            },
        )

        Spacer(Modifier.height(9.dp))

        ProviderButton(
            text = "Apple로 계속하기",
            backgroundColor = Color.Black,
            contentColor = Color.White,
            enabled = true,
            onClick = {},
            icon = { AppleMark() },
        )

        Spacer(Modifier.height(15.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 0.5.dp,
                color = DividerColor,
            )
            Text(
                text = "또는",
                color = LoginText,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 0.5.dp,
                color = DividerColor,
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FacebookButton()
            GoogleButton()
        }

        Spacer(Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            FooterLabel("회원가입")
            FooterSeparator()
            FooterLabel("기존 계정으로 로그인")
            FooterSeparator()
            FooterLabel("문의하기")
        }
    }
}

@Composable
private fun ProviderButton(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp),
        shape = RoundedCornerShape(24.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                contentColor = contentColor,
                disabledContainerColor = backgroundColor.copy(alpha = 0.72f),
                disabledContentColor = contentColor.copy(alpha = 0.76f),
            ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.align(Alignment.CenterStart),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun KakaoMark() {
    Canvas(
        modifier =
            Modifier
                .size(width = 21.dp, height = 19.dp)
                .semantics { contentDescription = "카카오" },
    ) {
        drawOval(
            color = KakaoBrown,
            topLeft = Offset(0f, 0f),
            size = Size(size.width, size.height * 0.78f),
        )
        val tail =
            Path().apply {
                moveTo(size.width * 0.25f, size.height * 0.62f)
                lineTo(size.width * 0.18f, size.height)
                lineTo(size.width * 0.48f, size.height * 0.72f)
                close()
            }
        drawPath(tail, KakaoBrown)
    }
}

@Composable
private fun AppleMark() {
    Canvas(
        modifier =
            Modifier
                .size(20.dp)
                .semantics { contentDescription = "Apple" },
    ) {
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f
        val body =
            Path().apply {
                moveTo(12f * scaleX, 7.2f * scaleY)
                cubicTo(10.4f * scaleX, 7.2f * scaleY, 9.2f * scaleX, 6.1f * scaleY, 7.4f * scaleX, 6.6f * scaleY)
                cubicTo(4.8f * scaleX, 7.3f * scaleY, 3.6f * scaleX, 10.2f * scaleY, 4.4f * scaleX, 13.2f * scaleY)
                cubicTo(5.1f * scaleX, 16.1f * scaleY, 7f * scaleX, 19.7f * scaleY, 9.2f * scaleX, 19.6f * scaleY)
                cubicTo(10.3f * scaleX, 19.5f * scaleY, 10.8f * scaleX, 18.9f * scaleY, 12f * scaleX, 18.9f * scaleY)
                cubicTo(13.2f * scaleX, 18.9f * scaleY, 13.8f * scaleX, 19.6f * scaleY, 14.9f * scaleX, 19.6f * scaleY)
                cubicTo(17.2f * scaleX, 19.6f * scaleY, 19f * scaleX, 16.2f * scaleY, 19.7f * scaleX, 13.8f * scaleY)
                cubicTo(17.4f * scaleX, 12.8f * scaleY, 16.8f * scaleX, 9.5f * scaleY, 18.8f * scaleX, 8.1f * scaleY)
                cubicTo(17.5f * scaleX, 6.6f * scaleY, 15.4f * scaleX, 6.1f * scaleY, 13.8f * scaleX, 6.7f * scaleY)
                cubicTo(13.1f * scaleX, 6.9f * scaleY, 12.6f * scaleX, 7.2f * scaleY, 12f * scaleX, 7.2f * scaleY)
                close()
            }
        val leaf =
            Path().apply {
                moveTo(12.1f * scaleX, 5.7f * scaleY)
                cubicTo(12.3f * scaleX, 3.8f * scaleY, 13.7f * scaleX, 2.5f * scaleY, 15.5f * scaleX, 2.3f * scaleY)
                cubicTo(15.5f * scaleX, 4.1f * scaleY, 14.4f * scaleX, 5.5f * scaleY, 12.1f * scaleX, 5.7f * scaleY)
                close()
            }
        drawPath(body, Color.White)
        drawPath(leaf, Color.White)
    }
}

@Composable
private fun FacebookButton() {
    Surface(
        onClick = {},
        modifier =
            Modifier
                .size(46.dp)
                .semantics { contentDescription = "Facebook으로 로그인" },
        shape = CircleShape,
        color = Color(0xFF0866FF),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "f",
                color = Color.White,
                fontSize = 30.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun GoogleButton() {
    Surface(
        onClick = {},
        modifier =
            Modifier
                .size(46.dp)
                .semantics { contentDescription = "Google로 로그인" },
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            GoogleMark()
        }
    }
}

@Composable
private fun GoogleMark() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.17f, cap = StrokeCap.Butt)
        val inset = size.minDimension * 0.14f
        val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
        val topLeft = Offset(inset, inset)
        drawArc(Color(0xFF4285F4), -42f, 82f, false, topLeft, arcSize, style = stroke)
        drawArc(Color(0xFF34A853), 40f, 92f, false, topLeft, arcSize, style = stroke)
        drawArc(Color(0xFFFBBC05), 132f, 72f, false, topLeft, arcSize, style = stroke)
        drawArc(Color(0xFFEA4335), 204f, 114f, false, topLeft, arcSize, style = stroke)
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.53f, size.height * 0.5f),
            end = Offset(size.width * 0.88f, size.height * 0.5f),
            strokeWidth = stroke.width,
        )
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.84f, size.height * 0.48f),
            end = Offset(size.width * 0.84f, size.height * 0.69f),
            strokeWidth = stroke.width,
        )
    }
}

@Composable
private fun FooterLabel(text: String) {
    Text(
        text = text,
        color = LoginText,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    )
}

@Composable
private fun FooterSeparator() {
    Text(
        text = "|",
        color = DividerColor,
        fontSize = 11.sp,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

@Preview(showBackground = true, widthDp = 409, heightDp = 852)
@Composable
private fun LoginScreenPreview() {
    WatchSafetyTheme {
        LoginScreen(
            isLoading = false,
            errorMessage = null,
            onKakaoLogin = {},
        )
    }
}
