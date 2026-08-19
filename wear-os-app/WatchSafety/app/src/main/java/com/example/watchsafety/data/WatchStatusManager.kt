package com.example.watchsafety.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log

import androidx.core.content.ContextCompat

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive


class WatchStatusManager(
    context: Context
) {

    private val appContext =
        context.applicationContext


    private val supabase =
        SupabaseClientProvider.client


    /*
     * =====================================================
     * 마지막으로 서버에 정상 저장된 배터리 %
     * =====================================================
     */

    private var lastSyncedBatteryPercent:
            Int? = null


    /*
     * =====================================================
     * 배터리 BroadcastReceiver
     * =====================================================
     */

    private var batteryReceiver:
            BroadcastReceiver? = null


    /*
     * =====================================================
     * 배터리 감시 시작
     * =====================================================
     */

    fun start(
        scope: CoroutineScope
    ) {

        /*
         * 이미 등록되어 있다면
         * Receiver 중복 등록 방지
         */
        if (
            batteryReceiver != null
        ) {
            return
        }


        val receiver =
            object : BroadcastReceiver() {

                override fun onReceive(
                    context: Context?,
                    intent: Intent?
                ) {

                    if (
                        intent == null
                    ) {
                        return
                    }


                    if (
                        intent.action !=
                        Intent.ACTION_BATTERY_CHANGED
                    ) {
                        return
                    }


                    /*
                     * 현재 배터리 %
                     */
                    val batteryPercent =
                        readBatteryPercent(
                            intent
                        )
                            ?: return


                    /*
                     * -------------------------------------------------
                     * 서버 전송 여부 결정
                     * -------------------------------------------------
                     *
                     * 11% 이상
                     * → 10% 단위로 감소할 때 전송
                     *
                     * 10% 이하
                     * → 1% 감소할 때마다 전송
                     */

                    if (
                        !shouldSyncBattery(
                            batteryPercent
                        )
                    ) {

                        return
                    }


                    /*
                     * -------------------------------------------------
                     * Supabase 전송
                     * -------------------------------------------------
                     */

                    scope.launch {

                        try {

                            updateBatteryStatus(
                                batteryPercent
                            )


                            /*
                             * 서버 저장에 성공했을 때만
                             * 마지막 저장값 변경
                             */
                            lastSyncedBatteryPercent =
                                batteryPercent


                            Log.d(
                                TAG,
                                "배터리 동기화 성공: $batteryPercent%"
                            )


                        } catch (
                            e: Exception
                        ) {

                            /*
                             * 보호자와 아직 연결되지 않은 경우
                             * Watch is not paired 오류 등이 발생할 수 있음.
                             *
                             * 앱은 종료하지 않는다.
                             */

                            Log.w(
                                TAG,
                                "배터리 동기화 실패: ${e.message}",
                                e
                            )
                        }
                    }
                }
            }


        batteryReceiver =
            receiver


        /*
         * Android 배터리 변경 이벤트 등록
         */

        ContextCompat.registerReceiver(

            appContext,

            receiver,

            IntentFilter(
                Intent.ACTION_BATTERY_CHANGED
            ),

            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }


    /*
     * =====================================================
     * 배터리 서버 전송 여부
     * =====================================================
     *
     * 예:
     *
     * 앱 실행
     * 78% → 저장
     *
     * 77 ❌
     * 76 ❌
     * 75 ❌
     * 74 ❌
     * 73 ❌
     * 72 ❌
     * 71 ❌
     * 70 ✅
     *
     * 69 ❌
     * ...
     * 60 ✅
     *
     * ...
     *
     * 20 ✅
     *
     * 19 ❌
     * ...
     * 11 ❌
     *
     * 10 ✅
     * 9  ✅
     * 8  ✅
     * 7  ✅
     * ...
     * 1  ✅
     * 0  ✅
     */

    private fun shouldSyncBattery(
        currentPercent: Int
    ): Boolean {

        val lastPercent =
            lastSyncedBatteryPercent


        /*
         * -------------------------------------------------
         * 아직 한 번도 전송하지 않았다면
         * 현재값을 서버에 저장
         * -------------------------------------------------
         */

        if (
            lastPercent == null
        ) {

            return true
        }


        /*
         * -------------------------------------------------
         * 같은 값은 전송하지 않음
         * -------------------------------------------------
         */

        if (
            currentPercent ==
            lastPercent
        ) {

            return false
        }


        /*
         * -------------------------------------------------
         * 현재는 배터리가 감소하는 경우만
         * 자동 동기화
         * -------------------------------------------------
         *
         * 충전 중 증가하는 값은
         * 여기서는 전송하지 않는다.
         *
         * 앱을 다시 실행하면
         * syncBatteryNow()에서 최신값이 저장된다.
         */

        if (
            currentPercent >
            lastPercent
        ) {

            return false
        }


        /*
         * -------------------------------------------------
         * 10% 이하
         * -------------------------------------------------
         *
         * 10 → 9 → 8 → 7 ...
         *
         * 1% 감소할 때마다 서버에 전송
         */

        if (
            currentPercent <= 10
        ) {

            return true
        }


        /*
         * -------------------------------------------------
         * 10% 초과
         * -------------------------------------------------
         *
         * 10% 단위일 때만 전송
         *
         * 70
         * 60
         * 50
         * 40
         * 30
         * 20
         */

        return (
                currentPercent % 10 == 0
                )
    }


    /*
     * =====================================================
     * 현재 배터리 즉시 동기화
     * =====================================================
     *
     * 사용 시점:
     *
     * 1. 이미 연결된 워치가 앱을 실행했을 때
     *
     * 2. 보호자와 새로 페어링됐을 때
     *
     * 이 경우에는 10% 단위 조건과 상관없이
     * 현재 실제 배터리 값을 한 번 저장한다.
     */

    suspend fun syncBatteryNow() {

        val batteryIntent =
            appContext.registerReceiver(

                null,

                IntentFilter(
                    Intent.ACTION_BATTERY_CHANGED
                )
            )
                ?: return


        val batteryPercent =
            readBatteryPercent(
                batteryIntent
            )
                ?: return


        /*
         * 이미 같은 값을 이번 앱 실행 중
         * 정상적으로 서버에 보냈다면
         * 중복 전송하지 않는다.
         */

        if (
            batteryPercent ==
            lastSyncedBatteryPercent
        ) {

            return
        }


        updateBatteryStatus(
            batteryPercent
        )


        lastSyncedBatteryPercent =
            batteryPercent


        Log.d(
            TAG,
            "현재 배터리 즉시 동기화: $batteryPercent%"
        )
    }


    /*
     * =====================================================
     * Supabase 배터리 상태 업데이트
     * =====================================================
     */

    private suspend fun updateBatteryStatus(
        batteryPercent: Int
    ) {

        /*
         * 잘못된 값 방지
         */
        require(
            batteryPercent in 0..100
        ) {
            "배터리 값은 0~100이어야 합니다."
        }


        /*
         * -------------------------------------------------
         * Supabase 익명 Auth 확인
         * -------------------------------------------------
         *
         * PairingManager와 같은 익명 Auth 세션 사용
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
         * -------------------------------------------------
         * RPC 파라미터
         * -------------------------------------------------
         */

        val parameters =
            JsonObject(

                mapOf(

                    "p_battery_percent" to
                            JsonPrimitive(
                                batteryPercent
                            )
                )
            )


        /*
         * -------------------------------------------------
         * Supabase RPC
         * -------------------------------------------------
         *
         * update_watch_status(
         *     p_battery_percent integer
         * )
         */

        supabase
            .postgrest
            .rpc(

                function =
                    "update_watch_status",

                parameters =
                    parameters
            )
    }


    /*
     * =====================================================
     * Android Intent → 배터리 %
     * =====================================================
     */

    private fun readBatteryPercent(
        intent: Intent
    ): Int? {

        val level =
            intent.getIntExtra(
                BatteryManager.EXTRA_LEVEL,
                -1
            )


        val scale =
            intent.getIntExtra(
                BatteryManager.EXTRA_SCALE,
                -1
            )


        /*
         * 정상적인 배터리 값을
         * 읽지 못한 경우
         */

        if (
            level < 0 ||
            scale <= 0
        ) {

            return null
        }


        /*
         * 배터리 %
         */

        return (
                level * 100 / scale
                )
            .coerceIn(
                0,
                100
            )
    }


    /*
     * =====================================================
     * 감시 종료
     * =====================================================
     */

    fun stop() {

        val receiver =
            batteryReceiver
                ?: return


        try {

            appContext
                .unregisterReceiver(
                    receiver
                )


        } catch (
            e: Exception
        ) {

            Log.w(
                TAG,
                "배터리 Receiver 해제 실패",
                e
            )
        }


        batteryReceiver =
            null
    }


    /*
     * =====================================================
     * TAG
     * =====================================================
     */

    companion object {

        private const val TAG =
            "WatchStatusManager"
    }
}