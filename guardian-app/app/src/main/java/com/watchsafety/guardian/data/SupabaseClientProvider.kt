package com.watchsafety.guardian.data

import com.watchsafety.guardian.BuildConfig

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime


object SupabaseClientProvider {

    val client: SupabaseClient by lazy {

        val url =
            BuildConfig
                .SUPABASE_URL
                .trim()

        val publishableKey =
            BuildConfig
                .SUPABASE_PUBLISHABLE_KEY
                .trim()

        require(
            url.isNotBlank()
        ) {
            "SUPABASE_URL이 설정되지 않았습니다."
        }

        require(
            publishableKey.isNotBlank()
        ) {
            "SUPABASE_PUBLISHABLE_KEY가 설정되지 않았습니다."
        }

        createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = publishableKey,
        ) {

            install(Auth)

            install(Postgrest)

            install(Realtime)
        }
    }


    fun createOrNull(): SupabaseClient? =
        runCatching {
            client
        }.getOrNull()
}