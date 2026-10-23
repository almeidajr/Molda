package com.almeidajr.molda.project.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.testing.viewModelScenario
import com.almeidajr.molda.R
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.measurement.MeasurementSource
import com.almeidajr.molda.project.domain.PatternDraftKind
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import com.almeidajr.molda.testing.October18Clock
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric supplies the real Bundle that saved state is parcelled through.
@RunWith(RobolectricTestRunner::class)
class ProjectsViewModelTest {
    private val storage = FakeProjectRepository(listOf(saved))
    private val shown = mutableListOf<Int>()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun projects_whenStorageChanges_followIt() = runTest {
        // Given
        val projects = projectsViewModel()

        // When
        storage.delete(saved.id).getOrThrow()

        // Then
        assertEquals(emptyList(), projects.state.value.projects)
    }

    @Test
    fun load_afterLoadFailure_showsStoredProjects() = runTest {
        // Given
        storage.failsLoad = true
        val projects = projectsViewModel()
        storage.failsLoad = false

        // When
        projects.load()

        // Then
        assertNull(projects.state.value.loadError)
        assertEquals(listOf(saved), projects.state.value.projects)
    }

    @Test
    fun startSaving_fromProfile_offersPatternProfileAndTodayAsName() = runTest {
        // Given
        val projects = projectsViewModel()

        // When
        projects.startSaving(fromProfile, patternName = "Saia godê")

        // Then
        assertEquals("Saia godê · Ana · 18/10/2026", projects.state.value.saving?.name)
    }

    @Test
    fun save_withEditedName_storesSnapshotUnderThatNameAndCloses() = runTest {
        // Given
        val projects = projectsViewModel()
        projects.startSaving(fromProfile, patternName = "Saia godê")
        projects.editName("Saia da formatura")

        // When
        projects.save(shown::add)

        // Then
        val created = storage.current.single { it.id != saved.id }
        assertEquals(ProjectName("Saia da formatura"), created.name)
        assertEquals(fromProfile, created.snapshot)
        assertEquals(October18Clock.instant(), created.createdAt)
        assertNull(projects.state.value.saving)
        assertEquals(listOf(R.string.project_saved), shown)
    }

    @Test
    fun save_withBlankName_flagsTheNameAndStoresNothing() = runTest {
        // Given
        val projects = projectsViewModel()
        projects.startSaving(fromProfile, patternName = "Saia godê")
        projects.editName("   ")

        // When
        projects.save(shown::add)

        // Then
        assertTrue(projects.state.value.saving?.hasBlankName == true)
        assertEquals(listOf(saved), storage.current)
    }

    @Test
    fun editName_afterBlankSaveWasRefused_clearsTheFlag() = runTest {
        // Given
        val projects = projectsViewModel()
        projects.startSaving(fromProfile, patternName = "Saia godê")
        projects.editName("   ")
        projects.save(shown::add)

        // When
        projects.editName("Saia")

        // Then
        assertFalse(projects.state.value.saving?.hasBlankName ?: true)
    }

    @Test
    fun save_whenStorageFails_keepsTheDialogAndShowsTheFailure() = runTest {
        // Given
        storage.failsCommands = true
        val projects = projectsViewModel()
        projects.startSaving(fromProfile, patternName = "Saia godê")
        projects.editName("Saia da formatura")

        // When
        projects.save(shown::add)

        // Then
        assertEquals(
            ProjectSaving(fromProfile, "Saia da formatura", error = R.string.project_save_failed),
            projects.state.value.saving,
        )
        assertEquals(emptyList<Int>(), shown)
        assertEquals(listOf(saved), storage.current)
    }

    @Test
    fun save_withoutShowsSaved_showsNoProject() = runTest {
        // Given
        val projects = projectsViewModel()
        projects.startSaving(fromProfile, patternName = "Saia godê")

        // When
        projects.save(shown::add)

        // Then
        assertNull(projects.state.value.projectToShow)
    }

    @Test
    fun save_withShowsSaved_keepsTheOriginalAndShowsTheNewProject() = runTest {
        // Given
        val projects = projectsViewModel()
        projects.startSaving(fromProfile, patternName = "Saia godê", showsSaved = true)

        // When
        projects.save(shown::add)

        // Then
        val created = storage.current.single { it.id != saved.id }
        assertTrue(saved in storage.current)
        assertEquals(created.id, projects.state.value.projectToShow)
    }

    @Test
    fun onProjectShown_afterSaveWithShowsSaved_clearsTheProjectToShow() = runTest {
        // Given
        val projects = projectsViewModel()
        projects.startSaving(fromProfile, patternName = "Saia godê", showsSaved = true)
        projects.save(shown::add)

        // When
        projects.onProjectShown()

        // Then
        assertNull(projects.state.value.projectToShow)
    }

    @Test
    fun cancel_withSaveDialogOpen_closesIt() = runTest {
        // Given
        val projects = projectsViewModel()
        projects.startSaving(fromProfile, patternName = "Saia godê")

        // When
        projects.cancel()

        // Then
        assertNull(projects.state.value.saving)
    }

    @Test
    fun cancel_whileSaving_keepsTheDialog() = runTest {
        // Given
        storage.commandGate = CompletableDeferred()
        val projects = projectsViewModel()
        projects.startSaving(fromProfile, patternName = "Saia godê")
        projects.save(shown::add)

        // When
        projects.cancel()

        // Then
        assertTrue(projects.state.value.saving?.isBusy == true)
    }

    @Test
    fun update_existingProject_replacesItsSnapshotAndShowsIt() = runTest {
        // Given
        val projects = projectsViewModel()

        // When
        projects.update(saved.id, fromProfile, shown::add)

        // Then
        val state = projects.state.value
        assertEquals(
            listOf(saved.copy(snapshot = fromProfile, updatedAt = October18Clock.instant())),
            storage.current,
        )
        assertEquals(listOf(R.string.project_updated), shown)
        assertEquals(saved.id, state.projectToShow)
        assertFalse(state.isUpdating)
    }

    @Test
    fun update_whenStorageFails_keepsTheProjectAndShowsTheFailure() = runTest {
        // Given
        storage.failsCommands = true
        val projects = projectsViewModel()

        // When
        projects.update(saved.id, fromProfile, shown::add)

        // Then
        val state = projects.state.value
        assertEquals(listOf(saved), storage.current)
        assertEquals(listOf(R.string.project_update_failed), shown)
        assertNull(state.projectToShow)
        assertFalse(state.isUpdating)
    }

    @Test
    fun startRenaming_ofProject_offersItsCurrentName() = runTest {
        // Given
        val projects = projectsViewModel()

        // When
        projects.startRenaming(saved)

        // Then
        assertEquals("Saia antiga", projects.state.value.renaming?.name)
    }

    @Test
    fun rename_withEditedName_storesTheNameAndCloses() = runTest {
        // Given
        val projects = projectsViewModel()
        projects.startRenaming(saved)
        projects.editName("Saia da formatura")

        // When
        projects.rename(shown::add)

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
        assertNull(projects.state.value.renaming)
        assertEquals(listOf(R.string.project_renamed), shown)
    }

    @Test
    fun rename_withBlankName_flagsTheNameAndStoresNothing() = runTest {
        // Given
        val projects = projectsViewModel()
        projects.startRenaming(saved)
        projects.editName("   ")

        // When
        projects.rename(shown::add)

        // Then
        assertTrue(projects.state.value.renaming?.hasBlankName == true)
        assertEquals(listOf(saved), storage.current)
    }

    @Test
    fun editName_afterBlankRenameWasRefused_clearsTheFlag() = runTest {
        // Given
        val projects = projectsViewModel()
        projects.startRenaming(saved)
        projects.editName("   ")
        projects.rename(shown::add)

        // When
        projects.editName("Saia")

        // Then
        assertFalse(projects.state.value.renaming?.hasBlankName ?: true)
    }

    @Test
    fun rename_whenStorageFails_keepsTheDialogAndShowsTheFailure() = runTest {
        // Given
        storage.failsCommands = true
        val projects = projectsViewModel()
        projects.startRenaming(saved)
        projects.editName("Saia da formatura")

        // When
        projects.rename(shown::add)

        // Then
        assertEquals(
            ProjectRenaming(saved.id, "Saia da formatura", error = R.string.project_rename_failed),
            projects.state.value.renaming,
        )
        assertEquals(listOf(saved), storage.current)
    }

    @Test
    fun delete_afterConfirmation_removesTheProject() = runTest {
        // Given
        val projects = projectsViewModel()
        projects.startDeleting(saved)

        // When
        projects.delete(shown::add)

        // Then
        assertEquals(emptyList(), storage.current)
        assertNull(projects.state.value.deleting)
        assertEquals(listOf(R.string.project_deleted), shown)
    }

    @Test
    fun delete_whenStorageFails_keepsTheProjectAndShowsTheFailure() = runTest {
        // Given
        storage.failsCommands = true
        val projects = projectsViewModel()
        projects.startDeleting(saved)

        // When
        projects.delete(shown::add)

        // Then
        assertEquals(
            ProjectDeleting(saved.id, "Saia antiga", error = R.string.project_delete_failed),
            projects.state.value.deleting,
        )
        assertEquals(listOf(saved), storage.current)
    }

    @Test
    fun cancel_withDeleteConfirmationOpen_keepsTheProject() = runTest {
        // Given
        val projects = projectsViewModel()
        projects.startDeleting(saved)

        // When
        projects.cancel()

        // Then
        assertNull(projects.state.value.deleting)
        assertEquals(listOf(saved), storage.current)
    }

    @Test
    fun recreate_withSaveDialogOpen_keepsTheDialogAndItsName() = runTest {
        // When
        recreatedAfter { projects ->
            projects.startSaving(fromProfile, patternName = "Saia godê", showsSaved = true)
            projects.editName("Saia da formatura")
        }.use { scenario ->
            // Then
            assertEquals(
                ProjectSaving(fromProfile, "Saia da formatura", showsSaved = true),
                scenario.viewModel.state.value.saving,
            )
        }
    }

    @Test
    fun recreate_withRenameDialogOpen_renamesToTheTypedName() = runTest {
        // Given
        recreatedAfter { projects ->
            projects.startRenaming(saved)
            projects.editName("Saia da formatura")
        }.use { scenario ->
            // When
            scenario.viewModel.rename(shown::add)

            // Then
            assertEquals(ProjectName("Saia da formatura"), storage.current.single().name)
        }
    }

    @Test
    fun recreate_withDeleteConfirmationOpen_deletesOnConfirmation() = runTest {
        // Given
        recreatedAfter { projects -> projects.startDeleting(saved) }.use { scenario ->
            // When
            scenario.viewModel.delete(shown::add)

            // Then
            assertEquals(emptyList(), storage.current)
        }
    }

    @Test
    fun recreate_duringAStorageCall_offersTheDialogAgain() = runTest {
        // Given
        // A command that never finishes, as when the process dies during one.
        storage.commandGate = CompletableDeferred()

        // When
        recreatedAfter { projects ->
            projects.startDeleting(saved)
            projects.delete(shown::add)
        }.use { scenario ->
            // Then
            val deleting = assertNotNull(scenario.viewModel.state.value.deleting)
            assertFalse(deleting.isBusy)
        }
    }

    /** A view model that went through [actions], then was recreated after process death. */
    private fun recreatedAfter(actions: (ProjectsViewModel) -> Unit) = viewModelScenario {
        ProjectsViewModel(storage, October18Clock, createSavedStateHandle())
    }.also {
        actions(it.viewModel)
        it.recreate()
    }

    private fun projectsViewModel() = ProjectsViewModel(storage, October18Clock, SavedStateHandle())
}

private val fromProfile = SkirtSnapshot(
    SkirtDraft(Length(80.0), Length(2.0), Length(60.0), Length(1.0), Length(2.0)),
    SkirtRadii(Length(13.05), Length(73.05), Length(12.05), Length(75.05)),
    MeasurementSource("Ana", LocalDate.of(2026, 10, 12)),
)

private val saved = Project(
    ProjectId(1),
    ProjectName("Saia antiga"),
    PatternDraftKind.FullCircleSkirt,
    fromProfile.copy(source = null),
    createdAt = Instant.parse("2026-09-01T10:00:00Z"),
    updatedAt = Instant.parse("2026-09-01T10:00:00Z"),
)
