package com.almeidajr.molda.project.navigation

import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.project.domain.ProjectId
import kotlinx.serialization.Serializable

/** The Projetos root. */
@Serializable
internal data object ProjectsKey : NavKey

/** A saved project, opened read-only from Projetos. */
@Serializable
internal data class ProjectKey(val id: ProjectId) : NavKey

/** The fabric-folding guide on a saved project's radii, opened from that project. */
@Serializable
internal data class ProjectGuideKey(val id: ProjectId) : NavKey

/** A saved project loaded into the skirt calculator, on the Projetos stack. */
@Serializable
internal data class ProjectEditorKey(val id: ProjectId) : NavKey

/** The result key this editor listens on for a profile from its picker; unique to its project. */
internal val ProjectEditorKey.profileResultKey: String get() = "profile/project/${id.value}"

/** The fabric-folding guide on the radii calculated while editing a project. */
@Serializable
internal data object ProjectEditorGuideKey : NavKey

/** The dialog that confirms deleting the saved project [id], over the stack that opened it. */
@Serializable
internal data class DeleteProjectKey(val id: ProjectId) : NavKey
