package com.watchsafety.guardian.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Watch

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.watchsafety.guardian.data.MockGuardianRepository
import com.watchsafety.guardian.domain.model.GuardianUser
import com.watchsafety.guardian.domain.model.NotificationSettings
import com.watchsafety.guardian.domain.model.WatchStatus
import com.watchsafety.guardian.ui.components.GuardianTopBar
import com.watchsafety.guardian.ui.theme.DividerColor
import com.watchsafety.guardian.ui.theme.SafeGreen
import com.watchsafety.guardian.ui.theme.SafeGreenContainer
import com.watchsafety.guardian.ui.theme.TextSecondary
import com.watchsafety.guardian.ui.theme.TrustBlue
import com.watchsafety.guardian.ui.theme.TrustBlueContainer
import com.watchsafety.guardian.ui.theme.WatchSafetyTheme


@Composable
fun SettingsScreen(

    user: GuardianUser,

    watchStatus: WatchStatus,

    settings: NotificationSettings,

    onSettingsChange:
        (NotificationSettings) -> Unit,

    onUserStatusClick:
        () -> Unit,

    onSafeZonesClick:
        () -> Unit,

    /*
     * 워치 연결 화면 이동
     */
    onWatchPairingClick:
        () -> Unit,

    ) {

    Scaffold(

        topBar = {

            GuardianTopBar(
                title =
                    "설정"
            )
        }

    ) { innerPadding ->


        LazyColumn(

            modifier =
                Modifier.padding(
                    innerPadding
                ),

            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(
                        16.dp
                    ),

            verticalArrangement =
                Arrangement.spacedBy(
                    14.dp
                ),

            ) {


            /*
             * =================================================
             * 사용자 / 워치 상태
             * =================================================
             */

            item {

                UserConnectionCard(

                    user =
                        user,

                    watchStatus =
                        watchStatus,

                    onClick =
                        onUserStatusClick,
                )
            }


            item {

                BatteryStatusCard(
                    watchStatus =
                        watchStatus
                )
            }


            /*
             * =================================================
             * 알림 설정
             * =================================================
             */

            item {

                SectionTitle(
                    "알림 설정"
                )
            }


            item {

                SettingsCard {


                    NotificationSettingRow(

                        title =
                            "SOS 긴급 알림",

                        checked =
                            settings.sosAlert,

                        ) {

                        onSettingsChange(

                            settings.copy(
                                sosAlert =
                                    it
                            )
                        )
                    }


                    HorizontalDivider(
                        color =
                            DividerColor
                    )


                    NotificationSettingRow(

                        title =
                            "안전구역 이탈 알림",

                        checked =
                            settings
                                .safeZoneExitAlert,

                        ) {

                        onSettingsChange(

                            settings.copy(
                                safeZoneExitAlert =
                                    it
                            )
                        )
                    }


                    HorizontalDivider(
                        color =
                            DividerColor
                    )


                    NotificationSettingRow(

                        title =
                            "집 도착 알림",

                        checked =
                            settings.arrivalAlert,

                        ) {

                        onSettingsChange(

                            settings.copy(
                                arrivalAlert =
                                    it
                            )
                        )
                    }


                    HorizontalDivider(
                        color =
                            DividerColor
                    )


                    NotificationSettingRow(

                        title =
                            "배터리 부족 알림",

                        checked =
                            settings
                                .batteryLowAlert,

                        ) {

                        onSettingsChange(

                            settings.copy(
                                batteryLowAlert =
                                    it
                            )
                        )
                    }
                }
            }


            /*
             * =================================================
             * 일반
             * =================================================
             */

            item {

                SectionTitle(
                    "일반"
                )
            }


            item {

                SettingsCard {


                    /*
                     * -----------------------------
                     * 워치 연결
                     * -----------------------------
                     */

                    MenuSettingRow(

                        title =
                            "워치 연결",

                        icon =
                            Icons.Outlined.Watch,

                        onClick =
                            onWatchPairingClick,
                    )


                    HorizontalDivider(
                        color =
                            DividerColor
                    )


                    /*
                     * -----------------------------
                     * 안전구역
                     * -----------------------------
                     */

                    MenuSettingRow(

                        title =
                            "안전구역 관리",

                        icon =
                            Icons.Outlined.Security,

                        onClick =
                            onSafeZonesClick,
                    )


                    HorizontalDivider(
                        color =
                            DividerColor
                    )


                    /*
                     * -----------------------------
                     * 위치 데이터
                     * -----------------------------
                     */

                    MenuSettingRow(

                        title =
                            "위치 데이터 관리",

                        icon =
                            Icons.Outlined.Description,

                        onClick = {
                            // 추후 구현
                        },
                    )
                }
            }
        }
    }
}


/*
 * =========================================================
 * 사용자 연결 카드
 * =========================================================
 */

@Composable
private fun UserConnectionCard(

    user: GuardianUser,

    watchStatus: WatchStatus,

    onClick: () -> Unit,

    ) {

    Card(

        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick =
                    onClick
            ),

        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        Color.White
                ),

        border =
            BorderStroke(
                1.dp,
                DividerColor
            ),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        ) {


        Row(

            modifier =
                Modifier.padding(
                    16.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically,

            ) {


            /*
             * 사용자 아이콘
             */

            Box(

                modifier =
                    Modifier.size(
                        50.dp
                    ),

                contentAlignment =
                    Alignment.Center,

                ) {

                Card(

                    colors =
                        CardDefaults
                            .cardColors(
                                containerColor =
                                    TrustBlueContainer
                            ),

                    shape =
                        CircleShape,

                    ) {

                    Box(

                        modifier =
                            Modifier.size(
                                50.dp
                            ),

                        contentAlignment =
                            Alignment.Center,

                        ) {

                        Icon(

                            imageVector =
                                Icons.Outlined.Person,

                            contentDescription =
                                null,

                            tint =
                                TrustBlue,
                        )
                    }
                }
            }


            /*
             * 사용자 정보
             */

            Column(

                modifier =
                    Modifier
                        .weight(
                            1f
                        )
                        .padding(
                            start =
                                12.dp
                        ),

                ) {


                Text(

                    text =
                        "${user.name} 님",

                    fontWeight =
                        FontWeight.Bold,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                )


                Row(

                    verticalAlignment =
                        Alignment.CenterVertically,

                    ) {


                    Text(

                        text =
                            "${watchStatus.deviceName} ${
                                if (
                                    watchStatus.isConnected
                                ) {
                                    "연결됨"
                                } else {
                                    "연결 끊김"
                                }
                            }",

                        color =
                            TextSecondary,

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                    )


                    /*
                     * 연결 상태 점
                     */

                    Card(

                        modifier =
                            Modifier
                                .padding(
                                    start =
                                        7.dp
                                )
                                .size(
                                    7.dp
                                ),

                        colors =
                            CardDefaults
                                .cardColors(

                                    containerColor =
                                        if (
                                            watchStatus
                                                .isConnected
                                        ) {

                                            SafeGreen

                                        } else {

                                            TextSecondary
                                        }
                                ),

                        shape =
                            CircleShape,

                        ) {

                        Box(
                            modifier =
                                Modifier.size(
                                    7.dp
                                )
                        )
                    }
                }


                Text(

                    text =
                        "보호자: ${user.guardianName} (${user.guardianRelationship})",

                    color =
                        TextSecondary,

                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,
                )
            }


            Icon(

                imageVector =
                    Icons
                        .AutoMirrored
                        .Rounded
                        .KeyboardArrowRight,

                contentDescription =
                    "상세 보기",

                tint =
                    TextSecondary,
            )
        }
    }
}


/*
 * =========================================================
 * 배터리 상태
 * =========================================================
 */

@Composable
private fun BatteryStatusCard(

    watchStatus: WatchStatus,

    ) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        SafeGreenContainer
                ),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        ) {


        Row(

            modifier =
                Modifier.padding(
                    18.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically,

            ) {


            Icon(

                imageVector =
                    Icons.Outlined
                        .BatteryFull,

                contentDescription =
                    null,

                tint =
                    SafeGreen,

                modifier =
                    Modifier.size(
                        30.dp
                    ),
            )


            Text(

                text =
                    "배터리 ${watchStatus.batteryPercent}%",

                modifier =
                    Modifier
                        .weight(
                            1f
                        )
                        .padding(
                            start =
                                8.dp
                        ),

                color =
                    SafeGreen,

                fontWeight =
                    FontWeight.Bold,
            )


            Column {


                Text(

                    text =
                        "마지막 연결",

                    color =
                        TextSecondary,

                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,
                )


                Text(

                    text =
                        watchStatus
                            .lastConnectedLabel,

                    color =
                        SafeGreen,

                    fontWeight =
                        FontWeight.Bold,
                )
            }
        }
    }
}


/*
 * =========================================================
 * 공통 설정 카드
 * =========================================================
 */

@Composable
private fun SettingsCard(

    content:
    @Composable () -> Unit,

    ) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        Color.White
                ),

        border =
            BorderStroke(
                1.dp,
                DividerColor
            ),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        ) {

        Column(

            modifier =
                Modifier.padding(
                    horizontal =
                        16.dp
                ),

            content = {
                content()
            },
        )
    }
}


/*
 * =========================================================
 * 알림 설정 Row
 * =========================================================
 */

@Composable
private fun NotificationSettingRow(

    title: String,

    checked: Boolean,

    onCheckedChange:
        (Boolean) -> Unit,

    ) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical =
                        10.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically,

        ) {


        Text(

            text =
                title,

            modifier =
                Modifier.weight(
                    1f
                ),

            fontWeight =
                FontWeight.SemiBold,
        )


        Switch(

            checked =
                checked,

            onCheckedChange =
                onCheckedChange,

            colors =
                SwitchDefaults
                    .colors(

                        checkedThumbColor =
                            Color.White,

                        checkedTrackColor =
                            TrustBlue,
                    ),
        )
    }
}


/*
 * =========================================================
 * 일반 메뉴 Row
 * =========================================================
 */

@Composable
private fun MenuSettingRow(

    title: String,

    icon: ImageVector,

    onClick:
        () -> Unit,

    ) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick =
                        onClick
                )
                .padding(
                    vertical =
                        16.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically,

        ) {


        Box(

            modifier =
                Modifier.size(
                    34.dp
                ),

            contentAlignment =
                Alignment.Center,

            ) {

            Icon(

                imageVector =
                    icon,

                contentDescription =
                    null,

                tint =
                    TextSecondary,
            )
        }


        Text(

            text =
                title,

            modifier =
                Modifier
                    .weight(
                        1f
                    )
                    .padding(
                        start =
                            6.dp
                    ),

            fontWeight =
                FontWeight.SemiBold,
        )


        Icon(

            imageVector =
                Icons
                    .AutoMirrored
                    .Rounded
                    .KeyboardArrowRight,

            contentDescription =
                null,

            tint =
                TextSecondary,
        )
    }
}


/*
 * =========================================================
 * 섹션 제목
 * =========================================================
 */

@Composable
private fun SectionTitle(
    text: String,
) {

    Text(

        text =
            text,

        color =
            TextSecondary,

        fontWeight =
            FontWeight.SemiBold,

        style =
            MaterialTheme
                .typography
                .bodyMedium,
    )
}


/*
 * =========================================================
 * Preview
 * =========================================================
 */

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
)
@Composable
private fun SettingsDashboardPreview() {

    val preview =
        MockGuardianRepository()
            .snapshot
            .value


    WatchSafetyTheme {

        SettingsScreen(

            user =
                preview.user,

            watchStatus =
                preview.watchStatus,

            settings =
                preview
                    .notificationSettings,

            onSettingsChange = {},

            onUserStatusClick = {},

            onSafeZonesClick = {},

            onWatchPairingClick = {},
        )
    }
}