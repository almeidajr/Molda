package com.almeidajr.molda

import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.rive.Result
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.skirt.ui.DrawingLoad
import com.almeidajr.molda.skirt.ui.FoldingStep
import com.almeidajr.molda.skirt.ui.GuideFrame
import com.almeidajr.molda.skirt.ui.GuideProgress
import com.almeidajr.molda.ui.theme.MoldaTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The guide where it needs a device: keyboard users, since leaving touch mode needs one, and the
 * drawing's native Rive frames on screen. [FabricFoldingFileTest] owns what the drawings show.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class FabricFoldingGuideTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @get:Rule(order = 2)
    val keyboardMode = KeyboardModeRule(composeRule)

    @get:Rule(order = 3)
    val animatorScale = AnimatorDurationScaleRule(composeRule)

    @Test
    fun guide_withMotionOff_drawsEachStepsDrawingOnScreen() {
        // Given
        openGuideWithMotionOff()
        composeRule.awaitRiveFrame("Dobra do tecido em Rive: etapa 1")

        // When
        composeRule.onNodeWithText("Próximo").performClick()

        // Then
        composeRule.awaitRiveFrame("Dobra do tecido em Rive: etapa 2")
    }

    @Test
    fun back_closesPlayer_returningKeyboardFocusToEntry_withResultsUnchanged() {
        // Given
        openGuideWithMotionOff()
        composeRule.onNodeWithText("Próximo").performClick()
        composeRule.onNodeWithText("Etapa 2 de 6").assertIsDisplayed()
        // Keyboard users are the ones who need focus back.
        keyboardMode.enter()

        // When
        composeRule.pressBack()

        // Then
        composeRule.onNodeWithText("Etapa 2 de 6").assertDoesNotExist()
        composeRule.onNodeWithText("Guia de dobra do tecido").assertIsDisplayed()
            .assertIsFocused()
        composeRule.onNodeWithContentDescription(ReferenceRadii.first()).assertExists()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assertTextContains("80")
    }

    @Test
    fun frame_enterAndSpace_toggleTheFocusedDrawing() {
        // Given: the frame with a stand-in drawing, as GuideFrameTest shows it.
        composeRule.launchApp(frame = null)
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                var progress by remember { mutableStateOf(GuideProgress(FoldingStep.FirstFold)) }
                MoldaTheme {
                    GuideFrame(
                        Result.Success(Unit),
                        progress,
                        motionEnabled = true,
                        drawingSize = 288.dp,
                        onTogglePause = { progress = progress.togglePause() },
                    ) { _, isPlaying, onLoaded ->
                        LaunchedEffect(Unit) { onLoaded(DrawingLoad.Drawn) }
                        Text("playing=$isPlaying")
                    }
                }
            }
        }
        keyboardMode.enter()
        val frame = composeRule.onNode(hasClickAction())
        frame.requestFocus().assertIsFocused()

        // When
        composeRule.pressKey(KeyEvent.KEYCODE_ENTER)

        // Then
        frame.assert(hasStateDescription("Pausado"))

        // When
        composeRule.pressKey(KeyEvent.KEYCODE_SPACE)

        // Then
        frame.assert(hasStateDescription("Reproduzindo"))
    }

    /** Opens the guide on the reference results with motion off, so Compose goes idle. */
    private fun openGuideWithMotionOff() {
        composeRule.launchApp()
        composeRule.openSkirtCalculator()
        composeRule.fillSkirtDraft()
        composeRule.closeKeyboard()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
        // With motion off the guide's drawings hold still.
        animatorScale.set(0f)
        composeRule.openGuideFromResults()
    }
}
