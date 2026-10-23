package com.almeidajr.molda.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.project.navigation.ProjectEditorKey
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How an update of the project ended. */
enum class ProjectUpdateOutcome { Updated, Failed }

/** An update of the edited project: running, or ended with an [outcome] the editor has to show. */
data class ProjectUpdateState(
    val isUpdating: Boolean = false,
    val outcome: ProjectUpdateOutcome? = null,
)

/** Stores the snapshot of the project editor on [key] as its stored project's. */
@HiltViewModel(assistedFactory = ProjectUpdateViewModel.Factory::class)
internal class ProjectUpdateViewModel @AssistedInject constructor(
    private val repository: ProjectRepository,
    private val clock: Clock,
    // The key, not its ProjectId: Dagger cannot pass a value class to an assisted constructor.
    @Assisted private val key: ProjectEditorKey,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProjectUpdateState())
    val state: StateFlow<ProjectUpdateState> = mutableState.asStateFlow()

    /** Replaces the project's snapshot with [snapshot], unless an update is running. */
    fun update(snapshot: SkirtSnapshot) {
        if (mutableState.value.isUpdating) return
        mutableState.value = ProjectUpdateState(isUpdating = true)
        viewModelScope.launch {
            val result = repository.update(key.id, snapshot, clock.instant())
            val outcome = if (result.isSuccess) {
                ProjectUpdateOutcome.Updated
            } else {
                ProjectUpdateOutcome.Failed
            }
            mutableState.value = ProjectUpdateState(outcome = outcome)
        }
    }

    /** Forgets the outcome once the editor has shown it. */
    fun outcomeShown() = mutableState.update { it.copy(outcome = null) }

    @AssistedFactory
    interface Factory {
        fun create(key: ProjectEditorKey): ProjectUpdateViewModel
    }
}
