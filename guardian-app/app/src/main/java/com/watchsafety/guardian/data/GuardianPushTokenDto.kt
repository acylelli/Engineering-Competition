package com.watchsafety.guardian.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class GuardianPushTokenDto(

    @SerialName("guardian_id")
    val guardianId: String,

    @SerialName("fcm_token")
    val fcmToken: String,

    val platform: String = "ANDROID",
)