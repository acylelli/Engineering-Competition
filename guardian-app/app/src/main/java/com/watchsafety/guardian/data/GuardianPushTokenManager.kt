package com.watchsafety.guardian.data

import android.util.Log

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from


class GuardianPushTokenManager(
    private val supabase: SupabaseClient,
) {

    suspend fun syncToken(
        token: String,
    ) {

        /*
         * 기존 Supabase Auth 세션 복원이
         * 끝날 때까지 기다린다.
         */
        supabase
            .auth
            .awaitInitialization()


        val session =
            supabase
                .auth
                .currentSessionOrNull()

                ?: run {

                    Log.w(
                        TAG,
                        "Supabase 세션 없음 - FCM Token 저장 보류"
                    )

                    return
                }


        val guardianId =
            session
                .user
                ?.id

                ?: return


        Log.d(
            TAG,
            "FCM Token 저장 시작 guardian=$guardianId"
        )


        /*
         * 동일 FCM token이면 UPDATE,
         * 없으면 INSERT.
         */
        supabase
            .from(
                "guardian_push_tokens"
            )
            .upsert(

                GuardianPushTokenDto(

                    guardianId =
                        guardianId,

                    fcmToken =
                        token,
                )

            ) {

                onConflict =
                    "fcm_token"
            }


        Log.d(
            TAG,
            "FCM Token Supabase 저장 완료"
        )
    }


    private companion object {

        const val TAG =
            "GuardianPushToken"
    }
}