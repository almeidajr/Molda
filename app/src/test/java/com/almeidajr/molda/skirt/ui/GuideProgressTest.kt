package com.almeidajr.molda.skirt.ui

import androidx.compose.runtime.saveable.SaverScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.R
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment

// Robolectric resolves the step bar's copy from the app's string resources.
@RunWith(AndroidJUnit4::class)
class GuideProgressTest {
    private val text: (Int) -> String = RuntimeEnvironment.getApplication()::getString

    @Test
    fun guideProgress_firstStep_offersNextButNotPrevious() {
        // When
        val first = GuideProgress()

        // Then
        assertEquals(FoldingStep.Preparation, first.step)
        assertFalse(first.canGoBack)
        assertFalse(first.isLastStep)
        assertEquals("Próximo", text(first.forwardLabel))
        assertEquals(first, first.previous())
    }

    @Test
    fun guideProgress_lastStep_offersPreviousAndConcluirThatFinishes() {
        // When
        val last = GuideProgress(FoldingStep.Unfolding)

        // Then
        assertTrue(last.canGoBack)
        assertTrue(last.isLastStep)
        assertEquals("Concluir", text(last.forwardLabel))
        assertNull(last.next())
    }

    @Test
    fun nextAndPrevious_fromFirstStep_walkEveryStepWithItsNumber() {
        // When
        val forward = generateSequence(GuideProgress()) { it.next() }.toList()

        // Then
        assertEquals(FoldingStep.entries, forward.map { it.step })
        assertEquals((1..6).toList(), forward.map { it.stepNumber })
        assertEquals(forward.dropLast(1), forward.drop(1).map { it.previous() })
    }

    @Test
    fun nextAndPrevious_afterPausing_playTheNewStep() {
        // Given
        val paused = GuideProgress(FoldingStep.FirstFold).togglePause()

        // When
        val moved = listOfNotNull(paused.next(), paused.previous())

        // Then
        assertTrue(paused.isPaused)
        assertEquals(listOf(FoldingStep.SecondFold, FoldingStep.Preparation), moved.map { it.step })
        assertTrue(moved.all { it.isPlaying(motionEnabled = true) })
    }

    @Test
    fun togglePause_twice_pausesThenPlays() {
        // When
        val paused = GuideProgress().togglePause()

        // Then
        assertFalse(paused.isPlaying(motionEnabled = true))
        assertEquals(R.drawable.ic_play_arrow, paused.badgeIcon)
        assertTrue(paused.togglePause().isPlaying(motionEnabled = true))
        assertEquals(R.drawable.ic_pause, paused.togglePause().badgeIcon)
    }

    @Test
    fun motionChanged_whilePaused_clearsThePause() {
        // When
        val changed = GuideProgress(FoldingStep.Marking).togglePause().motionChanged()

        // Then
        assertEquals(GuideProgress(FoldingStep.Marking), changed)
    }

    @Test
    fun isPlaying_withMotionDisabled_isFalse() {
        // When/Then
        assertFalse(GuideProgress().isPlaying(motionEnabled = false))
    }

    @Test
    fun isPausable_byMotionDrawingAndFile_needsAllThree() {
        // When/Then
        assertTrue(isPausable(motionEnabled = true, DrawingLoad.Drawn, isFileLoaded = true))
        assertFalse(isPausable(motionEnabled = false, DrawingLoad.Drawn, isFileLoaded = true))
        assertFalse(isPausable(motionEnabled = true, DrawingLoad.Failed, isFileLoaded = true))
        assertFalse(isPausable(motionEnabled = true, drawing = null, isFileLoaded = true))
        assertFalse(isPausable(motionEnabled = true, DrawingLoad.Drawn, isFileLoaded = false))
    }

    @Test
    fun guideProgressSaver_pausedStep_restoresItPlaying() {
        // Given
        val scope = SaverScope { true }
        val paused = GuideProgress(FoldingStep.Cutting).togglePause()

        // When
        val saved = with(GuideProgressSaver) { scope.save(paused) }

        // Then
        assertEquals(GuideProgress(FoldingStep.Cutting), saved?.let(GuideProgressSaver::restore))
    }

    @Test
    fun guideProgressSaver_everyStep_restoresIt() {
        // Given
        val scope = SaverScope { true }

        FoldingStep.entries.map(::GuideProgress).forEach { progress ->
            // When
            val saved = with(GuideProgressSaver) { scope.save(progress) }

            // Then
            assertEquals(progress, saved?.let(GuideProgressSaver::restore))
        }
    }

    @Test
    fun guideProgressSaver_unknownPosition_restoresFirstStep() {
        // When/Then
        assertEquals(GuideProgress(), GuideProgressSaver.restore(FoldingStep.entries.size))
    }
}
