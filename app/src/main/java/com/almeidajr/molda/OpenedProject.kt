package com.almeidajr.molda

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.ui.ProjectsState

/**
 * Shows the stored project [id] with [content] once projects load, or closes with [onClose] when
 * it is no longer stored.
 */
@Composable
internal fun OpenedProject(
    id: ProjectId,
    state: ProjectsState,
    onClose: () -> Unit,
    content: @Composable (Project) -> Unit,
) {
    val project = state.projects.find { it.id == id }
    when {
        project != null -> content(project)
        !state.isLoading && state.loadError == null -> LaunchedEffect(Unit) { onClose() }
    }
}
