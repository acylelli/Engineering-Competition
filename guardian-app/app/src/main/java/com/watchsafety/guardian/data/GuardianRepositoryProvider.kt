package com.watchsafety.guardian.data

object GuardianRepositoryProvider {

    fun create(): GuardianRepository {

        val client =
            requireNotNull(
                SupabaseClientProvider.createOrNull()
            ) {
                """
                Supabase 설정이 없습니다.
                보호자 앱의 local.properties에서
                SUPABASE_URL,
                SUPABASE_PUBLISHABLE_KEY를 확인하세요.
                """.trimIndent()
            }

        return SupabaseGuardianRepository(
            client
        )
    }
}