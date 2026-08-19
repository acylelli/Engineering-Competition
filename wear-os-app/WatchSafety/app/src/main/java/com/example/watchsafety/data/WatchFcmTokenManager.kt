package com.example.watchsafety.data

import android.util.Log

import com.example.watchsafety.pairing.PairingManager
import com.google.firebase.messaging.FirebaseMessaging

import io.github.jan.supabase.postgrest.postgrest

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class WatchFcmTokenManager {


    companion object {

        private const val TAG =
            "WatchFCM"
    }


    private val supabase =
        SupabaseClientProvider.client


    private val pairingManager =
        PairingManager()


    /*
     * =====================================================
     * 현재 Firebase Token 가져와서 Supabase 저장
     * =====================================================
     */

    suspend fun syncCurrentToken() {


        pairingManager
            .ensureAuthenticated()


        val token =
            getCurrentFirebaseToken()


        updateToken(
            token
        )
    }


    /*
     * =====================================================
     * Supabase RPC
     * =====================================================
     */

    suspend fun updateToken(
        token: String
    ) {


        if (
            token.isBlank()
        ) {

            return
        }


        pairingManager
            .ensureAuthenticated()


        supabase
            .postgrest
            .rpc(

                function =
                    "update_watch_fcm_token",

                parameters =
                    buildJsonObject {

                        put(
                            "p_token",
                            token
                        )
                    }
            )


        Log.d(
            TAG,
            "FCM 토큰 Supabase 저장 완료"
        )
    }


    /*
     * =====================================================
     * Firebase Token
     * =====================================================
     */

    private suspend fun getCurrentFirebaseToken():
            String =


        suspendCoroutine {
                continuation ->


            FirebaseMessaging
                .getInstance()
                .token
                .addOnCompleteListener {
                        task ->


                    if (
                        task.isSuccessful
                    ) {


                        val token =
                            task.result


                        Log.d(
                            TAG,
                            "FCM 토큰 발급 성공"
                        )


                        continuation
                            .resume(
                                token
                            )


                    } else {


                        continuation
                            .resumeWithException(

                                task.exception

                                    ?: IllegalStateException(
                                        "FCM 토큰 발급 실패"
                                    )
                            )
                    }
                }
        }
}