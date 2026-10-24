package com.almeidajr.molda.project.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.R
import com.almeidajr.molda.about.navigation.AboutKey
import com.almeidajr.molda.data.LoadResult
import com.almeidajr.molda.pattern.navigation.PatternCatalogKey
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.navigation.DeleteProjectKey
import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.project.navigation.ProjectKey
import com.almeidajr.molda.project.navigation.ProjectsKey
import com.almeidajr.molda.project.navigation.RenameProjectKey
import com.almeidajr.molda.project.navigation.SaveProjectKey
import com.almeidajr.molda.project.navigation.closeProject
import com.almeidajr.molda.project.navigation.isDeletingProject
import com.almeidajr.molda.project.navigation.isShowingProject
import com.almeidajr.molda.project.navigation.openProject
import com.almeidajr.molda.project.navigation.showProject
import com.almeidajr.molda.ui.adaptive.LocalIsTwoPane
import com.almeidajr.molda.ui.adaptive.ReadablePane
import com.almeidajr.molda.ui.command.OutcomeEffect
import com.almeidajr.molda.ui.navigation.CatalogSelection
import com.almeidajr.molda.ui.navigation.Navigator
import com.almeidajr.molda.ui.navigation.WindowOverlaySceneStrategy.Companion.windowOverlay

/**
 * Projetos: the saved projects beside the open one, whose pattern draft [savedResult] shows. The
 * editor and guides opened from a project belong to its pattern draft, so the app wires them.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.projectSection(
    navigator: Navigator,
    onShowSnackbar: (Int) -> Unit,
    savedResult: @Composable (Project) -> Unit,
) {
    entry<ProjectsKey>(
        metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = {
            ReadablePane {
                Text(stringResource(R.string.project_choose), Modifier.padding(16.dp))
            }
        }),
    ) {
        ProjectList(navigator)
    }
    entry<ProjectKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
        OpenedProject(key.id, onDeleted = {
            navigator.closeDeletedProject(key.id, onShowSnackbar)
        }) { project ->
            ProjectScreen(
                project,
                // Beside the list the project is a pane, closed by choosing from the list or Back.
                onUp = if (LocalIsTwoPane.current) null else navigator::back,
                onEdit = { navigator.push(ProjectEditorKey(project.id, project.snapshot)) },
                // Its delete dialog closes it once the project is deleted.
                onRename = { navigator.push(RenameProjectKey(project.id)) },
                onDelete = { navigator.push(DeleteProjectKey(project.id)) },
            ) {
                savedResult(project)
            }
        }
    }
    // Project dialogs are window-overlay entries keyed by what they act on; Back leaves the key.
    entry<SaveProjectKey>(metadata = windowOverlay()) { key ->
        SaveProjectEntry(key, navigator, onShowSnackbar)
    }
    entry<RenameProjectKey>(metadata = windowOverlay()) { key ->
        RenameProjectEntry(key, navigator, onShowSnackbar)
    }
    entry<DeleteProjectKey>(metadata = windowOverlay()) { key ->
        DeleteProjectEntry(key, navigator, onShowSnackbar)
    }
}

@Composable
private fun SaveProjectEntry(
    key: SaveProjectKey,
    navigator: Navigator,
    onShowSnackbar: (Int) -> Unit,
) {
    val dialog = hiltViewModel<SaveProjectViewModel, SaveProjectViewModel.Factory> {
        it.create(key)
    }
    val state by dialog.state.collectAsStateWithLifecycle()
    OutcomeEffect(state.outcome, dialog::acknowledge) { savedId ->
        onShowSnackbar(R.string.project_saved)
        navigator.remove(key)
        if (key.showsSaved) navigator.showProject(savedId)
    }
    SaveProjectDialog(
        defaultName = dialog.defaultName(stringResource(key.snapshot.kind.label)),
        isBusy = state.isBusy,
        error = state.error,
        onSave = dialog::save,
        onCancel = { if (!state.isBusy) navigator.remove(key) },
    )
}

/**
 * Shows the outcome of the project editor's [update] of the project [id] once: a stored update
 * opens the project in place of the editor.
 */
@Composable
internal fun ProjectUpdateEffect(
    update: ProjectUpdateViewModel,
    id: ProjectId,
    navigator: Navigator,
    onShowSnackbar: (Int) -> Unit,
) {
    val state by update.state.collectAsStateWithLifecycle()
    OutcomeEffect(state.outcome, update::acknowledge) {
        onShowSnackbar(R.string.project_updated)
        navigator.showProject(id)
    }
    OutcomeEffect(state.deleted, update::acknowledge) {
        navigator.closeDeletedProject(id, onShowSnackbar)
    }
    // The editor stays open after a failure, so it shows as a snackbar once rather than inline.
    OutcomeEffect(state.error, update::acknowledgeError, onShowSnackbar)
}

@Composable
private fun RenameProjectEntry(
    key: RenameProjectKey,
    navigator: Navigator,
    onShowSnackbar: (Int) -> Unit,
) {
    val dialog = hiltViewModel<RenameProjectViewModel, RenameProjectViewModel.Factory> {
        it.create(key)
    }
    val state by dialog.state.collectAsStateWithLifecycle()
    OutcomeEffect(state.outcome, dialog::acknowledge) {
        onShowSnackbar(R.string.project_renamed)
        navigator.remove(key)
    }
    val closeDeleted = { navigator.closeDeletedProject(key.id, onShowSnackbar) }
    OutcomeEffect(state.deleted, dialog::acknowledge) { closeDeleted() }
    OpenedProject(
        key.id,
        onDeleted = closeDeleted,
        failed = { ProjectLoadFailureDialog(it, onDismiss = { navigator.remove(key) }) },
    ) { project ->
        RenameProjectDialog(
            currentName = project.name.text,
            isBusy = state.isBusy,
            error = state.error,
            onRename = dialog::rename,
            onCancel = { if (!state.isBusy) navigator.remove(key) },
        )
    }
}

@Composable
private fun DeleteProjectEntry(
    key: DeleteProjectKey,
    navigator: Navigator,
    onShowSnackbar: (Int) -> Unit,
) {
    val dialog = hiltViewModel<DeleteProjectViewModel, DeleteProjectViewModel.Factory> {
        it.create(key)
    }
    val state by dialog.state.collectAsStateWithLifecycle()
    // The dialog reports the deletion and closes every entry of the project, which those entries
    // leave to it while it is open.
    OutcomeEffect(state.outcome, dialog::acknowledge) {
        onShowSnackbar(R.string.project_deleted)
        navigator.closeProject(key.id)
    }
    // Only the first of the dialog's reactions to the deletion finds the dialog open.
    val closeDeleted = {
        if (navigator.closeProject(key.id)) onShowSnackbar(R.string.project_deleted_meanwhile)
    }
    OutcomeEffect(state.deleted, dialog::acknowledge) { closeDeleted() }
    OpenedProject(
        key.id,
        // Gone by its own delete, which reports through its outcome, or deleted meanwhile.
        onDeleted = {
            val own = dialog.state.value
            if (!own.isBusy && own.outcome == null) closeDeleted()
        },
        failed = { ProjectLoadFailureDialog(it, onDismiss = { navigator.remove(key) }) },
    ) { project ->
        DeleteProjectDialog(
            name = project.name.text,
            isBusy = state.isBusy,
            error = state.error,
            onDelete = dialog::delete,
            onCancel = { if (!state.isBusy) navigator.remove(key) },
        )
    }
}

@Composable
private fun ProjectList(navigator: Navigator) {
    val list = hiltViewModel<ProjectListViewModel>()
    val state by list.state.collectAsStateWithLifecycle()
    val isTwoPane = LocalIsTwoPane.current
    ProjectsScreen(
        state,
        list::load,
        onSeePatterns = { navigator.select(PatternCatalogKey) },
        onAbout = { navigator.push(AboutKey) },
        onOpen = navigator::openProject,
        selection = { id ->
            when {
                !isTwoPane -> CatalogSelection.None
                navigator.isShowingProject(id) -> CatalogSelection.Selected
                else -> CatalogSelection.Unselected
            }
        },
        onRename = { navigator.push(RenameProjectKey(it.id)) },
        onDelete = { navigator.push(DeleteProjectKey(it.id)) },
    )
}

/**
 * Closes every entry of the project [id], found deleted meanwhile, showing "Este projeto foi
 * excluído." once; while its delete dialog is open, that dialog reports and closes them instead.
 */
internal fun Navigator.closeDeletedProject(id: ProjectId, onShowSnackbar: (Int) -> Unit) {
    if (!isDeletingProject(id) && closeProject(id)) {
        onShowSnackbar(R.string.project_deleted_meanwhile)
    }
}

/**
 * Shows the stored project [id] with [content] once it loads, the load failure with its retry
 * through [failed], or calls [onDeleted] when it is no longer stored. It follows the project in a
 * ViewModel of the entry it is composed in.
 */
@Composable
internal fun OpenedProject(
    id: ProjectId,
    onDeleted: () -> Unit,
    failed: @Composable (onRetry: () -> Unit) -> Unit = {
        ProjectLoadFailure(it, Modifier.padding(16.dp))
    },
    content: @Composable (Project) -> Unit,
) {
    val opened = hiltViewModel<OpenedProjectViewModel, OpenedProjectViewModel.Factory> {
        it.create(ProjectKey(id))
    }
    val state by opened.state.collectAsStateWithLifecycle()
    when (val load = state) {
        LoadResult.Loading -> Unit

        is LoadResult.Error -> failed(opened::retry)

        is LoadResult.Success -> when (val project = load.value) {
            null -> LaunchedEffect(Unit) { onDeleted() }
            else -> content(project)
        }
    }
}
