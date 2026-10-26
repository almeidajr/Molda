package com.almeidajr.molda

import android.content.Context
import com.almeidajr.molda.data.room.openMoldaDatabase
import com.almeidajr.molda.profile.data.LocalProfileRepository
import com.almeidajr.molda.project.data.LocalProjectRepository

/** Runs [block] on profiles in the database file [name], on a connection closed afterward. */
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

/** Runs [block] on projects in the database file [name], on a connection closed afterward. */
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
