package com.almeidajr.molda

import androidx.activity.compose.setContent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
    fun fullTutorial_keepsStepBarAndDrawingInView_onEveryStep() {
        listOf(false to 1f, true to 2f).forEach { (dark, fontScale) ->
            openTutorial(dark, fontScale)
            listOf(
                "Preparar o tecido",
                "Primeira dobra",
                "Segunda dobra",
                "Marcar os raios",
                "Cortar os arcos",
                "Desdobrar o círculo",
            ).forEachIndexed { index, title ->
                val context = "$title dark=$dark fontScale=$fontScale"
                val heading = composeRule.onNodeWithText(title).assertIsDisplayed().assert(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading),
                )
                heading.assertReadableTextContrast(context)
                val layouts = mutableListOf<TextLayoutResult>()
                heading.performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                    assertTrue(it(layouts))
                }
                val layout = layouts.single()
                // Text bounds can differ from rounded pixel size by a fraction of a pixel.
                assertTrue(
                    "$context must wrap within the available width",
                    (0 until layout.lineCount).all {
                        layout.getLineRight(it) <= layout.size.width + 1f &&
                            layout.getLineBottom(it) <= layout.size.height + 1f
                    },
                )
                val playLabel = if (index == 3) "Reproduzir marcação" else "Reproduzir etapa"
                composeRule.waitUntil(15_000) {
                    composeRule.onAllNodes(hasText(playLabel)).fetchSemanticsNodes().isNotEmpty()
                }
                // Reading to the end of the instructions leaves the drawing's controls in view.
                composeRule.onNodeWithText(
                    if (index >= 3) "Antes de cortar tecido da peça" else "A imagem parada mostra",
                    substring = true,
                ).performScrollTo().assertIsDisplayed()
                composeRule.onNodeWithText(playLabel).assertIsDisplayed()
                if (index >= 3) {
                    composeRule.onNodeWithText("Margem de cintura zero: as linhas 1 e 2 coincidem.")
                        .performScrollTo().assertIsDisplayed()
                }
                composeRule.onNodeWithText("Etapa ${index + 1} de 6").assertIsDisplayed()
                composeRule.onNodeWithText("Anterior").assertIsDisplayed().let {
                    if (index == 0) it.assertIsNotEnabled() else it.assertIsEnabled()
                }
                composeRule.onNodeWithText(if (index == 5) "Concluir" else "Próximo")
                    .assertIsDisplayed()
                    .assertHeightIsAtLeast(48.dp)
                    .assertWidthIsAtLeast(48.dp)
                    .performClick()
            }
            composeRule.onNodeWithText("Etapa 6 de 6").assertDoesNotExist()
            composeRule.onNodeWithText("Tutorial de dobra do tecido").assertIsDisplayed()
            composeRule.onNodeWithText(MeasurementField.Length.label).performScrollTo()
                .performTextReplacement("70")
            composeRule.onNodeWithText("Tutorial de dobra do tecido").assertDoesNotExist()
        }
    }

    @Test
    fun closeAction_isTallEnough_andReturnsToResults() {
        openTutorial(dark = false, fontScale = 2f)
        composeRule.onNodeWithContentDescription("Fechar tutorial")
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithText("Preparar o tecido").assertDoesNotExist()
        composeRule.onNodeWithText("Tutorial de dobra do tecido").assertIsDisplayed()
    }

    private fun openTutorial(dark: Boolean, fontScale: Float) {
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                    MoldaTheme(darkTheme = dark, dynamicColor = false) {
                        SkirtCalculatorRoute()
                    }
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
            .performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
    }
}
