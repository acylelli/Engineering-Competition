package com.watchsafety.guardian.ui.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.watchsafety.guardian.ui.PairingUiState
import com.watchsafety.guardian.ui.components.GuardianTopBar
import com.watchsafety.guardian.ui.theme.SafeGreen
import kotlinx.coroutines.delay


@Composable
fun WatchPairingScreen(

    state: PairingUiState,

    onPairingClick:
        (String) -> Unit,

    onBack:
        () -> Unit,

    onSuccessFinished:
        () -> Unit,

    ) {

    var code by
    remember {
        mutableStateOf(
            ""
        )
    }


    /*
     * 연결 성공 후 잠깐 성공 화면 표시
     * → 이전 화면으로 이동
     */
    LaunchedEffect(
        state.isSuccess
    ) {

        if (
            state.isSuccess
        ) {

            delay(
                1_300
            )

            onSuccessFinished()
        }
    }


    Scaffold(

        topBar = {

            GuardianTopBar(
                title =
                    "워치 연결",

                onBack =
                    onBack,
            )
        }

    ) { innerPadding ->


        if (
            state.isSuccess
        ) {

            /*
             * =================================================
             * 연결 성공
             * =================================================
             */

            Column(

                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        innerPadding
                    )
                    .padding(
                        24.dp
                    ),

                horizontalAlignment =
                    Alignment.CenterHorizontally,

                verticalArrangement =
                    Arrangement.Center,

                ) {

                Icon(

                    imageVector =
                        Icons.Rounded
                            .CheckCircle,

                    contentDescription =
                        null,

                    tint =
                        SafeGreen,
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            16.dp
                        )
                )


                Text(

                    text =
                        "워치 연결 완료",

                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall,

                    fontWeight =
                        FontWeight.Bold,
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )


                Text(

                    text =
                        "보호자 앱과 워치가\n정상적으로 연결되었습니다.",

                    textAlign =
                        TextAlign.Center,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                )
            }

            return@Scaffold
        }


        /*
         * =====================================================
         * 연결 코드 입력
         * =====================================================
         */

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(
                    innerPadding
                )
                .padding(
                    24.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            ) {


            Spacer(
                modifier =
                    Modifier.height(
                        36.dp
                    )
            )


            Text(

                text =
                    "워치에 표시된\n6자리 코드를 입력해주세요",

                style =
                    MaterialTheme
                        .typography
                        .titleLarge,

                fontWeight =
                    FontWeight.Bold,

                textAlign =
                    TextAlign.Center,
            )


            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            Text(

                text =
                    "코드는 발급 후 10분 동안 사용할 수 있습니다.",

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,

                textAlign =
                    TextAlign.Center,
            )


            Spacer(
                modifier =
                    Modifier.height(
                        32.dp
                    )
            )


            /*
             * 6자리 입력
             */
            OutlinedTextField(

                value =
                    code,

                onValueChange = {
                        newValue ->

                    code =
                        newValue
                            .filter {
                                it.isDigit()
                            }
                            .take(
                                6
                            )
                },

                modifier =
                    Modifier.fillMaxWidth(),

                singleLine =
                    true,

                label = {

                    Text(
                        "연결 코드"
                    )
                },

                placeholder = {

                    Text(
                        "583241"
                    )
                },

                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Number
                    ),

                isError =
                    state.errorMessage !=
                            null,

                supportingText = {

                    if (
                        state.errorMessage !=
                        null
                    ) {

                        Text(
                            state.errorMessage
                        )
                    }
                },
            )


            Spacer(
                modifier =
                    Modifier.height(
                        22.dp
                    )
            )


            Button(

                onClick = {

                    onPairingClick(
                        code
                    )
                },

                enabled =
                    code.length == 6 &&
                            !state.isLoading,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            52.dp
                        ),

                ) {

                if (
                    state.isLoading
                ) {

                    Row(

                        horizontalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically,

                        ) {

                        CircularProgressIndicator()

                        Text(
                            "연결 중..."
                        )
                    }

                } else {

                    Text(

                        text =
                            "연결하기",

                        fontWeight =
                            FontWeight.Bold,
                    )
                }
            }
        }
    }
}