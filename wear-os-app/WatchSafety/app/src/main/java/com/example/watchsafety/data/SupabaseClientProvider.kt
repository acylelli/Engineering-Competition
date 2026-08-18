package com.example.watchsafety.data

import com.example.watchsafety.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClientProvider {

    val client: SupabaseClient by lazy {

        require(
            BuildConfig.SUPABASE_URL.isNotBlank()
        ) {
            "SUPABASE_URL이 설정되지 않았습니다."
        }

        require(
            BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()
        ) {
            "SUPABASE_PUBLISHABLE_KEY가 설정되지 않았습니다."
        }

        createSupabaseClient(
            supabaseUrl =
                BuildConfig.SUPABASE_URL,

            supabaseKey =
                BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {

            install(Auth)

            install(Postgrest)
        }
    }
}