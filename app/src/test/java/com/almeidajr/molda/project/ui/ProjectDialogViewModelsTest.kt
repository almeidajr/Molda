package com.almeidajr.molda.project.ui

import com.almeidajr.molda.R
import com.almeidajr.molda.project.ReferenceSnapshot
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.navigation.DeleteProjectKey
import com.almeidajr.molda.project.navigation.RenameProjectKey
import com.almeidajr.molda.project.navigation.SaveProjectKey
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import com.almeidajr.molda.testing.October18Clock
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * The save, rename and delete dialogs' ViewModels, each running its own storage command; the
 * command's own rules are in [ProjectCommandTest].
 */
class ProjectDialogViewModelsTest {
    private val storage = FakeProjectRepository(listOf(saved))

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun defaultName_fromProfile_offersKindProfileAndToday() = runTest {
        // When/Then
        assertEquals("Saia godê · Ana · 18/10/2026", save().defaultName("Saia godê"))
    }

    @Test
    fun save_withName_storesTheSnapshotUnderThatNameAndHoldsItsId() = runTest {
        // Given
        val dialog = save()

        // When
        dialog.save(ProjectName("Saia da formatura"))

        // Then
        val created = storage.current.single { it.id != saved.id }
        assertEquals(ProjectName("Saia da formatura"), created.name)
        assertEquals(ReferenceSnapshot, created.snapshot)
        assertEquals(October18Clock.instant(), created.createdAt)
        assertEquals(ProjectCommandState.Done(created.id), dialog.state.value)
    }

    @Test
    fun save_whenStorageFails_showsTheFailureAndStoresNothing() = runTest {
        // Given
        storage.failsCommands = true
        val dialog = save()

        // When
        dialog.save(ProjectName("Saia da formatura"))

        // Then
        assertEquals(ProjectCommandState.Failed(R.string.project_save_failed), dialog.state.value)
        assertEquals(listOf(saved), storage.current)
    }

    @Test
    fun rename_storedProject_storesTheNameAndIsDone() = runTest {
        // Given
        val dialog = rename()

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
        val dialog = rename()

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
    fun delete_storedProject_removesItAndIsDone() = runTest {
        // Given
        val dialog = delete()

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
        val dialog = delete()

        // When
        dialog.delete()

        // Then
        assertEquals(
            ProjectCommandState.Failed(R.string.project_delete_failed),
            dialog.state.value,
        )
        assertEquals(listOf(saved), storage.current)
    }

    private fun save() =
        SaveProjectViewModel(storage, October18Clock, SaveProjectKey(ReferenceSnapshot))

    private fun rename() =
        RenameProjectViewModel(storage, October18Clock, RenameProjectKey(saved.id))

    private fun delete() = DeleteProjectViewModel(storage, DeleteProjectKey(saved.id))
}
