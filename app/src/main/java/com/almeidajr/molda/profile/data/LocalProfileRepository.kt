package com.almeidajr.molda.profile.data

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.profile.domain.ProfileRepository
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalProfileRepository(
    context: Context,
    databaseName: String = "profiles.db",
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ProfileRepository,
    AutoCloseable {
    private val database = ProfileDatabase(context.applicationContext, databaseName)

    override suspend fun list(): List<MeasurementProfile> = withContext(ioDispatcher) {
        database.readableDatabase.query(
            "profiles",
            arrayOf("id", "name", "waist", "measurement_date", "notes"),
            null,
            null,
            null,
            null,
            "id ASC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        MeasurementProfile(
                            ProfileId(cursor.getLong(cursor.getColumnIndexOrThrow("id"))),
                            ProfileMeasurements(
                                ProfileName(cursor.getString(cursor.getColumnIndexOrThrow("name"))),
                                Length(cursor.getDouble(cursor.getColumnIndexOrThrow("waist"))),
                                LocalDate.parse(
                                    cursor.getString(
                                        cursor.getColumnIndexOrThrow("measurement_date"),
                                    ),
                                ),
                                cursor.getString(cursor.getColumnIndexOrThrow("notes")),
                            ),
                        ),
                    )
                }
            }
        }
    }

    override suspend fun create(measurements: ProfileMeasurements): MeasurementProfile =
        withContext(ioDispatcher) {
            MeasurementProfile(
                ProfileId(
                    database.writableDatabase.insertOrThrow(
                        "profiles",
                        null,
                        measurements.toValues(),
                    ),
                ),
                measurements,
            )
        }

    override suspend fun update(profile: MeasurementProfile): Unit = withContext(ioDispatcher) {
        check(
            database.writableDatabase.update(
                "profiles",
                profile.measurements.toValues(),
                "id = ?",
                arrayOf(profile.id.value.toString()),
            ) == 1,
        ) { "Profile no longer exists" }
    }

    override suspend fun delete(id: ProfileId): Unit = withContext(ioDispatcher) {
        check(
            database.writableDatabase.delete("profiles", "id = ?", arrayOf(id.value.toString())) ==
                1,
        ) { "Profile no longer exists" }
    }

    override fun close() = database.close()
}

private fun ProfileMeasurements.toValues(): ContentValues = ContentValues().apply {
    put("name", name.text)
    put("waist", waist.centimetres)
    put("measurement_date", measurementDate.toString())
    put("notes", notes)
}

private class ProfileDatabase(context: Context, name: String) :
    SQLiteOpenHelper(
        context,
        name,
        null,
        1,
        // Default corruption handling deletes records; preserve them and report the failure instead.
        DatabaseErrorHandler { throw SQLiteException("Profile database is corrupt") },
    ) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE profiles (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL CHECK(length(trim(name)) > 0), " +
                "waist REAL NOT NULL CHECK(waist > 0), measurement_date TEXT NOT NULL, " +
                "notes TEXT NOT NULL)",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int): Unit =
        throw SQLiteException("Unsupported profile database version")
}
