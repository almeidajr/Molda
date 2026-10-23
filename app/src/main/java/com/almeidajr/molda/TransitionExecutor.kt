package com.almeidajr.molda

import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.profile.navigation.isProfileEditor
import com.almeidajr.molda.profile.navigation.openPickerEditor
import com.almeidajr.molda.profile.navigation.openProfileEditor
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

        is Transition.OpenProfileEditor ->
            openProfileEditor(transition.profile, transition.discardsDraft)

        is Transition.OpenPickerEditor -> openPickerEditor(transition.resultKey)
    }
}

/**
 * The profile editor that finishing closes on [stack]: the one the holder answers, or the topmost
 * profile editor there in the frame before a just-pushed one first reports.
 */
private fun EditorStatusHolder.profileEditorOn(stack: List<NavKey>): NavKey? = editorOn(stack)
    ?.takeIf { it.report.kind == EditorKind.Profile }?.key
    ?: stack.lastOrNull { it.isProfileEditor }
