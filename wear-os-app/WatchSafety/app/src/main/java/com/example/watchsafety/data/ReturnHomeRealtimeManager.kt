package com.example.watchsafety.data

import android.util.Log

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

import java.time.Instant


class ReturnHomeRealtimeManager(
    private val scope: CoroutineScope
) {

    companion object {

        private const val TAG =
            "ReturnHomeRealtime"
    }


    private val supabase =
        SupabaseClientProvider.client


    private var realtimeJob:
            Job? = null


    /*
     * =====================================================
     * 귀가 요청 Realtime 시작
     * =====================================================
     */

    fun start(

        guardianId: String,

        wearerId: String,

        onReturnHomeRequested:
            (requestId: String) -> Unit

    ) {

        if (
            realtimeJob?.isActive == true
        ) {

            return
        }


        realtimeJob =
            scope.launch {


                val channel =
                    supabase.channel(
                        "return-home-$wearerId"
                    )


                val changeFlow =

                    channel
                        .postgresChangeFlow<
                                PostgresAction.Insert
                                >(
                            schema = "public"
                        ) {

                            table =
                                "return_home_requests"
                        }


                try {


                    /*
                     * Realtime 이벤트 수신
                     */
                    launch {


                        changeFlow
                            .collect {
                                    change ->


                                val record =
                                    change.record


                                val requestId =

                                    record[
                                        "id"
                                    ]
                                        ?.jsonPrimitive
                                        ?.contentOrNull

                                        ?: return@collect


                                val requestGuardianId =

                                    record[
                                        "guardian_id"
                                    ]
                                        ?.jsonPrimitive
                                        ?.contentOrNull

                                        ?: return@collect


                                val requestWearerId =

                                    record[
                                        "wearer_id"
                                    ]
                                        ?.jsonPrimitive
                                        ?.contentOrNull

                                        ?: return@collect


                                val status =

                                    record[
                                        "status"
                                    ]
                                        ?.jsonPrimitive
                                        ?.contentOrNull

                                        ?: return@collect


                                /*
                                 * 현재 보호자의 요청만
                                 */
                                if (
                                    requestGuardianId !=
                                    guardianId
                                ) {

                                    return@collect
                                }


                                /*
                                 * 현재 착용자 요청만
                                 */
                                if (
                                    requestWearerId !=
                                    wearerId
                                ) {

                                    return@collect
                                }


                                /*
                                 * 신규 REQUESTED만
                                 */
                                if (
                                    status !=
                                    "REQUESTED"
                                ) {

                                    return@collect
                                }


                                Log.d(
                                    TAG,
                                    "귀가 요청 Realtime 수신: $requestId"
                                )


                                onReturnHomeRequested(
                                    requestId
                                )
                            }
                    }


                    /*
                     * Supabase Realtime 구독
                     */
                    channel
                        .subscribe(
                            blockUntilSubscribed =
                                true
                        )


                    Log.d(
                        TAG,
                        "return_home_requests Realtime 구독 완료"
                    )


                    awaitCancellation()


                } finally {


                    withContext(
                        NonCancellable
                    ) {


                        runCatching {

                            channel
                                .unsubscribe()
                        }
                    }
                }
            }
    }


    /*
     * =====================================================
     * 귀가 요청 수락
     *
     * REQUESTED
     *      ↓
     * ACCEPTED
     * =====================================================
     */

    suspend fun acceptRequest(

        requestId: String,

        guardianId: String,

        wearerId: String

    ) {


        supabase
            .from(
                "return_home_requests"
            )
            .update({

                set(
                    "status",
                    "ACCEPTED"
                )


                set(
                    "responded_at",
                    Instant
                        .now()
                        .toString()
                )

            }) {


                filter {


                    eq(
                        "id",
                        requestId
                    )


                    eq(
                        "guardian_id",
                        guardianId
                    )


                    eq(
                        "wearer_id",
                        wearerId
                    )


                    /*
                     * REQUESTED일 때만
                     * 수락 가능
                     */
                    eq(
                        "status",
                        "REQUESTED"
                    )
                }
            }


        Log.d(
            TAG,
            "귀가 요청 ACCEPTED: $requestId"
        )
    }


    /*
     * =====================================================
     * 길안내 시작
     *
     * ACCEPTED
     *      ↓
     * NAVIGATING
     * =====================================================
     */

    suspend fun startNavigation(

        requestId: String,

        guardianId: String,

        wearerId: String

    ) {


        supabase
            .from(
                "return_home_requests"
            )
            .update({

                set(
                    "status",
                    "NAVIGATING"
                )

            }) {


                filter {


                    eq(
                        "id",
                        requestId
                    )


                    eq(
                        "guardian_id",
                        guardianId
                    )


                    eq(
                        "wearer_id",
                        wearerId
                    )


                    eq(
                        "status",
                        "ACCEPTED"
                    )
                }
            }


        Log.d(
            TAG,
            "귀가 요청 NAVIGATING: $requestId"
        )
    }


    /*
     * =====================================================
     * 귀가 요청 취소
     *
     * 워치에서 "나중에" 선택
     *
     * REQUESTED
     *      ↓
     * CANCELLED
     * =====================================================
     */

    suspend fun cancelRequest(

        requestId: String,

        guardianId: String,

        wearerId: String

    ) {


        supabase
            .from(
                "return_home_requests"
            )
            .update({

                set(
                    "status",
                    "CANCELLED"
                )


                /*
                 * 착용자가 응답한 시점
                 */
                set(
                    "responded_at",
                    Instant
                        .now()
                        .toString()
                )

            }) {


                filter {


                    eq(
                        "id",
                        requestId
                    )


                    eq(
                        "guardian_id",
                        guardianId
                    )


                    eq(
                        "wearer_id",
                        wearerId
                    )


                    /*
                     * 아직 처리되지 않은
                     * REQUESTED만 취소
                     */
                    eq(
                        "status",
                        "REQUESTED"
                    )
                }
            }


        Log.d(
            TAG,
            "귀가 요청 CANCELLED: $requestId"
        )
    }


    /*
     * =====================================================
     * Realtime 종료
     * =====================================================
     */

    fun stop() {


        realtimeJob
            ?.cancel()


        realtimeJob =
            null
    }
}