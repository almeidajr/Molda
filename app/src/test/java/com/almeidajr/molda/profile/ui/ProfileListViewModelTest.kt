package com.almeidajr.molda.profile.ui

import com.almeidajr.molda.R
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.testing.FakeProfileRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ProfileListViewModelTest {
    private val storage = FakeProfileRepository(listOf(ana))

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun profiles_afterRepositoryChange_followItWithoutReload() = runTest {
        // Given
        val profiles = ProfileListViewModel(storage)

        // When
        val added = storage.create(bia.measurements).getOrThrow()

        // Then
        assertEquals(listOf(ana, added), profiles.state.value.profiles)
    }

    @Test
    fun load_whenRepositoryFails_showsLoadFailure() = runTest {
        // Given
        storage.failsLoad = true

        // When
        val profiles = ProfileListViewModel(storage)

        // Then
        assertEquals(R.string.profile_load_failed, profiles.state.value.loadError)
    }

    @Test
    fun load_retryAfterLoadFailure_showsStoredProfiles() = runTest {
        // Given
        storage.failsLoad = true
        val profiles = ProfileListViewModel(storage)
        storage.failsLoad = false

        // When
        profiles.load()

        // Then
        val retried = profiles.state.value
        assertNull(retried.loadError)
        assertEquals(listOf(ana), retried.profiles)
    }

    @Test
    fun load_whileStillLoading_keepsOneRepositoryCollector() = runTest {
        // Given
        storage.loadGate = CompletableDeferred()
        val counted = CollectorCounting(storage)
        val profiles = ProfileListViewModel(counted)

        // When
        profiles.load()
        profiles.load()
        storage.loadGate?.complete(Unit)

        // Then
        assertEquals(1, counted.activeCollectors)
        assertEquals(listOf(ana), profiles.state.value.profiles)
    }

    @Test
    fun delete_storedProfile_removesItAndReportsTheDeletion() = runTest {
        // Given
        val profiles = ProfileListViewModel(storage)

        // When
        profiles.delete(ana.id)

        // Then
        val deleted = profiles.state.value
        assertEquals(emptyList(), deleted.profiles)
        assertEquals(ana.id, deleted.deleted)
    }

    @Test
    fun deletionShown_afterDeleting_forgetsTheDeletion() = runTest {
        // Given
        val profiles = ProfileListViewModel(storage)
        profiles.delete(ana.id)

        // When
        profiles.deletionShown()

        // Then
        assertNull(profiles.state.value.deleted)
    }

    @Test
    fun delete_whenStorageFails_keepsTheProfileAndShowsTheFailure() = runTest {
        // Given
        storage.failsCommands = true
        val profiles = ProfileListViewModel(storage)

        // When
        profiles.delete(ana.id)

        // Then
        val failed = profiles.state.value
        assertEquals(R.string.profile_delete_failed, failed.saveError)
        assertNull(failed.deleted)
        assertEquals(ana.id, failed.deleteId)
        assertEquals(listOf(ana), failed.profiles)
    }

    @Test
    fun delete_whileItsOwnDeleteRuns_isIgnored() = runTest {
        // Given
        val storage = FakeProfileRepository(listOf(ana, bia)).apply {
            commandGate = CompletableDeferred()
        }
        val profiles = ProfileListViewModel(storage)
        profiles.delete(ana.id)

        // When
        profiles.delete(bia.id)
        storage.commandGate?.complete(Unit)

        // Then
        assertEquals(listOf(bia), storage.current)
    }
}

/** Counts the collectors of [profiles], to show a retry replaces the pending one. */
private class CollectorCounting(private val profiles: ProfileRepository) :
    ProfileRepository by profiles {
    var activeCollectors = 0
        private set

    override fun profiles(): Flow<List<MeasurementProfile>> = profiles.profiles()
        .onStart { activeCollectors++ }
        .onCompletion { activeCollectors-- }
}
