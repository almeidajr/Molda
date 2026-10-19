package com.almeidajr.molda.data.room

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.almeidajr.molda.profile.data.ProfileDao
import com.almeidajr.molda.profile.data.ProfileEntity
import com.almeidajr.molda.project.data.ProjectDao
import com.almeidajr.molda.project.data.ProjectEntity

// Every schema change after version 1 ships a Migration (docs/adr/0002).
@Database(entities = [ProfileEntity::class, ProjectEntity::class], version = 2)
abstract class MoldaDatabase : RoomDatabase() {
    abstract fun profiles(): ProfileDao
    abstract fun projects(): ProjectDao
}

/** Version 2 adds the projects table; profiles are untouched. */
val AddProjects = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `projects` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                "`waist` REAL NOT NULL, `ease` REAL NOT NULL, `finished_length` REAL NOT NULL, " +
                "`waist_seam_allowance` REAL NOT NULL, `hem_allowance` REAL NOT NULL, " +
                "`waist_stitching_radius` REAL NOT NULL, `finished_hem_radius` REAL NOT NULL, " +
                "`waist_cutting_radius` REAL NOT NULL, `hem_cutting_radius` REAL NOT NULL, " +
                "`profile_name` TEXT, `measurement_date` TEXT, " +
                "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)",
        )
    }
}

/** Opens Molda's database; the old `profiles.db` is deliberately not read (docs/adr/0002). */
fun openMoldaDatabase(context: Context, name: String = "molda.db"): MoldaDatabase = Room
    .databaseBuilder<MoldaDatabase>(context.applicationContext, name)
    .setDriver(AndroidSQLiteDriver())
    .addMigrations(AddProjects)
    .build()
