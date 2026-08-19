package com.watchsafety.guardian

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue

import androidx.core.content.ContextCompat

import androidx.lifecycle.lifecycleScope

import com.google.firebase.messaging.FirebaseMessaging

import com.watchsafety.guardian.data.GuardianPushTokenManager
import com.watchsafety.guardian.data.SupabaseClientProvider

import com.watchsafety.guardian.ui.GuardianApp
import com.watchsafety.guardian.ui.theme.WatchSafetyTheme

import kotlinx.coroutines.launch


class MainActivity :
    ComponentActivity() {


    /*
     * =====================================================
     * 긴급화면 이동 요청
     * =====================================================
     *
     * 0 = 일반 앱 실행
     *
     * 1 이상 =
     * FCM 알림을 눌러서 앱이 열림
     *
     * 새로운 SOS 알림을 누를 때마다 +1
     * Compose가 변경을 감지해서
     * EMERGENCY 화면으로 이동한다.
     */

    private var emergencyRequestVersion by
    mutableLongStateOf(
        0L
    )


    /*
     * =====================================================
     * 알림 권한 요청
     * =====================================================
     */

    private val notificationPermissionLauncher =

        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            Log.d(
                FCM_TAG,
                "알림 권한 허용 = $granted"
            )
        }


    /*
     * =====================================================
     * onCreate
     * =====================================================
     */

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        /*
         * -------------------------------------------------
         * FCM 알림 클릭으로 앱이 실행된 경우
         * -------------------------------------------------
         */

        handleEmergencyIntent(
            intent
        )


        /*
         * -------------------------------------------------
         * Android 13 이상 알림 권한
         * -------------------------------------------------
         */

        requestNotificationPermission()


        /*
         * -------------------------------------------------
         * FCM Token
         * -------------------------------------------------
         */

        fetchAndSyncFcmToken()


        /*
         * -------------------------------------------------
         * Compose
         * -------------------------------------------------
         */

        setContent {

            WatchSafetyTheme {

                GuardianApp(

                    emergencyRequestVersion =
                        emergencyRequestVersion
                )
            }
        }
    }


    /*
     * =====================================================
     * 이미 실행 중인 앱에서
     * 알림을 눌렀을 때
     * =====================================================
     *
     * Manifest:
     *
     * android:launchMode="singleTop"
     *
     * 이기 때문에 기존 MainActivity가
     * 살아있으면 여기로 새로운 Intent가 온다.
     */

    override fun onNewIntent(
        intent: Intent
    ) {

        super.onNewIntent(
            intent
        )


        /*
         * Activity의 현재 Intent도
         * 새로운 Intent로 갱신
         */
        setIntent(
            intent
        )


        handleEmergencyIntent(
            intent
        )
    }


    /*
     * =====================================================
     * FCM Intent 처리
     * =====================================================
     */

    private fun handleEmergencyIntent(
        intent: Intent?
    ) {


        if (
            intent == null
        ) {

            return
        }


        /*
         * Edge Function에서 FCM data에:
         *
         * open_emergency = "true"
         *
         * 로 보내고 있다.
         */
        val openEmergencyString =

            intent
                .getStringExtra(
                    EXTRA_OPEN_EMERGENCY
                )


        /*
         * 나중에 직접 Notification을 만들 경우
         * Boolean extra를 사용하는 경우도 대응
         */
        val openEmergencyBoolean =

            intent
                .getBooleanExtra(
                    EXTRA_OPEN_EMERGENCY,
                    false
                )


        val shouldOpenEmergency =

            openEmergencyString
                ?.equals(
                    "true",
                    ignoreCase = true
                ) == true ||

                    openEmergencyBoolean


        if (
            !shouldOpenEmergency
        ) {

            return
        }


        /*
         * -------------------------------------------------
         * 추가 FCM 정보
         * -------------------------------------------------
         */

        val eventId =

            intent
                .getStringExtra(
                    EXTRA_EVENT_ID
                )


        val eventType =

            intent
                .getStringExtra(
                    EXTRA_EVENT_TYPE
                )


        Log.d(
            NAVIGATION_TAG,
            "🚨 FCM 긴급 알림 클릭"
        )


        Log.d(
            NAVIGATION_TAG,
            "eventId=$eventId"
        )


        Log.d(
            NAVIGATION_TAG,
            "eventType=$eventType"
        )


        /*
         * -------------------------------------------------
         * Compose에 긴급화면 이동 요청
         * -------------------------------------------------
         */

        emergencyRequestVersion++


        Log.d(
            NAVIGATION_TAG,
            "emergencyRequestVersion=$emergencyRequestVersion"
        )
    }


    /*
     * =====================================================
     * FCM Token 가져오기 + Supabase 저장
     * =====================================================
     */

    private fun fetchAndSyncFcmToken() {


        FirebaseMessaging
            .getInstance()
            .token
            .addOnCompleteListener { task ->


                /*
                 * Token 실패
                 */

                if (
                    !task.isSuccessful
                ) {

                    Log.e(
                        FCM_TAG,
                        "FCM Token 발급 실패",
                        task.exception
                    )


                    return@addOnCompleteListener
                }


                /*
                 * Token
                 */

                val token =
                    task.result


                Log.d(
                    FCM_TAG,
                    "================================"
                )

                Log.d(
                    FCM_TAG,
                    "FCM TOKEN"
                )

                Log.d(
                    FCM_TAG,
                    token
                )

                Log.d(
                    FCM_TAG,
                    "================================"
                )


                /*
                 * Supabase Client
                 */

                val supabase =

                    SupabaseClientProvider
                        .createOrNull()


                if (
                    supabase == null
                ) {

                    Log.e(
                        PUSH_TOKEN_TAG,
                        "Supabase Client 생성 실패"
                    )


                    return@addOnCompleteListener
                }


                /*
                 * Supabase에 Token 저장
                 */

                lifecycleScope.launch {


                    runCatching {


                        GuardianPushTokenManager(
                            supabase =
                                supabase
                        )
                            .syncToken(
                                token =
                                    token
                            )


                    }.onSuccess {


                        Log.d(
                            PUSH_TOKEN_TAG,
                            "FCM Token Supabase 동기화 완료"
                        )


                    }.onFailure { error ->


                        Log.e(
                            PUSH_TOKEN_TAG,
                            "FCM Token Supabase 동기화 실패",
                            error
                        )
                    }
                }
            }
    }


    /*
     * =====================================================
     * Android 13 이상 알림 권한
     * =====================================================
     */

    private fun requestNotificationPermission() {


        /*
         * Android 13 미만
         */
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.TIRAMISU
        ) {

            return
        }


        /*
         * 이미 허용
         */
        if (

            ContextCompat
                .checkSelfPermission(

                    this,

                    Manifest.permission
                        .POST_NOTIFICATIONS

                ) ==
            PackageManager.PERMISSION_GRANTED

        ) {

            Log.d(
                FCM_TAG,
                "알림 권한 이미 허용됨"
            )


            return
        }


        /*
         * 권한 요청
         */
        notificationPermissionLauncher
            .launch(

                Manifest.permission
                    .POST_NOTIFICATIONS
            )
    }


    /*
     * =====================================================
     * Constants
     * =====================================================
     */

    private companion object {


        const val FCM_TAG =
            "GuardianFCM"


        const val PUSH_TOKEN_TAG =
            "GuardianPushToken"


        const val NAVIGATION_TAG =
            "GuardianPushNavigation"


        /*
         * Edge Function에서 보내는 data key와
         * 정확히 동일해야 한다.
         */

        const val EXTRA_OPEN_EMERGENCY =
            "open_emergency"


        const val EXTRA_EVENT_ID =
            "event_id"


        const val EXTRA_EVENT_TYPE =
            "type"
    }
}