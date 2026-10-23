package com.almeidajr.molda

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.idling.concurrent.IdlingThreadPoolExecutor
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.data.room.MoldaDatabase
import com.almeidajr.molda.data.room.openMoldaDatabase
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.asCoroutineDispatcher

/**
 * A fresh Molda database in the file [name], for one test's isolated storage. Room runs its
 * queries on an executor registered with Espresso, so Compose waits for database work by itself.
 */
class IsolatedStorage internal constructor(private val name: String) {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // Registers itself as an idling resource and unregisters once shut down.
    private val queries = IdlingThreadPoolExecutor(
        "$name queries",
        QUERY_THREADS,
        QUERY_THREADS,
        0,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(),
        Executors.defaultThreadFactory(),
    )

    val database: MoldaDatabase = openMoldaDatabase(
        context.also { it.deleteDatabase(name) },
        name,
        queries.asCoroutineDispatcher(),
    )

    /** Closes [scenario] first, so the app stops using the database, then deletes the file. */
    fun remove(scenario: ActivityScenario<*>) {
        scenario.close()
        database.close()
        queries.shutdown()
        context.deleteDatabase(name)
    }

    private companion object {
        const val QUERY_THREADS = 4
    }
}
