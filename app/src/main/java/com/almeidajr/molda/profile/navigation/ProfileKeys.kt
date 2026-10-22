package com.almeidajr.molda.profile.navigation

import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.profile.domain.ProfileId
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

/** The profile picker sheet over a calculator. */
@Serializable
internal data object ProfilePickerKey : NavKey

internal val NavKey.isProfileEditor: Boolean
    get() = this is ProfileEditorKey || this == PickerProfileEditorKey
