package com.almeidajr.molda.project.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.R
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectsState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = true,
    @param:StringRes val loadError: Int? = null,
)

/** The Projetos list: the stored projects, followed as they change. */
@HiltViewModel
internal class ProjectListViewModel @Inject constructor(
    private val repository: ProjectRepository,
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
        mutableState.update { it.copy(isLoading = true, loadError = null) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.projects()
                .map { Result.success(it) }
                .catch { failure -> emit(Result.failure(failure)) }
                .collect { result ->
                    mutableState.update { state ->
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
}
