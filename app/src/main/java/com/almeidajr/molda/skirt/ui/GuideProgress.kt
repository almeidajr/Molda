package com.almeidajr.molda.skirt.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.saveable.Saver
import com.almeidajr.molda.R

/** The fabric-folding guide's steps in order, each drawn by one Rive artboard. */
enum class FoldingStep(@param:StringRes val title: Int, val artboard: String) {
    Preparation(R.string.guide_step_preparation, "Preparation"),
    FirstFold(R.string.guide_step_first_fold, "First fold"),
    SecondFold(R.string.guide_step_second_fold, "Second fold"),
    Marking(R.string.guide_step_marking, "Marking"),
    Cutting(R.string.guide_step_cutting, "Cutting"),
    Unfolding(R.string.guide_step_unfolding, "Unfolding"),
}

/** How a step's drawing finished loading: drawn, or failed with its error sentence shown. */
internal enum class DrawingLoad { Drawn, Failed }

/**
 * The guide's playback: the step the player shows, whether the sewist paused its drawing, and
 * what its fixed step bar offers from there.
 */
data class GuideProgress(
    val step: FoldingStep = FoldingStep.Preparation,
    val isPaused: Boolean = false,
) {
    val canGoBack: Boolean get() = step.ordinal > 0

    /** On the last step the forward action is Concluir, which finishes the guide. */
    val isLastStep: Boolean get() = step == FoldingStep.entries.last()

    /** The step's 1-based position among [FoldingStep.entries]. */
    val stepNumber: Int get() = step.ordinal + 1

    @get:StringRes
    val forwardLabel: Int
        get() = if (isLastStep) R.string.guide_finish else R.string.guide_next

    /** The badge's glyph: what a tap on the drawing does, pause or play. */
    @get:DrawableRes
    val badgeIcon: Int
        get() = if (isPaused) R.drawable.ic_play_arrow else R.drawable.ic_pause

    /** Whether the drawing moves: system motion is on and the sewist has not paused it. */
    fun isPlaying(motionEnabled: Boolean): Boolean = motionEnabled && !isPaused

    /** The earlier step, playing. */
    fun previous(): GuideProgress =
        GuideProgress(FoldingStep.entries[(step.ordinal - 1).coerceAtLeast(0)])

    /** The following step, playing, or null when Concluir finishes the guide. */
    fun next(): GuideProgress? =
        FoldingStep.entries.getOrNull(step.ordinal + 1)?.let(::GuideProgress)

    /** A tap on the drawing: pauses it, or continues it when paused. */
    fun togglePause(): GuideProgress = copy(isPaused = !isPaused)

    /** System motion was switched on or off: the step plays again, so it never stays stuck. */
    fun motionChanged(): GuideProgress = copy(isPaused = false)
}

/** Whether the drawing is a pause control: motion is on, it drew, and the guide file loaded. */
internal fun isPausable(motionEnabled: Boolean, drawing: DrawingLoad?, isFileLoaded: Boolean) =
    motionEnabled && drawing == DrawingLoad.Drawn && isFileLoaded

/**
 * Saves the step as its position so it survives rotation and process recreation; the pause does
 * not, so a recreated step plays. An unknown position restores the first step.
 */
val GuideProgressSaver: Saver<GuideProgress, Int> = Saver(
    save = { it.step.ordinal },
    restore = { GuideProgress(FoldingStep.entries.getOrElse(it) { FoldingStep.Preparation }) },
)
