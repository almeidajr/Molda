package com.almeidajr.molda

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.testing.October18Clock
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The calculator with the soft keyboard open, which only a device shows and hides. */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SkirtCalculatorTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Before
    fun openSkirtCalculator() {
        composeRule.launchApp(clock = October18Clock)
        composeRule.showSkirtCalculator()
    }

    @Test
    fun calculateButton_withKeyboardOpen_hidesKeyboardAndShowsWholeCuttingCard() {
        // Given
        fillInputsWithKeyboardOpen(ReferenceInputs)

        // When
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()

        // Then
        assertCuttingCardOnScreenWithoutKeyboard()
    }

    @Test
    fun imeDone_onLastField_hidesKeyboardAndShowsWholeCuttingCard() {
        // Given
        fillInputsWithKeyboardOpen(ReferenceInputs)

        // When
        composeRule.onNodeWithText(SkirtDraftField.HemAllowance.labelText).performImeAction()

        // Then
        assertCuttingCardOnScreenWithoutKeyboard()
    }

    @Test
    fun imeDone_withInvalidWaistAndKeyboardOpen_focusesAndRevealsWaist() {
        // Given
        fillInputsWithKeyboardOpen(listOf("", "2", "60", "1", "2"))

        // When
        composeRule.onNodeWithText(SkirtDraftField.HemAllowance.labelText).performImeAction()

        // Then
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText)
            .assertIsFocused().assertIsDisplayed()
        composeRule.onNodeWithText("Informe um valor", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
    }

    private fun fillInputsWithKeyboardOpen(values: List<String>) {
        composeRule.fillSkirtDraft(values)
        composeRule.onNodeWithText(SkirtDraftField.HemAllowance.labelText)
            .performScrollTo().performClick()
        composeRule.awaitKeyboardVisible()
    }

    private fun assertCuttingCardOnScreenWithoutKeyboard() {
        composeRule.awaitKeyboardHidden()
        // No performScrollTo: the radii must already be on screen.
        ReferenceRadii.forEach { composeRule.onNodeWithContentDescription(it).assertIsDisplayed() }
    }
}
