package com.watchsafety.guardian.data

import com.watchsafety.guardian.domain.model.NotificationSettings
import com.watchsafety.guardian.domain.model.SafetyEventType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockGuardianRepositoryTest {
    @Test
    fun addAndToggleSafeZone_updatesSharedSnapshot() = runBlocking {
        val repository = MockGuardianRepository()

        repository.addSafeZone(name = "테스트 공원", radiusMeters = 400)
        val addedZone = repository.snapshot.value.safeZones.last()

        assertEquals("테스트 공원", addedZone.name)
        assertEquals(400, addedZone.radiusMeters)
        assertTrue(addedZone.enabled)

        repository.setSafeZoneEnabled(addedZone.id, enabled = false)
        assertFalse(repository.snapshot.value.safeZones.last().enabled)
    }

    @Test
    fun returnHomeAndNotificationChanges_arePersisted() = runBlocking {
        val repository = MockGuardianRepository()
        val changedSettings = NotificationSettings(
            sosAlert = false,
            safeZoneExitAlert = true,
            arrivalAlert = false,
            batteryLowAlert = true,
        )

        repository.sendReturnHomeRequest()
        repository.updateNotificationSettings(changedSettings)

        assertTrue(repository.snapshot.value.returnHomeRequested)
        assertEquals(changedSettings, repository.snapshot.value.notificationSettings)
        assertEquals(
            SafetyEventType.RETURN_HOME_REQUESTED,
            repository.snapshot.value.events.first().type,
        )
    }
}
