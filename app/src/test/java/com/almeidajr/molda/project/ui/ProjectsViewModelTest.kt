package com.almeidajr.molda.project.ui

import com.almeidajr.molda.R
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.project.domain.MeasurementSource
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProjectsViewModelTest {
    private val storage = FakeProjects()

    // Runs viewModelScope work eagerly, so each call has settled before the assertions.
    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun useTestMainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun resetMainDispatcher() = Dispatchers.resetMain()

    @Test
    fun projects_followTheRepository() = runTest {
        val projects = projectsViewModel()
        assertEquals(listOf(saved), projects.state.value.projects)

        storage.stored.update { emptyList() }

        assertEquals(emptyList<Project>(), projects.state.value.projects)
    }

    @Test
    fun load_afterLoadFailure_retryShowsStoredProjects() = runTest {
        storage.failsLoad = true
        val projects = projectsViewModel()
        assertEquals(R.string.project_load_failed, projects.state.value.loadError)

        storage.failsLoad = false
        projects.load()

        assertNull(projects.state.value.loadError)
        assertEquals(listOf(saved), projects.state.value.projects)
    }

    @Test
    fun startSaving_fromProfile_offersPatternProfileAndTodayAsName() = runTest {
        val projects = projectsViewModel()

        projects.startSaving(fromProfile, patternName = "Saia godê")

        assertEquals("Saia godê · Ana · 18/10/2026", projects.state.value.saving?.name)
    }

    @Test
    fun startSaving_typedByHand_offersNameWithoutProfile() = runTest {
        val projects = projectsViewModel()

        projects.startSaving(fromProfile.copy(source = null), patternName = "Saia godê")

        assertEquals("Saia godê · 18/10/2026", projects.state.value.saving?.name)
    }

    @Test
    fun save_editedName_storesSnapshotNewestFirstAndCloses() = runTest {
        val projects = projectsViewModel()

        projects.startSaving(fromProfile, patternName = "Saia godê")
        projects.editName("  Saia da formatura ")
        projects.save()

        val state = projects.state.value
        val created = state.projects.first()
        assertEquals(ProjectName("Saia da formatura"), created.name)
        assertEquals(fromProfile, created.snapshot)
        assertEquals(clock.instant(), created.updatedAt)
        assertEquals(listOf(created, saved), state.projects)
        assertNull(state.saving)
        assertEquals(R.string.project_saved, state.savedMessage)
    }

    @Test
    fun save_blankName_isRefusedWithoutStoring() = runTest {
        val projects = projectsViewModel()

        projects.startSaving(fromProfile, patternName = "Saia godê")
        projects.editName("   ")
        projects.save()

        val saving = projects.state.value.saving
        assertTrue(saving?.hasBlankName == true)
        assertEquals(listOf(saved), projects.state.value.projects)

        projects.editName("Saia")
        assertFalse(projects.state.value.saving?.hasBlankName == true)
    }

    @Test
    fun save_whenStorageFails_keepsTheNameAndShowsTheFailure() = runTest {
        storage.failsCommands = true
        val projects = projectsViewModel()

        projects.startSaving(fromProfile, patternName = "Saia godê")
        projects.editName("Saia da formatura")
        projects.save()

        val saving = projects.state.value.saving
        assertEquals("Saia da formatura", saving?.name)
        assertEquals(fromProfile, saving?.snapshot)
        assertEquals(R.string.project_save_failed, saving?.saveError)
        assertFalse(saving?.isSaving == true)
        assertEquals(listOf(saved), projects.state.value.projects)
    }

    @Test
    fun cancelSaving_closesWithoutStoring() = runTest {
        val projects = projectsViewModel()

        projects.startSaving(fromProfile, patternName = "Saia godê")
        projects.cancel()

        assertNull(projects.state.value.saving)
        assertEquals(listOf(saved), projects.state.value.projects)
    }

    @Test
    fun update_replacesTheSnapshotInPlace_thenShowsTheProject() = runTest {
        val projects = projectsViewModel()

        projects.update(saved.id, fromProfile)

        val state = projects.state.value
        assertEquals(
            listOf(saved.copy(snapshot = fromProfile, updatedAt = clock.instant())),
            state.projects,
        )
        assertEquals(R.string.project_updated, state.savedMessage)
        assertEquals(saved.id, state.projectToShow)
        assertFalse(state.isUpdating)
    }

    @Test
    fun update_whenStorageFails_keepsTheProjectAndSaysSo() = runTest {
        storage.failsCommands = true
        val projects = projectsViewModel()

        projects.update(saved.id, fromProfile)

        val state = projects.state.value
        assertEquals(listOf(saved), state.projects)
        assertEquals(R.string.project_update_failed, state.savedMessage)
        assertNull(state.projectToShow)
        assertFalse(state.isUpdating)
    }

    @Test
    fun saveAsNew_keepsTheOriginal_thenShowsTheNewProject() = runTest {
        val projects = projectsViewModel()

        projects.startSaving(fromProfile, patternName = "Saia godê", showsSaved = true)
        projects.save()

        val state = projects.state.value
        val created = state.projects.first()
        assertEquals(listOf(created, saved), state.projects)
        assertEquals(fromProfile, created.snapshot)
        assertEquals(created.id, state.projectToShow)

        projects.onProjectShown()
        assertNull(projects.state.value.projectToShow)
    }

    @Test
    fun save_freshCalculation_showsNoProject() = runTest {
        val projects = projectsViewModel()

        projects.startSaving(fromProfile, patternName = "Saia godê")
        projects.save()

        assertNull(projects.state.value.projectToShow)
    }

    @Test
    fun rename_trimmedName_updatesTheListImmediately() = runTest {
        val projects = projectsViewModel()

        projects.startRenaming(saved)
        assertEquals("Saia antiga", projects.state.value.renaming?.name)
        projects.editName("  Saia da formatura ")
        projects.rename()

        val state = projects.state.value
        assertEquals(
            listOf(
                saved.copy(name = ProjectName("Saia da formatura"), updatedAt = clock.instant()),
            ),
            state.projects,
        )
        assertNull(state.renaming)
        assertEquals(R.string.project_renamed, state.savedMessage)
    }

    @Test
    fun rename_blankName_isRefusedWithoutStoring() = runTest {
        val projects = projectsViewModel()

        projects.startRenaming(saved)
        projects.editName("   ")
        projects.rename()

        assertTrue(projects.state.value.renaming?.hasBlankName == true)
        assertEquals(listOf(saved), projects.state.value.projects)
        projects.editName("Saia")
        assertFalse(projects.state.value.renaming?.hasBlankName == true)
    }

    @Test
    fun rename_whenStorageFails_keepsTheDialogAndShowsTheFailure() = runTest {
        storage.failsCommands = true
        val projects = projectsViewModel()

        projects.startRenaming(saved)
        projects.editName("Saia da formatura")
        projects.rename()

        val renaming = projects.state.value.renaming
        assertEquals("Saia da formatura", renaming?.name)
        assertEquals(R.string.project_rename_failed, renaming?.error)
        assertFalse(renaming?.isBusy == true)
        assertEquals(listOf(saved), projects.state.value.projects)
    }

    @Test
    fun delete_afterConfirmation_removesTheProject() = runTest {
        val projects = projectsViewModel()

        projects.startDeleting(saved)
        assertEquals(listOf(saved), projects.state.value.projects)
        projects.delete()

        val state = projects.state.value
        assertEquals(emptyList<Project>(), state.projects)
        assertNull(state.deleting)
        assertEquals(R.string.project_deleted, state.savedMessage)
    }

    @Test
    fun delete_whenStorageFails_keepsTheProjectAndShowsTheFailure() = runTest {
        storage.failsCommands = true
        val projects = projectsViewModel()

        projects.startDeleting(saved)
        projects.delete()

        val deleting = projects.state.value.deleting
        assertEquals(saved, deleting?.project)
        assertEquals(R.string.project_delete_failed, deleting?.error)
        assertFalse(deleting?.isBusy == true)
        assertEquals(listOf(saved), projects.state.value.projects)
    }

    @Test
    fun cancelDeleting_keepsTheProject() = runTest {
        val projects = projectsViewModel()

        projects.startDeleting(saved)
        projects.cancel()

        assertNull(projects.state.value.deleting)
        assertEquals(listOf(saved), projects.state.value.projects)
    }

    private fun projectsViewModel() = ProjectsViewModel(storage, clock)

    private val clock = Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC)
}

private val fromProfile = SkirtSnapshot(
    SkirtDraft(Length(80.0), Length(2.0), Length(60.0), Length(1.0), Length(2.0)),
    SkirtRadii(Length(13.05), Length(73.05), Length(12.05), Length(75.05)),
    MeasurementSource("Ana", LocalDate.of(2026, 10, 12)),
)

private val saved = Project(
    ProjectId(1),
    ProjectName("Saia antiga"),
    fromProfile.copy(source = null),
    createdAt = Instant.parse("2026-09-01T10:00:00Z"),
    updatedAt = Instant.parse("2026-09-01T10:00:00Z"),
)

/** In-memory projects that emit again after each change, most recent first like Room. */
private class FakeProjects : ProjectRepository {
    val stored = MutableStateFlow(listOf(saved))
    var failsLoad = false
    var failsCommands = false

    override fun projects(): Flow<List<Project>> = flow {
        check(!failsLoad) { "Controlled load failure" }
        emitAll(stored)
    }

    override suspend fun create(
        name: ProjectName,
        snapshot: SkirtSnapshot,
        savedAt: Instant,
    ): Result<Project> = if (failsCommands) {
        Result.failure(IllegalStateException("Controlled storage failure"))
    } else {
        val created = Project(ProjectId(stored.value.size + 1L), name, snapshot, savedAt, savedAt)
        stored.update { listOf(created) + it }
        Result.success(created)
    }

    override suspend fun update(id: ProjectId, snapshot: SkirtSnapshot, updatedAt: Instant) =
        command { projects ->
            val updated = projects.single { it.id == id }
                .copy(snapshot = snapshot, updatedAt = updatedAt)
            listOf(updated) + projects.filter { it.id != id }
        }

    override suspend fun rename(id: ProjectId, name: ProjectName, renamedAt: Instant) =
        command { projects ->
            val renamed = projects.single { it.id == id }.copy(name = name, updatedAt = renamedAt)
            listOf(renamed) + projects.filter { it.id != id }
        }

    override suspend fun delete(id: ProjectId) = command { projects ->
        projects.filter { it.id != id }
    }

    private fun command(change: (List<Project>) -> List<Project>): Result<Unit> =
        if (failsCommands) {
            Result.failure(IllegalStateException("Controlled storage failure"))
        } else {
            Result.success(stored.update(change))
        }
}
