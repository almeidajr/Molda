package com.almeidajr.molda.project.ui

import com.almeidajr.molda.R
import com.almeidajr.molda.project.ReferenceSnapshot
import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import com.almeidajr.molda.testing.October18Clock
import com.almeidajr.molda.ui.navigation.EditorKind
import com.almeidajr.molda.ui.navigation.EditorReport
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** The project editor's update; its command's own rules are in [ProjectCommandTest]. */
class ProjectUpdateViewModelTest {
    private val storage = FakeProjectRepository(listOf(saved))

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun update_existingProject_replacesItsSnapshotAndReportsItUpdated() = runTest {
        // Given
        val update = updateViewModel()

        // When
        update.update(ReferenceSnapshot)

        // Then
        assertEquals(
            listOf(saved.copy(snapshot = ReferenceSnapshot, updatedAt = October18Clock.instant())),
            storage.current,
        )
        assertEquals(ProjectCommandState.Done(Unit), update.state.value)
    }

    @Test
    fun update_whenStorageFails_keepsTheProjectAndReportsTheFailure() = runTest {
        // Given
        storage.failsCommands = true
        val update = updateViewModel()

        // When
        update.update(ReferenceSnapshot)

        // Then
        assertEquals(listOf(saved), storage.current)
        assertEquals(
            ProjectCommandState.Failed(R.string.project_update_failed),
            update.state.value,
        )
    }

    @Test
    fun report_whileADirtyDraftUpdates_isTheDirtyUpdatingProjectEditor() = runTest {
        // Given
        storage.commandGate = CompletableDeferred()
        val isEditorDirty = MutableStateFlow(false)
        val update = updateViewModel(isEditorDirty)

        // When
        isEditorDirty.value = true
        update.update(ReferenceSnapshot)

        // Then
        assertEquals(
            EditorReport(EditorKind.Project, isDirty = true, isSaving = true),
            update.report.value,
        )
    }

    private fun updateViewModel(isEditorDirty: StateFlow<Boolean> = MutableStateFlow(false)) =
        ProjectUpdateViewModel(
            storage,
            October18Clock,
            ProjectEditorKey(saved.id, saved.snapshot),
            isEditorDirty,
        )
}
