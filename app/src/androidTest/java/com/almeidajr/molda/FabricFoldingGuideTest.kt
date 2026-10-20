package com.almeidajr.molda

import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.rive.Result
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.skirt.ui.FoldingStep
import com.almeidajr.molda.skirt.ui.GuideFrame
import com.almeidajr.molda.ui.theme.MoldaTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The guide's step flow and playback controls on the Compose test clock. Nothing here waits for
 * Rive: the player's steps never depend on the native file, and the controls are tested through
 * [GuideFrame] with a stand-in drawing. Native frames are [FabricFoldingRenderingTest]'s job.
 */
@RunWith(AndroidJUnit4::class)
class FabricFoldingGuideTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun openSkirtCalculator() = composeRule.showSkirtCalculator()

    @Test
    fun foldingSteps_canBeReadAndRevisited_beforeMarkingCurrentRadii() {
        // Given
        openGuide()
        composeRule.onNodeWithText("Preparar o tecido").assertIsDisplayed()
        composeRule.onNodeWithText("Anterior").assertIsNotEnabled()
        composeRule.onNodeWithText("Alise o tecido sem esticar e respeite o fio.")
            .assertIsDisplayed()
        expandDetails()
        composeRule.onNodeWithText("Comece com papel ou retalho.")
            .performScrollTo().assertIsDisplayed()

        // When
        repeat(2) { composeRule.onNodeWithText("Próximo").performClick() }
        composeRule.onNodeWithText("Segunda dobra").assertIsDisplayed()
        composeRule.onNodeWithText("Anterior").performClick()
        composeRule.onNodeWithText("Primeira dobra").assertIsDisplayed()
        repeat(2) { composeRule.onNodeWithText("Próximo").performClick() }

        // Then
        composeRule.onNodeWithText("Marcar os raios").assertIsDisplayed()
        composeRule.onNodeWithText("1. Costura da cintura: 13,05 cm")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Anterior").performClick()
        composeRule.onNodeWithText("Segunda dobra").assertIsDisplayed()
    }

    @Test
    fun cuttingAndUnfolding_showConstructionBoundaries_andConcluirClosesGuide() {
        // Given
        openGuide()
        repeat(4) { composeRule.onNodeWithText("Próximo").performClick() }
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
        composeRule.onNodeWithText("Próximo").performClick()
        composeRule.onNodeWithText("Desdobrar o círculo").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Centro do recorte da cintura: mesma origem de todos os raios, agora no centro do círculo.",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Próximo").assertDoesNotExist()
        expandDetails()
        composeRule.onNodeWithText(
            "O círculo desdobrado não é um molde completo e não garante caimento.",
        ).performScrollTo().assertIsDisplayed()

        // When
        composeRule.onNodeWithText("Concluir").assertIsEnabled().performClick()

        // Then
        composeRule.onNodeWithText("Desdobrar o círculo").assertDoesNotExist()
        composeRule.onNodeWithText("Guia de dobra do tecido").assertIsDisplayed()
    }

    @Test
    fun closingMarking_returnsToResults_andEditingInputDropsGuideEntry() {
        // Given
        openGuide()
        repeat(3) { composeRule.onNodeWithText("Próximo").performClick() }
        composeRule.onNodeWithText("Marcar os raios").assertIsDisplayed()
        composeRule.onNodeWithText("1. Costura da cintura: 13,05 cm")
            .performScrollTo().assertIsDisplayed()

        // When
        composeRule.onNodeWithContentDescription("Fechar guia").performClick()

        // Then
        composeRule.onNodeWithText("Como levar os raios ao papel")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(SkirtDraftField.FinishedLength.labelText)
            .performScrollTo().performTextReplacement("70")
        composeRule.onNodeWithText("Guia de dobra do tecido").assertDoesNotExist()
    }

    @Test
    fun back_closesPlayer_returningKeyboardFocusToEntry_withResultsUnchanged() {
        // Given
        openGuide()
        composeRule.onNodeWithText("Próximo").performClick()
        composeRule.onNodeWithText("Etapa 2 de 6").assertIsDisplayed()
        // Touch mode leaves buttons unfocusable; keyboard users are the ones who need focus back.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        try {
            instrumentation.setInTouchMode(false)

            // When
            pressBack()

            // Then
            composeRule.onNodeWithText("Etapa 2 de 6").assertDoesNotExist()
            composeRule.onNodeWithText("Guia de dobra do tecido").assertIsDisplayed()
                .assertIsFocused()
        } finally {
            instrumentation.setInTouchMode(true)
        }
        composeRule.onNodeWithContentDescription(
            "Linha 1, linha contínua. Costura da cintura: 13,05 cm",
        ).assertExists()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assertTextContains("80")
    }

    @Test
    fun recreation_keepsCurrentStep() {
        // Given
        openGuide()
        repeat(2) { composeRule.onNodeWithText("Próximo").performClick() }

        // When
        composeRule.activityRule.scenario.recreate()

        // Then
        composeRule.onNodeWithText("Segunda dobra").assertIsDisplayed()
        composeRule.onNodeWithText("Etapa 3 de 6").assertIsDisplayed()
    }

    @Test
    fun frame_beforePlay_offersPlayAndKeepsDrawingStill() {
        // When
        showFrame(FoldingStep.FirstFold)

        // Then
        composeRule.onNodeWithText("Reproduzir etapa").assertIsDisplayed()
        composeRule.onNodeWithText("Pausar animação").assertDoesNotExist()
        composeRule.onNodeWithText("play=false replay=0 paused=false").assertExists()
    }

    @Test
    fun frame_play_startsDrawingAndOffersRepeatAndPause() {
        // Given
        showFrame(FoldingStep.FirstFold)

        // When
        composeRule.onNodeWithText("Reproduzir etapa").performClick()

        // Then
        composeRule.onNodeWithText("play=true replay=1 paused=false").assertExists()
        composeRule.onNodeWithText("Repetir etapa").assertIsDisplayed()
        composeRule.onNodeWithText("Pausar animação").assertIsDisplayed()
    }

    @Test
    fun frame_pause_holdsDrawingAndOffersContinue() {
        // Given
        showFrame(FoldingStep.FirstFold)
        composeRule.onNodeWithText("Reproduzir etapa").performClick()

        // When
        composeRule.onNodeWithText("Pausar animação").performClick()

        // Then
        composeRule.onNodeWithText("play=true replay=1 paused=true").assertExists()
        composeRule.onNodeWithText("Continuar animação").assertIsDisplayed()
    }

    @Test
    fun frame_repeatWhilePaused_restartsAndResumes() {
        // Given
        showFrame(FoldingStep.FirstFold)
        composeRule.onNodeWithText("Reproduzir etapa").performClick()
        composeRule.onNodeWithText("Pausar animação").performClick()

        // When
        composeRule.onNodeWithText("Repetir etapa").performClick()

        // Then
        composeRule.onNodeWithText("play=true replay=2 paused=false").assertExists()
        composeRule.onNodeWithText("Pausar animação").assertIsDisplayed()
    }

    @Test
    fun frame_onMarking_namesItsMarking() {
        // Given
        showFrame(FoldingStep.Marking)
        composeRule.onNodeWithText("Reproduzir marcação").assertIsDisplayed()

        // When
        composeRule.onNodeWithText("Reproduzir marcação").performClick()

        // Then
        composeRule.onNodeWithText("Repetir marcação").assertIsDisplayed()
    }

    @Test
    fun frame_withMotionDisabled_replayKeepsDrawingStillWithoutPause() {
        // Given
        showFrame(FoldingStep.FirstFold, motionEnabled = false)

        // When
        composeRule.onNodeWithText("Reproduzir etapa").performClick()

        // Then
        composeRule.onNodeWithText("play=false replay=1 paused=false").assertExists()
        composeRule.onNodeWithText("Pausar animação").assertDoesNotExist()
    }

    @Test
    fun frame_whileFileLoads_showsLoadingWithoutControls() {
        // When
        showFrame(FoldingStep.FirstFold, file = Result.Loading)

        // Then
        composeRule.onNodeWithText("Carregando animação…").assertIsDisplayed()
        composeRule.onNodeWithText("Reproduzir etapa").assertDoesNotExist()
    }

    /** Shows the drawing frame with a stand-in drawing that prints the playback it receives. */
    private fun showFrame(
        step: FoldingStep,
        motionEnabled: Boolean = true,
        file: Result<Unit> = Result.Success(Unit),
    ) {
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                MoldaTheme {
                    GuideFrame(file, step, motionEnabled, drawingSize = 288.dp) {
                            _,
                            play,
                            replay,
                            paused,
                        ->
                        Text("play=$play replay=$replay paused=$paused")
                    }
                }
            }
        }
    }

    private fun openGuide() {
        SkirtDraftField.entries.zip(listOf("80", "2", "60", "1", "2")).forEach { (field, value) ->
            composeRule.onNodeWithText(field.labelText).performScrollTo()
                .performTextReplacement(value)
        }
        closeSoftKeyboard()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
        composeRule.onNodeWithText("Guia de dobra do tecido").performScrollTo().performClick()
    }

    /** Opens the current step's collapsed "Detalhes" disclosure. */
    private fun expandDetails() {
        composeRule.onNodeWithText("Detalhes").performScrollTo().performClick()
    }
}
