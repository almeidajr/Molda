package com.almeidajr.molda.project.navigation

import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.ui.navigation.Navigator

/** Whether Projetos shows the project [id] on top. */
internal fun Navigator.isShowingProject(id: ProjectId): Boolean =
    state.stackOf(ProjectsKey).last() == ProjectKey(id)

/** Opens the project [id] from the Projetos list, in place of any project and guide open. */
internal fun Navigator.openProject(id: ProjectId) {
    if (isShowingProject(id)) return
    removeAll { it is ProjectKey || it is ProjectGuideKey }
    push(ProjectKey(id), root = ProjectsKey)
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
 * Closes every entry showing the project [id]: opened, its guide, its editor with the editor's
 * guide, and its dialogs; returns whether one was open.
 */
internal fun Navigator.closeProject(id: ProjectId): Boolean {
    val isEditorOf: (NavKey) -> Boolean = { it is ProjectEditorKey && it.id == id }
    val hasEditor = state.stacks.any { it.any(isEditorOf) }
    val keys =
        setOf(ProjectKey(id), ProjectGuideKey(id), RenameProjectKey(id), DeleteProjectKey(id))
    val showsProject: (NavKey) -> Boolean = { key ->
        when (key) {
            is ProjectEditorKey -> isEditorOf(key)

            // One project is edited at a time, so an editor's guide is that editor's.
            is ProjectEditorGuideKey -> hasEditor

            else -> key in keys
        }
    }
    return state.stacks.any { it.any(showsProject) }.also { removeAll(showsProject) }
}

/** Whether the delete dialog of the project [id] is open; it reports that project's deletion. */
internal fun Navigator.isDeletingProject(id: ProjectId): Boolean =
    state.stacks.any { DeleteProjectKey(id) in it }

/** Closes the project editor and its guide, back to what was under them. */
internal fun Navigator.closeProjectEditor() =
    removeAll { it is ProjectEditorKey || it is ProjectEditorGuideKey }
