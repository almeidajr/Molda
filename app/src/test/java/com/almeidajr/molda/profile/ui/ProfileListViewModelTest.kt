package com.almeidajr.molda.profile.ui

import com.almeidajr.molda.R
import com.almeidajr.molda.data.LoadResult
import com.almeidajr.molda.profile.Ana
import com.almeidajr.molda.profile.Bia
import com.almeidajr.molda.testing.FakeProfileRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ProfileListViewModelTest {
    private val storage = FakeProfileRepository(listOf(Ana))

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun profiles_afterRepositoryChange_followItWithoutReload() = runTest {
        // Given
        val profiles = ProfileListViewModel(storage)

        // When
        val added = storage.create(Bia.measurements).getOrThrow()

        // Then
        assertEquals(LoadResult.Success(listOf(Ana, added)), profiles.state.value.load)
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
        assertEquals(LoadResult.Success(listOf(Ana)), profiles.state.value.load)
    }

    @Test
    fun delete_storedProfile_removesItAndReportsTheDeletion() = runTest {
        // Given
        val profiles = ProfileListViewModel(storage)

        // When
        profiles.delete(Ana.id)

        // Then
        val deleted = profiles.state.value
        assertEquals(LoadResult.Success(emptyList()), deleted.load)
        assertEquals(Ana.id, deleted.deleted)
    }

    @Test
    fun delete_whileInFlight_marksItsRowAndBlocksEveryRow() = runTest {
        // Given
        val storage = FakeProfileRepository(listOf(Ana, Bia))
        storage.commandGate = CompletableDeferred()
        val profiles = ProfileListViewModel(storage)

        // When
        profiles.delete(Ana.id)

        // Then
        val deleting = profiles.state.value
        assertTrue(deleting.isDeleting(Ana.id))
        assertFalse(deleting.isDeleting(Bia.id))
        assertFalse(deleting.canChangeProfiles)
    }

    @Test
    fun delete_whileAnotherDeleteRuns_isIgnored() = runTest {
        // Given
        val gate = CompletableDeferred<Unit>()
        val storage = FakeProfileRepository(listOf(Ana, Bia)).apply { commandGate = gate }
        val profiles = ProfileListViewModel(storage)
        profiles.delete(Ana.id)

        // When
        profiles.delete(Bia.id)
        gate.complete(Unit)

        // Then
        assertEquals(listOf(Bia), storage.current)
        assertTrue(profiles.state.value.canChangeProfiles)
    }

    @Test
    fun acknowledge_afterDeleting_forgetsTheOutcome() = runTest {
        // Given
        val profiles = ProfileListViewModel(storage)
        profiles.delete(Ana.id)

        // When
        profiles.acknowledge()

        // Then
        assertNull(profiles.state.value.deleted)
    }

    @Test
    fun delete_whenStorageFails_keepsTheProfileAndShowsTheFailureOnItsRowOnly() = runTest {
        // Given
        val storage = FakeProfileRepository(listOf(Ana, Bia)).apply { failsCommands = true }
        val profiles = ProfileListViewModel(storage)

        // When
        profiles.delete(Ana.id)

        // Then
        val failed = profiles.state.value
        assertEquals(R.string.profile_delete_failed, failed.deleteError(Ana.id))
        assertNull(failed.deleteError(Bia.id))
        assertFalse(failed.isDeleting(Ana.id))
        assertTrue(failed.canChangeProfiles)
        assertNull(failed.deleted)
        assertEquals(LoadResult.Success(listOf(Ana, Bia)), failed.load)
    }

    @Test
    fun delete_ofProfileDeletedMeanwhile_reportsItDeletedWithoutAFailure() = runTest {
        // Given
        val gate = CompletableDeferred<Unit>()
        storage.commandGate = gate
        val profiles = ProfileListViewModel(storage)
        profiles.delete(Ana.id)
        storage.forget(Ana.id)

        // When
        gate.complete(Unit)

        // Then
        val deleted = profiles.state.value
        assertEquals(Ana.id, deleted.deleted)
        assertNull(deleted.deleteError(Ana.id))
        assertFalse(deleted.isDeleting(Ana.id))
    }

    @Test
    fun delete_afterAFailedOne_clearsThatRowsError() = runTest {
        // Given
        val storage = FakeProfileRepository(listOf(Ana, Bia)).apply { failsCommands = true }
        val profiles = ProfileListViewModel(storage)
        profiles.delete(Ana.id)
        storage.failsCommands = false

        // When
        profiles.delete(Bia.id)

        // Then
        assertNull(profiles.state.value.deleteError(Ana.id))
        assertEquals(listOf(Ana), storage.current)
    }
}
