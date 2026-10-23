package com.almeidajr.molda.project.ui

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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

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
    fun defaultName_fromProfile_offersKindProfileAndToday() = runTest {
        // Given
        val projects = projectsViewModel()

        // When/Then
        assertEquals(
            "Saia godê · Ana · 18/10/2026",
            projects.defaultName(fromProfile, kindLabel = "Saia godê"),
        )
    }

    @Test
    fun save_withName_storesSnapshotUnderThatNameAndReportsIt() = runTest {
        // Given
        val projects = projectsViewModel()
        val reported = mutableListOf<ProjectId>()

        // When
        projects.save(ProjectName("Saia da formatura"), fromProfile, reported::add)

        // Then
        val created = storage.current.single { it.id != saved.id }
        assertEquals(ProjectName("Saia da formatura"), created.name)
        assertEquals(fromProfile, created.snapshot)
        assertEquals(October18Clock.instant(), created.createdAt)
        assertTrue(saved in storage.current)
        assertEquals(listOf(created.id), reported)
        assertNull(projects.state.value.saving)
    }

    @Test
    fun save_whenStorageFails_showsTheFailureAndReportsNothing() = runTest {
        // Given
        storage.failsCommands = true
        val projects = projectsViewModel()
        val reported = mutableListOf<ProjectId>()

        // When
        projects.save(ProjectName("Saia da formatura"), fromProfile, reported::add)

        // Then
        assertEquals(
            ProjectSaving(error = R.string.project_save_failed),
            projects.state.value.saving,
        )
        assertEquals(emptyList(), reported)
        assertEquals(listOf(saved), storage.current)
    }

    @Test
    fun cancelSaving_afterAFailure_forgetsTheFailure() = runTest {
        // Given
        storage.failsCommands = true
        val projects = projectsViewModel()
        projects.save(ProjectName("Saia"), fromProfile) {}

        // When
        projects.cancelSaving()

        // Then
        assertNull(projects.state.value.saving)
    }

    @Test
    fun cancelSaving_duringTheStorageCall_keepsSaving() = runTest {
        // Given
        storage.commandGate = CompletableDeferred()
        val projects = projectsViewModel()
        projects.save(ProjectName("Saia"), fromProfile) {}

        // When
        projects.cancelSaving()

        // Then
        assertEquals(ProjectSaving(isBusy = true), projects.state.value.saving)
    }

    @Test
    fun rename_storesTheNameAndReportsIt() = runTest {
        // Given
        val projects = projectsViewModel()

        // When
        projects.rename(saved.id, ProjectName("Saia da formatura"), shown::add)

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
    fun rename_whenStorageFails_keepsTheProjectAndShowsTheFailure() = runTest {
        // Given
        storage.failsCommands = true
        val projects = projectsViewModel()

        // When
        projects.rename(saved.id, ProjectName("Saia da formatura"), shown::add)

        // Then
        assertEquals(
            ProjectRenaming(saved.id, error = R.string.project_rename_failed),
            projects.state.value.renaming,
        )
        assertEquals(listOf(saved), storage.current)
        assertEquals(emptyList(), shown)
    }

    @Test
    fun cancelRenaming_afterAFailure_forgetsTheFailure() = runTest {
        // Given
        storage.failsCommands = true
        val projects = projectsViewModel()
        projects.rename(saved.id, ProjectName("Saia da formatura"), shown::add)

        // When
        projects.cancelRenaming()

        // Then
        assertNull(projects.state.value.renaming)
        assertEquals(listOf(saved), storage.current)
    }

    @Test
    fun cancelRenaming_duringTheStorageCall_keepsRenaming() = runTest {
        // Given
        storage.commandGate = CompletableDeferred()
        val projects = projectsViewModel()
        projects.rename(saved.id, ProjectName("Saia da formatura"), shown::add)

        // When
        projects.cancelRenaming()

        // Then
        assertEquals(ProjectRenaming(saved.id, isBusy = true), projects.state.value.renaming)
    }

    @Test
    fun delete_removesTheProjectAndReportsIt() = runTest {
        // Given
        val projects = projectsViewModel()

        // When
        projects.delete(saved.id, shown::add)

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

        // When
        projects.delete(saved.id, shown::add)

        // Then
        assertEquals(
            ProjectDeleting(saved.id, error = R.string.project_delete_failed),
            projects.state.value.deleting,
        )
        assertEquals(listOf(saved), storage.current)
        assertEquals(emptyList(), shown)
    }

    @Test
    fun cancelDeleting_afterAFailure_forgetsTheFailure() = runTest {
        // Given
        storage.failsCommands = true
        val projects = projectsViewModel()
        projects.delete(saved.id, shown::add)

        // When
        projects.cancelDeleting()

        // Then
        assertNull(projects.state.value.deleting)
        assertEquals(listOf(saved), storage.current)
    }

    @Test
    fun cancelDeleting_duringTheStorageCall_keepsDeleting() = runTest {
        // Given
        storage.commandGate = CompletableDeferred()
        val projects = projectsViewModel()
        projects.delete(saved.id, shown::add)

        // When
        projects.cancelDeleting()

        // Then
        assertEquals(ProjectDeleting(saved.id, isBusy = true), projects.state.value.deleting)
    }

    private fun projectsViewModel() = ProjectsViewModel(storage, October18Clock)
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
