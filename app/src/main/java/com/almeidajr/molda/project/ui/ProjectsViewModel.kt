package com.almeidajr.molda.project.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.R
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.project.domain.defaultProjectName
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** The saving of a new project, running or failed with [error]. */
data class ProjectSaving(val isBusy: Boolean = false, @param:StringRes val error: Int? = null)

/** The renaming of the stored project [id], running or failed with [error]. */
data class ProjectRenaming(
    val id: ProjectId,
    val isBusy: Boolean = false,
    @param:StringRes val error: Int? = null,
)

/** The deletion of the stored project [id], running or failed with [error]. */
data class ProjectDeleting(
    val id: ProjectId,
    val isBusy: Boolean = false,
    @param:StringRes val error: Int? = null,
)

data class ProjectsState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = true,
    @param:StringRes val loadError: Int? = null,
    val saving: ProjectSaving? = null,
    val renaming: ProjectRenaming? = null,
    val deleting: ProjectDeleting? = null,
    /** Whether an edited project's snapshot is being stored. */
    val isUpdating: Boolean = false,
)

// One command per project action the UI offers.
@Suppress("TooManyFunctions")
@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val clock: Clock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProjectsState())
    val state: StateFlow<ProjectsState> = mutableState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    /** Follows the stored projects; after a load failure, calling it again is the retry. */
    fun load() {
        val current = mutableState.value
        if (!current.isLoading && current.loadError == null) return
        transition { it.copy(isLoading = true, loadError = null) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.projects()
                .map { Result.success(it) }
                .catch { failure -> emit(Result.failure(failure)) }
                .collect { result ->
                    transition { state ->
                        result.fold(
                            onSuccess = { state.copy(projects = it, isLoading = false) },
                            onFailure = {
                                state.copy(
                                    isLoading = false,
                                    loadError = R.string.project_load_failed,
                                )
                            },
                        )
                    }
                }
        }
    }

    /** The name offered for a new project from [snapshot], after its kind's [kindLabel]. */
    fun defaultName(snapshot: SkirtSnapshot, kindLabel: String): String =
        defaultProjectName(kindLabel, snapshot.source?.profileName, LocalDate.now(clock))

    /**
     * Stores [snapshot] as a new project called [name]; once the project list holds it, [onSaved]
     * gets its identity.
     */
    fun save(name: ProjectName, snapshot: SkirtSnapshot, onSaved: (ProjectId) -> Unit) {
        if (mutableState.value.saving?.isBusy == true) return
        submit(
            command = {
                repository.create(name, snapshot, clock.instant())
                    // Room's insert returns before the list emits the project, which may open next.
                    .onSuccess { created -> awaitListed(created.id) }
            },
            progress = { state, isBusy, error ->
                state.copy(saving = ProjectSaving(isBusy, error))
            },
            failure = R.string.project_save_failed,
            report = { result -> result.onSuccess { onSaved(it.id) } },
        ) { state, _ -> state.copy(saving = null) }
    }

    /** Forgets a failed save, unless its storage call is running. */
    fun cancelSaving() = transition { state ->
        if (state.saving?.isBusy == true) state else state.copy(saving = null)
    }

    /**
     * Stores [snapshot] as the project [id]'s, then calls [onUpdated]; [onShowSnackbar] gets the
     * message of either outcome, since no dialog is there to show a failure.
     */
    fun update(
        id: ProjectId,
        snapshot: SkirtSnapshot,
        onShowSnackbar: (Int) -> Unit,
        onUpdated: () -> Unit,
    ) {
        if (mutableState.value.isUpdating) return
        submit(
            command = { repository.update(id, snapshot, clock.instant()) },
            progress = { state, isBusy, _ -> state.copy(isUpdating = isBusy) },
            failure = R.string.project_update_failed,
            report = { result ->
                if (result.isSuccess) {
                    onShowSnackbar(R.string.project_updated)
                    onUpdated()
                } else {
                    onShowSnackbar(R.string.project_update_failed)
                }
            },
        ) { state, _ -> state.copy(isUpdating = false) }
    }

    /** Renames the project [id] to [name]; once stored, [onRenamed] gets the message to show. */
    fun rename(id: ProjectId, name: ProjectName, onRenamed: (Int) -> Unit) {
        if (mutableState.value.renaming?.isBusy == true) return
        submit(
            command = { repository.rename(id, name, clock.instant()) },
            progress = { state, isBusy, error ->
                state.copy(renaming = ProjectRenaming(id, isBusy, error))
            },
            failure = R.string.project_rename_failed,
            report = { if (it.isSuccess) onRenamed(R.string.project_renamed) },
        ) { state, _ -> state.copy(renaming = null) }
    }

    /** Deletes the project [id]; once deleted, [onDeleted] gets the message to show. */
    fun delete(id: ProjectId, onDeleted: (Int) -> Unit) {
        if (mutableState.value.deleting?.isBusy == true) return
        submit(
            command = { repository.delete(id) },
            progress = { state, isBusy, error ->
                state.copy(deleting = ProjectDeleting(id, isBusy, error))
            },
            failure = R.string.project_delete_failed,
            report = { if (it.isSuccess) onDeleted(R.string.project_deleted) },
        ) { state, _ -> state.copy(deleting = null) }
    }

    /** Forgets a failed deletion, unless its storage call is running. */
    fun cancelDeleting() = transition { state ->
        if (state.deleting?.isBusy == true) state else state.copy(deleting = null)
    }

    /** Forgets a failed renaming, unless its storage call is running. */
    fun cancelRenaming() = transition { state ->
        if (state.renaming?.isBusy == true) state else state.copy(renaming = null)
    }

    /**
     * Runs a storage [command], marking its dialog busy through [progress] meanwhile; a success
     * moves on with [succeeded], a failure shows [failure] through [progress]; either way [report]
     * then hears the result.
     */
    private fun <T> submit(
        command: suspend () -> Result<T>,
        progress: (ProjectsState, isBusy: Boolean, error: Int?) -> ProjectsState,
        @StringRes failure: Int,
        report: (Result<T>) -> Unit,
        succeeded: (ProjectsState, T) -> ProjectsState,
    ) {
        transition { progress(it, true, null) }
        viewModelScope.launch {
            val result = command()
            transition { state ->
                result.fold(
                    onSuccess = { succeeded(state, it) },
                    onFailure = { progress(state, false, failure) },
                )
            }
            report(result)
        }
    }

    /** Waits until the project list holds [id], or fails to load. */
    private suspend fun awaitListed(id: ProjectId) {
        mutableState.first { state ->
            state.loadError != null || state.projects.any { it.id == id }
        }
    }

    // Main-thread only, so reading back the new value cannot race another transition.
    private fun transition(next: (ProjectsState) -> ProjectsState) {
        val state = next(mutableState.value)
        mutableState.value = state
    }
}
