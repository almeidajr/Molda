package com.almeidajr.molda.project.ui

import com.almeidajr.molda.R
import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import com.almeidajr.molda.testing.October18Clock
import com.almeidajr.molda.ui.navigation.EditorKind
import com.almeidajr.molda.ui.navigation.EditorReport
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ProjectUpdateViewModelTest {
    private val storage = FakeProjectRepository(listOf(saved))

    // Nothing settles until the test advances, so a report holds before its flows emit.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun update_existingProject_replacesItsSnapshotAndReportsItUpdated() = runTest {
        // Given
        val update = updateViewModel()

        // When
        update.update(fromProfile)
        advanceUntilIdle()

        // Then
        assertEquals(
            listOf(saved.copy(snapshot = fromProfile, updatedAt = October18Clock.instant())),
            storage.current,
        )
        assertEquals(ProjectCommandState.Done(Unit), update.state.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun update_whenStorageFails_keepsTheProjectAndReportsTheFailure() = runTest {
        // Given
        storage.failsCommands = true
        val update = updateViewModel()

        // When
        update.update(fromProfile)
        advanceUntilIdle()

        // Then
        assertEquals(listOf(saved), storage.current)
        assertEquals(
            ProjectCommandState.Failed(R.string.project_update_failed),
            update.state.value,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun update_ofProjectDeletedMeanwhile_reportsItDeletedWithoutAFailure() = runTest {
        // Given
        val update = updateViewModel()
        storage.delete(saved.id).getOrThrow()

        // When
        update.update(fromProfile)
        advanceUntilIdle()

        // Then
        assertEquals(ProjectCommandState.Deleted, update.state.value)
        assertEquals(emptyList(), storage.current)
    }

    @Test
    fun update_duringTheStorageCall_isBusy() = runTest {
        // Given
        storage.commandGate = CompletableDeferred()
        val update = updateViewModel()

        // When
        update.update(fromProfile)

        // Then
        assertEquals(ProjectCommandState.Running, update.state.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun acknowledge_afterAnUpdate_spendsTheCommand() = runTest {
        // Given
        val update = updateViewModel()
        update.update(fromProfile)
        advanceUntilIdle()

        // When
        update.acknowledge()

        // Then
        assertEquals(ProjectCommandState.Spent, update.state.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun acknowledgeError_afterAFailure_forgetsTheFailure() = runTest {
        // Given
        storage.failsCommands = true
        val update = updateViewModel()
        update.update(fromProfile)
        advanceUntilIdle()

        // When
        update.acknowledgeError()

        // Then
        assertEquals(ProjectCommandState.Idle, update.state.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun acknowledgeError_afterAnUpdate_keepsTheOutcome() = runTest {
        // Given: a retry succeeded before the failure's reaction acknowledged it
        val update = updateViewModel()
        update.update(fromProfile)
        advanceUntilIdle()

        // When
        update.acknowledgeError()

        // Then
        assertEquals(ProjectCommandState.Done(Unit), update.state.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun update_beforeTheOutcomeIsAcknowledged_isIgnored() = runTest {
        // Given
        val update = updateViewModel()
        update.update(fromProfile)
        advanceUntilIdle()

        // When
        update.update(saved.snapshot)
        advanceUntilIdle()

        // Then
        assertEquals(fromProfile, storage.current.single().snapshot)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun report_whileADirtyDraftUpdates_isTheDirtyUpdatingProjectEditor() = runTest {
        // Given
        storage.commandGate = CompletableDeferred()
        val isEditorDirty = MutableStateFlow(false)
        val update = updateViewModel(isEditorDirty)

        // When
        isEditorDirty.value = true
        update.update(fromProfile)
        advanceUntilIdle()

        // Then
        assertEquals(
            EditorReport(EditorKind.Project, isDirty = true, isSaving = true),
            update.report.value,
        )
    }

    @Test
    fun report_onCreation_isTheEditorsCurrentStatus() = runTest {
        // Given
        val isEditorDirty = MutableStateFlow(true)

        // When
        val update = updateViewModel(isEditorDirty)

        // Then
        assertEquals(EditorReport(EditorKind.Project, isDirty = true), update.report.value)
    }

    private fun updateViewModel(isEditorDirty: StateFlow<Boolean> = MutableStateFlow(false)) =
        ProjectUpdateViewModel(
            storage,
            October18Clock,
            ProjectEditorKey(saved.id, saved.snapshot),
            isEditorDirty,
        )
}
