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
import com.almeidajr.molda.project.navigation.ProjectEditorGuideKey
import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.project.navigation.ProjectGuideKey
import com.almeidajr.molda.project.navigation.ProjectKey
import com.almeidajr.molda.project.navigation.ProjectsKey
import com.almeidajr.molda.ui.adaptive.LocalIsTwoPane
import com.almeidajr.molda.ui.adaptive.ReadablePane
import com.almeidajr.molda.ui.navigation.CatalogSelection
import com.almeidajr.molda.ui.navigation.Navigator

/**
 * Projetos: the saved projects beside the open one, whose pattern draft [savedResult] shows. The
 * editor and guides opened from a project belong to its pattern draft, so the app wires them.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.projectSection(
    navigator: Navigator,
    projects: ProjectsViewModel,
    onEdit: (Project) -> Unit,
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
                onEdit = { onEdit(project) },
                // Deleting the open project closes it: OpenedProject no longer finds it.
                onRename = { projects.startRenaming(project) },
                onDelete = { projects.startDeleting(project) },
            ) {
                savedResult(project)
            }
        }
    }
}

/**
 * Shows the project that a save from the editor stored in place of the editor, once the list
 * holds it: a new project's insert returns before the list emits it.
 */
@Composable
internal fun ShowSavedProject(navigator: Navigator, projects: ProjectsViewModel) {
    val state by projects.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.projectToShow, state.projects) {
        state.projectToShow?.takeIf { id -> state.projects.any { it.id == id } }?.let { id ->
            navigator.removeAll {
                it is ProjectEditorKey || it == ProjectEditorGuideKey || it is ProjectKey ||
                    it is ProjectGuideKey
            }
            navigator.push(ProjectKey(id), root = ProjectsKey)
            projects.onProjectShown()
        }
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
        onRename = projects::startRenaming,
        onDelete = projects::startDeleting,
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
