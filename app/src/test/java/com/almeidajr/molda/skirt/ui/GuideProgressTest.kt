package com.almeidajr.molda.skirt.ui

import androidx.compose.runtime.saveable.SaverScope
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

// Robolectric resolves the step bar's copy from the app's string resources.
@RunWith(RobolectricTestRunner::class)
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
    fun guideProgressSaver_unknownPosition_restoresNothing() {
        // When/Then
        assertNull(GuideProgressSaver.restore(FoldingStep.entries.size))
    }
}
