package com.almeidajr.molda

import android.animation.ValueAnimator
import android.os.SystemClock
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.skirt.ui.MeasurementField
import java.io.FileInputStream
import kotlin.math.atan2
import kotlin.math.hypot
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FabricMarkingTutorialTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun disabledSystemMotion_keepsCompletedFramesAfterReplay_forEveryStep() {
        val originalScale = shell("settings get global animator_duration_scale")
        try {
            shell("settings put global animator_duration_scale 1")
            openTutorial(listOf("80", "2", "60", "1", "2"), advanceToMarking = false)
            // Change the actual system preference while the tutorial is already open.
            shell("settings put global animator_duration_scale 0")
            composeRule.waitUntil(5_000) { !ValueAnimator.areAnimatorsEnabled() }
            composeRule.onNodeWithText(
                "Movimento desativado no sistema. O desenho mostra o resultado da etapa.",
            ).performScrollTo().assertIsDisplayed()
            listOf(
                "Dobra do tecido em Rive: etapa 1",
                "Dobra do tecido em Rive: etapa 2",
                "Dobra do tecido em Rive: etapa 3",
                "Marcação proporcional em Rive",
                "Corte proporcional em Rive",
                "Círculo desdobrado proporcional em Rive",
            ).forEachIndexed { index, description ->
                composeRule.waitUntil(15_000) {
                    composeRule.onAllNodes(
                        androidx.compose.ui.test.hasContentDescription(description),
                    )
                        .fetchSemanticsNodes().isNotEmpty()
                }
                val drawing = composeRule.onNodeWithContentDescription(
                    description,
                ).performScrollTo()
                composeRule.onNodeWithText(
                    "Movimento desativado no sistema. O desenho mostra o resultado da etapa.",
                ).performScrollTo().assertIsDisplayed()
                drawing.performScrollTo()
                fun frame(): List<Int> {
                    val pixels = drawing.captureToImage().toPixelMap()
                    return (0 until pixels.height step 4).flatMap { y ->
                        (0 until pixels.width step 4).map { x -> pixels[x, y].toArgb() }
                    }
                }
                composeRule.waitUntil(10_000) { frame().count { it == 0xff202020.toInt() } > 10 }
                val completed = frame()
                val playLabel = if (index == 3) "Reproduzir marcação" else "Reproduzir etapa"
                composeRule.onNodeWithText(playLabel).performScrollTo().performClick()
                drawing.performScrollTo()
                assertCompletedFrameAfterReplay(description, completed, ::frame)
                composeRule.onNodeWithText("Próximo").performScrollTo().let {
                    if (index == 5) it.assertIsNotEnabled() else it.performClick()
                }
            }
            shell("settings put global animator_duration_scale 1")
            composeRule.waitUntil(5_000) { ValueAnimator.areAnimatorsEnabled() }
            composeRule.onNodeWithText(
                "Movimento desativado no sistema. O desenho mostra o resultado da etapa.",
            ).assertDoesNotExist()
            composeRule.onNodeWithText("Reproduzir etapa").performScrollTo().assertIsDisplayed()
        } finally {
            shell(
                if (originalScale == "null") {
                    "settings delete global animator_duration_scale"
                } else {
                    "settings put global animator_duration_scale $originalScale"
                },
            )
        }
    }

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

    @Test
    fun foldingSteps_canBeReadReplayedAndRevisited_beforeMarkingCurrentRadii() {
        openTutorial(listOf("80", "2", "60", "1", "2"), advanceToMarking = false)
        composeRule.onNodeWithText("Preparar o tecido").assertIsDisplayed()
        composeRule.onNodeWithText("Anterior").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText(
            "Comece com papel ou retalho. Alise o tecido sem esticar e respeite o fio.",
        )
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Próximo").performScrollTo().performClick()
        composeRule.onNodeWithText("Primeira dobra").assertIsDisplayed()
        composeRule.onNodeWithText("Reproduzir etapa").performScrollTo().performClick()
        composeRule.onNodeWithText("Repetir etapa").performScrollTo().performClick()
        composeRule.onNodeWithText("Próximo").performScrollTo().performClick()
        composeRule.onNodeWithText("Segunda dobra").assertIsDisplayed()
        composeRule.onNodeWithText("Anterior").performScrollTo().performClick()
        composeRule.onNodeWithText("Primeira dobra").assertIsDisplayed()
        composeRule.onNodeWithText("Reproduzir etapa").performScrollTo().assertIsDisplayed()
        repeat(2) { composeRule.onNodeWithText("Próximo").performScrollTo().performClick() }
        composeRule.onNodeWithText("Marcar os raios").assertIsDisplayed()
        composeRule.onNodeWithText(
            "1. Costura da cintura: 13,05 cm",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Anterior").performScrollTo().performClick()
        composeRule.onNodeWithText("Segunda dobra").assertIsDisplayed()
    }

    @Test
    fun cuttingAndUnfolding_completeSequence_withConstructionBoundariesAndReplay() {
        openTutorial(listOf("80", "2", "60", "1", "2"))
        composeRule.onNodeWithText("Próximo").performScrollTo().performClick()
        composeRule.onNodeWithText("Cortar os arcos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2. Corte da cintura: 12,05 cm")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("4. Corte da barra: 75,05 cm")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reproduzir etapa").performScrollTo().performClick()
        composeRule.onNodeWithText("Repetir etapa").performScrollTo().performClick()
        composeRule.onNodeWithText("Próximo").performScrollTo().performClick()
        composeRule.onNodeWithText("Desdobrar o círculo").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            "Centro do recorte da cintura: mesma origem de todos os raios, agora no centro do círculo.",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Próximo").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText(
            "O círculo desdobrado não é um molde completo e não garante caimento.",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Anterior").performScrollTo().performClick()
        composeRule.onNodeWithText("Cortar os arcos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reproduzir etapa").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun cuttingAndUnfoldingPixels_preserveCurrentRadiiAndCoincidentAllowances() {
        listOf(
            listOf("80", "2", "60", "1", "2") to listOf(0.173892, 0.160568, 0.973352, 1.0),
            listOf("80", "2", "120", "1", "2") to listOf(0.096635, 0.089230, 0.985191, 1.0),
            listOf("80", "2", "60", "0", "0") to listOf(0.178653, 1.0),
        ).forEach { (inputs, ratios) ->
            openTutorial(inputs)
            composeRule.onNodeWithText("Próximo").performScrollTo().performClick()
            assertCircularDrawing("Corte proporcional em Rive", ratios, unfolded = false)
            composeRule.onNodeWithText("Próximo").performScrollTo().performClick()
            assertCircularDrawing(
                "Círculo desdobrado proporcional em Rive",
                ratios,
                unfolded = true,
            )
            composeRule.onNodeWithText("Fechar tutorial").performScrollTo().performClick()
        }
    }

    private fun assertCompletedFrameAfterReplay(
        description: String,
        completed: List<Int>,
        frame: () -> List<Int>,
    ) {
        val start = SystemClock.elapsedRealtime()
        composeRule.waitUntil(5_000) {
            assertTrue(
                "$description must retain the completed diagram with motion disabled",
                completed == frame(),
            )
            SystemClock.elapsedRealtime() - start >= 500
        }
    }

    private fun shell(command: String): String =
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
            .use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).bufferedReader().use {
                    it.readText().trim()
                }
            }

    private fun assertCircularDrawing(
        description: String,
        ratios: List<Double>,
        unfolded: Boolean,
    ) {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasContentDescription(description))
                .fetchSemanticsNodes().isNotEmpty()
        }
        val drawing = composeRule.onNodeWithContentDescription(description).performScrollTo()
        composeRule.waitUntil(10_000) {
            val pixels = drawing.captureToImage().toPixelMap()
            val scale = pixels.width / 448.0
            val origin = (if (unfolded) 224 else 24) * scale
            val outerRadius = (if (unfolded) 200 else 400) * scale
            val quadrants = if (unfolded) {
                listOf(1 to 1, -1 to 1, -1 to -1, 1 to -1)
            } else {
                listOf(1 to 1)
            }
            quadrants.all { (horizontal, vertical) ->
                val visibleRadii = mutableListOf<Double>()
                for (y in 0 until pixels.height) {
                    for (x in 0 until pixels.width) {
                        val dx = (x - origin) * horizontal
                        val dy = (y - origin) * vertical
                        val color = pixels[x, y]
                        if (atan2(dy, dx) in 0.2..1.35 &&
                            maxOf(color.red, color.green, color.blue) < 0.25f
                        ) {
                            visibleRadii += hypot(dx, dy) / outerRadius
                        }
                    }
                }
                ratios.all { expected ->
                    visibleRadii.count { kotlin.math.abs(it - expected) < 0.012 } >
                        3
                }
            } &&
                pixels[(origin + outerRadius * ratios.min() * 0.5).toInt(), origin.toInt()].red >
                0.95f &&
                pixels[
                    (origin + outerRadius * 0.5).toInt(),
                    (origin + outerRadius * 0.5).toInt(),
                ].blue < 0.9f
        }
    }

    private fun openTutorial(inputs: List<String>, advanceToMarking: Boolean = true) {
        MeasurementField.entries.zip(inputs).forEach { (field, value) ->
            composeRule.onNodeWithText(field.label).performScrollTo().performTextReplacement(value)
        }
        closeSoftKeyboard()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
        composeRule.onNodeWithText("Tutorial de dobra do tecido").performScrollTo().performClick()
        if (advanceToMarking) {
            repeat(3) {
                composeRule.onNodeWithText("Próximo").performScrollTo().performClick()
            }
        }
    }
}
