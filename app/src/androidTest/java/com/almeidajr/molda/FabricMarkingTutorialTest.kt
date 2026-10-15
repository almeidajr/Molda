package com.almeidajr.molda

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.skirt.ui.MeasurementField
import kotlin.math.atan2
import kotlin.math.hypot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FabricMarkingTutorialTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun markingPixels_preserveCalculatedRatios_whenLengthChangesAndAllowancesAreZero() {
        // Independently worked examples: waist 80 + ease 2, then 60/120 cm lengths.
        listOf(
            listOf("80", "2", "60", "1", "2") to listOf(0.173892, 0.160568, 0.973352, 1.0),
            listOf("80", "2", "120", "1", "2") to listOf(0.096635, 0.089230, 0.985191, 1.0),
            listOf("80", "2", "60", "0", "0") to listOf(0.178653, 1.0),
        ).forEach { (inputs, expectedRatios) ->
            openTutorial(inputs)
            composeRule.waitUntil(15_000) {
                composeRule.onAllNodes(
                    androidx.compose.ui.test.hasContentDescription("Marcação proporcional em Rive"),
                )
                    .fetchSemanticsNodes().isNotEmpty()
            }
            val drawing = composeRule.onNodeWithContentDescription("Marcação proporcional em Rive")
                .performScrollTo()
            composeRule.waitUntil(10_000) {
                val pixels = drawing.captureToImage().toPixelMap()
                val scale = pixels.width / 448.0
                val origin = 24 * scale
                val visibleRadii = mutableListOf<Double>()
                for (y in 0 until pixels.height) {
                    for (x in 0 until pixels.width) {
                        val color = pixels[x, y]
                        val angle = atan2(y - origin, x - origin)
                        if (angle in 0.2..1.35 && maxOf(
                                color.red,
                                color.green,
                                color.blue,
                            ) < 0.25f
                        ) {
                            visibleRadii += hypot(x - origin, y - origin) / (400 * scale)
                        }
                    }
                }
                expectedRatios.all { expected ->
                    visibleRadii.count {
                        kotlin.math.abs(it - expected) <
                            0.009
                    } >
                        8
                }
            }
            if (inputs[3] == "0") {
                composeRule.onNodeWithText(
                    "Margem de cintura zero: linhas 1 e 2 coincidem.",
                ).performScrollTo().assertIsDisplayed()
                composeRule.onNodeWithText(
                    "Margem de barra zero: linhas 3 e 4 coincidem.",
                ).performScrollTo().assertIsDisplayed()
            }
            composeRule.onNodeWithText("Fechar tutorial").performScrollTo().performClick()
        }
    }

    @Test
    fun validCalculation_opensOfflineMarkingWithCurrentDimensions() {
        openTutorial(listOf("80", "2", "60", "1", "2"))
        composeRule.onNodeWithText("Marcar os raios").assertIsDisplayed()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasContentDescription("Marcação proporcional em Rive"),
            )
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription(
            "Marcação proporcional em Rive",
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "1. Costura da cintura: 13,05 cm",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reproduzir marcação").performScrollTo().performClick()
        composeRule.onNodeWithText("Fechar tutorial").performScrollTo().performClick()
        composeRule.onNodeWithText(
            "Como levar os raios ao papel",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            MeasurementField.Length.label,
        ).performScrollTo().performTextReplacement("70")
        composeRule.onNodeWithText("Tutorial de dobra do tecido").assertDoesNotExist()
    }

    private fun openTutorial(inputs: List<String>) {
        MeasurementField.entries.zip(inputs).forEach { (field, value) ->
            composeRule.onNodeWithText(field.label).performScrollTo().performTextReplacement(value)
        }
        closeSoftKeyboard()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
        composeRule.onNodeWithText("Tutorial de dobra do tecido").performScrollTo().performClick()
    }
}
