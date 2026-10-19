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

data class ProjectsState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = true,
    @param:StringRes val loadError: Int? = null,
    /** The open save dialog; it lives with the result, so it does not survive process death. */
    val saving: ProjectSaving? = null,
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

    fun editName(text: String) = transition { state ->
        state.copy(saving = state.saving?.copy(name = text, hasBlankName = false))
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

    fun cancelSaving() = transition { state ->
        if (state.saving?.isSaving == true) state else state.copy(saving = null)
    }

    fun dismissMessage() = transition { it.copy(savedMessage = null) }

    // Main-thread only, so reading back the new value cannot race another transition.
    private fun transition(next: (ProjectsState) -> ProjectsState) {
        mutableState.value = next(mutableState.value)
    }
}
