package com.example.watchsafety.data

import android.util.Log

import io.github.jan.supabase.postgrest.postgrest
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

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put


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
     *
     * 보호자 앱에서 새 return_home_requests 행이
     * REQUESTED 상태로 생성되면 워치에서 감지한다.
     *
     * INSERT 이벤트만 감지한다.
     * =====================================================
     */

    fun start(

        guardianId: String,

        wearerId: String,

        onReturnHomeRequested:
            (requestId: String) -> Unit

    ) {

        /*
         * 이미 Realtime 구독 중이면
         * 중복 구독하지 않는다.
         */
        if (
            realtimeJob?.isActive == true
        ) {

            Log.d(
                TAG,
                "Realtime 이미 실행 중"
            )


            return
        }


        realtimeJob =
            scope.launch {


                val channel =
                    supabase
                        .channel(
                            "return-home-$wearerId"
                        )


                /*
                 * =================================================
                 * return_home_requests INSERT 감지
                 * =================================================
                 */
                val changeFlow =

                    channel
                        .postgresChangeFlow<
                                PostgresAction.Insert
                                >(
                            schema =
                                "public"
                        ) {

                            table =
                                "return_home_requests"
                        }


                try {


                    /*
                     * =============================================
                     * Realtime 이벤트 수신
                     * =============================================
                     */
                    launch {


                        changeFlow
                            .collect {
                                    change ->


                                val record =
                                    change.record


                                /*
                                 * 요청 ID
                                 */
                                val requestId =

                                    record[
                                        "id"
                                    ]
                                        ?.jsonPrimitive
                                        ?.contentOrNull

                                        ?: return@collect


                                /*
                                 * 보호자 ID
                                 */
                                val requestGuardianId =

                                    record[
                                        "guardian_id"
                                    ]
                                        ?.jsonPrimitive
                                        ?.contentOrNull

                                        ?: return@collect


                                /*
                                 * 착용자 ID
                                 */
                                val requestWearerId =

                                    record[
                                        "wearer_id"
                                    ]
                                        ?.jsonPrimitive
                                        ?.contentOrNull

                                        ?: return@collect


                                /*
                                 * 귀가 요청 상태
                                 */
                                val status =

                                    record[
                                        "status"
                                    ]
                                        ?.jsonPrimitive
                                        ?.contentOrNull

                                        ?: return@collect


                                /*
                                 * =========================================
                                 * 현재 연결된 보호자의 요청인지 확인
                                 * =========================================
                                 */
                                if (
                                    requestGuardianId !=
                                    guardianId
                                ) {

                                    Log.d(
                                        TAG,
                                        "다른 보호자의 귀가 요청 무시 " +
                                                "requestGuardianId=$requestGuardianId"
                                    )


                                    return@collect
                                }


                                /*
                                 * =========================================
                                 * 현재 워치 착용자의 요청인지 확인
                                 * =========================================
                                 */
                                if (
                                    requestWearerId !=
                                    wearerId
                                ) {

                                    Log.d(
                                        TAG,
                                        "다른 착용자의 귀가 요청 무시 " +
                                                "requestWearerId=$requestWearerId"
                                    )


                                    return@collect
                                }


                                /*
                                 * =========================================
                                 * 신규 REQUESTED만 워치에 표시
                                 * =========================================
                                 */
                                if (
                                    status !=
                                    "REQUESTED"
                                ) {

                                    return@collect
                                }


                                Log.d(
                                    TAG,
                                    "귀가 요청 Realtime 수신 " +
                                            "requestId=$requestId"
                                )


                                /*
                                 * MainActivity에 전달
                                 */
                                onReturnHomeRequested(
                                    requestId
                                )
                            }
                    }


                    /*
                     * =============================================
                     * Supabase Realtime 실제 구독
                     * =============================================
                     */
                    channel
                        .subscribe(
                            blockUntilSubscribed =
                                true
                        )


                    Log.d(
                        TAG,
                        "return_home_requests Realtime 구독 완료 " +
                                "guardianId=$guardianId " +
                                "wearerId=$wearerId"
                    )


                    /*
                     * start()의 Coroutine을 계속 유지
                     */
                    awaitCancellation()


                } finally {


                    /*
                     * Coroutine 종료 시
                     * Realtime 채널 정리
                     */
                    withContext(
                        NonCancellable
                    ) {


                        runCatching {

                            channel
                                .unsubscribe()

                        }.onFailure { error ->

                            Log.w(
                                TAG,
                                "Realtime unsubscribe 실패",
                                error
                            )
                        }
                    }


                    Log.d(
                        TAG,
                        "return_home_requests Realtime 종료"
                    )
                }
            }
    }


    /*
     * =====================================================
     * 귀가 요청 수락
     * =====================================================
     *
     * 워치에서
     *
     * "집으로 가기"
     *
     * 버튼을 누르면 실행.
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


        updateReturnHomeStatus(

            requestId =
                requestId,

            targetStatus =
                "ACCEPTED",

            guardianId =
                guardianId,

            wearerId =
                wearerId
        )
    }


    /*
     * =====================================================
     * 길안내 시작
     * =====================================================
     *
     * TMAP 경로 검색 성공 후 실행.
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


        updateReturnHomeStatus(

            requestId =
                requestId,

            targetStatus =
                "NAVIGATING",

            guardianId =
                guardianId,

            wearerId =
                wearerId
        )
    }


    /*
     * =====================================================
     * 나중에
     * =====================================================
     *
     * 보호자의 귀가 요청 화면에서
     * 워치 사용자가 "나중에" 선택.
     *
     * REQUESTED
     *      ↓
     * CANCELLED
     *
     * 보호자 앱은
     * return_home_requests Realtime으로
     * CANCELLED를 감지하고
     *
     * "집으로 귀가 요청"
     * 버튼을 다시 활성화한다.
     * =====================================================
     */

    suspend fun cancelRequest(

        requestId: String,

        guardianId: String,

        wearerId: String

    ) {


        updateReturnHomeStatus(

            requestId =
                requestId,

            targetStatus =
                "CANCELLED",

            guardianId =
                guardianId,

            wearerId =
                wearerId
        )
    }


    /*
     * =====================================================
     * 귀가 완료
     * =====================================================
     *
     * TmapRouteTestScreen에서
     *
     * 현재 GPS 위치
     *      ↓
     * HOME 안전구역 반경 안으로 진입
     *
     * 하면 실행.
     *
     * NAVIGATING
     *      ↓
     * COMPLETED
     *
     * 보호자 앱은 COMPLETED를 수신하면
     * 귀가 요청 버튼을 다시 활성화한다.
     * =====================================================
     */

    suspend fun completeRequest(

        requestId: String,

        guardianId: String,

        wearerId: String

    ) {


        updateReturnHomeStatus(

            requestId =
                requestId,

            targetStatus =
                "COMPLETED",

            guardianId =
                guardianId,

            wearerId =
                wearerId
        )
    }


    /*
     * =====================================================
     * 귀가 상태 변경 공통 RPC
     * =====================================================
     *
     * 기존 방식:
     *
     * 워치
     * → return_home_requests 직접 UPDATE
     *
     * RLS 정책 때문에
     * UPDATE 대상이 0건이어도
     * 앱에서는 성공처럼 보일 수 있었다.
     *
     *
     * 변경 방식:
     *
     * 워치
     *      ↓
     * update_watch_return_home_status RPC
     *      ↓
     * Supabase DB
     *
     *
     * RPC 내부에서:
     *
     * auth.uid()
     *      ↓
     * devices.watch_auth_id
     *      ↓
     * guardian_id / wearer_id 검증
     *
     * 후 상태를 변경한다.
     * =====================================================
     */

    private suspend fun updateReturnHomeStatus(

        requestId: String,

        targetStatus: String,

        guardianId: String,

        wearerId: String

    ) {


        /*
         * =============================================
         * 기본값 검증
         * =============================================
         */

        require(
            requestId.isNotBlank()
        ) {

            "귀가 요청 ID가 없습니다."
        }


        require(
            guardianId.isNotBlank()
        ) {

            "보호자 ID가 없습니다."
        }


        require(
            wearerId.isNotBlank()
        ) {

            "착용자 ID가 없습니다."
        }


        require(
            targetStatus in
                    setOf(
                        "ACCEPTED",
                        "CANCELLED",
                        "NAVIGATING",
                        "COMPLETED"
                    )
        ) {

            "지원하지 않는 귀가 상태입니다: $targetStatus"
        }


        Log.d(
            TAG,
            "귀가 상태 변경 요청 " +
                    "requestId=$requestId " +
                    "targetStatus=$targetStatus " +
                    "guardianId=$guardianId " +
                    "wearerId=$wearerId"
        )


        /*
         * =============================================
         * RPC 파라미터
         * =============================================
         *
         * Supabase SQL:
         *
         * update_watch_return_home_status(
         *     p_request_id uuid,
         *     p_target_status text
         * )
         */
        val parameters =

            buildJsonObject {


                put(
                    "p_request_id",
                    requestId
                )


                put(
                    "p_target_status",
                    targetStatus
                )
            }


        /*
         * =============================================
         * Supabase RPC 실행
         * =============================================
         *
         * RPC에서 실제 변경이 0건이면
         * exception을 발생시키도록 만들어 두었기 때문에
         * 이제 성공/실패를 확실하게 구분할 수 있다.
         */
        supabase
            .postgrest
            .rpc(

                function =
                    "update_watch_return_home_status",

                parameters =
                    parameters
            )


        /*
         * 여기까지 왔다는 것은
         * DB 상태 변경까지 성공했다는 의미.
         */
        Log.d(
            TAG,
            "✅ 귀가 상태 변경 완료 " +
                    "requestId=$requestId " +
                    "status=$targetStatus"
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


        Log.d(
            TAG,
            "귀가 요청 Realtime stop()"
        )
    }
}
