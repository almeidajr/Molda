package com.almeidajr.molda

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.skirt.domain.SkirtCalculation
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.skirt.domain.calculateSkirtRadii
import com.almeidajr.molda.skirt.ui.FoldingStep
import com.almeidajr.molda.skirt.ui.GuideAnimation
import com.almeidajr.molda.skirt.ui.guidePalette
import com.almeidajr.molda.ui.theme.MoldaTheme
import com.almeidajr.molda.ui.theme.moldaColorScheme
import java.io.FileInputStream
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FabricMarkingGuideTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun openSkirtCalculator() = composeRule.showSkirtCalculator()

    @Test
    fun enabledMotion_everyStepLoopsAfterPlay_withoutBlankFrames() {
        val originalScale = shell("settings get global animator_duration_scale")
        try {
            shell("settings put global animator_duration_scale 1")
            composeRule.waitUntil(5_000) { ValueAnimator.areAnimatorsEnabled() }
            openGuide(listOf("80", "2", "60", "1", "2"), advanceToMarking = false)
            val colors = drawingColors()
            listOf(
                "Dobra do tecido em Rive: etapa 1",
                "Dobra do tecido em Rive: etapa 2",
                "Dobra do tecido em Rive: etapa 3",
                "Marcação proporcional em Rive",
                "Corte proporcional em Rive",
                "Círculo desdobrado proporcional em Rive",
            ).forEachIndexed { index, description ->
                val label = if (index == 3) "Reproduzir marcação" else "Reproduzir etapa"
                composeRule.waitUntil(15_000) {
                    composeRule.onAllNodes(hasText(label))
                        .fetchSemanticsNodes().isNotEmpty()
                }
                val drawing = composeRule.onNodeWithContentDescription(description)
                // Native rendering starts after Compose exposes the drawing's semantics.
                composeRule.waitUntil(10_000) {
                    val image = drawing.captureToImage()
                    val frame = IntArray(image.width * image.height)
                    image.readPixels(frame)
                    frame.count(colors::isFabric) > 20
                }
                // Keep the rendered drawing onscreen while starting the infinite loop.
                composeRule.mainClock.autoAdvance = false
                composeRule.onNodeWithText(label)
                    .performSemanticsAction(SemanticsActions.OnClick)
                assertContinuousPlayback(description, index == 3, colors)
                composeRule.onNodeWithText("Pausar animação")
                    .performSemanticsAction(SemanticsActions.OnClick)
                composeRule.mainClock.advanceTimeBy(160)
                val paused = drawing.captureToImage().toPixelMap()
                composeRule.mainClock.advanceTimeBy(1_000)
                val stillPaused = drawing.captureToImage().toPixelMap()
                assertTrue(
                    "$description must remain still while paused",
                    (0 until paused.height step 8).all { y ->
                        (0 until paused.width step 8).all { x -> paused[x, y] == stillPaused[x, y] }
                    },
                )
                composeRule.onNodeWithText("Continuar animação")
                    .performSemanticsAction(SemanticsActions.OnClick)
                composeRule.mainClock.advanceTimeBy(160)
                composeRule.onNodeWithText("Pausar animação")
                    .performSemanticsAction(SemanticsActions.OnClick)
                composeRule.mainClock.autoAdvance = true
                advanceOrAssertConcluir(index)
            }
        } finally {
            shell("settings put global animator_duration_scale 0")
            composeRule.mainClock.autoAdvance = true
            shell("settings put global animator_duration_scale $originalScale")
        }
    }

    @Test
    fun disabledSystemMotion_keepsCompletedFramesAfterReplay_forEveryStep() {
        val originalScale = shell("settings get global animator_duration_scale")
        try {
            shell("settings put global animator_duration_scale 1")
            openGuide(listOf("80", "2", "60", "1", "2"), advanceToMarking = false)
            val colors = drawingColors()
            // Change the actual system preference while the guide is already open.
            shell("settings put global animator_duration_scale 0")
            composeRule.waitUntil(5_000) { !ValueAnimator.areAnimatorsEnabled() }
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
                        hasContentDescription(description),
                    )
                        .fetchSemanticsNodes().isNotEmpty()
                }
                val drawing = composeRule.onNodeWithContentDescription(description)
                // Visible without opening "Detalhes", above the step's lead.
                composeRule.onNodeWithText(
                    "Movimento desativado no sistema. O desenho mostra o resultado da etapa.",
                ).performScrollTo().assertIsDisplayed()
                // Every pixel: at low density the dashes are one pixel wide and a sampling grid
                // can miss them all.
                fun frame(): IntArray {
                    val image = drawing.captureToImage()
                    return IntArray(image.width * image.height).also { image.readPixels(it) }
                }
                composeRule.waitUntil(10_000) {
                    frame().count(colors::isInk) > 10
                }
                val completed = frame()
                val playLabel = if (index == 3) "Reproduzir marcação" else "Reproduzir etapa"
                composeRule.onNodeWithText(playLabel).performClick()
                assertCompletedFrameAfterReplay(description, completed, ::frame)
                advanceOrAssertConcluir(index)
            }
            shell("settings put global animator_duration_scale 1")
            composeRule.waitUntil(5_000) { ValueAnimator.areAnimatorsEnabled() }
            composeRule.onNodeWithText(
                "Movimento desativado no sistema. O desenho mostra o resultado da etapa.",
            ).assertDoesNotExist()
            composeRule.onNodeWithText("Reproduzir etapa").assertIsDisplayed()
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
            openGuide(inputs)
            val colors = drawingColors()
            composeRule.waitUntil(15_000) {
                composeRule.onAllNodes(
                    hasContentDescription("Marcação proporcional em Rive"),
                )
                    .fetchSemanticsNodes().isNotEmpty()
            }
            val drawing = composeRule.onNodeWithContentDescription("Marcação proporcional em Rive")
            composeRule.waitUntil(10_000) {
                val pixels = drawing.captureToImage().toPixelMap()
                val scale = pixels.width / 448.0
                val origin = 24 * scale
                val visibleRadii = mutableListOf<Double>()
                for (y in 0 until pixels.height) {
                    for (x in 0 until pixels.width) {
                        val angle = atan2(y - origin, x - origin)
                        if (angle in 0.2..1.35 && colors.isInk(pixels[x, y].toArgb())) {
                            visibleRadii += hypot(x - origin, y - origin) / (400 * scale)
                        }
                    }
                }
                expectedRatios.all { expected ->
                    visibleRadii.count {
                        abs(it - expected) <
                            0.009
                    } >
                        8
                }
            }
            if (inputs[3] == "0") {
                composeRule.onNodeWithText(
                    "Margem de cintura zero: as linhas 1 e 2 coincidem.",
                ).performScrollTo().assertIsDisplayed()
                composeRule.onNodeWithText(
                    "Margem de barra zero: as linhas 3 e 4 coincidem.",
                ).performScrollTo().assertIsDisplayed()
            }
            closeGuide()
        }
    }

    @Test
    fun validCalculation_opensOfflineMarkingWithCurrentDimensions() {
        openGuide(listOf("80", "2", "60", "1", "2"))
        composeRule.onNodeWithText("Marcar os raios").assertIsDisplayed()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodes(
                hasContentDescription("Marcação proporcional em Rive"),
            )
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription(
            "Marcação proporcional em Rive",
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "1. Costura da cintura: 13,05\u00A0cm",
        ).performScrollTo().assertIsDisplayed()
        playThenPause("Reproduzir marcação")
        closeGuide()
        composeRule.onNodeWithText(
            "Como levar os raios ao papel",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            SkirtDraftField.FinishedLength.labelText,
        ).performScrollTo().performTextReplacement("70")
        composeRule.onNodeWithText("Guia de dobra do tecido").assertDoesNotExist()
    }

    @Test
    fun foldingSteps_canBeReadReplayedAndRevisited_beforeMarkingCurrentRadii() {
        openGuide(listOf("80", "2", "60", "1", "2"), advanceToMarking = false)
        composeRule.onNodeWithText("Preparar o tecido").assertIsDisplayed()
        composeRule.onNodeWithText("Anterior").assertIsNotEnabled()
        composeRule.onNodeWithText("Alise o tecido sem esticar e respeite o fio.")
            .assertIsDisplayed()
        expandDetails()
        composeRule.onNodeWithText("Comece com papel ou retalho.")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Próximo").performClick()
        composeRule.onNodeWithText("Primeira dobra").assertIsDisplayed()
        awaitText("Reproduzir etapa")
        playThenPause("Reproduzir etapa") {
            composeRule.onNodeWithText("Repetir etapa")
                .performSemanticsAction(SemanticsActions.OnClick)
        }
        composeRule.onNodeWithText("Próximo").performClick()
        composeRule.onNodeWithText("Segunda dobra").assertIsDisplayed()
        composeRule.onNodeWithText("Anterior").performClick()
        composeRule.onNodeWithText("Primeira dobra").assertIsDisplayed()
        awaitText("Reproduzir etapa")
        composeRule.onNodeWithText("Reproduzir etapa").assertIsDisplayed()
        repeat(2) { composeRule.onNodeWithText("Próximo").performClick() }
        composeRule.onNodeWithText("Marcar os raios").assertIsDisplayed()
        composeRule.onNodeWithText(
            "1. Costura da cintura: 13,05\u00A0cm",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Anterior").performClick()
        composeRule.onNodeWithText("Segunda dobra").assertIsDisplayed()
    }

    @Test
    fun cuttingAndUnfolding_completeSequence_withConstructionBoundariesAndReplay() {
        openGuide(listOf("80", "2", "60", "1", "2"))
        composeRule.onNodeWithText("Próximo").performClick()
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
        awaitText("Reproduzir etapa")
        playThenPause("Reproduzir etapa") {
            composeRule.onNodeWithText("Repetir etapa")
                .performSemanticsAction(SemanticsActions.OnClick)
        }
        composeRule.onNodeWithText("Próximo").performClick()
        composeRule.onNodeWithText("Desdobrar o círculo").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Centro do recorte da cintura: mesma origem de todos os raios, agora no centro do círculo.",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Próximo").assertDoesNotExist()
        composeRule.onNodeWithText("Concluir").assertIsDisplayed().assertIsEnabled()
        expandDetails()
        composeRule.onNodeWithText(
            "O círculo desdobrado não é um molde completo e não garante caimento.",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Anterior").performClick()
        composeRule.onNodeWithText("Cortar os arcos").assertIsDisplayed()
        awaitText("Reproduzir etapa")
        composeRule.onNodeWithText("Reproduzir etapa").assertIsDisplayed()
    }

    @Test
    fun back_closesPlayer_returningKeyboardFocusToEntry_withResultsUnchanged() {
        openGuide(listOf("80", "2", "60", "1", "2"), advanceToMarking = false)
        composeRule.onNodeWithText("Próximo").performClick()
        composeRule.onNodeWithText("Etapa 2 de 6").assertIsDisplayed()
        // Touch mode leaves buttons unfocusable; keyboard users are the ones who need focus back.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        try {
            instrumentation.setInTouchMode(false)
            pressBack()
            composeRule.onNodeWithText("Etapa 2 de 6").assertDoesNotExist()
            composeRule.onNodeWithText("Guia de dobra do tecido").assertIsDisplayed()
                .assertIsFocused()
        } finally {
            instrumentation.setInTouchMode(true)
        }
        composeRule.onNodeWithContentDescription(
            "Linha 1, linha contínua. Costura da cintura: 13,05\u00A0cm",
        ).assertExists()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assertTextContains("80")
    }

    @Test
    fun recreation_keepsCurrentStep() {
        openGuide(listOf("80", "2", "60", "1", "2"), advanceToMarking = false)
        repeat(2) { composeRule.onNodeWithText("Próximo").performClick() }
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithText("Segunda dobra").assertIsDisplayed()
        composeRule.onNodeWithText("Etapa 3 de 6").assertIsDisplayed()
    }

    @Test
    fun cuttingAndUnfoldingPixels_preserveCurrentRadiiAndCoincidentAllowances() {
        listOf(
            SkirtDraft(Length(80.0), Length(2.0), Length(60.0), Length(1.0), Length(2.0)) to
                listOf(0.173892, 0.160568, 0.973352, 1.0),
            SkirtDraft(Length(80.0), Length(2.0), Length(120.0), Length(1.0), Length(2.0)) to
                listOf(0.096635, 0.089230, 0.985191, 1.0),
            SkirtDraft(Length(80.0), Length(2.0), Length(60.0), Length(0.0), Length(0.0)) to
                listOf(0.178653, 1.0),
        ).forEachIndexed { index, (draft, ratios) ->
            val radii = (calculateSkirtRadii(draft) as SkirtCalculation.Success).radii
            // The drawing paints no sheet of its own, so the dark theme needs its own check.
            (if (index == 0) listOf(false, true) else listOf(false)).forEach { darkTheme ->
                val colors = drawingColors(darkTheme)
                showDrawing(radii, FoldingStep.Cutting, darkTheme)
                assertCircularDrawing("Corte proporcional em Rive", ratios, false, colors)
                showDrawing(radii, FoldingStep.Unfolding, darkTheme)
                assertCircularDrawing(
                    "Círculo desdobrado proporcional em Rive",
                    ratios,
                    true,
                    colors,
                )
            }
        }
    }

    /**
     * Shows one step's still drawing at a fixed size. The player sizes it by window height, which
     * on a short low-density screen leaves the unfolded waist lines closer than one pixel apart.
     */
    private fun showDrawing(radii: SkirtRadii, step: FoldingStep, darkTheme: Boolean) {
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                // The full width of the narrowest phone; the still frame ignores motion.
                MoldaTheme(darkTheme = darkTheme) {
                    GuideAnimation(radii, step, motionEnabled = false, drawingSize = 288.dp)
                }
            }
        }
    }

    private fun assertContinuousPlayback(
        description: String,
        marking: Boolean,
        colors: DrawingColors,
    ) {
        val drawing = composeRule.onNodeWithContentDescription(description)
        val lateFrames = mutableSetOf<Int>()
        val replayLabel = if (marking) "Repetir marcação" else "Repetir etapa"
        var replayed = false
        val started = composeRule.mainClock.currentTime
        var elapsed = 0L
        // Samples a fixed span of test-clock time. A wall-clock wait would only measure how
        // fast the emulator renders those frames, and timed out on slow CI emulators.
        while (elapsed <= 22_000) {
            composeRule.mainClock.advanceTimeBy(160)
            elapsed = composeRule.mainClock.currentTime - started
            if (elapsed > 1_000 && !replayed) {
                // Invoke the accessible control without scrolling the drawing out of view.
                composeRule.onNodeWithText(replayLabel)
                    .performSemanticsAction(SemanticsActions.OnClick)
                composeRule.mainClock.advanceTimeBy(160)
                if (description == "Dobra do tecido em Rive: etapa 2") {
                    // Restart brings the moving left half back over this known fabric point.
                    val restarted = drawing.captureToImage().toPixelMap()
                    assertTrue(
                        "Replay must restart the fold, not only update its label",
                        colors.isFabric(
                            restarted[restarted.width / 4, restarted.height / 2].toArgb(),
                        ),
                    )
                }
                replayed = true
            }
            val image = drawing.captureToImage()
            val frame = IntArray(image.width * image.height)
            image.readPixels(frame)
            assertTrue(
                "$description: fabric must remain visible throughout playback " +
                    "at ${elapsed}ms (${image.width}x${image.height}); most frequent colours: " +
                    frame.toList().groupingBy { Integer.toHexString(it) }.eachCount()
                        .entries.sortedByDescending { it.value }.take(6),
                frame.count(colors::isFabric) > 20,
            )
            // Even the longest step has completed two full cycles by 18 seconds.
            if (elapsed > 18_000) lateFrames += frame.contentHashCode()
        }
        assertTrue("$description must keep moving after two cycles", lateFrames.size > 3)
    }

    private fun assertCompletedFrameAfterReplay(
        description: String,
        completed: IntArray,
        frame: () -> IntArray,
    ) {
        val start = SystemClock.elapsedRealtime()
        composeRule.waitUntil(5_000) {
            assertTrue(
                "$description must retain the completed diagram with motion disabled",
                completed.contentEquals(frame()),
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
        colors: DrawingColors,
    ) {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodes(hasContentDescription(description))
                .fetchSemanticsNodes().isNotEmpty()
        }
        val drawing = composeRule.onNodeWithContentDescription(description)
        // Include partially covered stroke pixels when software MSAA downsamples thin lines.
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
                        if (atan2(dy, dx) in 0.2..1.35 && colors.isInk(pixels[x, y].toArgb())) {
                            visibleRadii += hypot(dx, dy) / outerRadius
                        }
                    }
                }
                ratios.all { expected ->
                    visibleRadii.count { abs(it - expected) < 0.012 } >
                        3
                }
            } &&
                // The waist cutout shows the player's frame through the drawing.
                colors.isFrame(
                    pixels[(origin + outerRadius * ratios.min() * 0.5).toInt(), origin.toInt()]
                        .toArgb(),
                ) &&
                colors.isFabric(
                    pixels[
                        (origin + outerRadius * 0.5).toInt(),
                        (origin + outerRadius * 0.5).toInt(),
                    ].toArgb(),
                )
        }
    }

    /** The colours the drawing binds under [darkTheme], as [MoldaTheme] resolves them. */
    private fun drawingColors(
        darkTheme: Boolean = composeRule.activity.resources.configuration.isNightModeActive,
    ): DrawingColors = DrawingColors(moldaColorScheme(composeRule.activity, darkTheme, true))

    /** Classifies captured pixels as the drawing's ink, fabric or the frame behind it. */
    private class DrawingColors(scheme: ColorScheme) {
        private val palette = guidePalette(scheme).mapValues { it.value.toArgb() }
        private val ink = palette.getValue("ink")
        private val fabric = palette.getValue("fabric")
        private val frame = scheme.surfaceContainerHigh.toArgb()

        // Summed channel difference allowed for rasterized solid fills.
        private val tolerance = 12

        fun isFabric(pixel: Int): Boolean = distance(pixel, fabric) <= tolerance ||
            distance(pixel, palette.getValue("fabricFold")) <= tolerance

        fun isFrame(pixel: Int): Boolean = distance(pixel, frame) <= tolerance

        // Anti-aliased strokes blend into the fabric; count those nearer the ink.
        fun isInk(pixel: Int): Boolean = distance(pixel, ink) * 2 < distance(ink, fabric)

        private fun distance(first: Int, second: Int): Int =
            abs(Color.red(first) - Color.red(second)) +
                abs(Color.green(first) - Color.green(second)) +
                abs(Color.blue(first) - Color.blue(second))
    }

    private fun openGuide(inputs: List<String>, advanceToMarking: Boolean = true) {
        SkirtDraftField.entries.zip(inputs).forEach { (field, value) ->
            composeRule.onNodeWithText(
                field.labelText,
            ).performScrollTo().performTextReplacement(value)
        }
        closeSoftKeyboard()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
        composeRule.onNodeWithText("Guia de dobra do tecido").performScrollTo().performClick()
        if (advanceToMarking) {
            repeat(3) { composeRule.onNodeWithText("Próximo").performClick() }
        }
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Opens the current step's collapsed "Detalhes" disclosure. */
    private fun expandDetails() {
        composeRule.onNodeWithText("Detalhes").performScrollTo().performClick()
    }

    private fun closeGuide() {
        composeRule.onNodeWithContentDescription("Fechar guia").performClick()
    }

    private fun advanceOrAssertConcluir(index: Int) {
        if (index == 5) {
            composeRule.onNodeWithText("Concluir").assertIsDisplayed()
        } else {
            composeRule.onNodeWithText("Próximo").performClick()
        }
    }

    /**
     * Starts playback, runs [whilePlaying], then pauses. A playing Rive loop awaits every frame,
     * so Compose never idles while the test clock auto-advances; the clock is held meanwhile.
     */
    private fun playThenPause(playLabel: String, whilePlaying: () -> Unit = {}) {
        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithText(playLabel).performSemanticsAction(SemanticsActions.OnClick)
            composeRule.mainClock.advanceTimeByFrame()
            whilePlaying()
            composeRule.mainClock.advanceTimeByFrame()
            // With system motion off nothing plays, so there is no pause control.
            val pause = composeRule.onAllNodes(hasText("Pausar animação")).fetchSemanticsNodes()
            if (pause.isNotEmpty()) {
                composeRule.onNodeWithText("Pausar animação")
                    .performSemanticsAction(SemanticsActions.OnClick)
                composeRule.mainClock.advanceTimeByFrame()
            }
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }
}
