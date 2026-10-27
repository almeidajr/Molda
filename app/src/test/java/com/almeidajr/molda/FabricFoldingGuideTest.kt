package com.almeidajr.molda

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The guide player's step flow, opened from the skirt's results. Without Rive's native code the
 * frame shows its written fallback, so nothing here waits for a drawing.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class FabricFoldingGuideTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Before
    fun openGuide() {
        composeRule.launchApp()
        composeRule.openSkirtCalculator()
        composeRule.fillSkirtDraft()
        composeRule.click("Calcular")
        composeRule.click("Guia de dobra do tecido")
    }

    @Test
    fun previous_returnsToTheEarlierStep() {
        // Given
        next(times = 2)
        composeRule.onNodeWithText("Segunda dobra").assertIsDisplayed()

        // When
        composeRule.onNodeWithText("Anterior").performClick()

        // Then
        composeRule.onNodeWithText("Primeira dobra").assertIsDisplayed()
    }

    @Test
    fun cuttingStep_warnsWhichLinesStayUncut() {
        // When
        next(times = 4)

        // Then
        composeRule.onNodeWithText("Cortar os arcos").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Corte o arco 2 e depois o arco 4, atravessando as quatro camadas alinhadas.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Não corte as linhas 1 e 3", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            "Para tecido da peça, abertura, margens do fechamento e cós pedem instruções " +
                "à parte antes do corte.",
        ).performScrollTo().assertIsDisplayed()
        expandDetails()
        composeRule.onNodeWithText("Pratique em papel ou retalho", substring = true)
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun unfoldingStep_warnsItIsNoCompletePattern() {
        // When
        next(times = 5)

        // Then
        composeRule.onNodeWithText("Desdobrar o círculo").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Centro do recorte da cintura: mesma origem de todos os raios, " +
                "agora no centro do círculo.",
        ).performScrollTo().assertIsDisplayed()
        expandDetails()
        composeRule.onNodeWithText(
            "O círculo desdobrado não é um molde completo e não garante caimento.",
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun concluir_onLastStep_closesGuide() {
        // Given
        next(times = 5)

        // When
        composeRule.onNodeWithText("Concluir").performClick()

        // Then
        composeRule.onNodeWithText("Desdobrar o círculo").assertDoesNotExist()
        composeRule.onNodeWithText("Guia de dobra do tecido").assertIsDisplayed()
    }

    @Test
    fun recreation_keepsCurrentStep() {
        // Given
        next(times = 2)

        // When
        composeRule.recreate()

        // Then
        composeRule.onNodeWithText("Segunda dobra").assertIsDisplayed()
        composeRule.onNodeWithText("Etapa 3 de 6").assertIsDisplayed()
    }

    private fun next(times: Int) = repeat(times) {
        composeRule.onNodeWithText("Próximo").performClick()
    }

    /** Opens the current step's collapsed "Detalhes" disclosure. */
    private fun expandDetails() {
        composeRule.onNodeWithText("Detalhes").performScrollTo().performClick()
    }
}
