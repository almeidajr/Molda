package com.almeidajr.molda.project.ui

import com.almeidajr.molda.R
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.navigation.DeleteProjectKey
import com.almeidajr.molda.project.navigation.RenameProjectKey
import com.almeidajr.molda.project.navigation.SaveProjectKey
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import com.almeidajr.molda.testing.October18Clock
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** The save, rename and delete dialogs' ViewModels, each running its own storage command. */
class ProjectDialogViewModelsTest {
    private val storage = FakeProjectRepository(listOf(saved))

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun defaultName_fromProfile_offersKindProfileAndToday() = runTest {
        // When/Then
        assertEquals("Saia godê · Ana · 18/10/2026", saveViewModel().defaultName("Saia godê"))
    }

    @Test
    fun save_withName_storesTheSnapshotUnderThatNameAndHoldsItsId() = runTest {
        // Given
        val dialog = saveViewModel()

        // When
        dialog.save(ProjectName("Saia da formatura"))

        // Then
        val created = storage.current.single { it.id != saved.id }
        assertEquals(ProjectName("Saia da formatura"), created.name)
        assertEquals(fromProfile, created.snapshot)
        assertEquals(October18Clock.instant(), created.createdAt)
        assertEquals(ProjectCommandState.Done(created.id), dialog.state.value)
    }

    @Test
    fun save_whenStorageFails_showsTheFailureAndStoresNothing() = runTest {
        // Given
        storage.failsCommands = true
        val dialog = saveViewModel()

        // When
        dialog.save(ProjectName("Saia da formatura"))

        // Then
        assertEquals(ProjectCommandState.Failed(R.string.project_save_failed), dialog.state.value)
        assertEquals(listOf(saved), storage.current)
    }

    @Test
    fun save_duringTheStorageCall_isBusyAndIgnoresAnotherSave() = runTest {
        // Given
        val gate = CompletableDeferred<Unit>()
        storage.commandGate = gate
        val dialog = saveViewModel()
        dialog.save(ProjectName("Saia"))

        // When
        dialog.save(ProjectName("Saia"))

        // Then
        assertEquals(ProjectCommandState.Running, dialog.state.value)
        gate.complete(Unit)
        assertEquals(2, storage.current.size)
    }

    @Test
    fun save_afterSaving_storesNoOtherProject() = runTest {
        // Given
        val dialog = saveViewModel()
        dialog.save(ProjectName("Saia"))

        // When
        dialog.save(ProjectName("Saia"))

        // Then
        assertEquals(2, storage.current.size)
    }

    @Test
    fun save_afterTheOutcomeIsAcknowledged_storesNoOtherProject() = runTest {
        // Given
        val dialog = saveViewModel()
        dialog.save(ProjectName("Saia"))
        dialog.acknowledge()

        // When
        dialog.save(ProjectName("Saia"))

        // Then
        assertEquals(ProjectCommandState.Spent, dialog.state.value)
        assertEquals(2, storage.current.size)
    }

    @Test
    fun save_afterAFailure_runsAgain() = runTest {
        // Given
        storage.failsCommands = true
        val dialog = saveViewModel()
        dialog.save(ProjectName("Saia"))
        storage.failsCommands = false

        // When
        dialog.save(ProjectName("Saia"))

        // Then
        assertEquals(2, storage.current.size)
    }

    @Test
    fun rename_storesTheNameAndIsDone() = runTest {
        // Given
        val dialog = RenameProjectViewModel(storage, October18Clock, RenameProjectKey(saved.id))

        // When
        dialog.rename(ProjectName("Saia da formatura"))

        // Then
        assertEquals(
            listOf(
                saved.copy(
                    name = ProjectName("Saia da formatura"),
                    updatedAt = October18Clock.instant(),
                ),
            ),
            storage.current,
        )
        assertEquals(ProjectCommandState.Done(Unit), dialog.state.value)
    }

    @Test
    fun rename_whenStorageFails_keepsTheProjectAndShowsTheFailure() = runTest {
        // Given
        storage.failsCommands = true
        val dialog = RenameProjectViewModel(storage, October18Clock, RenameProjectKey(saved.id))

        // When
        dialog.rename(ProjectName("Saia da formatura"))

        // Then
        assertEquals(
            ProjectCommandState.Failed(R.string.project_rename_failed),
            dialog.state.value,
        )
        assertEquals(listOf(saved), storage.current)
    }

    @Test
    fun rename_duringTheStorageCall_isBusyAndIgnoresAnotherRename() = runTest {
        // Given
        val gate = CompletableDeferred<Unit>()
        storage.commandGate = gate
        val dialog = RenameProjectViewModel(storage, October18Clock, RenameProjectKey(saved.id))
        dialog.rename(ProjectName("Saia da formatura"))

        // When
        dialog.rename(ProjectName("Saia do casamento"))

        // Then
        assertEquals(ProjectCommandState.Running, dialog.state.value)
        gate.complete(Unit)
        assertEquals(ProjectName("Saia da formatura"), storage.current.single().name)
    }

    @Test
    fun delete_duringTheStorageCall_isBusyAndIgnoresAnotherDelete() = runTest {
        // Given
        val gate = CompletableDeferred<Unit>()
        storage.commandGate = gate
        val dialog = DeleteProjectViewModel(storage, DeleteProjectKey(saved.id))
        dialog.delete()

        // When
        dialog.delete()

        // Then
        assertEquals(ProjectCommandState.Running, dialog.state.value)
        gate.complete(Unit)
        // A second delete would fail on the project the first removed.
        assertEquals(ProjectCommandState.Done(Unit), dialog.state.value)
    }

    @Test
    fun delete_removesTheProjectAndIsDone() = runTest {
        // Given
        val dialog = DeleteProjectViewModel(storage, DeleteProjectKey(saved.id))

        // When
        dialog.delete()

        // Then
        assertEquals(emptyList(), storage.current)
        assertEquals(ProjectCommandState.Done(Unit), dialog.state.value)
    }

    @Test
    fun delete_whenStorageFails_keepsTheProjectAndShowsTheFailure() = runTest {
        // Given
        storage.failsCommands = true
        val dialog = DeleteProjectViewModel(storage, DeleteProjectKey(saved.id))

        // When
        dialog.delete()

        // Then
        assertEquals(
            ProjectCommandState.Failed(R.string.project_delete_failed),
            dialog.state.value,
        )
        assertEquals(listOf(saved), storage.current)
    }

    private fun saveViewModel() =
        SaveProjectViewModel(storage, October18Clock, SaveProjectKey(fromProfile))
}
