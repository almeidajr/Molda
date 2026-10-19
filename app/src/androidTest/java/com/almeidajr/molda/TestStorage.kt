package com.almeidajr.molda

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.data.room.MoldaDatabase
import com.almeidajr.molda.data.room.openMoldaDatabase
import com.almeidajr.molda.profile.data.LocalProfileRepository
import com.almeidajr.molda.project.data.LocalProjectRepository

/** A fresh Molda database in the file [name], for one test's isolated storage. */
internal class IsolatedStorage(private val name: String) {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    val database: MoldaDatabase = openMoldaDatabase(context.also { it.deleteDatabase(name) }, name)

    /** Closes [scenario] first, so the app stops using the database, then deletes the file. */
    fun remove(scenario: ActivityScenario<*>) {
        scenario.close()
        database.close()
        context.deleteDatabase(name)
    }
}

/** Runs [block] on profiles in the database file [name], on a connection closed afterwards. */
internal inline fun <T> withProfiles(
    context: Context,
    name: String,
    block: (LocalProfileRepository) -> T,
): T {
    val database = openMoldaDatabase(context, name)
    return try {
        block(LocalProfileRepository(database.profiles()))
    } finally {
        database.close()
    }
}

/** Runs [block] on projects in the database file [name], on a connection closed afterwards. */
internal inline fun <T> withProjects(
    context: Context,
    name: String,
    block: (LocalProjectRepository) -> T,
): T {
    val database = openMoldaDatabase(context, name)
    return try {
        block(LocalProjectRepository(database.projects()))
    } finally {
        database.close()
    }
}
