package com.watchsafety.guardian.data

import com.watchsafety.guardian.domain.model.NotificationSettings
import com.watchsafety.guardian.domain.model.SafeZoneKind
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

        repository.addSafeZone(
            name = "테스트 공원",
            radiusMeters = 400,
            latitude = 37.5,
            longitude = 126.9,
            isHome = false,
        )
        val addedZone = repository.snapshot.value.safeZones.last()

        assertEquals("테스트 공원", addedZone.name)
        assertEquals(400, addedZone.radiusMeters)
        assertTrue(addedZone.enabled)

        repository.setSafeZoneEnabled(addedZone.id, enabled = false)
        assertFalse(repository.snapshot.value.safeZones.last().enabled)
    }

    @Test
    fun safeZones_supportUnlimitedAddEditAndDelete() = runBlocking {
        val repository = MockGuardianRepository()

        repeat(7) { index ->
            repository.addSafeZone(
                name = "추가 구역 $index",
                radiusMeters = 300,
                latitude = 37.5 + index * 0.001,
                longitude = 126.9 + index * 0.001,
                isHome = false,
            )
        }

        assertEquals(10, repository.snapshot.value.safeZones.size)

        val zone = repository.snapshot.value.safeZones.last()
        repository.updateSafeZone(
            zoneId = zone.id,
            name = "이름 변경 완료",
            radiusMeters = 650,
            latitude = 37.6,
            longitude = 127.0,
            isHome = true,
        )

        val updated = repository.snapshot.value.safeZones.first { it.id == zone.id }
        assertEquals("이름 변경 완료", updated.name)
        assertEquals(650, updated.radiusMeters)
        assertEquals(SafeZoneKind.HOME, updated.kind)
        assertEquals(
            1,
            repository.snapshot.value.safeZones.count { it.kind == SafeZoneKind.HOME },
        )

        repository.deleteSafeZone(zone.id)
        assertFalse(repository.snapshot.value.safeZones.any { it.id == zone.id })
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
