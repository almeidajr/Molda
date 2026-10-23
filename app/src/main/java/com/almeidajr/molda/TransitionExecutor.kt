package com.almeidajr.molda

import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.profile.navigation.PickerProfileEditorKey
import com.almeidajr.molda.profile.navigation.ProfileEditorKey
import com.almeidajr.molda.profile.navigation.ProfileManagementKey
import com.almeidajr.molda.profile.navigation.isProfileEditor
import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.skirt.navigation.SkirtCalculatorKey
import com.almeidajr.molda.ui.navigation.EditorKind
import com.almeidajr.molda.ui.navigation.EditorStatusHolder
import com.almeidajr.molda.ui.navigation.Navigator

/**
 * Carries out [transition], decided in MoldaTransitions.kt, on the top-level back stacks; [editors]
 * answer which profile editor finishing closes. An editor's ViewModel leaves with its key.
 */
internal fun Navigator.carryOut(transition: Transition, editors: EditorStatusHolder) {
    when (transition) {
        Transition.None -> Unit

        is Transition.Select -> select(transition.root)

        Transition.Back -> back()

        is Transition.AskToDiscard -> push(DiscardChangesKey(transition.afterDiscard))

        // The selected stack's profile editor; the picker's returns to the picker below it.
        Transition.FinishProfileEditor -> editors.profileEditorOn(state.current)?.let(::remove)

        Transition.CloseProjectEditor -> removeAll { it is ProjectEditorKey }

        // Popping to the calculator also closes the picker and its editor above it.
        Transition.ShowCalculator -> popTo(SkirtCalculatorKey)

        is Transition.OpenProfileEditor -> openProfileEditor(transition)

        is Transition.OpenPickerEditor -> openPickerEditor(transition.resultKey)
    }
}

/**
 * Opens a new profile's editor over the picker on [resultKey], replacing one already open for that
 * picker, whose draft a confirmed discard drops: the picker never has two editors.
 */
private fun Navigator.openPickerEditor(resultKey: String) {
    removeAll { it is PickerProfileEditorKey && it.resultKey == resultKey }
    push(PickerProfileEditorKey(resultKey))
}

/**
 * The profile editor that finishing closes on [stack]: the one the holder answers, or the topmost
 * profile editor there in the frame before a just-pushed one first reports.
 */
private fun EditorStatusHolder.profileEditorOn(stack: List<NavKey>): NavKey? = editorOn(stack)
    ?.takeIf { it.report.kind == EditorKind.Profile }?.key
    ?: stack.lastOrNull { it.isProfileEditor }

/**
 * Opens the Perfis editor, which shows one beside its list, unless it already shows that profile;
 * a confirmed discard always gets a fresh one. The pickers' editors stay independent.
 */
private fun Navigator.openProfileEditor(transition: Transition.OpenProfileEditor) {
    val open = state.stackOf(ProfileManagementKey).last()
    val isOpen = open is ProfileEditorKey && open.profile == transition.profile
    if (isOpen && !transition.discardsDraft) return
    removeAll { it is ProfileEditorKey }
    push(ProfileEditorKey(transition.profile), root = ProfileManagementKey)
}
