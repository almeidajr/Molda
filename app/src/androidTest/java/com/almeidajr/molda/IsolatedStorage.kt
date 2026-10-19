package com.almeidajr.molda

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.data.room.MoldaDatabase
import com.almeidajr.molda.data.room.openMoldaDatabase

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
