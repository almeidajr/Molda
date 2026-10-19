package com.almeidajr.molda.project

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.measurement.MeasurementSource
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.withProjects
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProjectStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "projects-test.db"

    @Before
    @After
    fun deleteDatabase() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun create_reopenListsEveryFieldMostRecentFirst() = runBlocking {
        val saved = withProjects(context, databaseName) { storage ->
            listOf(
                storage.create(ProjectName("Saia de Ana"), fromProfile, january).getOrThrow(),
                storage.create(ProjectName("Saia manual"), typedByHand, february).getOrThrow(),
            )
        }

        val listed = withProjects(context, databaseName) { it.projects().first() }

        assertEquals(saved.reversed(), listed)
        assertEquals(fromProfile, listed.last().snapshot)
        assertEquals(typedByHand, listed.first().snapshot)
        assertEquals(january, listed.last().createdAt)
        assertEquals(january, listed.last().updatedAt)
    }

    @Test
    fun create_whenStorageFails_reportsFailureAndKeepsOtherProjects() = runBlocking {
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
            assertTrue(storage.create(ProjectName("Outra"), typedByHand, february).isFailure)
            assertEquals(listOf(kept), storage.projects().first())
        }
    }

    @Test
    fun rename_storesTheNameAndListsItAsTheLatestChange() = runBlocking {
        val (first, second) = withProjects(context, databaseName) { storage ->
            storage.create(ProjectName("Saia de Ana"), fromProfile, january).getOrThrow() to
                storage.create(ProjectName("Saia manual"), typedByHand, february).getOrThrow()
        }

        withProjects(context, databaseName) { storage ->
            assertTrue(storage.rename(first.id, ProjectName("Saia da formatura"), march).isSuccess)
        }

        val listed = withProjects(context, databaseName) { it.projects().first() }
        assertEquals(
            listOf(first.copy(name = ProjectName("Saia da formatura"), updatedAt = march), second),
            listed,
        )
    }

    @Test
    fun update_replacesTheSnapshotInPlaceAsTheLatestChange() = runBlocking {
        val (first, second) = withProjects(context, databaseName) { storage ->
            storage.create(ProjectName("Saia de Ana"), fromProfile, january).getOrThrow() to
                storage.create(ProjectName("Saia manual"), typedByHand, february).getOrThrow()
        }

        withProjects(context, databaseName) { storage ->
            assertTrue(storage.update(first.id, typedByHand, march).isSuccess)
        }

        val listed = withProjects(context, databaseName) { it.projects().first() }
        assertEquals(
            listOf(first.copy(snapshot = typedByHand, updatedAt = march), second),
            listed,
        )
    }

    @Test
    fun delete_removesOnlyThatProject() = runBlocking {
        val (first, second) = withProjects(context, databaseName) { storage ->
            storage.create(ProjectName("Saia de Ana"), fromProfile, january).getOrThrow() to
                storage.create(ProjectName("Saia manual"), typedByHand, february).getOrThrow()
        }

        withProjects(context, databaseName) { storage ->
            assertTrue(storage.delete(second.id).isSuccess)
            assertEquals(listOf(first), storage.projects().first())
        }
    }

    @Test
    fun renameAndDelete_ofAMissingProject_reportFailure() = runBlocking {
        withProjects(context, databaseName) { storage ->
            val gone = storage.create(ProjectName("Saia"), typedByHand, january).getOrThrow()
            storage.delete(gone.id).getOrThrow()

            assertTrue(storage.rename(gone.id, ProjectName("Outra"), march).isFailure)
            assertTrue(storage.update(gone.id, fromProfile, march).isFailure)
            assertTrue(storage.delete(gone.id).isFailure)
        }
    }

    private companion object {
        val march: Instant = Instant.parse("2026-03-10T09:00:00Z")

        val january: Instant = Instant.parse("2026-01-10T09:00:00Z")
        val february: Instant = Instant.parse("2026-02-10T09:00:00Z")

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
