package com.almeidajr.molda.project.ui

import androidx.annotation.StringRes
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
import com.almeidajr.molda.project.navigation.ProjectEditorGuideKey
import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.project.navigation.ProjectGuideKey
import com.almeidajr.molda.project.navigation.ProjectKey
import com.almeidajr.molda.project.navigation.ProjectsKey
import com.almeidajr.molda.project.navigation.RenameProjectKey
import com.almeidajr.molda.project.navigation.SaveProjectKey
import com.almeidajr.molda.ui.adaptive.LocalIsTwoPane
import com.almeidajr.molda.ui.adaptive.ReadablePane
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
        OpenedProject(key.id, onClose = { navigator.remove(key) }) { project ->
            ProjectScreen(
                project,
                // Beside the list the project is a pane, closed by choosing from the list or Back.
                onUp = if (LocalIsTwoPane.current) null else navigator::back,
                onEdit = { navigator.push(ProjectEditorKey(project.id, project.snapshot)) },
                // Deleting the open project closes it: OpenedProject no longer finds it.
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
    ClosesWhenDone(state, R.string.project_saved, onShowSnackbar) { savedId ->
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

/** Opens the stored project [id] on Projetos, in place of the editor and any project open. */
internal fun Navigator.showProject(id: ProjectId) {
    removeAll {
        it is ProjectEditorKey || it is ProjectEditorGuideKey || it is ProjectKey ||
            it is ProjectGuideKey
    }
    push(ProjectKey(id), root = ProjectsKey)
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
    LaunchedEffect(state.outcome) {
        when (state.outcome) {
            ProjectUpdateOutcome.Updated -> {
                onShowSnackbar(R.string.project_updated)
                navigator.showProject(id)
            }

            ProjectUpdateOutcome.Failed -> onShowSnackbar(R.string.project_update_failed)

            null -> return@LaunchedEffect
        }
        update.outcomeShown()
    }
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
    ClosesWhenDone(state, R.string.project_renamed, onShowSnackbar) { navigator.remove(key) }
    OpenedProject(
        key.id,
        onClose = { navigator.remove(key) },
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
    ClosesWhenDone(state, R.string.project_deleted, onShowSnackbar) { navigator.remove(key) }
    OpenedProject(
        key.id,
        onClose = { navigator.remove(key) },
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

/**
 * Once the dialog's command in [state] is done, shows [message] and passes its outcome to
 * [onDone], which closes the dialog.
 */
@Composable
private fun <T : Any> ClosesWhenDone(
    state: ProjectCommandState<T>,
    @StringRes message: Int,
    onShowSnackbar: (Int) -> Unit,
    onDone: (T) -> Unit,
) {
    val outcome = state.outcome
    LaunchedEffect(outcome) {
        if (outcome == null) return@LaunchedEffect
        onShowSnackbar(message)
        onDone(outcome)
    }
}

@Composable
private fun ProjectList(navigator: Navigator) {
    val list = hiltViewModel<ProjectListViewModel>()
    val state by list.state.collectAsStateWithLifecycle()
    val projetos = navigator.state.stackOf(ProjectsKey)
    val isTwoPane = LocalIsTwoPane.current
    ProjectsScreen(
        state,
        list::load,
        onSeePatterns = { navigator.select(PatternCatalogKey) },
        onAbout = { navigator.push(AboutKey) },
        onOpen = { id ->
            if (projetos.last() != ProjectKey(id)) {
                navigator.removeAll { it is ProjectKey || it is ProjectGuideKey }
                navigator.push(ProjectKey(id))
            }
        },
        selection = { id ->
            when {
                !isTwoPane -> CatalogSelection.None
                projetos.last() == ProjectKey(id) -> CatalogSelection.Selected
                else -> CatalogSelection.Unselected
            }
        },
        onRename = { navigator.push(RenameProjectKey(it.id)) },
        onDelete = { navigator.push(DeleteProjectKey(it.id)) },
    )
}

/**
 * Shows the stored project [id] with [content] once it loads, the load failure with its retry
 * through [failed], or closes with [onClose] when it is no longer stored. It follows the project in
 * a ViewModel of the entry it is composed in.
 */
@Composable
internal fun OpenedProject(
    id: ProjectId,
    onClose: () -> Unit,
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
            null -> LaunchedEffect(Unit) { onClose() }
            else -> content(project)
        }
    }
}
