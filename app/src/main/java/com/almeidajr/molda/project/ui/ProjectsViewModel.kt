package com.almeidajr.molda.project.ui

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.R
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.project.domain.defaultProjectName
import com.almeidajr.molda.project.domain.readProjectName
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** A result being saved as a project, under the [name] typed so far. */
@Serializable
data class ProjectSaving(
    val snapshot: SkirtSnapshot,
    val name: String,
    /** Whether the stored project opens once saved, as when saving an edit as a new one. */
    val showsSaved: Boolean = false,
    val hasBlankName: Boolean = false,
    val isBusy: Boolean = false,
    @param:StringRes val error: Int? = null,
)

/** The stored project [id] being renamed to the [name] typed so far. */
@Serializable
data class ProjectRenaming(
    val id: ProjectId,
    val name: String,
    val hasBlankName: Boolean = false,
    val isBusy: Boolean = false,
    @param:StringRes val error: Int? = null,
)

/**
 * The stored project [id], called [name], whose deletion is being confirmed; a failure keeps it
 * with [error].
 */
@Serializable
data class ProjectDeleting(
    val id: ProjectId,
    val name: String,
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
    /** A project just saved from an edit, to show until [ProjectsViewModel.onProjectShown]. */
    val projectToShow: ProjectId? = null,
)

/** The open dialogs, kept through process death; a storage call they started is not. */
@Serializable
private data class ProjectDialogs(
    val saving: ProjectSaving? = null,
    val renaming: ProjectRenaming? = null,
    val deleting: ProjectDeleting? = null,
)

// One command per project action the UI offers.
@Suppress("TooManyFunctions")
@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val clock: Clock,
    savedState: SavedStateHandle,
) : ViewModel() {
    private var savedDialogs by savedState.saved { ProjectDialogs() }
    private val mutableState = MutableStateFlow(
        ProjectsState(
            saving = savedDialogs.saving?.copy(isBusy = false),
            renaming = savedDialogs.renaming?.copy(isBusy = false),
            deleting = savedDialogs.deleting?.copy(isBusy = false),
        ),
    )
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

    /**
     * Opens the save dialog for [snapshot], named after [patternName], its profile and today;
     * with [showsSaved], the new project is shown once saved.
     */
    fun startSaving(snapshot: SkirtSnapshot, patternName: String, showsSaved: Boolean = false) {
        val today = LocalDate.now(clock)
        val name = defaultProjectName(patternName, snapshot.source?.profileName, today)
        transition { it.copy(saving = ProjectSaving(snapshot, name, showsSaved)) }
    }

    /** Edits the name typed in the open save or rename dialog. */
    fun editName(text: String) = transition { state ->
        state.copy(
            saving = state.saving?.copy(name = text, hasBlankName = false),
            renaming = state.renaming?.copy(name = text, hasBlankName = false),
        )
    }

    /** Stores the named snapshot; once stored, [onShowSnackbar] gets the message to show. */
    fun save(onShowSnackbar: (Int) -> Unit) {
        val saving = mutableState.value.saving?.takeUnless { it.isBusy } ?: return
        val name = readProjectName(saving.name)
            ?: return transition { it.copy(saving = saving.copy(hasBlankName = true)) }
        submit(
            command = { repository.create(name, saving.snapshot, clock.instant()) },
            progress = { state, isBusy, error ->
                state.copy(saving = state.saving?.copy(isBusy = isBusy, error = error))
            },
            failure = R.string.project_save_failed,
            report = { if (it) onShowSnackbar(R.string.project_saved) },
        ) { state, created ->
            state.copy(saving = null, projectToShow = created.id.takeIf { saving.showsSaved })
        }
    }

    /**
     * Stores [snapshot] as the project [id]'s, then shows that project; [onShowSnackbar] gets the
     * message of either outcome, since no dialog is there to show a failure.
     */
    fun update(id: ProjectId, snapshot: SkirtSnapshot, onShowSnackbar: (Int) -> Unit) {
        if (mutableState.value.isUpdating) return
        submit(
            command = { repository.update(id, snapshot, clock.instant()) },
            progress = { state, isBusy, _ -> state.copy(isUpdating = isBusy) },
            failure = R.string.project_update_failed,
            report = {
                onShowSnackbar(if (it) R.string.project_updated else R.string.project_update_failed)
            },
        ) { state, _ -> state.copy(isUpdating = false, projectToShow = id) }
    }

    fun onProjectShown() = transition { it.copy(projectToShow = null) }

    fun startRenaming(project: Project) = transition {
        it.copy(renaming = ProjectRenaming(project.id, project.name.text))
    }

    /** Stores the typed name; once stored, [onShowSnackbar] gets the message to show. */
    fun rename(onShowSnackbar: (Int) -> Unit) {
        val renaming = mutableState.value.renaming?.takeUnless { it.isBusy } ?: return
        val name = readProjectName(renaming.name)
            ?: return transition { it.copy(renaming = renaming.copy(hasBlankName = true)) }
        submit(
            command = { repository.rename(renaming.id, name, clock.instant()) },
            progress = { state, isBusy, error ->
                state.copy(renaming = state.renaming?.copy(isBusy = isBusy, error = error))
            },
            failure = R.string.project_rename_failed,
            report = { if (it) onShowSnackbar(R.string.project_renamed) },
        ) { state, _ -> state.copy(renaming = null) }
    }

    /** Asks to confirm deleting [project]; nothing is deleted until [delete]. */
    fun startDeleting(project: Project) = transition {
        it.copy(deleting = ProjectDeleting(project.id, project.name.text))
    }

    /** Deletes the project being confirmed; once deleted, [onShowSnackbar] gets the message. */
    fun delete(onShowSnackbar: (Int) -> Unit) {
        val deleting = mutableState.value.deleting?.takeUnless { it.isBusy } ?: return
        submit(
            command = { repository.delete(deleting.id) },
            progress = { state, isBusy, error ->
                state.copy(deleting = state.deleting?.copy(isBusy = isBusy, error = error))
            },
            failure = R.string.project_delete_failed,
            report = { if (it) onShowSnackbar(R.string.project_deleted) },
        ) { state, _ -> state.copy(deleting = null) }
    }

    /** Closes the open save, rename or delete dialog, unless its storage call is running. */
    fun cancel() = transition { state ->
        val isBusy = state.saving?.isBusy == true || state.renaming?.isBusy == true ||
            state.deleting?.isBusy == true
        if (isBusy) state else state.copy(saving = null, renaming = null, deleting = null)
    }

    /**
     * Runs a storage [command], marking its dialog busy through [progress] meanwhile; a success
     * moves on with [succeeded], a failure shows [failure] through [progress]; either way [report]
     * then hears whether it succeeded.
     */
    private fun <T> submit(
        command: suspend () -> Result<T>,
        progress: (ProjectsState, isBusy: Boolean, error: Int?) -> ProjectsState,
        @StringRes failure: Int,
        report: (isSuccess: Boolean) -> Unit,
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
            report(result.isSuccess)
        }
    }

    // Main-thread only, so reading back the new value cannot race another transition.
    private fun transition(next: (ProjectsState) -> ProjectsState) {
        val state = next(mutableState.value)
        mutableState.value = state
        savedDialogs = ProjectDialogs(state.saving, state.renaming, state.deleting)
    }
}
