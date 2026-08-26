package com.example.watchsafety.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
private data class EmergencyContactResponse(
    @SerialName("is_configured")
    val isConfigured: Boolean,
    @SerialName("contact_name")
    val contactName: String? = null,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
)


data class EmergencyContact(
    val name: String,
    val phoneNumber: String,
)


enum class EmergencyCallStatus {
    IDLE,
    CONNECTING,
    CALL_STARTED,
    CONTACT_NOT_CONFIGURED,
    PERMISSION_REQUIRED,
    PERMISSION_DENIED,
    CALLING_UNAVAILABLE,
    FAILED,
}


class EmergencyCallManager(
    context: Context,
) {

    private val applicationContext =
        context.applicationContext

    private val supabase =
        SupabaseClientProvider.client

    private val preferences =
        applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )


    fun hasCallPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.CALL_PHONE,
        ) == PackageManager.PERMISSION_GRANTED


    suspend fun refreshContact(): EmergencyContact? {
        supabase
            .auth
            .awaitInitialization()

        if (
            supabase
                .auth
                .currentUserOrNull() == null
        ) {
            supabase
                .auth
                .signInAnonymously()
        }

        val response =
            supabase
                .postgrest
                .rpc(
                    function =
                        "get_watch_emergency_contact"
                )
                .decodeSingle<EmergencyContactResponse>()

        val normalizedPhoneNumber =
            response
                .phoneNumber
                ?.normalizePhoneNumber()

        if (
            !response.isConfigured ||
            normalizedPhoneNumber == null
        ) {
            clearCachedContact()
            return null
        }

        val contact =
            EmergencyContact(
                name =
                    response
                        .contactName
                        .orEmpty()
                        .ifBlank {
                            "보호자"
                        },
                phoneNumber =
                    normalizedPhoneNumber,
            )

        preferences
            .edit()
            .putString(
                KEY_CONTACT_NAME,
                contact.name
            )
            .putString(
                KEY_PHONE_NUMBER,
                contact.phoneNumber
            )
            .apply()

        Log.d(
            TAG,
            "긴급 연락처 동기화 완료"
        )

        return contact
    }


    suspend fun placePrimaryGuardianCall(): EmergencyCallStatus {
        if (
            !hasCallPermission()
        ) {
            return EmergencyCallStatus.PERMISSION_REQUIRED
        }

        val contact =
            runCatching {
                refreshContact()
            }.onFailure { error ->
                Log.w(
                    TAG,
                    "긴급 연락처 온라인 조회 실패, 캐시 사용: ${error.message}",
                    error,
                )
            }.getOrNull()
                ?: cachedContact()
                ?: return EmergencyCallStatus.CONTACT_NOT_CONFIGURED

        val telecomManager =
            applicationContext
                .getSystemService(
                    TelecomManager::class.java
                )
                ?: return EmergencyCallStatus.CALLING_UNAVAILABLE

        return runCatching {
            val extras =
                Bundle().apply {
                    putBoolean(
                        TelecomManager
                            .EXTRA_START_CALL_WITH_SPEAKERPHONE,
                        true,
                    )
                }

            telecomManager.placeCall(
                Uri.fromParts(
                    "tel",
                    contact.phoneNumber,
                    null,
                ),
                extras,
            )

            Log.d(
                TAG,
                "보호자 긴급 통화 요청 시작"
            )

            EmergencyCallStatus.CALL_STARTED
        }.getOrElse { error ->
            Log.e(
                TAG,
                "보호자 긴급 통화 요청 실패",
                error,
            )

            when (error) {
                is SecurityException ->
                    EmergencyCallStatus.PERMISSION_REQUIRED

                else ->
                    EmergencyCallStatus.FAILED
            }
        }
    }


    private fun cachedContact(): EmergencyContact? {
        val phoneNumber =
            preferences
                .getString(
                    KEY_PHONE_NUMBER,
                    null,
                )
                ?.normalizePhoneNumber()
                ?: return null

        return EmergencyContact(
            name =
                preferences
                    .getString(
                        KEY_CONTACT_NAME,
                        null,
                    )
                    .orEmpty()
                    .ifBlank {
                        "보호자"
                    },
            phoneNumber =
                phoneNumber,
        )
    }


    private fun clearCachedContact() {
        preferences
            .edit()
            .remove(
                KEY_CONTACT_NAME
            )
            .remove(
                KEY_PHONE_NUMBER
            )
            .apply()
    }


    private fun String.normalizePhoneNumber(): String? {
        val normalized =
            trim()
                .filterIndexed { index, character ->
                    character.isDigit() ||
                            (character == '+' && index == 0)
                }

        return normalized
            .takeIf {
                it.matches(
                    Regex("^\\+?[0-9]{8,15}$")
                )
            }
    }


    companion object {
        private const val TAG =
            "EmergencyCall"

        private const val PREFERENCES_NAME =
            "emergency_call"

        private const val KEY_CONTACT_NAME =
            "contact_name"

        private const val KEY_PHONE_NUMBER =
            "phone_number"
    }
}
