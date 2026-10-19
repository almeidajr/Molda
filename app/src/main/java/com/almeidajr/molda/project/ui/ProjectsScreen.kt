package com.almeidajr.molda.project.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R
import com.almeidajr.molda.project.domain.PatternDraftKind
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectDateFormat
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.ui.navigation.CatalogSelection
import com.almeidajr.molda.ui.navigation.MoldaTopAppBar
import java.time.ZoneId

/**
 * The Projetos root: saved projects, most recently changed first, or an empty state. A row opens
 * its project with [onOpen]; [selection] marks the one shown beside the list.
 */
@Composable
fun ProjectsScreen(
    state: ProjectsState,
    onRetry: () -> Unit,
    onSeePatterns: () -> Unit,
    onAbout: () -> Unit,
    onRename: (Project) -> Unit,
    onDelete: (Project) -> Unit,
    onOpen: (ProjectId) -> Unit,
    selection: (ProjectId) -> CatalogSelection,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        MoldaTopAppBar(stringResource(R.string.navigation_projects), onAbout = onAbout)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val loadError = state.loadError
                when {
                    loadError != null -> {
                        Text(stringResource(loadError), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.action_retry_load))
                        }
                    }

                    state.isLoading -> Text(stringResource(R.string.project_loading))

                    state.projects.isEmpty() -> EmptyProjects(onSeePatterns)

                    else -> state.projects.forEach { project ->
                        ProjectRow(
                            project,
                            selection(project.id),
                            onOpen = { onOpen(project.id) },
                            onRename = { onRename(project) },
                            onDelete = { onDelete(project) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyProjects(onSeePatterns: () -> Unit) {
    Icon(
        painterResource(R.drawable.ic_folder),
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        stringResource(R.string.project_empty_title),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        stringResource(R.string.project_empty_definition),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(onClick = onSeePatterns, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(stringResource(R.string.project_see_patterns))
    }
}

/**
 * One saved project, opened by a click: its name, its molde and the date it last changed, then
 * its actions. Beside the opened project ([selection] other than [CatalogSelection.None]) the
 * selected row fills with the secondary container and is exposed as selected.
 */
@Composable
private fun ProjectRow(
    project: Project,
    selection: CatalogSelection,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val isSelected = selection == CatalogSelection.Selected
    Surface(
        onClick = onOpen,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
            if (selection != CatalogSelection.None) selected = isSelected
        },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(project.name.text, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.project_summary,
                    stringResource(project.kind.label),
                    project.updatedAt.atZone(ZoneId.systemDefault()).format(ProjectDateFormat),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ProjectActions(project, onRename, onDelete)
        }
    }
}

/** The name of the pattern draft (molde) a project was saved from. */
internal val PatternDraftKind.label: Int
    @StringRes get() = when (this) {
        PatternDraftKind.FullCircleSkirt -> R.string.project_pattern_skirt
    }

/** The "Ações: <nome>" menu: rename, or delete after a confirmation. */
@Composable
fun ProjectActions(
    project: Project,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        TextButton(onClick = { expanded = true }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.action_menu_for, project.name.text))
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.project_rename)) },
                onClick = {
                    expanded = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.project_delete)) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}
