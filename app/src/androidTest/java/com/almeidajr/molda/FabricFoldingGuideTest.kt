package com.almeidajr.molda

import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
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
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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
import com.almeidajr.molda.skirt.ui.DrawingLoad
import com.almeidajr.molda.skirt.ui.FoldingStep
import com.almeidajr.molda.skirt.ui.GUIDE_BADGE_PAUSE_TAG
import com.almeidajr.molda.skirt.ui.GUIDE_BADGE_PLAY_TAG
import com.almeidajr.molda.skirt.ui.GuideFrame
import com.almeidajr.molda.skirt.ui.GuideProgress
import com.almeidajr.molda.skirt.ui.IndicatorDelay
import com.almeidajr.molda.skirt.ui.IndicatorMinDuration
import com.almeidajr.molda.ui.theme.MoldaTheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
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
    fun next_afterPausing_playsTheStepShown() {
        // Given
        calculate()
        openPlayingGuide()
        next(times = 1)
        drawing(FIRST_FOLD).assert(hasStateDescription("Reproduzindo")).performClick()
        drawing(FIRST_FOLD).assert(hasStateDescription("Pausado"))

        // When
        next(times = 1)

        // Then
        drawing(SECOND_FOLD).assert(hasStateDescription("Reproduzindo"))
    }

    @Test
    fun motionChange_afterPausing_playsTheStep() {
        // Given
        calculate()
        openPlayingGuide()
        next(times = 1)
        drawing(FIRST_FOLD).performClick()
        drawing(FIRST_FOLD).assert(hasStateDescription("Pausado"))

        // When
        animatorScale.set(0f)
        animatorScale.set(1f)

        // Then
        drawing(FIRST_FOLD).assert(hasStateDescription("Reproduzindo"))
    }

    @Test
    fun recreation_afterPausing_playsTheStep() {
        // Given
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
        composeRule.onNodeWithText("playing=true", useUnmergedTree = true)
            .assertExists()
        pauseButton()
            .assertContentDescriptionEquals("Dobra do tecido em Rive: etapa 2")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(hasStateDescription("Reproduzindo"))
            .assert(hasClickLabel("Pausar animação"))
        // The frame's one click action: no inner control competes with it.
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
        badge(GUIDE_BADGE_PAUSE_TAG).assertIsDisplayed()
        badge(GUIDE_BADGE_PLAY_TAG).assertDoesNotExist()
    }

    @Test
    fun frame_tap_pausesThenContinues() {
        // Given
        showFrame(FoldingStep.FirstFold)

        // When
        pauseButton().performClick()

        // Then
        composeRule.onNodeWithText("playing=false", useUnmergedTree = true)
            .assertExists()
        pauseButton()
            .assert(hasStateDescription("Pausado"))
            .assert(hasClickLabel("Continuar animação"))
        badge(GUIDE_BADGE_PLAY_TAG).assertIsDisplayed()
        badge(GUIDE_BADGE_PAUSE_TAG).assertDoesNotExist()

        // When
        pauseButton().performClick()

        // Then
        composeRule.onNodeWithText("playing=true", useUnmergedTree = true)
            .assertExists()
        pauseButton().assert(hasStateDescription("Reproduzindo"))
        badge(GUIDE_BADGE_PAUSE_TAG).assertIsDisplayed()
    }

    @Test
    fun frame_enterAndSpace_toggleTheFocusedDrawing() {
        // Given
        showFrame(FoldingStep.FirstFold)
        keyboardMode.enter()
        pauseButton().requestFocus().assertIsFocused()

        // When
        composeRule.pressKey(KeyEvent.KEYCODE_ENTER)

        // Then
        pauseButton().assert(hasStateDescription("Pausado"))

        // When
        composeRule.pressKey(KeyEvent.KEYCODE_SPACE)

        // Then
        pauseButton().assert(hasStateDescription("Reproduzindo"))
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
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(0)
        badge(GUIDE_BADGE_PAUSE_TAG).assertDoesNotExist()
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
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(0)
        badge(GUIDE_BADGE_PAUSE_TAG).assertDoesNotExist()
    }

    @Test
    fun frame_whenDrawingFails_keepsOnlyItsSentence() {
        // Given
        composeRule.mainClock.autoAdvance = false

        // When
        showFrame(FoldingStep.FirstFold, load = DrawingLoad.Failed)
        composeRule.mainClock.advanceTimeBy(after(IndicatorDelay + IndicatorMinDuration))

        // Then
        composeRule.onNodeWithText("playing=true", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(0)
        composeRule.onNode(isLoadingIndicator).assertDoesNotExist()
        badge(GUIDE_BADGE_PAUSE_TAG).assertDoesNotExist()
    }

    @Test
    fun frame_quickLoad_neverShowsIndicator() {
        // Given
        composeRule.mainClock.autoAdvance = false
        val file = mutableStateOf<Result<Unit>>(Result.Loading)
        showFrame(FoldingStep.FirstFold, file = file)
        composeRule.mainClock.advanceTimeBy(before(IndicatorDelay))
        composeRule.onNode(isLoadingIndicator).assertDoesNotExist()

        // When
        file.value = Result.Success(Unit)
        composeRule.mainClock.advanceTimeBy(after(IndicatorDelay + IndicatorMinDuration))

        // Then
        composeRule.onNode(isLoadingIndicator).assertDoesNotExist()
        composeRule.onNodeWithText("playing=true", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun frame_longLoad_showsIndicatorAfterDelay_andHoldsItBeforeTheDrawing() {
        // Given
        composeRule.mainClock.autoAdvance = false
        val file = mutableStateOf<Result<Unit>>(Result.Loading)
        showFrame(FoldingStep.FirstFold, file = file)
        composeRule.mainClock.advanceTimeBy(before(IndicatorDelay))
        composeRule.onNode(isLoadingIndicator).assertDoesNotExist()
        composeRule.mainClock.advanceTimeBy((Margin * 2).inWholeMilliseconds)
        composeRule.onNode(isLoadingIndicator).assertIsDisplayed()

        // When: the drawing loads soon after the indicator appears.
        file.value = Result.Success(Unit)
        composeRule.mainClock.advanceTimeBy(Margin.inWholeMilliseconds)

        // Then
        composeRule.onNode(isLoadingIndicator).assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(IndicatorMinDuration.inWholeMilliseconds)
        composeRule.onNode(isLoadingIndicator).assertDoesNotExist()
    }

    /**
     * Shows the drawing frame on [step] with motion on, as the player does, with a stand-in drawing
     * that prints whether it plays and reports [load] once shown.
     */
    private fun showFrame(
        step: FoldingStep,
        file: State<Result<Unit>> = mutableStateOf(Result.Success(Unit)),
        load: DrawingLoad = DrawingLoad.Drawn,
    ) {
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                var progress by remember { mutableStateOf(GuideProgress(step)) }
                MoldaTheme {
                    GuideFrame(
                        file.value,
                        progress,
                        motionEnabled = true,
                        drawingSize = 288.dp,
                        onTogglePause = { progress = progress.togglePause() },
                    ) { _, isPlaying, onLoaded ->
                        LaunchedEffect(Unit) { onLoaded(load) }
                        Text("playing=$isPlaying")
                    }
                }
            }
        }
    }

    /** The stand-in drawing's frame, the only clickable node once it pauses. */
    private fun pauseButton() = composeRule.onNode(hasClickAction())

    /** The step's drawing frame labelled [description], once Rive has drawn it and it pauses. */
    private fun drawing(description: String): SemanticsNodeInteraction {
        composeRule.awaitRiveFrame(description)
        return composeRule.onNode(hasClickAction() and hasContentDescription(description))
    }

    /** The frame's decorative badge glyph tagged [tag]: what a tap does. */
    private fun badge(tag: String) = composeRule.onNode(hasTestTag(tag), useUnmergedTree = true)

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

    /** Opens the guide with motion off, so its drawings hold still and Compose goes idle. */
    private fun openGuide() {
        animatorScale.set(0f)
        composeRule.openGuideFromResults()
    }

    /** Opens the guide with motion on; the test clock drives it from here. */
    private fun openPlayingGuide() {
        animatorScale.set(1f)
        composeRule.openGuideFromResults()
    }

    /** Test-clock milliseconds just before [time]. */
    private fun before(time: Duration) = (time - Margin).inWholeMilliseconds

    /** Test-clock milliseconds just after [time]. */
    private fun after(time: Duration) = (time + Margin).inWholeMilliseconds

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

        /** Test-clock slack around the indicator's timings, a few frames. */
        val Margin = 50.milliseconds
    }
}
