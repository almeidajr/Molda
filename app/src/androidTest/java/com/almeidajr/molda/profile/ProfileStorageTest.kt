package com.almeidajr.molda.profile

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.withProfiles
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class ProfileStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "profile-storage-test.db"

    private val ana = ProfileMeasurements(
        ProfileName("Ana"),
        Length(80.5),
        LocalDate.of(2026, 10, 12),
        "Medida no cós",
    )

    @Before
    @After
    fun deleteDatabase() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun create_duplicateNames_keepsEveryFieldAndDistinctIdsAfterReopen() = runTest {
        // Given
        val other = ProfileMeasurements(ProfileName("Ana"), Length(92.25), LocalDate.of(2026, 9, 1))

        // When
        val created = withProfiles(context, databaseName) { storage ->
            listOf(storage.create(ana).getOrThrow(), storage.create(other).getOrThrow())
        }

        // Then
        assertNotEquals(created[0].id, created[1].id)
        assertEquals(created, withProfiles(context, databaseName) { it.profiles().first() })
    }

    @Test
    fun update_oneOfDuplicateNames_changesOnlyItAfterReopen() = runTest {
        // Given
        val (first, second) = createTwoAnas()
        val edited = first.copy(
            measurements = ProfileMeasurements(
                ProfileName("Ana atualizada"),
                Length(81.123456789),
                LocalDate.of(2026, 10, 13),
                "Depois",
            ),
        )

        // When
        withProfiles(context, databaseName) { it.update(edited).getOrThrow() }

        // Then
        assertEquals(
            listOf(edited, second),
            withProfiles(context, databaseName) { it.profiles().first() },
        )
    }

    @Test
    fun delete_oneOfDuplicateNames_removesOnlyItAfterReopen() = runTest {
        // Given
        val (first, second) = createTwoAnas()

        // When
        withProfiles(context, databaseName) { it.delete(first.id).getOrThrow() }

        // Then
        assertEquals(listOf(second), withProfiles(context, databaseName) { it.profiles().first() })
    }

    @Test
    fun updateAndDelete_ofMissingProfile_fail() = runTest {
        // Given
        val (first) = createTwoAnas()
        withProfiles(context, databaseName) { it.delete(first.id).getOrThrow() }

        // When/Then
        withProfiles(context, databaseName) { storage ->
            assertTrue(storage.update(first).isFailure, "update")
            assertTrue(storage.delete(first.id).isFailure, "delete")
        }
    }

    @Test
    fun profiles_withMalformedStoredMeasurements_failWithoutLosingRows() = runTest {
        // Given
        val stored = withProfiles(context, databaseName) { it.create(ana).getOrThrow() }
        val corruptions = listOf(
            "UPDATE profiles SET waist = 1e999",
            "UPDATE profiles SET measurement_date = 'not-a-date'",
        )

        // When/Then
        corruptions.forEach { corruption ->
            execSql(corruption)
            val failure = withProfiles(context, databaseName) { storage ->
                runCatching { storage.profiles().first() }.exceptionOrNull()
            }
            assertTrue(
                failure is IllegalArgumentException || failure is DateTimeParseException,
                "$corruption must fail to load, failed with $failure",
            )
            execSql("UPDATE profiles SET waist = 80.5, measurement_date = '2026-10-12'")
            assertEquals(
                listOf(stored),
                withProfiles(context, databaseName) { it.profiles().first() },
                corruption,
            )
        }
    }

    private suspend fun createTwoAnas(): List<MeasurementProfile> =
        withProfiles(context, databaseName) { storage ->
            listOf(storage.create(ana).getOrThrow(), storage.create(ana).getOrThrow())
        }

    private fun execSql(sql: String) = SQLiteDatabase.openDatabase(
        context.getDatabasePath(databaseName).path,
        null,
        SQLiteDatabase.OPEN_READWRITE,
    ).use { it.execSQL(sql) }
}
