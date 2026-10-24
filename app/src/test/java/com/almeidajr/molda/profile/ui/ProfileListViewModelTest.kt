package com.almeidajr.molda.profile.ui

import com.almeidajr.molda.R
import com.almeidajr.molda.data.LoadResult
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.testing.FakeProfileRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
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
        assertEquals(LoadResult.Success(listOf(ana, added)), profiles.state.value.load)
    }

    @Test
    fun load_whenRepositoryFails_showsLoadFailure() = runTest {
        // Given
        storage.failsLoad = true

        // When
        val profiles = ProfileListViewModel(storage)

        // Then
        assertIs<LoadResult.Error>(profiles.state.value.load)
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
        assertEquals(LoadResult.Success(listOf(ana)), profiles.state.value.load)
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
        assertEquals(LoadResult.Success(listOf(ana)), profiles.state.value.load)
    }

    @Test
    fun delete_storedProfile_removesItAndReportsTheDeletion() = runTest {
        // Given
        val profiles = ProfileListViewModel(storage)

        // When
        profiles.delete(ana.id)

        // Then
        val deleted = profiles.state.value
        assertEquals(LoadResult.Success(emptyList()), deleted.load)
        assertEquals(ana.id, deleted.deleted)
    }

    @Test
    fun acknowledge_afterDeleting_forgetsTheOutcome() = runTest {
        // Given
        val profiles = ProfileListViewModel(storage)
        profiles.delete(ana.id)

        // When
        profiles.acknowledge()

        // Then
        assertNull(profiles.state.value.deleted)
    }

    @Test
    fun delete_whenStorageFails_keepsTheProfileAndShowsTheFailureOnItsRowOnly() = runTest {
        // Given
        val storage = FakeProfileRepository(listOf(ana, bia)).apply { failsCommands = true }
        val profiles = ProfileListViewModel(storage)

        // When
        profiles.delete(ana.id)

        // Then
        val failed = profiles.state.value
        assertEquals(R.string.profile_delete_failed, failed.deleteError(ana.id))
        assertNull(failed.deleteError(bia.id))
        assertFalse(failed.isDeleting(ana.id))
        assertTrue(failed.canChangeProfiles)
        assertNull(failed.deleted)
        assertEquals(LoadResult.Success(listOf(ana, bia)), failed.load)
    }

    @Test
    fun delete_ofProfileDeletedMeanwhile_reportsItDeletedWithoutAFailure() = runTest {
        // Given
        val gate = CompletableDeferred<Unit>()
        storage.commandGate = gate
        val profiles = ProfileListViewModel(storage)
        profiles.delete(ana.id)
        storage.forget(ana.id)

        // When
        gate.complete(Unit)

        // Then
        val deleted = profiles.state.value
        assertEquals(ana.id, deleted.deleted)
        assertNull(deleted.deleteError(ana.id))
        assertFalse(deleted.isDeleting(ana.id))
    }

    @Test
    fun delete_afterAFailedOne_clearsThatRowsError() = runTest {
        // Given
        val storage = FakeProfileRepository(listOf(ana, bia)).apply { failsCommands = true }
        val profiles = ProfileListViewModel(storage)
        profiles.delete(ana.id)
        storage.failsCommands = false

        // When
        profiles.delete(bia.id)

        // Then
        assertNull(profiles.state.value.deleteError(ana.id))
        assertEquals(listOf(ana), storage.current)
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
