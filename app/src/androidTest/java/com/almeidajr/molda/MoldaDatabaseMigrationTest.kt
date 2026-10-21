package com.almeidajr.molda

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.data.room.MoldaDatabase
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MoldaDatabaseMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val databaseName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = instrumentation.targetContext.getDatabasePath(databaseName),
        driver = AndroidSQLiteDriver(),
        databaseClass = MoldaDatabase::class,
    )

    // createDatabase refuses an existing file, such as one left by an interrupted run.
    @Before
    @After
    fun deleteDatabase() {
        instrumentation.targetContext.deleteDatabase(databaseName)
    }

    @Test
    fun version1WithProfiles_opensKeepingProfilesAndNoProjects() = runTest {
        // Given
        helper.createDatabase(1).apply {
            execSQL(
                "INSERT INTO profiles (id, name, waist, measurement_date, notes) " +
                    "VALUES (7, 'Ana', 80.5, '2026-10-12', 'Medida no cós')",
            )
            close()
        }
        val context = instrumentation.targetContext

        // When/Then: the app's own builder must migrate and pass Room's schema validation.
        withProfiles(context, databaseName) { profiles ->
            assertEquals(
                listOf(
                    MeasurementProfile(
                        ProfileId(7),
                        ProfileMeasurements(
                            ProfileName("Ana"),
                            Length(80.5),
                            LocalDate.of(2026, 10, 12),
                            "Medida no cós",
                        ),
                    ),
                ),
                profiles.profiles().first(),
            )
        }
        withProjects(context, databaseName) {
            assertEquals(emptyList(), it.projects().first())
        }
    }
}
