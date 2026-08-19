package com.example.watchsafety.data

import android.content.Context


class ReturnHomeRequestStore(
    context: Context
) {


    companion object {

        private const val PREFS_NAME =
            "return_home_request_store"

        private const val KEY_NOTIFIED_IDS =
            "notified_request_ids"

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
     * 이미 알림 또는 화면으로 처리 시작한 요청인지
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
     * 알림 / 화면 처리 시작 기록
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
     * 이미 사용자가 수락해 처리 완료한 요청인지
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
     * 사용자가 귀가 요청을 수락했음
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
     *
     * 무한히 커지는 것을 막기 위해
     * 최대 50개까지만 유지
     * =====================================================
     */

    private fun addId(
        key: String,
        requestId: String
    ) {

        val ids =
            getSet(
                key
            )


        if (
            ids.contains(
                requestId
            )
        ) {

            return
        }


        if (
            ids.size >=
            MAX_IDS
        ) {

            ids.clear()
        }


        ids.add(
            requestId
        )


        prefs
            .edit()
            .putStringSet(
                key,
                ids
            )
            .apply()
    }
}