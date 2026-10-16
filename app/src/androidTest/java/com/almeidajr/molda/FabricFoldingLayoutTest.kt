package com.almeidajr.molda

import androidx.activity.compose.setContent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.skirt.ui.MeasurementField
import com.almeidajr.molda.skirt.ui.SkirtCalculatorRoute
import com.almeidajr.molda.ui.theme.MoldaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FabricFoldingLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun fullTutorial_keepsInstructionsAndControlsReachable() {
        listOf(false, true).forEach { dark ->
            openTutorial(dark)
            listOf(
                "Preparar o tecido",
                "Primeira dobra",
                "Segunda dobra",
                "Marcar os raios",
                "Cortar os arcos",
                "Desdobrar o círculo",
            ).forEachIndexed { index, title ->
                val heading = composeRule.onNodeWithText(title).performScrollTo()
                    .assertIsDisplayed().assert(
                        SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading),
                    )
                heading.assertReadableTextContrast("$title dark=$dark")
                val layouts = mutableListOf<TextLayoutResult>()
                heading.performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                    assertTrue(it(layouts))
                }
                val layout = layouts.single()
                // Text bounds can differ from rounded pixel size by a fraction of a pixel.
                assertTrue(
                    "$title must wrap within the available width",
                    (0 until layout.lineCount).all {
                        layout.getLineRight(it) <= layout.size.width + 1f &&
                            layout.getLineBottom(it) <= layout.size.height + 1f
                    },
                )
                if (index >= 3) {
                    composeRule.onNodeWithText("Margem de cintura zero: linhas 1 e 2 coincidem.")
                        .performScrollTo().assertIsDisplayed()
                    composeRule.onNodeWithText("Antes de cortar tecido da peça", substring = true)
                        .performScrollTo().assertIsDisplayed()
                }
                val next = composeRule.onNodeWithText(
                    "Próximo",
                ).performScrollTo().assertIsDisplayed()
                if (index == 5) {
                    next.assertIsNotEnabled()
                } else {
                    next.assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick()
                }
            }
            composeRule.onNodeWithText(
                "O círculo desdobrado não é um molde completo e não garante caimento.",
            ).performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithText("Fechar tutorial").performScrollTo()
                .assertHeightIsAtLeast(48.dp).performClick()
            composeRule.onNodeWithText(MeasurementField.Length.label).performScrollTo()
                .performTextReplacement("70")
            composeRule.onNodeWithText("Tutorial de dobra do tecido").assertDoesNotExist()
        }
    }

    private fun openTutorial(dark: Boolean) {
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                MoldaTheme(darkTheme = dark, dynamicColor = false) {
                    SkirtCalculatorRoute()
                }
            }
        }
        MeasurementField.entries.zip(listOf("80", "2", "60", "0", "0"))
            .forEach { (field, value) ->
                composeRule.onNodeWithText(field.label).performScrollTo()
                    .performTextReplacement(value)
            }
        closeSoftKeyboard()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
        composeRule.onNodeWithText("Tutorial de dobra do tecido")
            .performScrollTo().performClick()
    }
}
