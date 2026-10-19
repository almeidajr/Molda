package com.almeidajr.molda

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.almeidajr.molda.project.domain.MeasurementSource
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.project.ui.ProjectsState
import com.almeidajr.molda.skirt.ui.CalculatorState
import java.time.LocalDate

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

/** The calculated result as a project snapshot, or null before a successful calculation. */
internal fun CalculatorState.snapshot(): SkirtSnapshot? {
    val draft = draft
    val radii = radii
    if (draft == null || radii == null) return null
    val source = form.sourceProfileName?.let { name ->
        form.sourceMeasurementDate?.let { MeasurementSource(name, LocalDate.parse(it)) }
    }
    return SkirtSnapshot(draft, radii, source)
}
