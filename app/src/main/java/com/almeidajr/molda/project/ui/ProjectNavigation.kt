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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.R
import com.almeidajr.molda.about.navigation.AboutKey
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
    projects: ProjectsViewModel,
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
        ProjectList(navigator, projects)
    }
    entry<ProjectKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
        OpenedProject(key.id, projects, onClose = { navigator.remove(key) }) { project ->
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
        SaveProjectEntry(key, navigator, projects, onShowSnackbar)
    }
    entry<RenameProjectKey>(metadata = windowOverlay()) { key ->
        RenameProjectEntry(key, navigator, projects, onShowSnackbar)
    }
    entry<DeleteProjectKey>(metadata = windowOverlay()) { key ->
        DeleteProjectEntry(key, navigator, projects, onShowSnackbar)
    }
}

@Composable
private fun SaveProjectEntry(
    key: SaveProjectKey,
    navigator: Navigator,
    projects: ProjectsViewModel,
    onShowSnackbar: (Int) -> Unit,
) {
    val state by projects.state.collectAsStateWithLifecycle()
    val isBusy = state.saving?.isBusy == true
    SaveProjectDialog(
        defaultName = projects.defaultName(key.snapshot, stringResource(key.snapshot.kind.label)),
        isBusy = isBusy,
        error = state.saving?.error,
        onSave = { name ->
            projects.save(name, key.snapshot) { id ->
                onShowSnackbar(R.string.project_saved)
                navigator.remove(key)
                if (key.showsSaved) navigator.showProject(id)
            }
        },
        onCancel = {
            if (!isBusy) {
                projects.cancelSaving()
                navigator.remove(key)
            }
        },
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
    projects: ProjectsViewModel,
    onShowSnackbar: (Int) -> Unit,
) {
    val state by projects.state.collectAsStateWithLifecycle()
    val renaming = state.renaming?.takeIf { it.id == key.id }
    val isBusy = renaming?.isBusy == true
    OpenedProject(key.id, projects, onClose = { navigator.remove(key) }) { project ->
        RenameProjectDialog(
            currentName = project.name.text,
            isBusy = isBusy,
            error = renaming?.error,
            onRename = { name ->
                projects.rename(key.id, name) {
                    onShowSnackbar(it)
                    navigator.remove(key)
                }
            },
            onCancel = {
                if (!isBusy) {
                    projects.cancelRenaming()
                    navigator.remove(key)
                }
            },
        )
    }
}

@Composable
private fun DeleteProjectEntry(
    key: DeleteProjectKey,
    navigator: Navigator,
    projects: ProjectsViewModel,
    onShowSnackbar: (Int) -> Unit,
) {
    val state by projects.state.collectAsStateWithLifecycle()
    val deleting = state.deleting?.takeIf { it.id == key.id }
    val isBusy = deleting?.isBusy == true
    OpenedProject(key.id, projects, onClose = { navigator.remove(key) }) { project ->
        DeleteProjectDialog(
            name = project.name.text,
            isBusy = isBusy,
            error = deleting?.error,
            onDelete = {
                projects.delete(key.id) {
                    onShowSnackbar(it)
                    navigator.remove(key)
                }
            },
            onCancel = {
                if (!isBusy) {
                    projects.cancelDeleting()
                    navigator.remove(key)
                }
            },
        )
    }
}

@Composable
private fun ProjectList(navigator: Navigator, projects: ProjectsViewModel) {
    val state by projects.state.collectAsStateWithLifecycle()
    val projetos = navigator.state.stackOf(ProjectsKey)
    val isTwoPane = LocalIsTwoPane.current
    ProjectsScreen(
        state,
        projects::load,
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
 * Shows the stored project [id] with [content] once projects load, or closes with [onClose] when
 * it is no longer stored.
 */
@Composable
internal fun OpenedProject(
    id: ProjectId,
    projects: ProjectsViewModel,
    onClose: () -> Unit,
    content: @Composable (Project) -> Unit,
) {
    val state by projects.state.collectAsStateWithLifecycle()
    val project = state.projects.find { it.id == id }
    when {
        project != null -> content(project)
        !state.isLoading && state.loadError == null -> LaunchedEffect(Unit) { onClose() }
    }
}
