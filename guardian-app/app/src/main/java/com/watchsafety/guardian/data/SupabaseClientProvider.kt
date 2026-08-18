package com.watchsafety.guardian.data

import com.watchsafety.guardian.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClientProvider {
    fun createOrNull(): SupabaseClient? {
        val url = BuildConfig.SUPABASE_URL.trim()
        val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
        if (url.isBlank() || publishableKey.isBlank()) return null

        return createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = publishableKey,
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}
