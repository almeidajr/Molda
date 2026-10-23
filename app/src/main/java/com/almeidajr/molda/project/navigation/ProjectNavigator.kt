package com.almeidajr.molda.project.navigation

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

/** Closes the project editor and its guide, back to what was under them. */
internal fun Navigator.closeProjectEditor() =
    removeAll { it is ProjectEditorKey || it is ProjectEditorGuideKey }
