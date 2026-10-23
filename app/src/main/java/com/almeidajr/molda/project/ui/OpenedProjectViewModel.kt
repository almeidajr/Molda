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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Where one opened project stands in storage. */
internal sealed interface OpenedProjectState {
    /** Not loaded yet, or its load failed, which the Projetos list shows with its retry. */
    data object Loading : OpenedProjectState

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
    // shortcut: a failed load stays Loading until the entry is reopened; retry here if it matters.
    val state: StateFlow<OpenedProjectState> = repository.projects()
        .map { projects ->
            projects.find { it.id == key.id }?.let(OpenedProjectState::Shown)
                ?: OpenedProjectState.Gone
        }
        .catch { }
        .stateIn(viewModelScope, SharingStarted.Eagerly, OpenedProjectState.Loading)

    @AssistedFactory
    interface Factory {
        fun create(key: ProjectKey): OpenedProjectViewModel
    }
}
