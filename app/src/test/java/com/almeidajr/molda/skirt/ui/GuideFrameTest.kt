package com.almeidajr.molda.skirt.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.rive.Result
import com.almeidajr.molda.isLoadingIndicator
import com.almeidajr.molda.ui.theme.MoldaTheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The guide's drawing frame and its playback control on the Compose test clock, through a
 * stand-in drawing that prints whether it plays and reports when it has drawn.
 */
@RunWith(AndroidJUnit4::class)
class GuideFrameTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun frame_afterStepChange_offersNoPauseUntilTheNewDrawingDraws() {
        // Given
        var progress by mutableStateOf(GuideProgress(FoldingStep.FirstFold))
        val drawnSteps = mutableStateListOf(FoldingStep.FirstFold)
        composeRule.setContent {
            MoldaTheme {
                GuideFrame(
                    Result.Success(Unit),
                    progress,
                    motionEnabled = true,
                    drawingSize = 288.dp,
                    onTogglePause = {},
                ) { _, _, onLoaded ->
                    val isDrawn = progress.step in drawnSteps
                    LaunchedEffect(isDrawn) { if (isDrawn) onLoaded(DrawingLoad.Drawn) }
                }
            }
        }
        composeRule.onNode(hasClickAction()).assertExists()

        // When
        progress = GuideProgress(FoldingStep.SecondFold)

        // Then
        composeRule.onNode(hasClickAction()).assertDoesNotExist()
        composeRule.onNode(hasStateDescription("Carregando")).assertExists()
        composeRule.runOnIdle { drawnSteps += FoldingStep.SecondFold }
        composeRule.onNode(hasClickAction()).assertExists()
        composeRule.onNode(hasStateDescription("Carregando")).assertDoesNotExist()
    }

    @Test
    fun frame_onceLoaded_playsWithoutPressing_andOffersPause() {
        // When
        showFrame(FoldingStep.FirstFold)

        // Then
        composeRule.onNodeWithText("playing=true", useUnmergedTree = true).assertExists()
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
        composeRule.onNodeWithText("playing=false", useUnmergedTree = true).assertExists()
        pauseButton()
            .assert(hasStateDescription("Pausado"))
            .assert(hasClickLabel("Continuar animação"))
        badge(GUIDE_BADGE_PLAY_TAG).assertIsDisplayed()
        badge(GUIDE_BADGE_PAUSE_TAG).assertDoesNotExist()

        // When
        pauseButton().performClick()

        // Then
        composeRule.onNodeWithText("playing=true", useUnmergedTree = true).assertExists()
        pauseButton().assert(hasStateDescription("Reproduzindo"))
        badge(GUIDE_BADGE_PAUSE_TAG).assertIsDisplayed()
    }

    @Test
    fun frame_whileFileLoads_isEmptyAtFinalSize_andKeepsStepDescription() {
        // When
        showFrame(FoldingStep.SecondFold, file = mutableStateOf(Result.Loading))

        // Then
        composeRule.onAllNodes(hasText("", substring = true)).assertCountEquals(0)
        // Molda reserves the drawing's square while it loads, so the instructions never jump:
        // the 288dp frame less its 8dp padding on each side.
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
        composeRule.onNodeWithText("playing=true", useUnmergedTree = true).assertIsDisplayed()
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
        composeRule.onNodeWithText("playing=true", useUnmergedTree = true).assertIsDisplayed()
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
        composeRule.setContent {
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

    /** The stand-in drawing's frame, the only clickable node once it pauses. */
    private fun pauseButton() = composeRule.onNode(hasClickAction())

    /** The frame's decorative badge glyph tagged [tag]: what a tap does. */
    private fun badge(tag: String) = composeRule.onNode(hasTestTag(tag), useUnmergedTree = true)

    private fun hasClickLabel(label: String) = SemanticsMatcher("click label \"$label\"") {
        it.config.getOrElseNullable(SemanticsActions.OnClick) { null }?.label == label
    }

    /** Test-clock milliseconds just before [time]. */
    private fun before(time: Duration) = (time - Margin).inWholeMilliseconds

    /** Test-clock milliseconds just after [time]. */
    private fun after(time: Duration) = (time + Margin).inWholeMilliseconds

    private companion object {
        /** Test-clock slack around the indicator's timings, a few frames. */
        val Margin = 50.milliseconds
    }
}
