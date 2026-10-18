package com.almeidajr.molda.skirt.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.saveable.Saver
import com.almeidajr.molda.R

/** The fabric-folding tutorial's steps in order, each drawn by one Rive artboard. */
enum class FoldingStep(@param:StringRes val title: Int, val artboard: String) {
    Preparation(R.string.tutorial_step_preparation, "Preparation"),
    FirstFold(R.string.tutorial_step_first_fold, "First fold"),
    SecondFold(R.string.tutorial_step_second_fold, "Second fold"),
    Marking(R.string.tutorial_step_marking, "Marking"),
    Cutting(R.string.tutorial_step_cutting, "Cutting"),
    Unfolding(R.string.tutorial_step_unfolding, "Unfolding"),
}

/** Where the tutorial player is, and what its fixed step bar offers from there. */
data class TutorialProgress(val step: FoldingStep = FoldingStep.Preparation) {
    val canGoBack: Boolean get() = step.ordinal > 0

    /** On the last step the forward action is Concluir, which finishes the tutorial. */
    val isLastStep: Boolean get() = step == FoldingStep.entries.last()

    /** The step's 1-based position among [FoldingStep.entries]. */
    val stepNumber: Int get() = step.ordinal + 1

    @get:StringRes
    val forwardLabel: Int
        get() = if (isLastStep) R.string.tutorial_finish else R.string.tutorial_next

    fun previous(): TutorialProgress =
        TutorialProgress(FoldingStep.entries[(step.ordinal - 1).coerceAtLeast(0)])

    /** The following step, or null when Concluir finishes the tutorial. */
    fun next(): TutorialProgress? =
        FoldingStep.entries.getOrNull(step.ordinal + 1)?.let(::TutorialProgress)
}

/** Saves the step as its position so it survives rotation and process recreation. */
val TutorialProgressSaver: Saver<TutorialProgress, Int> = Saver(
    save = { it.step.ordinal },
    // An unknown position restores nothing, so the player starts from the first step.
    restore = { FoldingStep.entries.getOrNull(it)?.let(::TutorialProgress) },
)
