package com.example.watchsafety.data

import android.content.Context


class ReturnHomeRequestStore(
    context: Context
) {

    companion object {

        private const val PREFS_NAME =
            "return_home_request_store"

        /*
         * 워치에서 알림 또는 요청 화면을
         * 한 번이라도 표시한 요청 ID
         */
        private const val KEY_NOTIFIED_IDS =
            "notified_request_ids"

        /*
         * 워치 사용자가 이미 처리한 요청 ID
         *
         * 예:
         * - 나중에 → CANCELLED
         * - 집으로 가기 → ACCEPTED / NAVIGATING
         * - 집 도착 → COMPLETED
         *
         * HOME 좌표/반경은 이 Store에 저장하지 않는다.
         * HOME 정보는 항상 HomeSafeZoneManager를 통해
         * Supabase에서 다시 조회한다.
         */
        private const val KEY_HANDLED_IDS =
            "handled_request_ids"

        private const val MAX_IDS =
            50
    }


    private val prefs =
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )


    /*
     * =====================================================
     * 이미 알림 또는 화면으로 표시한 요청인지
     * =====================================================
     */
    @Synchronized
    fun wasNotified(
        requestId: String
    ): Boolean {

        return getSet(
            KEY_NOTIFIED_IDS
        ).contains(
            requestId
        )
    }


    /*
     * =====================================================
     * 알림 / 화면 표시 기록
     * =====================================================
     */
    @Synchronized
    fun markNotified(
        requestId: String
    ) {

        addId(
            KEY_NOTIFIED_IDS,
            requestId
        )
    }


    /*
     * =====================================================
     * 이미 워치 사용자가 처리한 요청인지
     * =====================================================
     */
    @Synchronized
    fun isHandled(
        requestId: String
    ): Boolean {

        return getSet(
            KEY_HANDLED_IDS
        ).contains(
            requestId
        )
    }


    /*
     * =====================================================
     * 요청 처리 완료 기록
     * =====================================================
     */
    @Synchronized
    fun markHandled(
        requestId: String
    ) {

        addId(
            KEY_HANDLED_IDS,
            requestId
        )
    }


    /*
     * =====================================================
     * 특정 요청의 로컬 처리 기록 삭제
     *
     * 실기기 테스트 중 같은 requestId를 다시 확인해야 할 때 사용 가능.
     * 일반 앱 흐름에서는 호출할 필요 없다.
     * =====================================================
     */
    @Synchronized
    fun clearRequest(
        requestId: String
    ) {

        removeId(
            KEY_NOTIFIED_IDS,
            requestId
        )

        removeId(
            KEY_HANDLED_IDS,
            requestId
        )
    }


    /*
     * =====================================================
     * 모든 귀가 요청 로컬 기록 초기화
     *
     * 테스트용.
     * Supabase의 return_home_requests 데이터는 삭제하지 않는다.
     * =====================================================
     */
    @Synchronized
    fun clearAll() {

        prefs
            .edit()
            .remove(
                KEY_NOTIFIED_IDS
            )
            .remove(
                KEY_HANDLED_IDS
            )
            .apply()
    }


    /*
     * =====================================================
     * 내부 Set 조회
     * =====================================================
     */
    private fun getSet(
        key: String
    ): MutableSet<String> {

        return prefs
            .getStringSet(
                key,
                emptySet()
            )
            ?.toMutableSet()
            ?: mutableSetOf()
    }


    /*
     * =====================================================
     * ID 추가
     * =====================================================
     */
    private fun addId(
        key: String,
        requestId: String
    ) {

        val normalizedRequestId =
            requestId.trim()


        if (
            normalizedRequestId.isBlank()
        ) {

            return
        }


        val ids =
            getSet(
                key
            )


        if (
            ids.contains(
                normalizedRequestId
            )
        ) {

            return
        }


        if (
            ids.size >=
            MAX_IDS
        ) {

            /*
             * 요청 ID는 시간순 Set이 아니므로
             * 테스트 단계에서는 최대치 도달 시 초기화한다.
             */
            ids.clear()
        }


        ids.add(
            normalizedRequestId
        )


        prefs
            .edit()
            .putStringSet(
                key,
                ids
            )
            .apply()
    }


    /*
     * =====================================================
     * ID 삭제
     * =====================================================
     */
    private fun removeId(
        key: String,
        requestId: String
    ) {

        val ids =
            getSet(
                key
            )


        if (
            !ids.remove(
                requestId
            )
        ) {

            return
        }


        prefs
            .edit()
            .putStringSet(
                key,
                ids
            )
            .apply()
    }
}
