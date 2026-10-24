package com.almeidajr.molda.project

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.data.LoadResult
import com.almeidajr.molda.data.asLoadResult
import com.almeidajr.molda.data.room.openMoldaDatabase
import com.almeidajr.molda.domain.NoLongerExistsException
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.measurement.MeasurementSource
import com.almeidajr.molda.project.data.LocalProjectRepository
import com.almeidajr.molda.project.data.ProjectEntity
import com.almeidajr.molda.project.data.SkirtColumns
import com.almeidajr.molda.project.domain.PatternDraftKind
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.withProjects
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Projects on real Room: each command, reopened from disk. */
class ProjectStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "projects-test.db"

    @Before
    @After
    fun deleteDatabase() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun create_thenReopen_listsEveryFieldMostRecentFirst() = runTest {
        // Given
        val saved = withProjects(context, databaseName) { storage ->
            listOf(
                storage.create(ProjectName("Saia de Ana"), fromProfile, january).getOrThrow(),
                storage.create(ProjectName("Saia manual"), typedByHand, february).getOrThrow(),
            )
        }

        // When
        val listed = withProjects(context, databaseName) { it.projects().first() }

        // Then
        assertEquals(saved.reversed(), listed)
        assertEquals(PatternDraftKind.FullCircleSkirt, listed.last().snapshot.kind)
        assertEquals(fromProfile, listed.last().snapshot)
        assertEquals(typedByHand, listed.first().snapshot)
        assertEquals(january, listed.last().createdAt)
        assertEquals(january, listed.last().updatedAt)
    }

    @Test
    fun create_whenStorageFails_reportsFailureAndKeepsOtherProjects() = runTest {
        // Given
        val kept = withProjects(context, databaseName) { storage ->
            storage.create(ProjectName("Saia de Ana"), fromProfile, january).getOrThrow()
        }
        SQLiteDatabase.openDatabase(
            context.getDatabasePath(databaseName).path,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        ).use { db ->
            // Rejects every new row, as a full or read-only disk would.
            db.execSQL(
                "CREATE TRIGGER refuse_projects BEFORE INSERT ON projects " +
                    "BEGIN SELECT RAISE(ABORT, 'refused'); END",
            )
        }

        withProjects(context, databaseName) { storage ->
            // When
            val created = storage.create(ProjectName("Outra"), typedByHand, february)

            // Then
            assertTrue(created.isFailure)
            assertEquals(listOf(kept), storage.projects().first())
        }
    }

    @Test
    fun rename_storesTheNameAndListsItAsTheLatestChange() = runTest {
        // Given
        val (first, second) = withProjects(context, databaseName) { storage ->
            storage.create(ProjectName("Saia de Ana"), fromProfile, january).getOrThrow() to
                storage.create(ProjectName("Saia manual"), typedByHand, february).getOrThrow()
        }

        // When
        withProjects(context, databaseName) { storage ->
            storage.rename(first.id, ProjectName("Saia da formatura"), march).getOrThrow()
        }

        // Then
        val listed = withProjects(context, databaseName) { it.projects().first() }
        assertEquals(
            listOf(first.copy(name = ProjectName("Saia da formatura"), updatedAt = march), second),
            listed,
        )
    }

    @Test
    fun update_replacesTheSnapshotInPlaceAsTheLatestChange() = runTest {
        // Given
        val (first, second) = withProjects(context, databaseName) { storage ->
            storage.create(ProjectName("Saia de Ana"), fromProfile, january).getOrThrow() to
                storage.create(ProjectName("Saia manual"), typedByHand, february).getOrThrow()
        }

        // When
        withProjects(context, databaseName) { storage ->
            storage.update(first.id, typedByHand, march).getOrThrow()
        }

        // Then
        val listed = withProjects(context, databaseName) { it.projects().first() }
        assertEquals(
            listOf(first.copy(snapshot = typedByHand, updatedAt = march), second),
            listed,
        )
    }

    @Test
    fun delete_removesOnlyThatProject() = runTest {
        // Given
        val (first, second) = withProjects(context, databaseName) { storage ->
            storage.create(ProjectName("Saia de Ana"), fromProfile, january).getOrThrow() to
                storage.create(ProjectName("Saia manual"), typedByHand, february).getOrThrow()
        }

        // When
        withProjects(context, databaseName) { storage -> storage.delete(second.id).getOrThrow() }

        // Then
        val listed = withProjects(context, databaseName) { it.projects().first() }
        assertEquals(listOf(first), listed)
    }

    @Test
    fun commands_onAMissingProject_failAsNoLongerExisting() = runTest {
        withProjects(context, databaseName) { storage ->
            // Given
            val gone = storage.create(ProjectName("Saia"), typedByHand, january).getOrThrow()
            storage.delete(gone.id).getOrThrow()

            // When/Then
            assertIs<NoLongerExistsException>(
                storage.rename(gone.id, ProjectName("Outra"), march).exceptionOrNull(),
            )
            assertIs<NoLongerExistsException>(
                storage.update(gone.id, fromProfile, march).exceptionOrNull(),
            )
            assertIs<NoLongerExistsException>(storage.delete(gone.id).exceptionOrNull())
        }
    }

    @Test
    fun project_storedOrMissing_loadsItOrFailsAsNoLongerExisting() = runTest {
        withProjects(context, databaseName) { storage ->
            // Given
            val kept = storage.create(ProjectName("Saia"), typedByHand, january).getOrThrow()
            val gone = storage.create(ProjectName("Outra"), fromProfile, march).getOrThrow()
            storage.delete(gone.id).getOrThrow()

            // When/Then
            assertEquals(kept, storage.project(kept.id).first())
            assertFailsWith<NoLongerExistsException> { storage.project(gone.id).first() }
        }
    }

    @Test
    fun projects_withAnUnknownPatternKind_failAsStorageExpects() = runTest {
        // Given
        val database = openMoldaDatabase(context, databaseName)
        val loads = try {
            database.projects().insert(
                ProjectEntity(
                    name = "Saia de uma versão nova",
                    patternKind = "origami_crane",
                    skirt = SkirtColumns(
                        waist = 80.5,
                        ease = 2.0,
                        finishedLength = 60.0,
                        waistSeamAllowance = 1.0,
                        hemAllowance = 2.5,
                        waistStitchingRadius = 13.13,
                        finishedHemRadius = 73.13,
                        waistCuttingRadius = 12.13,
                        hemCuttingRadius = 75.63,
                        profileName = null,
                        measurementDate = null,
                    ),
                    createdAt = january.toEpochMilli(),
                    updatedAt = january.toEpochMilli(),
                ),
            )

            // When
            LocalProjectRepository(database.projects()).projects().asLoadResult().toList()
        } finally {
            database.close()
        }

        // Then
        assertEquals(LoadResult.Loading, loads.first())
        val failure = assertIs<LoadResult.Error>(loads.last()).failure
        assertIs<IllegalStateException>(failure)
        assertTrue("origami_crane" in failure.message.orEmpty())
        assertEquals(2, loads.size)
    }

    private companion object {
        val january: Instant = Instant.parse("2026-01-10T09:00:00Z")
        val february: Instant = Instant.parse("2026-02-10T09:00:00Z")
        val march: Instant = Instant.parse("2026-03-10T09:00:00Z")

        val fromProfile = SkirtSnapshot(
            SkirtDraft(
                waist = Length(80.5),
                ease = Length(2.0),
                finishedLength = Length(60.0),
                waistSeamAllowance = Length(1.0),
                hemAllowance = Length(2.5),
            ),
            SkirtRadii(Length(13.13), Length(73.13), Length(12.13), Length(75.63)),
            MeasurementSource("Ana", LocalDate.of(2026, 1, 3)),
        )
        val typedByHand = SkirtSnapshot(
            SkirtDraft(Length(70.0), Length(0.0), Length(45.0), Length(0.0), Length(0.0)),
            SkirtRadii(Length(11.14), Length(56.14), Length(11.14), Length(56.14)),
            source = null,
        )
    }
}
