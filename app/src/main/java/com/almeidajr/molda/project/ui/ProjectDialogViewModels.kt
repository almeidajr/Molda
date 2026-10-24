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

/** A project storage command's state; [Done] holds the outcome its entry has yet to react to. */
sealed interface ProjectCommandState<out T : Any> {
    /** Ready to run: never run, or failed and acknowledged. */
    data object Idle : ProjectCommandState<Nothing>

    data object Running : ProjectCommandState<Nothing>

    /** Failed with [message]; it runs again when submitted. */
    data class Failed(@param:StringRes val message: Int) : ProjectCommandState<Nothing>

    data class Done<out T : Any>(override val outcome: T) : ProjectCommandState<T>

    /** Succeeded and acknowledged; it never runs again. */
    data object Spent : ProjectCommandState<Nothing>

    val isBusy: Boolean get() = this == Running

    @get:StringRes
    val error: Int? get() = (this as? Failed)?.message

    /** The outcome awaiting its entry's reaction, held only while [Done]. */
    val outcome: T? get() = null
}

/**
 * A project storage command, run in [scope] once: it runs again only after a failure, which shows
 * [failure], and keeps its outcome until acknowledged.
 */
internal class ProjectCommand<T : Any>(
    private val scope: CoroutineScope,
    @param:StringRes private val failure: Int,
) {
    private val mutableState = MutableStateFlow<ProjectCommandState<T>>(ProjectCommandState.Idle)
    val state: StateFlow<ProjectCommandState<T>> = mutableState.asStateFlow()

    /** Runs [command], unless it is running, done or spent. */
    fun submit(command: suspend () -> Result<T>) {
        val current = mutableState.value
        if (current != ProjectCommandState.Idle && current !is ProjectCommandState.Failed) return
        mutableState.value = ProjectCommandState.Running
        scope.launch {
            mutableState.value = command().fold(
                onSuccess = { ProjectCommandState.Done(it) },
                onFailure = { ProjectCommandState.Failed(failure) },
            )
        }
    }

    /** Spends the command once its entry has reacted to the outcome. */
    fun acknowledge() = mutableState.update {
        if (it is ProjectCommandState.Done) ProjectCommandState.Spent else it
    }

    /** Forgets a failure once its entry has shown it; an outcome stays until acknowledged. */
    fun acknowledgeError() = mutableState.update {
        if (it is ProjectCommandState.Failed) ProjectCommandState.Idle else it
    }
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
