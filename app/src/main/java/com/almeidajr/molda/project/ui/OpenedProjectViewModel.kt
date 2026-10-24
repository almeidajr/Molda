package com.almeidajr.molda.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.data.LoadResult
import com.almeidajr.molda.data.asLoadResult
import com.almeidajr.molda.domain.NoLongerExistsException
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.project.navigation.ProjectKey
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Follows the stored project that [key] names, for any entry that shows or acts on it. */
@HiltViewModel(assistedFactory = OpenedProjectViewModel.Factory::class)
internal class OpenedProjectViewModel @AssistedInject constructor(
    repository: ProjectRepository,
    // The key, not its ProjectId: Dagger cannot pass a value class to an assisted constructor.
    @Assisted key: ProjectKey,
) : ViewModel() {
    // Each retry starts the load again.
    private val attempts = MutableStateFlow(0)

    /** The project, or null once it is no longer stored; [retry] loads again after a failure. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<LoadResult<Project?>> = attempts
        .flatMapLatest { _ ->
            repository.project(key.id)
                .catch<Project?> { failure ->
                    if (failure is NoLongerExistsException) emit(null) else throw failure
                }
                .asLoadResult()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LoadResult.Loading)

    /** Loads the project again after the load failed. */
    fun retry() = attempts.update { it + 1 }

    @AssistedFactory
    interface Factory {
        fun create(key: ProjectKey): OpenedProjectViewModel
    }
}
