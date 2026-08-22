package com.watchsafety.guardian.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp


@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onKakaoLogin: () -> Unit,
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 28.dp
                ),
        verticalArrangement =
            Arrangement.Center,
        horizontalAlignment =
            Alignment.CenterHorizontally,
    ) {

        Text(
            text =
                "WatchSafety",
            style =
                MaterialTheme
                    .typography
                    .headlineLarge,
            fontWeight =
                FontWeight.Bold,
        )


        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )


        Text(
            text =
                "보호자 계정으로 로그인해\n워치와 연결하세요.",
            style =
                MaterialTheme
                    .typography
                    .bodyLarge,
            textAlign =
                TextAlign.Center,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
        )


        Spacer(
            modifier =
                Modifier.height(
                    40.dp
                )
        )


        Button(
            onClick =
                onKakaoLogin,
            enabled =
                !isLoading,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        56.dp
                    ),
            colors =
                ButtonDefaults
                    .buttonColors(
                        containerColor =
                            Color(
                                0xFFFEE500
                            ),
                        contentColor =
                            Color(
                                0xFF191919
                            ),
                    ),
        ) {

            if (
                isLoading
            ) {

                CircularProgressIndicator()

            } else {

                Text(
                    text =
                        "카카오로 로그인",
                    fontWeight =
                        FontWeight.SemiBold,
                )
            }
        }


        if (
            errorMessage != null
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            Text(
                text =
                    errorMessage,
                color =
                    MaterialTheme
                        .colorScheme
                        .error,
                textAlign =
                    TextAlign.Center,
            )
        }
    }
}