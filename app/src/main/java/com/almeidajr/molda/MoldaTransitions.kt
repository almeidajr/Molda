package com.almeidajr.molda

import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.profile.navigation.EditedProfile
import com.almeidajr.molda.profile.navigation.isProfileEditor
import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.project.navigation.ProjectsKey
import com.almeidajr.molda.ui.navigation.EditorKind
import kotlinx.serialization.Serializable

/** What confirming the discard of a dirty editor does next. */
@Serializable
internal sealed interface AfterDiscard {
    @Serializable
    data object CloseEditor : AfterDiscard

    @Serializable
    data class OpenEditor(val profile: EditedProfile) : AfterDiscard

    /** Opens a new profile's editor for the picker on [resultKey]. */
    @Serializable
    data class OpenPickerEditor(val resultKey: String) : AfterDiscard

    @Serializable
    data object CalculateOwn : AfterDiscard

    @Serializable
    data object CloseProjectEditor : AfterDiscard

    /** Re-selecting Projetos over a dirty project editor: back to its root. */
    @Serializable
    data object ShowProjects : AfterDiscard
}

/** Asks to discard the dirty editor, then does [afterDiscard]. */
@Serializable
internal data class DiscardChangesKey(val afterDiscard: AfterDiscard) : NavKey

/** Which editor [this] discards, which the discard dialog names. */
internal val AfterDiscard.discardedEditor: EditorKind
    get() = when (this) {
        AfterDiscard.CloseEditor,
        is AfterDiscard.OpenEditor,
        is AfterDiscard.OpenPickerEditor,
        AfterDiscard.CalculateOwn,
        -> EditorKind.Profile

        AfterDiscard.CloseProjectEditor, AfterDiscard.ShowProjects -> EditorKind.Project
    }

/**
 * The unsaved and in-flight work of the editor an action concerns, which decides what a navigation
 * request may do.
 */
internal sealed interface EditorStatus {
    data object None : EditorStatus

    data class Profile(val isDirty: Boolean = false, val isSaving: Boolean = false) : EditorStatus

    data class Project(val isDirty: Boolean = false, val isUpdating: Boolean = false) :
        EditorStatus
}

/** What a navigation request does to the back stacks and the editors. */
internal sealed interface Transition {
    data object None : Transition

    /** Switches to [root]'s destination, or returns it to its root when already selected. */
    data class Select(val root: NavKey) : Transition

    data object Back : Transition

    data class AskToDiscard(val afterDiscard: AfterDiscard) : Transition

    /** Drops the profile draft and closes the picker's editor. */
    data object FinishProfileEditor : Transition

    data object CloseProjectEditor : Transition

    /** Shows the Moldes calculator, closing what is above it, the picker's editor included. */
    data object ShowCalculator : Transition

    data class OpenProfileEditor(val profile: EditedProfile, val discardsDraft: Boolean = false) :
        Transition

    /**
     * Opens a new profile's editor above the picker on [resultKey], in place of any editor already
     * open for that picker.
     */
    data class OpenPickerEditor(val resultKey: String) : Transition
}

/** Selecting [root] while [selected] shows [stack]. */
internal fun selectDestination(
    root: NavKey,
    selected: NavKey,
    stack: List<NavKey>,
    status: EditorStatus,
): Transition {
    val top = stack.last()
    return when {
        root != selected -> Transition.Select(root)

        // An open discard dialog is already asking; popping it would orphan the draft.
        top is DiscardChangesKey -> Transition.None

        // An open editor closes the usual way when its destination is re-selected.
        top.isProfileEditor -> closeOpenProfileEditor(status)

        // Returning to the root would drop the project editor's unsaved changes.
        status is EditorStatus.Project && status.isDirty ->
            if (status.isUpdating) {
                Transition.None
            } else {
                Transition.AskToDiscard(AfterDiscard.ShowProjects)
            }

        else -> Transition.Select(root)
    }
}

/** Back while the selected destination shows [stack]. */
internal fun goBack(stack: List<NavKey>, status: EditorStatus): Transition {
    val top = stack.last()
    return when {
        top is ProjectEditorKey -> closeProjectEditor(status)
        top.isProfileEditor -> closeOpenProfileEditor(status)
        else -> Transition.Back
    }
}

/**
 * Leaving the example guide for the calculator, once Moldes shows [stack]: it keeps the calculator
 * and its draft, closing what is open above it; the picker's editor closes the usual way first.
 */
internal fun calculateOwn(stack: List<NavKey>, status: EditorStatus): Transition = when {
    stack.last() is DiscardChangesKey -> Transition.None

    status is EditorStatus.Profile && status.isDirty ->
        Transition.AskToDiscard(AfterDiscard.CalculateOwn)

    else -> Transition.ShowCalculator
}

/** Opening the editor of [profile], asking first when the open one has changes. */
internal fun requestProfileEditor(profile: EditedProfile, status: EditorStatus): Transition =
    requestEditor(
        status,
        open = Transition.OpenProfileEditor(profile),
        afterDiscard = AfterDiscard.OpenEditor(profile),
    )

/**
 * Opening a new profile's editor from the picker on [resultKey], asking first when the open one
 * has changes.
 */
internal fun requestPickerEditor(resultKey: String, status: EditorStatus): Transition =
    requestEditor(
        status,
        open = Transition.OpenPickerEditor(resultKey),
        afterDiscard = AfterDiscard.OpenPickerEditor(resultKey),
    )

/** Closing the profile editor, asking first when it has changes. */
internal fun cancelProfileEditor(status: EditorStatus): Transition =
    if (status is EditorStatus.Profile && status.isDirty) {
        Transition.AskToDiscard(AfterDiscard.CloseEditor)
    } else {
        Transition.FinishProfileEditor
    }

/** Closing the project editor, asking first when it has changes; not while it saves. */
internal fun closeProjectEditor(status: EditorStatus): Transition = when (status) {
    is EditorStatus.Project -> when {
        status.isUpdating -> Transition.None
        status.isDirty -> Transition.AskToDiscard(AfterDiscard.CloseProjectEditor)
        else -> Transition.CloseProjectEditor
    }

    EditorStatus.None, is EditorStatus.Profile -> Transition.CloseProjectEditor
}

/** What confirming the discard does: the request that asked, now dropping the draft. */
internal fun AfterDiscard.transition(): Transition = when (this) {
    AfterDiscard.CloseEditor -> Transition.FinishProfileEditor
    is AfterDiscard.OpenEditor -> Transition.OpenProfileEditor(profile, discardsDraft = true)
    is AfterDiscard.OpenPickerEditor -> Transition.OpenPickerEditor(resultKey)
    AfterDiscard.CalculateOwn -> Transition.ShowCalculator
    AfterDiscard.CloseProjectEditor -> Transition.CloseProjectEditor
    AfterDiscard.ShowProjects -> Transition.Select(ProjectsKey)
}

/**
 * A request to open an editor; ignored while the open profile editor saves. The controls that
 * open one are disabled then, and this guard covers the frame between the save starting and
 * them disabling, when a tap would replace the saving editor and drop its outcome.
 */
private fun requestEditor(
    status: EditorStatus,
    open: Transition,
    afterDiscard: AfterDiscard,
): Transition = when (status) {
    is EditorStatus.Profile -> when {
        status.isSaving -> Transition.None
        status.isDirty -> Transition.AskToDiscard(afterDiscard)
        else -> open
    }

    EditorStatus.None, is EditorStatus.Project -> open
}

/** Closing the profile editor on top, as Back and re-selecting do; not while it saves. */
private fun closeOpenProfileEditor(status: EditorStatus): Transition =
    if (status is EditorStatus.Profile && status.isSaving) {
        Transition.None
    } else {
        cancelProfileEditor(status)
    }
