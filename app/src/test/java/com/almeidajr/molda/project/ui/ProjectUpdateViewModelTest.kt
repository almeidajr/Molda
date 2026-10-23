package com.almeidajr.molda.project.ui

import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.measurement.MeasurementSource
import com.almeidajr.molda.project.domain.PatternDraftKind
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import com.almeidajr.molda.testing.October18Clock
import java.time.Instant
import java.time.LocalDate
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
