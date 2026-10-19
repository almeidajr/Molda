package com.almeidajr.molda.skirt.ui

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

// Robolectric resolves the step bar's copy from the app's string resources.
@RunWith(RobolectricTestRunner::class)
class GuideProgressTest {
    private val text: (Int) -> String = RuntimeEnvironment.getApplication()::getString

    @Test
    fun firstStep_offersNextButNotPrevious() {
        val first = GuideProgress()

        assertEquals(FoldingStep.Preparation, first.step)
        assertFalse(first.canGoBack)
        assertFalse(first.isLastStep)
        assertEquals("Próximo", text(first.forwardLabel))
        assertEquals(first, first.previous())
    }

    @Test
    fun lastStep_offersPreviousAndConcluirThatFinishes() {
        val last = GuideProgress(FoldingStep.Unfolding)

        assertTrue(last.canGoBack)
        assertTrue(last.isLastStep)
        assertEquals("Concluir", text(last.forwardLabel))
        assertNull(last.next())
    }

    @Test
    fun nextAndPrevious_walkEveryStepWithItsPosition() {
        val forward = generateSequence(GuideProgress()) { it.next() }.toList()

        assertEquals(FoldingStep.entries, forward.map { it.step })
        assertEquals((1..6).toList(), forward.map { it.stepNumber })
        assertEquals(forward.dropLast(1), forward.drop(1).map { it.previous() })
    }

    @Test
    fun saver_restoresEveryStep() {
        val scope = SaverScope { true }

        FoldingStep.entries.map(::GuideProgress).forEach { progress ->
            val saved = with(GuideProgressSaver) { scope.save(progress) }

            assertEquals(progress, saved?.let(GuideProgressSaver::restore))
        }
    }

    @Test
    fun saver_withUnknownPosition_restoresNothing() {
        assertNull(GuideProgressSaver.restore(FoldingStep.entries.size))
    }
}
