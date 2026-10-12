package com.almeidajr.molda.profile.data

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileRepository
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalProfileRepository(context: Context, databaseName: String = "profiles.db") :
    ProfileRepository,
    AutoCloseable {
    private val database = ProfileDatabase(context.applicationContext, databaseName)

    override suspend fun list(): List<MeasurementProfile> = withContext(Dispatchers.IO) {
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
                            cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                            ProfileMeasurements(
                                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                                cursor.getDouble(cursor.getColumnIndexOrThrow("waist")),
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
        withContext(Dispatchers.IO) {
            val checked = measurements.copy()
            val values = ContentValues().apply {
                put("name", checked.name)
                put("waist", checked.waist)
                put("measurement_date", checked.measurementDate.toString())
                put("notes", checked.notes)
            }
            MeasurementProfile(
                database.writableDatabase.insertOrThrow("profiles", null, values),
                checked,
            )
        }

    override fun close() = database.close()
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
