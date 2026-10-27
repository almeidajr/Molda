package com.almeidajr.molda.skirt.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.rive.Result
import com.almeidajr.molda.ui.theme.MoldaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The guide's drawing frame, through a stand-in drawing that reports when it has drawn. */
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
}
