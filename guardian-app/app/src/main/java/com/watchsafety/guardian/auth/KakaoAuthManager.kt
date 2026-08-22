package com.watchsafety.guardian.auth

import android.content.Context

import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Kakao
import io.github.jan.supabase.auth.providers.builtin.IDToken

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class KakaoAuthManager(
    private val supabase: SupabaseClient,
) {

    /*
     * =====================================================
     * 저장된 카카오 로그인 세션 확인
     * =====================================================
     */

    suspend fun hasKakaoSession(): Boolean {

        /*
         * Android 저장소에 저장되어 있는
         * Supabase 세션 로드가 끝날 때까지 대기
         */
        supabase
            .auth
            .awaitInitialization()


        /*
         * 과거에 사용하던 익명 세션이 있을 수도 있으므로
         * 단순히 session != null 만 확인하면 안 된다.
         *
         * Kakao Identity가 실제로 연결된 계정인지 확인한다.
         */
        return supabase
            .auth
            .currentIdentitiesOrNull()
            .orEmpty()
            .any { identity ->

                identity.provider ==
                        "kakao"
            }
    }


    /*
     * =====================================================
     * 카카오 → Supabase 로그인
     * =====================================================
     */

    suspend fun login(
        context: Context,
    ): String {

        android.util.Log.d(
            "GuardianAuth",
            "1. Kakao SDK 로그인 시작"
        )

        val kakaoToken =
            loginWithKakao(
                context
            )

        android.util.Log.d(
            "GuardianAuth",
            "2. Kakao SDK 로그인 성공"
        )

        val kakaoIdToken =
            kakaoToken.idToken
                ?: error(
                    "카카오 ID Token을 받지 못했습니다."
                )

        android.util.Log.d(
            "GuardianAuth",
            "3. Kakao ID Token 확인 완료"
        )

        supabase
            .auth
            .awaitInitialization()

        android.util.Log.d(
            "GuardianAuth",
            "4. Supabase 로그인 요청 시작"
        )

        supabase
            .auth
            .signInWith(
                IDToken
            ) {

                idToken =
                    kakaoIdToken

                provider =
                    Kakao
            }

        android.util.Log.d(
            "GuardianAuth",
            "5. Supabase 로그인 요청 성공"
        )

        val session =
            requireNotNull(
                supabase
                    .auth
                    .currentSessionOrNull()
            ) {
                "Supabase 로그인 세션이 생성되지 않았습니다."
            }

        android.util.Log.d(
            "GuardianAuth",
            "6. Supabase Session 확인 완료"
        )

        val kakaoIdentityExists =
            supabase
                .auth
                .currentIdentitiesOrNull()
                .orEmpty()
                .any { identity ->

                    identity.provider ==
                            "kakao"
                }

        check(
            kakaoIdentityExists
        ) {
            "Supabase Kakao Identity를 확인할 수 없습니다."
        }

        android.util.Log.d(
            "GuardianAuth",
            "7. Kakao Identity 확인 완료"
        )

        return requireNotNull(
            session.user?.id
        ) {
            "Supabase 사용자 ID가 없습니다."
        }
    }


    /*
     * =====================================================
     * Kakao SDK 로그인
     * =====================================================
     */

    private suspend fun loginWithKakao(
        context: Context,
    ): OAuthToken =

        suspendCoroutine {
                continuation ->


            /*
             * 카카오계정 로그인
             */
            val accountCallback:
                        (
                OAuthToken?,
                Throwable?
            ) -> Unit =
                { token, error ->

                    when {

                        error != null -> {

                            continuation
                                .resumeWithException(
                                    error
                                )
                        }


                        token != null -> {

                            continuation
                                .resume(
                                    token
                                )
                        }


                        else -> {

                            continuation
                                .resumeWithException(
                                    IllegalStateException(
                                        "카카오 로그인 결과가 없습니다."
                                    )
                                )
                        }
                    }
                }


            /*
             * 카카오톡 설치 여부
             */
            if (
                UserApiClient
                    .instance
                    .isKakaoTalkLoginAvailable(
                        context
                    )
            ) {

                /*
                 * 카카오톡 로그인
                 */
                UserApiClient
                    .instance
                    .loginWithKakaoTalk(
                        context
                    ) { token, error ->


                        /*
                         * 성공
                         */
                        if (
                            token != null
                        ) {

                            continuation
                                .resume(
                                    token
                                )

                            return@loginWithKakaoTalk
                        }


                        /*
                         * 사용자가 로그인 취소
                         */
                        if (
                            error is ClientError &&
                            error.reason ==
                            ClientErrorCause.Cancelled
                        ) {

                            continuation
                                .resumeWithException(
                                    error
                                )

                            return@loginWithKakaoTalk
                        }


                        /*
                         * 카카오톡 로그인 실패
                         *
                         * → 카카오계정 로그인으로 fallback
                         */
                        UserApiClient
                            .instance
                            .loginWithKakaoAccount(
                                context =
                                    context,
                                callback =
                                    accountCallback,
                            )
                    }

            } else {

                /*
                 * 카카오톡이 없으면
                 * 카카오계정 로그인
                 */
                UserApiClient
                    .instance
                    .loginWithKakaoAccount(
                        context =
                            context,
                        callback =
                            accountCallback,
                    )
            }
        }
}