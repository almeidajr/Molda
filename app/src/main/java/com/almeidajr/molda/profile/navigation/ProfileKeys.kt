package com.almeidajr.molda.profile.navigation

import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.profile.domain.ProfileId
import java.util.UUID
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

/**
 * The open profile editor. Each opening has its own [opening], so its entry gets a new ViewModel:
 * reopening right after a discard never brings the discarded draft back.
 */
@Serializable
internal data class ProfileEditorKey(val profile: EditedProfile, val opening: String) : NavKey {
    constructor(profile: EditedProfile) : this(profile, newOpening())
}

/**
 * A new profile's editor, opened from the picker on [resultKey]: it sends the profile it creates
 * there. Each opening has its own [opening], as [ProfileEditorKey] has.
 */
@Serializable
internal data class PickerProfileEditorKey(val resultKey: String, val opening: String) : NavKey {
    constructor(resultKey: String) : this(resultKey, newOpening())
}

/** The dialog that confirms deleting the stored profile [id], over the stack that opened it. */
@Serializable
internal data class DeleteProfileKey(val id: ProfileId) : NavKey

private fun newOpening() = UUID.randomUUID().toString()

/**
 * The profile picker sheet over a calculator, which sends the chosen profile to [resultKey]: the
 * key that calculator listens on, unique to it.
 */
@Serializable
internal data class ProfilePickerKey(val resultKey: String) : NavKey

internal val NavKey.isProfileEditor: Boolean
    get() = this is ProfileEditorKey || this is PickerProfileEditorKey
