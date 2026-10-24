package com.almeidajr.molda.ui.command

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OutcomeEffectTest {
    @get:Rule
    val composeRule = createComposeRule()

    // The outcome a ViewModel holds until acknowledged, and what the effect did with it.
    private var outcome by mutableStateOf<Int?>(null)
    private val reactions = mutableListOf<Int>()
    private var acknowledgements = 0

    private fun acknowledge() {
        acknowledgements++
        outcome = null
    }

    @Test
    fun outcome_reactsOnceThenAcknowledges() {
        // Given
        composeRule.setContent { OutcomeEffect(outcome, ::acknowledge) { reactions += it } }

        // When
        composeRule.runOnIdle { outcome = 1 }

        // Then
        composeRule.runOnIdle {
            assertEquals(listOf(1), reactions)
            assertEquals(1, acknowledgements)
            assertNull(outcome)
        }
    }

    @Test
    fun outcome_afterTheReactionIsSwapped_runsTheLatestReaction() {
        // Given
        val firstReactions = mutableListOf<Int>()
        var reaction by mutableStateOf<(Int) -> Unit>({ firstReactions += it })
        composeRule.setContent { OutcomeEffect(outcome, ::acknowledge, reaction) }
        composeRule.runOnIdle { reaction = { reactions += it } }

        // When
        composeRule.runOnIdle { outcome = 1 }

        // Then
        composeRule.runOnIdle {
            assertEquals(emptyList(), firstReactions)
            assertEquals(listOf(1), reactions)
        }
    }

    @Test
    fun sameOutcome_afterAcknowledgement_reactsAgain() {
        // Given
        composeRule.setContent { OutcomeEffect(outcome, ::acknowledge) { reactions += it } }
        composeRule.runOnIdle { outcome = 1 }

        // When
        composeRule.runOnIdle { outcome = 1 }

        // Then
        composeRule.runOnIdle {
            assertEquals(listOf(1, 1), reactions)
            assertEquals(2, acknowledgements)
        }
    }

    @Test
    fun unacknowledgedOutcome_whenTheEffectReturns_reactsOnce() {
        // Given
        var isShown by mutableStateOf(true)
        composeRule.setContent {
            if (isShown) OutcomeEffect(outcome, ::acknowledge) { reactions += it }
        }
        composeRule.runOnIdle { isShown = false }
        composeRule.runOnIdle { outcome = 1 }

        // When
        composeRule.runOnIdle { isShown = true }

        // Then
        composeRule.runOnIdle {
            assertEquals(listOf(1), reactions)
            assertEquals(1, acknowledgements)
        }
    }
}
