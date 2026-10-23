package com.almeidajr.molda.project.ui

import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import com.almeidajr.molda.testing.October18Clock
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ProjectUpdateViewModelTest {
    private val storage = FakeProjectRepository(listOf(saved))

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun update_existingProject_replacesItsSnapshotAndReportsItUpdated() = runTest {
        // Given
        val update = updateViewModel()

        // When
        update.update(fromProfile)

        // Then
        assertEquals(
            listOf(saved.copy(snapshot = fromProfile, updatedAt = October18Clock.instant())),
            storage.current,
        )
        assertEquals(ProjectUpdateState(outcome = ProjectUpdateOutcome.Updated), update.state.value)
    }

    @Test
    fun update_whenStorageFails_keepsTheProjectAndReportsTheFailure() = runTest {
        // Given
        storage.failsCommands = true
        val update = updateViewModel()

        // When
        update.update(fromProfile)

        // Then
        assertEquals(listOf(saved), storage.current)
        assertEquals(ProjectUpdateState(outcome = ProjectUpdateOutcome.Failed), update.state.value)
    }

    @Test
    fun update_duringTheStorageCall_isUpdating() = runTest {
        // Given
        storage.commandGate = CompletableDeferred()
        val update = updateViewModel()

        // When
        update.update(fromProfile)

        // Then
        assertEquals(ProjectUpdateState(isUpdating = true), update.state.value)
    }

    @Test
    fun outcomeShown_afterAnUpdate_forgetsTheOutcome() = runTest {
        // Given
        val update = updateViewModel()
        update.update(fromProfile)

        // When
        update.outcomeShown()

        // Then
        assertEquals(ProjectUpdateState(), update.state.value)
    }

    private fun updateViewModel() =
        ProjectUpdateViewModel(storage, October18Clock, ProjectEditorKey(saved.id, saved.snapshot))
}
