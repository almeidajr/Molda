package com.almeidajr.molda.testing

import com.almeidajr.molda.data.resultOf
import com.almeidajr.molda.domain.NoLongerExistsException
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.project.domain.SkirtSnapshot
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory projects, seeded with [projects], listed and re-emitted after each change like Room.
 */
class FakeProjectRepository(projects: List<Project> = emptyList()) : ProjectRepository {
    private val stored = MutableStateFlow(projects.newestFirst())

    /** The stored projects right now, most recently changed first. */
    val current: List<Project> get() = stored.value

    /** Makes loading the projects throw. */
    @Volatile var failsLoad = false

    /** Makes every command fail with it, without changing the stored projects. */
    @Volatile var commandFailure: Throwable? = null

    /** Makes every command fail as storage does, without changing the stored projects. */
    var failsCommands: Boolean
        get() = commandFailure != null
        set(fails) {
            commandFailure =
                if (fails) IllegalStateException("Controlled storage failure") else null
        }

    /** When set, loading waits for it to complete, so tests can observe the pending state. */
    @Volatile var loadGate: CompletableDeferred<Unit>? = null

    /** When set, every command waits for it to complete, so tests can observe the pending state. */
    @Volatile var commandGate: CompletableDeferred<Unit>? = null

    override fun projects(): Flow<List<Project>> = flow {
        loadGate?.await()
        check(!failsLoad) { "Controlled load failure" }
        emitAll(stored)
    }

    override fun project(id: ProjectId): Flow<Project> = projects().map { projects ->
        projects.find { it.id == id } ?: throw NoLongerExistsException("No project $id")
    }

    override suspend fun create(name: ProjectName, snapshot: SkirtSnapshot, savedAt: Instant) =
        command {
            val id = ProjectId((current.maxOfOrNull { it.id.value } ?: 0) + 1)
            Project(id, name, snapshot, savedAt, savedAt)
                .also { created -> stored.update { (it + created).newestFirst() } }
        }

    override suspend fun update(id: ProjectId, snapshot: SkirtSnapshot, updatedAt: Instant) =
        command { change(id) { it.copy(snapshot = snapshot, updatedAt = updatedAt) } }

    override suspend fun rename(id: ProjectId, name: ProjectName, renamedAt: Instant) =
        command { change(id) { it.copy(name = name, updatedAt = renamedAt) } }

    override suspend fun delete(id: ProjectId) = command { remove(id) }

    /** Removes the project [id] at once, as a deletion elsewhere does, bypassing every gate. */
    fun forget(id: ProjectId) = remove(id)

    private fun remove(id: ProjectId) {
        requireStored(id)
        stored.update { projects -> projects.filterNot { it.id == id } }
    }

    private fun change(id: ProjectId, change: (Project) -> Project) {
        requireStored(id)
        stored.update { projects ->
            projects.map { if (it.id == id) change(it) else it }.newestFirst()
        }
    }

    private fun requireStored(id: ProjectId) {
        if (current.none { it.id == id }) throw NoLongerExistsException("No project $id")
    }

    private suspend fun <T> command(block: () -> T): Result<T> {
        commandGate?.await()
        return commandFailure?.let { Result.failure(it) } ?: resultOf { block() }
    }
}

// Room's order: latest change first, then newest identity.
private fun List<Project>.newestFirst() =
    sortedWith(compareByDescending<Project> { it.updatedAt }.thenByDescending { it.id.value })
