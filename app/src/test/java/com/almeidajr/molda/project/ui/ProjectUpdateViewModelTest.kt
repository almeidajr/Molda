package com.almeidajr.molda.project.ui

import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import com.almeidajr.molda.testing.October18Clock
import com.almeidajr.molda.ui.navigation.EditorKind
import com.almeidajr.molda.ui.navigation.EditorReport
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

    @Test
    fun report_whileADirtyDraftUpdates_isTheDirtyUpdatingProjectEditor() = runTest {
        // Given
        storage.commandGate = CompletableDeferred()
        val isEditorDirty = MutableStateFlow(false)
        val update = updateViewModel(isEditorDirty)

        // When
        isEditorDirty.value = true
        update.update(fromProfile)

        // Then
        assertEquals(
            EditorReport(EditorKind.Project, isDirty = true, isSaving = true),
            update.report.value,
        )
    }

    private fun updateViewModel(isEditorDirty: Flow<Boolean> = MutableStateFlow(false)) =
        ProjectUpdateViewModel(
            storage,
            October18Clock,
            ProjectEditorKey(saved.id, saved.snapshot),
            isEditorDirty,
        )
}
