package com.almeidajr.molda.skirt.ui

import androidx.compose.runtime.saveable.Saver

/** The fabric-folding tutorial's steps in order, each drawn by one Rive artboard. */
enum class FoldingStep(val title: String, val artboard: String) {
    Preparation("Preparar o tecido", "Preparation"),
    FirstFold("Primeira dobra", "First fold"),
    SecondFold("Segunda dobra", "Second fold"),
    Marking("Marcar os raios", "Marking"),
    Cutting("Cortar os arcos", "Cutting"),
    Unfolding("Desdobrar o círculo", "Unfolding"),
}

/** Where the tutorial player is, and what its fixed step bar offers from there. */
data class TutorialProgress(val step: FoldingStep = FoldingStep.Preparation) {
    val canGoBack: Boolean get() = step.ordinal > 0

    /** On the last step the forward action is Concluir, which finishes the tutorial. */
    val isLastStep: Boolean get() = step == FoldingStep.entries.last()

    val position: String get() = "Etapa ${step.ordinal + 1} de ${FoldingStep.entries.size}"

    val forwardLabel: String get() = if (isLastStep) "Concluir" else "Próximo"

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
