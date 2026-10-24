package com.almeidajr.molda

import com.almeidajr.molda.profile.navigation.isProfileEditor
import com.almeidajr.molda.profile.navigation.openPickerEditor
import com.almeidajr.molda.profile.navigation.openProfileEditor
import com.almeidajr.molda.project.navigation.closeProjectEditor
import com.almeidajr.molda.skirt.navigation.SkirtCalculatorKey
import com.almeidajr.molda.ui.navigation.Navigator

/**
 * Carries out [transition], decided in MoldaTransitions.kt, on the top-level back stacks. An
 * editor's ViewModel leaves with its key.
 */
internal fun Navigator.carryOut(transition: Transition) {
    when (transition) {
        Transition.None -> Unit

        is Transition.Select -> select(transition.root)

        Transition.Back -> back()

        is Transition.AskToDiscard -> push(DiscardChangesKey(transition.afterDiscard))

        // The selected stack's profile editor; the picker's returns to the picker below it.
        Transition.FinishProfileEditor -> state.current.lastOrNull {
            it.isProfileEditor
        }?.let(::remove)

        Transition.CloseProjectEditor -> closeProjectEditor()

        // Popping to the calculator also closes the picker and its editor above it.
        Transition.ShowCalculator -> popTo(SkirtCalculatorKey)

        is Transition.OpenProfileEditor ->
            openProfileEditor(transition.profile, transition.discardsDraft)

        is Transition.OpenPickerEditor -> openPickerEditor(transition.resultKey)
    }
}
