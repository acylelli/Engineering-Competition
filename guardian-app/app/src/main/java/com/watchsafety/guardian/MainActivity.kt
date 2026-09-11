package com.watchsafety.guardian

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.core.content.ContextCompat

import androidx.lifecycle.lifecycleScope

import com.google.firebase.messaging.FirebaseMessaging

import com.watchsafety.guardian.auth.KakaoAuthManager
import com.watchsafety.guardian.data.GuardianPushTokenManager
import com.watchsafety.guardian.data.SupabaseClientProvider

import com.watchsafety.guardian.ui.GuardianApp
import com.watchsafety.guardian.ui.login.LoginScreen
import com.watchsafety.guardian.ui.theme.WatchSafetyTheme

import kotlinx.coroutines.launch


class MainActivity :
    ComponentActivity() {


    /*
     * =====================================================
     * 로그인 화면 상태
     * =====================================================
     */

    private var authUiState by
    mutableStateOf(
        AuthUiState.CHECKING
    )


    private var loginLoading by
    mutableStateOf(
        false
    )


    private var loginErrorMessage by
    mutableStateOf<String?>(
        null
    )


    /*
     * =====================================================
     * 긴급 화면 이동 요청
     * =====================================================
     */

    private var emergencyRequestVersion by
    mutableLongStateOf(
        0L
    )


    /*
     * =====================================================
     * Supabase / Kakao Auth
     * =====================================================
     */

    private val kakaoAuthManager by lazy {

        KakaoAuthManager(
            supabase =
                SupabaseClientProvider.client
        )
    }


    /*
     * =====================================================
     * 알림 권한
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
         * FCM 알림으로 앱 실행
         * -------------------------------------------------
         */

        handleEmergencyIntent(
            intent
        )


        /*
         * -------------------------------------------------
         * 알림 권한
         * -------------------------------------------------
         */

        requestNotificationPermission()


        /*
         * -------------------------------------------------
         * Compose
         * -------------------------------------------------
         */

        setContent {

            WatchSafetyTheme {

                when (
                    authUiState
                ) {


                    /*
                     * =========================================
                     * 저장된 로그인 세션 확인 중
                     * =========================================
                     */

                    AuthUiState.CHECKING -> {

                        Box(
                            modifier =
                                Modifier.fillMaxSize(),
                            contentAlignment =
                                Alignment.Center,
                        ) {

                            CircularProgressIndicator()
                        }
                    }


                    /*
                     * =========================================
                     * 로그인 필요
                     * =========================================
                     */

                    AuthUiState.SIGNED_OUT -> {

                        LoginScreen(
                            isLoading =
                                loginLoading,
                            errorMessage =
                                loginErrorMessage,
                            onKakaoLogin = {
                                startKakaoLogin()
                            },
                        )
                    }


                    /*
                     * =========================================
                     * 로그인 완료
                     * =========================================
                     */

                    AuthUiState.SIGNED_IN -> {

                        GuardianApp(
                            emergencyRequestVersion =
                                emergencyRequestVersion,
                            onLogout =
                                ::startLogout,
                        )
                    }
                }
            }
        }


        /*
         * -------------------------------------------------
         * 저장된 Supabase 로그인 확인
         * -------------------------------------------------
         */

        checkSavedLoginSession()
    }


    /*
     * =====================================================
     * 로그아웃
     * =====================================================
     */

    private fun startLogout() {

        lifecycleScope.launch {

            runCatching {
                kakaoAuthManager.logout()
            }.onSuccess {

                loginErrorMessage = null
                authUiState = AuthUiState.SIGNED_OUT

                Log.d(
                    AUTH_TAG,
                    "Kakao/Supabase 로그아웃 완료",
                )

            }.onFailure { error ->

                Log.e(
                    AUTH_TAG,
                    "Kakao/Supabase 로그아웃 실패",
                    error,
                )

                Toast
                    .makeText(
                        this@MainActivity,
                        "로그아웃하지 못했습니다. 다시 시도해주세요.",
                        Toast.LENGTH_SHORT,
                    )
                    .show()
            }
        }
    }


    /*
     * =====================================================
     * 저장된 로그인 세션 확인
     * =====================================================
     */

    private fun checkSavedLoginSession() {

        lifecycleScope.launch {

            runCatching {

                kakaoAuthManager
                    .hasKakaoSession()

            }.onSuccess { hasSession ->


                if (
                    hasSession
                ) {

                    Log.d(
                        AUTH_TAG,
                        "저장된 Kakao/Supabase 세션 확인 완료"
                    )


                    /*
                     * 메인 화면
                     */
                    authUiState =
                        AuthUiState.SIGNED_IN


                    /*
                     * 로그인된 guardian_id로
                     * FCM Token 저장
                     */
                    fetchAndSyncFcmToken()


                } else {

                    Log.d(
                        AUTH_TAG,
                        "저장된 Kakao 세션 없음"
                    )


                    authUiState =
                        AuthUiState.SIGNED_OUT
                }


            }.onFailure { error ->


                Log.e(
                    AUTH_TAG,
                    "로그인 상태 확인 실패",
                    error
                )


                loginErrorMessage =
                    "로그인 상태를 확인하지 못했습니다."


                authUiState =
                    AuthUiState.SIGNED_OUT
            }
        }
    }


    /*
     * =====================================================
     * 카카오 로그인 시작
     * =====================================================
     */

    private fun startKakaoLogin() {


        if (
            loginLoading
        ) {

            return
        }


        loginLoading =
            true


        loginErrorMessage =
            null


        lifecycleScope.launch {


            runCatching {

                kakaoAuthManager
                    .login(
                        context =
                            this@MainActivity
                    )

            }.onSuccess { guardianId ->


                Log.d(
                    AUTH_TAG,
                    "Kakao → Supabase 로그인 완료"
                )


                Log.d(
                    AUTH_TAG,
                    "guardianId=$guardianId"
                )


                loginLoading =
                    false


                authUiState =
                    AuthUiState.SIGNED_IN


                /*
                 * 로그인 성공한 사용자에게
                 * FCM Token 연결
                 */
                fetchAndSyncFcmToken()


            }.onFailure { error ->


                loginLoading =
                    false


                loginErrorMessage =
                    error.message
                        ?: "카카오 로그인에 실패했습니다."


                Log.e(
                    AUTH_TAG,
                    "Kakao/Supabase 로그인 실패",
                    error
                )
            }
        }
    }


    /*
     * =====================================================
     * 이미 실행 중일 때 FCM 알림 클릭
     * =====================================================
     */

    override fun onNewIntent(
        intent: Intent
    ) {

        super.onNewIntent(
            intent
        )


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


        val openEmergencyString =

            intent
                .getStringExtra(
                    EXTRA_OPEN_EMERGENCY
                )


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
         * GuardianApp이 아직 안 떠 있어도
         * version 값은 유지된다.
         *
         * 로그인 완료 후 GuardianApp이 생성되면
         * 긴급 화면으로 이동 가능하다.
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


                val token =
                    task.result


                /*
                 * FCM Token 전체 문자열은
                 * Logcat에 출력하지 않는다.
                 */

                Log.d(
                    FCM_TAG,
                    "FCM Token 발급 완료"
                )


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


        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.TIRAMISU
        ) {

            return
        }


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


        notificationPermissionLauncher
            .launch(
                Manifest.permission
                    .POST_NOTIFICATIONS
            )
    }


    /*
     * =====================================================
     * Auth UI State
     * =====================================================
     */

    private enum class AuthUiState {

        CHECKING,

        SIGNED_OUT,

        SIGNED_IN,
    }


    /*
     * =====================================================
     * Constants
     * =====================================================
     */

    private companion object {


        const val AUTH_TAG =
            "GuardianAuth"


        const val FCM_TAG =
            "GuardianFCM"


        const val PUSH_TOKEN_TAG =
            "GuardianPushToken"


        const val NAVIGATION_TAG =
            "GuardianPushNavigation"


        const val EXTRA_OPEN_EMERGENCY =
            "open_emergency"


        const val EXTRA_EVENT_ID =
            "event_id"


        const val EXTRA_EVENT_TYPE =
            "type"
    }
}
