package com.watchsafety.guardian.data

object GuardianRepositoryProvider {
    fun create(): GuardianRepository {
        val client = SupabaseClientProvider.createOrNull()
        return if (client == null) {
            MockGuardianRepository()
        } else {
            SupabaseGuardianRepository(client)
        }
    }
}
