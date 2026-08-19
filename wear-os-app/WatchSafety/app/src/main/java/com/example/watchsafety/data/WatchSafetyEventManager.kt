package com.example.watchsafety.data

import android.util.Log

import com.example.watchsafety.location.WatchLocation

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive


class WatchSafetyEventManager {

    private val supabase =
        SupabaseClientProvider.client


    /*
     * =====================================================
     * 낙상 감지
     * =====================================================
     */

    suspend fun recordFallSuspected(
        location: WatchLocation?
    ) {

        recordEvent(
            type = "FALL_SUSPECTED",
            title = "낙상 감지",
            description =
                "워치에서 낙상 가능성이 감지되었습니다.",
            location = location,
            trigger = "fall_detection"
        )
    }


    /*
     * =====================================================
     * 사용자가 괜찮다고 응답
     * =====================================================
     */

    suspend fun recordFallConfirmedSafe(
        location: WatchLocation?
    ) {

        recordEvent(
            type = "FALL_CONFIRMED_SAFE",
            title = "사용자 상태 확인",
            description =
                "낙상 확인 화면에서 사용자가 괜찮다고 응답했습니다.",
            location = location,
            trigger = "user_confirmed_safe"
        )
    }


    /*
     * =====================================================
     * 사용자가 직접 도움 요청
     * =====================================================
     */

    suspend fun recordManualSos(
        location: WatchLocation?
    ) {

        recordEvent(
            type = "SOS_MANUAL",
            title = "긴급 구조 요청",
            description =
                "사용자가 워치에서 직접 도움을 요청했습니다.",
            location = location,
            trigger = "help_button"
        )
    }


    /*
     * =====================================================
     * 10초 무응답 → 자동 SOS
     * =====================================================
     */

    suspend fun recordAutomaticSos(
        location: WatchLocation?
    ) {

        recordEvent(
            type = "SOS_AUTOMATIC",
            title = "자동 구조 요청",
            description =
                "낙상 감지 후 10초 동안 응답이 없어 자동으로 구조를 요청했습니다.",
            location = location,
            trigger = "fall_timeout"
        )
    }


    /*
     * =====================================================
     * 공통 이벤트 저장
     * =====================================================
     */

    private suspend fun recordEvent(

        type: String,

        title: String,

        description: String,

        location: WatchLocation?,

        trigger: String

    ) {

        /*
         * 익명 Auth 세션 확인
         */
        if (
            supabase
                .auth
                .currentUserOrNull() == null
        ) {

            supabase
                .auth
                .signInAnonymously()
        }


        /*
         * RPC 파라미터
         */
        val parameters =
            mutableMapOf<String, JsonElement>()


        parameters["p_type"] =
            JsonPrimitive(
                type
            )


        parameters["p_title"] =
            JsonPrimitive(
                title
            )


        parameters["p_description"] =
            JsonPrimitive(
                description
            )


        /*
         * GPS가 잡혀 있을 때만
         * 위도/경도를 전달한다.
         */
        if (
            location != null
        ) {

            parameters["p_latitude"] =
                JsonPrimitive(
                    location.latitude
                )


            parameters["p_longitude"] =
                JsonPrimitive(
                    location.longitude
                )


            parameters["p_address"] =
                JsonPrimitive(
                    "GPS 위치"
                )
        }


        /*
         * metadata JSON
         */
        parameters["p_metadata"] =
            JsonObject(
                mapOf(

                    "source" to
                            JsonPrimitive(
                                "watch"
                            ),

                    "trigger" to
                            JsonPrimitive(
                                trigger
                            )
                )
            )


        /*
         * Supabase RPC
         */
        supabase
            .postgrest
            .rpc(
                function =
                    "record_watch_safety_event",

                parameters =
                    JsonObject(
                        parameters
                    )
            )


        Log.d(
            TAG,
            "안전 이벤트 저장 성공: $type"
        )
    }


    companion object {

        private const val TAG =
            "WatchSafetyEvent"
    }
}