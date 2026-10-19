package com.almeidajr.molda.project.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.R
import com.almeidajr.molda.project.domain.Project
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

/** A result being saved as a project, under the [name] typed so far. */
data class ProjectSaving(
    val snapshot: SkirtSnapshot,
    val name: String,
    val hasBlankName: Boolean = false,
    val isSaving: Boolean = false,
    @param:StringRes val saveError: Int? = null,
)

/** A stored [project] being renamed to the [name] typed so far. */
data class ProjectRenaming(
    val project: Project,
    val name: String,
    val hasBlankName: Boolean = false,
    val isBusy: Boolean = false,
    @param:StringRes val error: Int? = null,
)

/** A stored [project] whose deletion is being confirmed; a failure keeps it with [error]. */
data class ProjectDeleting(
    val project: Project,
    val isBusy: Boolean = false,
    @param:StringRes val error: Int? = null,
)

data class ProjectsState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = true,
    @param:StringRes val loadError: Int? = null,
    /** The open save dialog; it lives with the result, so it does not survive process death. */
    val saving: ProjectSaving? = null,
    val renaming: ProjectRenaming? = null,
    val deleting: ProjectDeleting? = null,
    @param:StringRes val savedMessage: Int? = null,
)

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

    /** Opens the save dialog for [snapshot], named after [patternName], its profile and today. */
    fun startSaving(snapshot: SkirtSnapshot, patternName: String) {
        val today = LocalDate.now(clock)
        val name = defaultProjectName(patternName, snapshot.source?.profileName, today)
        transition { it.copy(saving = ProjectSaving(snapshot, name)) }
    }

    /** Edits the name typed in the open save or rename dialog. */
    fun editName(text: String) = transition { state ->
        state.copy(
            saving = state.saving?.copy(name = text, hasBlankName = false),
            renaming = state.renaming?.copy(name = text, hasBlankName = false),
        )
    }

    fun save() {
        val saving = mutableState.value.saving
        if (saving == null || saving.isSaving) return
        val name = readProjectName(saving.name)
        if (name == null) {
            transition { it.copy(saving = saving.copy(hasBlankName = true)) }
            return
        }
        transition { it.copy(saving = saving.copy(isSaving = true, saveError = null)) }
        viewModelScope.launch {
            val result = repository.create(name, saving.snapshot, clock.instant())
            transition { state ->
                if (result.isSuccess) {
                    state.copy(saving = null, savedMessage = R.string.project_saved)
                } else {
                    state.copy(
                        saving = state.saving?.copy(
                            isSaving = false,
                            saveError = R.string.project_save_failed,
                        ),
                    )
                }
            }
        }
    }

    fun startRenaming(project: Project) = transition {
        it.copy(renaming = ProjectRenaming(project, project.name.text))
    }

    fun rename() {
        val renaming = mutableState.value.renaming
        if (renaming == null || renaming.isBusy) return
        val name = readProjectName(renaming.name)
        if (name == null) {
            transition { it.copy(renaming = renaming.copy(hasBlankName = true)) }
            return
        }
        transition { it.copy(renaming = renaming.copy(isBusy = true, error = null)) }
        viewModelScope.launch {
            val result = repository.rename(renaming.project.id, name, clock.instant())
            transition { state ->
                if (result.isSuccess) {
                    state.copy(renaming = null, savedMessage = R.string.project_renamed)
                } else {
                    state.copy(
                        renaming = state.renaming?.copy(
                            isBusy = false,
                            error = R.string.project_rename_failed,
                        ),
                    )
                }
            }
        }
    }

    /** Asks to confirm deleting [project]; nothing is deleted until [delete]. */
    fun startDeleting(project: Project) = transition {
        it.copy(deleting = ProjectDeleting(project))
    }

    fun delete() {
        val deleting = mutableState.value.deleting
        if (deleting == null || deleting.isBusy) return
        transition { it.copy(deleting = deleting.copy(isBusy = true, error = null)) }
        viewModelScope.launch {
            val result = repository.delete(deleting.project.id)
            transition { state ->
                if (result.isSuccess) {
                    state.copy(deleting = null, savedMessage = R.string.project_deleted)
                } else {
                    state.copy(
                        deleting = state.deleting?.copy(
                            isBusy = false,
                            error = R.string.project_delete_failed,
                        ),
                    )
                }
            }
        }
    }

    /** Closes the open save, rename or delete dialog, unless its storage call is running. */
    fun cancel() = transition { state ->
        val isBusy = state.saving?.isSaving == true || state.renaming?.isBusy == true ||
            state.deleting?.isBusy == true
        if (isBusy) state else state.copy(saving = null, renaming = null, deleting = null)
    }

    fun dismissMessage() = transition { it.copy(savedMessage = null) }

    // Main-thread only, so reading back the new value cannot race another transition.
    private fun transition(next: (ProjectsState) -> ProjectsState) {
        mutableState.value = next(mutableState.value)
    }
}
