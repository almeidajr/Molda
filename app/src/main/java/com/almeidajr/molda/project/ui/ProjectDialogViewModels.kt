package com.almeidajr.molda.project.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.R
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.project.domain.defaultProjectName
import com.almeidajr.molda.project.navigation.DeleteProjectKey
import com.almeidajr.molda.project.navigation.RenameProjectKey
import com.almeidajr.molda.project.navigation.SaveProjectKey
import com.almeidajr.molda.ui.command.CommandState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A project storage command's state: running, failed with [error], or ended with an [outcome] its
 * entry reacts to.
 */
data class ProjectCommandState<out T : Any>(
    override val isBusy: Boolean = false,
    @param:StringRes val error: Int? = null,
    override val outcome: T? = null,
) : CommandState<T>

/**
 * A project storage command, run in [scope] once at a time; it keeps how it ended until
 * acknowledged, and a failure shows [failure]. Only a repeatable command runs again after a
 * success.
 */
internal class ProjectCommand<T : Any>(
    private val scope: CoroutineScope,
    @param:StringRes private val failure: Int,
    private val isRepeatable: Boolean = false,
) {
    private val mutableState = MutableStateFlow(ProjectCommandState<T>())
    val state: StateFlow<ProjectCommandState<T>> = mutableState.asStateFlow()

    // Set by the first success of a command that is not repeatable; it never runs again.
    private var isDone = false

    /**
     * Runs [command], unless it is running, its outcome awaits acknowledgement, or it has already
     * succeeded and is not repeatable.
     */
    fun submit(command: suspend () -> Result<T>) {
        val current = mutableState.value
        if (current.isBusy || current.outcome != null || isDone) return
        mutableState.value = ProjectCommandState(isBusy = true)
        scope.launch {
            mutableState.value = command().fold(
                onSuccess = {
                    isDone = !isRepeatable
                    ProjectCommandState(outcome = it)
                },
                onFailure = { ProjectCommandState(error = failure) },
            )
        }
    }

    /** Forgets how the command ended, once its entry has reacted. */
    fun acknowledge() = mutableState.update { it.copy(error = null, outcome = null) }
}

/** Saves the snapshot of [key] as a new project; its outcome is the new project's id. */
@HiltViewModel(assistedFactory = SaveProjectViewModel.Factory::class)
internal class SaveProjectViewModel @AssistedInject constructor(
    private val repository: ProjectRepository,
    private val clock: Clock,
    @Assisted private val key: SaveProjectKey,
) : ViewModel() {
    private val command = ProjectCommand<ProjectId>(viewModelScope, R.string.project_save_failed)
    val state: StateFlow<ProjectCommandState<ProjectId>> = command.state

    /** The name offered for the new project, after its kind's [kindLabel]. */
    fun defaultName(kindLabel: String): String =
        defaultProjectName(kindLabel, key.snapshot.source?.profileName, LocalDate.now(clock))

    fun acknowledge() = command.acknowledge()

    fun save(name: ProjectName) =
        command.submit { repository.create(name, key.snapshot, clock.instant()).map { it.id } }

    @AssistedFactory
    interface Factory {
        fun create(key: SaveProjectKey): SaveProjectViewModel
    }
}

/** Renames the stored project of [key]. */
@HiltViewModel(assistedFactory = RenameProjectViewModel.Factory::class)
internal class RenameProjectViewModel @AssistedInject constructor(
    private val repository: ProjectRepository,
    private val clock: Clock,
    // The key, not its ProjectId: Dagger cannot pass a value class to an assisted constructor.
    @Assisted private val key: RenameProjectKey,
) : ViewModel() {
    private val command = ProjectCommand<Unit>(viewModelScope, R.string.project_rename_failed)
    val state: StateFlow<ProjectCommandState<Unit>> = command.state

    fun acknowledge() = command.acknowledge()

    fun rename(name: ProjectName) =
        command.submit { repository.rename(key.id, name, clock.instant()) }

    @AssistedFactory
    interface Factory {
        fun create(key: RenameProjectKey): RenameProjectViewModel
    }
}

/** Deletes the stored project of [key]. */
@HiltViewModel(assistedFactory = DeleteProjectViewModel.Factory::class)
internal class DeleteProjectViewModel @AssistedInject constructor(
    private val repository: ProjectRepository,
    // The key, not its ProjectId: Dagger cannot pass a value class to an assisted constructor.
    @Assisted private val key: DeleteProjectKey,
) : ViewModel() {
    private val command = ProjectCommand<Unit>(viewModelScope, R.string.project_delete_failed)
    val state: StateFlow<ProjectCommandState<Unit>> = command.state

    fun acknowledge() = command.acknowledge()

    fun delete() = command.submit { repository.delete(key.id) }

    @AssistedFactory
    interface Factory {
        fun create(key: DeleteProjectKey): DeleteProjectViewModel
    }
}
