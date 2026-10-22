package com.almeidajr.molda

import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.rive.Result
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.skirt.ui.FoldingStep
import com.almeidajr.molda.skirt.ui.GuideFrame
import com.almeidajr.molda.ui.theme.MoldaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The guide's step flow and playback controls on the Compose test clock. The player's steps never
 * depend on the native file, and the controls are tested through [GuideFrame] with a stand-in
 * drawing; only the playback flow waits for Rive to load, since its control appears with the
 * drawing. Native frames are [FabricFoldingRenderingTest]'s job.
 */
@RunWith(AndroidJUnit4::class)
class FabricFoldingGuideTest {
    @get:Rule(order = 0)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 1)
    val keyboardMode = KeyboardModeRule(composeRule)

    @get:Rule(order = 2)
    val animatorScale = AnimatorDurationScaleRule(composeRule)

    @Test
    fun opening_showsFirstStepWithPreviousDisabled() {
        // Given
        calculate()

        // When
        openGuide()

        // Then
        composeRule.onNodeWithText("Preparar o tecido").assertIsDisplayed()
        composeRule.onNodeWithText("Alise o tecido sem esticar e respeite o fio.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Anterior").assertIsNotEnabled()
        expandDetails()
        composeRule.onNodeWithText("Comece com papel ou retalho.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun previous_returnsToTheEarlierStep() {
        // Given
        calculate()
        openGuide()
        next(times = 2)
        composeRule.onNodeWithText("Segunda dobra").assertIsDisplayed()

        // When
        composeRule.onNodeWithText("Anterior").performClick()

        // Then
        composeRule.onNodeWithText("Primeira dobra").assertIsDisplayed()
    }

    @Test
    fun markingStep_showsTheCurrentRadii() {
        // Given
        calculate()
        openGuide()

        // When
        next(times = 3)

        // Then
        composeRule.onNodeWithText("Marcar os raios").assertIsDisplayed()
        composeRule.onNodeWithText("1. Costura da cintura: 13,05 cm")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun cuttingStep_warnsWhichLinesStayUncut() {
        // Given
        calculate()
        openGuide()

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
    fun unfoldingStep_isLast_andWarnsItIsNoCompletePattern() {
        // Given
        calculate()
        openGuide()

        // When
        next(times = 5)

        // Then
        composeRule.onNodeWithText("Desdobrar o círculo").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Centro do recorte da cintura: mesma origem de todos os raios, " +
                "agora no centro do círculo.",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Próximo").assertDoesNotExist()
        expandDetails()
        composeRule.onNodeWithText(
            "O círculo desdobrado não é um molde completo e não garante caimento.",
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun concluir_onLastStep_closesGuide() {
        // Given
        calculate()
        openGuide()
        next(times = 5)

        // When
        composeRule.onNodeWithText("Concluir").performClick()

        // Then
        composeRule.onNodeWithText("Desdobrar o círculo").assertDoesNotExist()
        composeRule.onNodeWithText("Guia de dobra do tecido").assertIsDisplayed()
    }

    @Test
    fun editingInput_afterCalculating_removesGuideEntry() {
        // Given
        calculate()

        // When
        composeRule.onNodeWithText(SkirtDraftField.FinishedLength.labelText)
            .performScrollTo().performTextReplacement("70")

        // Then
        composeRule.onNodeWithText("Guia de dobra do tecido").assertDoesNotExist()
    }

    @Test
    fun back_closesPlayer_returningKeyboardFocusToEntry_withResultsUnchanged() {
        // Given
        calculate()
        openGuide()
        next(times = 1)
        composeRule.onNodeWithText("Etapa 2 de 6").assertIsDisplayed()
        // Keyboard users are the ones who need focus back.
        keyboardMode.enter()

        // When
        composeRule.pressBack()

        // Then
        composeRule.onNodeWithText("Etapa 2 de 6").assertDoesNotExist()
        composeRule.onNodeWithText("Guia de dobra do tecido").assertIsDisplayed()
            .assertIsFocused()
        composeRule.onNodeWithContentDescription(
            "Linha 1, linha contínua. Costura da cintura: 13,05 cm",
        ).assertExists()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assertTextContains("80")
    }

    @Test
    fun nextAndPrevious_afterPausing_playTheStepShown() {
        // Given
        animatorScale.set(1f)
        calculate()
        openPlayingGuide()
        next(times = 1)
        drawing(FIRST_FOLD).assert(hasStateDescription("Reproduzindo")).performClick()
        drawing(FIRST_FOLD).assert(hasStateDescription("Pausado"))

        // When
        next(times = 1)

        // Then
        drawing(SECOND_FOLD).assert(hasStateDescription("Reproduzindo"))

        // When: this step is paused too, then left.
        drawing(SECOND_FOLD).performClick()
        composeRule.onNodeWithText("Anterior").performClick()

        // Then
        drawing(FIRST_FOLD).assert(hasStateDescription("Reproduzindo"))
    }

    @Test
    fun recreation_afterPausing_playsTheStep() {
        // Given
        animatorScale.set(1f)
        calculate()
        openPlayingGuide()
        next(times = 1)
        drawing(FIRST_FOLD).performClick()
        drawing(FIRST_FOLD).assert(hasStateDescription("Pausado"))

        // When
        composeRule.recreate()
        composeRule.showEmptyApp()

        // Then
        drawing(FIRST_FOLD).assert(hasStateDescription("Reproduzindo"))
    }

    @Test
    fun recreation_keepsCurrentStep() {
        // Given
        calculate()
        openGuide()
        next(times = 2)

        // When
        composeRule.recreate()
        composeRule.showEmptyApp()

        // Then
        composeRule.onNodeWithText("Segunda dobra").assertIsDisplayed()
        composeRule.onNodeWithText("Etapa 3 de 6").assertIsDisplayed()
    }

    @Test
    fun frame_onceLoaded_playsWithoutPressing_andOffersPause() {
        // When
        showFrame(FoldingStep.FirstFold)

        // Then
        composeRule.onNodeWithText("play=true paused=false", useUnmergedTree = true)
            .assertExists()
        composeRule.onNode(isToggleable())
            .assertContentDescriptionEquals("Dobra do tecido em Rive: etapa 2")
            .assert(hasStateDescription("Reproduzindo"))
            .assert(hasClickLabel("Pausar animação"))
        badge("pause").assertIsDisplayed()
        badge("play").assertDoesNotExist()
    }

    @Test
    fun frame_tap_pausesThenContinues() {
        // Given
        showFrame(FoldingStep.FirstFold)

        // When
        composeRule.onNode(isToggleable()).performClick()

        // Then
        composeRule.onNodeWithText("play=true paused=true", useUnmergedTree = true)
            .assertExists()
        composeRule.onNode(isToggleable())
            .assert(hasStateDescription("Pausado"))
            .assert(hasClickLabel("Continuar animação"))
        badge("play").assertIsDisplayed()
        badge("pause").assertDoesNotExist()

        // When
        composeRule.onNode(isToggleable()).performClick()

        // Then
        composeRule.onNodeWithText("play=true paused=false", useUnmergedTree = true)
            .assertExists()
        composeRule.onNode(isToggleable()).assert(hasStateDescription("Reproduzindo"))
        badge("pause").assertIsDisplayed()
    }

    @Test
    fun frame_enterAndSpace_toggleTheFocusedDrawing() {
        // Given
        showFrame(FoldingStep.FirstFold)
        keyboardMode.enter()
        composeRule.onNode(isToggleable()).requestFocus().assertIsFocused()

        // When
        composeRule.pressKey(KeyEvent.KEYCODE_ENTER)

        // Then
        composeRule.onNode(isToggleable()).assert(hasStateDescription("Pausado"))

        // When
        composeRule.pressKey(KeyEvent.KEYCODE_SPACE)

        // Then
        composeRule.onNode(isToggleable()).assert(hasStateDescription("Reproduzindo"))
    }

    @Test
    fun frame_withMotionDisabled_keepsDrawingStill_andIsNoControl() {
        // When
        showFrame(FoldingStep.FirstFold, motionEnabled = false)

        // Then
        composeRule.onNodeWithText("play=false paused=false", useUnmergedTree = true)
            .assertExists()
        composeRule.onAllNodes(isToggleable()).assertCountEquals(0)
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(0)
        badge("pause").assertDoesNotExist()
        badge("play").assertDoesNotExist()
    }

    @Test
    fun frame_whileFileLoads_isEmptyAtFinalSize_andKeepsStepDescription() {
        // When
        showFrame(FoldingStep.SecondFold, file = mutableStateOf(Result.Loading))

        // Then
        composeRule.onAllNodes(hasText("", substring = true)).assertCountEquals(0)
        // The 288dp frame less its 8dp padding on each side.
        composeRule.onNodeWithContentDescription("Dobra do tecido em Rive: etapa 3")
            .assertWidthIsEqualTo(272.dp).assertHeightIsEqualTo(272.dp)
        composeRule.onAllNodes(isToggleable()).assertCountEquals(0)
        badge("pause").assertDoesNotExist()
    }

    @Test
    fun frame_whenFileFails_pointsToWrittenInstructions() {
        // When
        showFrame(
            FoldingStep.FirstFold,
            file = mutableStateOf(Result.Error(IllegalStateException("corrupt"))),
        )

        // Then
        composeRule.onNodeWithText(
            "Não foi possível abrir a animação. Consulte as instruções abaixo.",
        ).assertIsDisplayed()
        composeRule.onAllNodes(isToggleable()).assertCountEquals(0)
        badge("pause").assertDoesNotExist()
    }

    @Test
    fun frame_quickLoad_neverShowsIndicator() {
        // Given
        composeRule.mainClock.autoAdvance = false
        val file = mutableStateOf<Result<Unit>>(Result.Loading)
        showFrame(FoldingStep.FirstFold, file = file)
        composeRule.mainClock.advanceTimeBy(350)
        composeRule.onNode(isLoadingIndicator).assertDoesNotExist()

        // When
        file.value = Result.Success(Unit)
        composeRule.mainClock.advanceTimeBy(1_000)

        // Then
        composeRule.onNode(isLoadingIndicator).assertDoesNotExist()
        composeRule.onNodeWithText("play=true paused=false", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun frame_longLoad_showsIndicatorAfterDelay_andHoldsItBeforeTheDrawing() {
        // Given
        composeRule.mainClock.autoAdvance = false
        val file = mutableStateOf<Result<Unit>>(Result.Loading)
        showFrame(FoldingStep.FirstFold, file = file)
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.onNode(isLoadingIndicator).assertDoesNotExist()
        composeRule.mainClock.advanceTimeBy(200)
        composeRule.onNode(isLoadingIndicator).assertIsDisplayed()

        // When: the drawing loads 100ms after the indicator appears.
        file.value = Result.Success(Unit)
        composeRule.mainClock.advanceTimeBy(100)

        // Then
        composeRule.onNode(isLoadingIndicator).assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNode(isLoadingIndicator).assertDoesNotExist()
    }

    /** Shows the drawing frame with a stand-in drawing that prints the playback it receives. */
    private fun showFrame(
        step: FoldingStep,
        motionEnabled: Boolean = true,
        file: State<Result<Unit>> = mutableStateOf(Result.Success(Unit)),
    ) {
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                MoldaTheme {
                    GuideFrame(file.value, step, motionEnabled, drawingSize = 288.dp) {
                            _,
                            play,
                            paused,
                            onLoaded,
                        ->
                        LaunchedEffect(Unit) { onLoaded() }
                        Text("play=$play paused=$paused")
                    }
                }
            }
        }
    }

    /** The step's drawing frame labelled [description], once Rive has drawn it and it toggles. */
    private fun drawing(description: String): SemanticsNodeInteraction {
        composeRule.awaitRiveFrame(description)
        return composeRule.onNode(isToggleable() and hasContentDescription(description))
    }

    /** The frame's decorative badge showing [glyph], "pause" or "play": what a tap does. */
    private fun badge(glyph: String) =
        composeRule.onNode(hasTestTag("guide_badge_$glyph"), useUnmergedTree = true)

    private fun hasStateDescription(state: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state)

    private fun hasClickLabel(label: String) = SemanticsMatcher("click label \"$label\"") {
        it.config.getOrElseNullable(SemanticsActions.OnClick) { null }?.label == label
    }

    /** Shows results for an 80 cm waist with 2 cm ease, 60 cm length and 1 and 2 cm allowances. */
    private fun calculate() {
        composeRule.showEmptyApp()
        composeRule.openSkirtCalculator()
        SkirtDraftField.entries.zip(listOf("80", "2", "60", "1", "2")).forEach { (field, value) ->
            composeRule.onNodeWithText(field.labelText).performScrollTo()
                .performTextReplacement(value)
        }
        closeSoftKeyboard()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
    }

    private val isLoadingIndicator = hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)

    private fun openGuide() {
        composeRule.onNodeWithText("Guia de dobra do tecido").performScrollTo().performClick()
    }

    /**
     * Opens the guide with motion on. A playing drawing keeps Compose busy, so the test clock
     * drives the guide from here: each wait for a drawing advances it.
     */
    private fun openPlayingGuide() {
        val entry = composeRule.onNodeWithText("Guia de dobra do tecido").performScrollTo()
        composeRule.mainClock.autoAdvance = false
        entry.performClick()
        // Past the transition into the player.
        composeRule.mainClock.advanceTimeBy(1_000)
    }

    private fun next(times: Int) = repeat(times) {
        composeRule.onNodeWithText("Próximo").performClick()
    }

    /** Opens the current step's collapsed "Detalhes" disclosure. */
    private fun expandDetails() {
        composeRule.onNodeWithText("Detalhes").performScrollTo().performClick()
    }

    private companion object {
        const val FIRST_FOLD = "Dobra do tecido em Rive: etapa 2"
        const val SECOND_FOLD = "Dobra do tecido em Rive: etapa 3"
    }
}
