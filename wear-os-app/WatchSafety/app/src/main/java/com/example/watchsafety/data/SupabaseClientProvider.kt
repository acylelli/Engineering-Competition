package com.example.watchsafety.data

import com.example.watchsafety.BuildConfig

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime


/*
 * =========================================================
 * SupabaseClientProvider
 * =========================================================
 *
 * Wear OS에서는 Foreground Service가 앱 화면이 백그라운드로
 * 내려간 뒤에도 Supabase RPC를 계속 호출해야 한다.
 *
 * supabase-kt의 Android 기본 Lifecycle 연동은 앱이 background로
 * 전환될 때 Auth session을 메모리에서 제거할 수 있다.
 *
 * 그래서 위치 Foreground Service가 계속 동작해야 하는 이 프로젝트에서는
 * enableLifecycleCallbacks = false 로 설정해
 * Activity lifecycle과 Supabase Auth session을 분리한다.
 */
object SupabaseClientProvider {


    val client:
            SupabaseClient by lazy {


        require(
            BuildConfig
                .SUPABASE_URL
                .isNotBlank()
        ) {

            "SUPABASE_URL이 설정되지 않았습니다."
        }


        require(
            BuildConfig
                .SUPABASE_PUBLISHABLE_KEY
                .isNotBlank()
        ) {

            "SUPABASE_PUBLISHABLE_KEY가 설정되지 않았습니다."
        }


        createSupabaseClient(

            supabaseUrl =
                BuildConfig
                    .SUPABASE_URL,

            supabaseKey =
                BuildConfig
                    .SUPABASE_PUBLISHABLE_KEY

        ) {


            /*
             * =================================================
             * Auth
             * =================================================
             *
             * 핵심:
             * 앱이 Home/background 상태가 되어도
             * 현재 워치 익명 세션을 메모리에서 제거하지 않는다.
             *
             * autoLoadFromStorage / alwaysAutoRefresh도 명시적으로 유지.
             */
            install(
                Auth
            ) {

                enableLifecycleCallbacks =
                    false


                autoLoadFromStorage =
                    true


                alwaysAutoRefresh =
                    true
            }


            /*
             * Database RPC / CRUD
             */
            install(
                Postgrest
            )


            /*
             * 귀가 요청 등 Realtime
             */
            install(
                Realtime
            )
        }
    }
}