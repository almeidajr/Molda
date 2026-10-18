package com.almeidajr.molda.profile

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.data.LocalProfileRepository
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileStorageTest {
    @Test
    fun updateAndDelete_reopenKeepsIdentityAndOtherProfilesWithDuplicateNames() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "profiles-mutations-test.db"
        context.deleteDatabase(databaseName)
        try {
            val original =
                ProfileMeasurements(
                    ProfileName("Ana"),
                    Length(80.5),
                    LocalDate.of(2026, 10, 12),
                    "Antes",
                )
            val profiles = LocalProfileRepository(context, databaseName).use { storage ->
                listOf(storage.create(original).getOrThrow(), storage.create(original).getOrThrow())
            }
            val edited = profiles[0].copy(
                measurements = ProfileMeasurements(
                    ProfileName("Ana atualizada"),
                    Length(81.123456789),
                    LocalDate.of(2026, 10, 13),
                    "Depois",
                ),
            )
            LocalProfileRepository(context, databaseName).use { storage ->
                storage.update(edited).getOrThrow()
            }
            LocalProfileRepository(context, databaseName).use { storage ->
                assertEquals(listOf(edited, profiles[1]), storage.profiles().first())
                storage.delete(edited.id).getOrThrow()
            }
            LocalProfileRepository(context, databaseName).use { storage ->
                assertEquals(listOf(profiles[1]), storage.profiles().first())
                assertTrue(storage.update(edited).isFailure)
                assertTrue(storage.delete(edited.id).isFailure)
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun malformedPersistedMeasurements_failWithoutResettingOtherProfiles() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "profiles-invalid-test.db"
        context.deleteDatabase(databaseName)
        try {
            val original = LocalProfileRepository(context, databaseName).use { storage ->
                storage.create(
                    ProfileMeasurements(
                        ProfileName("Ana"),
                        Length(80.5),
                        LocalDate.of(2026, 10, 12),
                    ),
                ).getOrThrow()
            }
            listOf(
                "UPDATE profiles SET waist = 1e999",
                "UPDATE profiles SET measurement_date = 'not-a-date'",
            ).forEach { corruption ->
                SQLiteDatabase.openDatabase(
                    context.getDatabasePath(databaseName).path,
                    null,
                    SQLiteDatabase.OPEN_READWRITE,
                ).use { db ->
                    db.execSQL(corruption)
                }
                LocalProfileRepository(context, databaseName).use { storage ->
                    var failed = false
                    try {
                        storage.profiles().first()
                    } catch (_: IllegalArgumentException) {
                        failed = true
                    } catch (_: DateTimeParseException) {
                        failed = true
                    }
                    assertTrue("Invalid persisted measurements must fail to load", failed)
                }
                SQLiteDatabase.openDatabase(
                    context.getDatabasePath(databaseName).path,
                    null,
                    SQLiteDatabase.OPEN_READWRITE,
                ).use { db ->
                    db.execSQL("UPDATE profiles SET waist = 80.5, measurement_date = '2026-10-12'")
                }
                LocalProfileRepository(context, databaseName).use { storage ->
                    assertEquals(listOf(original), storage.profiles().first())
                }
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun reopen_keepsAllFieldsAndDuplicateNamesDistinct() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "profiles-test.db"
        context.deleteDatabase(databaseName)
        try {
            val first =
                ProfileMeasurements(
                    ProfileName("Ana"),
                    Length(80.5),
                    LocalDate.of(2026, 10, 12),
                    "Medida no cós",
                )
            val second =
                ProfileMeasurements(ProfileName("Ana"), Length(92.25), LocalDate.of(2026, 9, 1), "")
            val identities = LocalProfileRepository(context, databaseName).use { storage ->
                listOf(storage.create(first).getOrThrow(), storage.create(second).getOrThrow())
            }
            assertNotEquals(identities[0].id, identities[1].id)
            LocalProfileRepository(context, databaseName).use { storage ->
                assertEquals(identities, storage.profiles().first())
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }
}
