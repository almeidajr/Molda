package com.almeidajr.molda.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Where one opened project stands in storage. */
internal sealed interface OpenedProjectState {
    data object Loading : OpenedProjectState

    /** The projects failed to load; [OpenedProjectViewModel.retry] loads them again. */
    data object Failed : OpenedProjectState

    data class Shown(val project: Project) : OpenedProjectState

    /** No longer stored. */
    data object Gone : OpenedProjectState
}

/** Follows the stored project that [key] names, for any entry that shows or acts on it. */
@HiltViewModel(assistedFactory = OpenedProjectViewModel.Factory::class)
internal class OpenedProjectViewModel @AssistedInject constructor(
    repository: ProjectRepository,
    // The key, not its ProjectId: Dagger cannot pass a value class to an assisted constructor.
    @Assisted key: ProjectKey,
) : ViewModel() {
    // Each retry starts the load again.
    private val attempts = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<OpenedProjectState> = attempts
        .flatMapLatest {
            repository.projects()
                .map<_, OpenedProjectState> { projects ->
                    projects.find { it.id == key.id }?.let(OpenedProjectState::Shown)
                        ?: OpenedProjectState.Gone
                }
                .onStart { emit(OpenedProjectState.Loading) }
                .catch { emit(OpenedProjectState.Failed) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, OpenedProjectState.Loading)

    /** Loads the projects again after [OpenedProjectState.Failed]. */
    fun retry() = attempts.update { it + 1 }

    @AssistedFactory
    interface Factory {
        fun create(key: ProjectKey): OpenedProjectViewModel
    }
}
