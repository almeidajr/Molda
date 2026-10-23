package com.almeidajr.molda.profile.navigation

import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.project.domain.ProjectId
import kotlinx.serialization.Serializable

/** The Perfis root: profile management. */
@Serializable
internal data object ProfileManagementKey : NavKey

/** The profile an editor works on. */
@Serializable
internal sealed interface EditedProfile {
    @Serializable
    data object New : EditedProfile

    @Serializable
    data class Existing(val id: ProfileId) : EditedProfile
}

/** The open profile editor. */
@Serializable
internal data class ProfileEditorKey(val profile: EditedProfile) : NavKey

/** A new profile's editor, opened from the profile picker. */
@Serializable
internal data object PickerProfileEditorKey : NavKey

/** The calculator that asked for a profile: the picker sends the chosen one to [resultKey]. */
@Serializable
internal sealed interface ProfileRequester {
    /** The result key the requester listens on for a profile; unique to each requester. */
    val resultKey: String

    /** The calculator on Moldes. */
    @Serializable
    data object MoldesCalculator : ProfileRequester {
        override val resultKey get() = "profile/moldes"
    }

    /** The calculator editing the saved project [id]. */
    @Serializable
    data class ProjectEditor(val id: ProjectId) : ProfileRequester {
        override val resultKey get() = "profile/project/${id.value}"
    }
}

/** The profile picker sheet over the calculator [requester]. */
@Serializable
internal data class ProfilePickerKey(val requester: ProfileRequester) : NavKey

internal val NavKey.isProfileEditor: Boolean
    get() = this is ProfileEditorKey || this == PickerProfileEditorKey
