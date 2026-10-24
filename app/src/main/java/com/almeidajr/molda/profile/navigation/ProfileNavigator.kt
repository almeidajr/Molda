package com.almeidajr.molda.profile.navigation

import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.ui.navigation.Navigator
import com.almeidajr.molda.ui.navigation.TopLevelBackStacks

/**
 * Opens the Perfis editor for [profile], which shows one beside its list, unless it already shows
 * that profile; with [discardsDraft], a confirmed discard, it always gets a fresh one. The
 * pickers' editors stay independent.
 */
internal fun Navigator.openProfileEditor(profile: EditedProfile, discardsDraft: Boolean = false) {
    val open = state.stackOf(ProfileManagementKey).last()
    if (open is ProfileEditorKey && open.profile == profile && !discardsDraft) return
    removeAll { it is ProfileEditorKey }
    push(ProfileEditorKey(profile), root = ProfileManagementKey)
}

/**
 * Opens a new profile's editor over the picker on [resultKey], replacing one already open for that
 * picker, whose draft a confirmed discard drops: the picker never has two editors.
 */
internal fun Navigator.openPickerEditor(resultKey: String) {
    removeAll { it is PickerProfileEditorKey && it.resultKey == resultKey }
    push(PickerProfileEditorKey(resultKey))
}

/** Closes the picker's [editor] together with the picker it was opened from. */
internal fun Navigator.closePickerEditor(editor: PickerProfileEditorKey) =
    removeAll { it == editor || it == ProfilePickerKey(editor.resultKey) }

/** Closes every editor of the deleted profile [id]; returns whether one was open. */
internal fun Navigator.closeEditorOf(id: ProfileId): Boolean {
    val isEditorOf: (NavKey) -> Boolean =
        { it is ProfileEditorKey && it.profile == EditedProfile.Existing(id) }
    val wasOpen = state.stacks.any { it.any(isEditorOf) }
    removeAll(isEditorOf)
    return wasOpen
}

/**
 * Whether Perfis shows its new-profile action: over its list or an editor, but not over another
 * screen such as About, nor an editor that fills the window, which is any editor unless the layout
 * is [isTwoPane].
 */
internal fun TopLevelBackStacks.showsProfileCreateAction(isTwoPane: Boolean): Boolean =
    selected == ProfileManagementKey &&
        current.last().let { it == ProfileManagementKey || it is ProfileEditorKey } &&
        (isTwoPane || stackOf(ProfileManagementKey).none { it is ProfileEditorKey })
