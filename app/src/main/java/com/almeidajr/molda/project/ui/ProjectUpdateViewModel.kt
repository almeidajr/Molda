package com.almeidajr.molda.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.R
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.ui.navigation.EditorKind
import com.almeidajr.molda.ui.navigation.EditorReport
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Stores the snapshot of the project editor on [key] as its stored project's, and reports the
 * editor's status from its updates and [isEditorDirty], whether its draft has unsaved changes,
 * which the calculator ViewModel on the same entry holds.
 */
@HiltViewModel(assistedFactory = ProjectUpdateViewModel.Factory::class)
internal class ProjectUpdateViewModel @AssistedInject constructor(
    private val repository: ProjectRepository,
    private val clock: Clock,
    // The key, not its ProjectId: Dagger cannot pass a value class to an assisted constructor.
    @Assisted private val key: ProjectEditorKey,
    @Assisted isEditorDirty: StateFlow<Boolean>,
) : ViewModel() {
    // Runs once: a stored update closes the editor.
    private val command = ProjectCommand<Unit>(viewModelScope, R.string.project_update_failed)
    val state: StateFlow<ProjectCommandState<Unit>> = command.state

    /** The project editor's status for the app, which asks before its changes are dropped. */
    val report: StateFlow<EditorReport> = combine(isEditorDirty, state) { isDirty, update ->
        EditorReport(EditorKind.Project, isDirty, update.isBusy)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        EditorReport(EditorKind.Project, isEditorDirty.value, state.value.isBusy),
    )

    /** Replaces the project's snapshot with [snapshot], unless an update is running or stored. */
    fun update(snapshot: SkirtSnapshot) =
        command.submit { repository.update(key.id, snapshot, clock.instant()) }

    fun acknowledge() = command.acknowledge()

    fun acknowledgeError() = command.acknowledgeError()

    @AssistedFactory
    interface Factory {
        fun create(key: ProjectEditorKey, isEditorDirty: StateFlow<Boolean>): ProjectUpdateViewModel
    }
}
