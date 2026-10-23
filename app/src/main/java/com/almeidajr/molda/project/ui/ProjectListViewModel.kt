package com.almeidajr.molda.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.data.LoadResult
import com.almeidajr.molda.data.asLoadResult
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The Projetos list: the stored projects, followed as they change. */
@HiltViewModel
internal class ProjectListViewModel @Inject constructor(
    private val repository: ProjectRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<LoadResult<List<Project>>>(LoadResult.Loading)
    val state: StateFlow<LoadResult<List<Project>>> = mutableState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    /** Follows the stored projects; after a load failure, calling it again is the retry. */
    fun load() {
        if (mutableState.value is LoadResult.Success) return
        // A retry while the first emission is pending replaces that collector.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.projects().asLoadResult().collect { mutableState.value = it }
        }
    }
}
