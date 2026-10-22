package com.almeidajr.molda

import android.graphics.Color
import androidx.activity.compose.setContent
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.skirt.domain.SkirtCalculation
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.skirt.domain.calculateSkirtRadii
import com.almeidajr.molda.skirt.ui.FoldingStep
import com.almeidajr.molda.skirt.ui.GuideAnimation
import com.almeidajr.molda.skirt.ui.guidePalette
import com.almeidajr.molda.skirt.ui.rememberGuideFile
import com.almeidajr.molda.ui.theme.MoldaTheme
import com.almeidajr.molda.ui.theme.moldaColorScheme
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Rive drawing's native pixels. Each test waits for frames only through [awaitRiveFrame];
 * the guide's flow and controls are [FabricFoldingGuideTest]'s, on the Compose clock.
 */
@RunWith(AndroidJUnit4::class)
class FabricFoldingRenderingTest {
    @get:Rule(order = 0)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 1)
    val animatorScale = AnimatorDurationScaleRule(composeRule)

    @Before
    fun openSkirtCalculator() {
        composeRule.showEmptyApp()
        composeRule.openSkirtCalculator()
    }

    @Test
    fun enabledMotion_everyStepLoopsAfterPlay_withoutBlankFrames() {
        // Given
        animatorScale.set(1f)
        openGuide(listOf("80", "2", "60", "1", "2"), advanceToMarking = false)
        val colors = drawingColors()
        STEP_DRAWINGS.forEachIndexed { index, description ->
            val label = if (index == 3) "Reproduzir marcação" else "Reproduzir etapa"
            composeRule.awaitRiveFrame(description) { it.pixels().count(colors::isFabric) > 20 }
            val drawing = composeRule.onNodeWithContentDescription(description)
            // Keep the rendered drawing onscreen while starting the infinite loop.
            composeRule.mainClock.autoAdvance = false

            // When
            composeRule.onNodeWithText(label).performSemanticsAction(SemanticsActions.OnClick)

            // Then
            assertContinuousPlayback(description, index == 3, colors)
            composeRule.onNodeWithText("Pausar animação")
                .performSemanticsAction(SemanticsActions.OnClick)
            composeRule.mainClock.advanceTimeBy(160)
            val paused = drawing.captureToImage().toPixelMap()
            composeRule.mainClock.advanceTimeBy(1_000)
            val stillPaused = drawing.captureToImage().toPixelMap()
            assertTrue(
                (0 until paused.height step 8).all { y ->
                    (0 until paused.width step 8).all { x -> paused[x, y] == stillPaused[x, y] }
                },
                "$description must remain still while paused",
            )
            composeRule.onNodeWithText("Continuar animação")
                .performSemanticsAction(SemanticsActions.OnClick)
            composeRule.mainClock.advanceTimeBy(160)
            composeRule.onNodeWithText("Pausar animação")
                .performSemanticsAction(SemanticsActions.OnClick)
            composeRule.mainClock.autoAdvance = true
            advanceOrAssertConcluir(index)
        }
    }

    @Test
    fun openingAndNext_showEachDrawingInPlace_withoutTextOrBlankFrames() {
        // Given
        animatorScale.set(1f)
        val colors = drawingColors()

        // When
        openGuide(listOf("80", "2", "60", "1", "2"), advanceToMarking = false)

        // Then
        STEP_DRAWINGS.forEachIndexed { index, description ->
            val frameText = hasText("", substring = true) and
                hasAnyAncestor(hasContentDescription(description))
            composeRule.awaitRiveFrame(description) {
                // Sampled on every poll, so it also holds while the drawing loads.
                composeRule.onAllNodes(frameText).assertCountEquals(0)
                it.pixels().count(colors::isFabric) > 20
            }
            // Once shown, the resting drawing never drops back to an empty frame.
            repeat(5) {
                composeRule.mainClock.advanceTimeBy(160)
                assertTrue(
                    composeRule.onNodeWithContentDescription(description).captureToImage()
                        .pixels().count(colors::isFabric) > 20,
                    "$description must stay drawn once it appears",
                )
            }
            advanceOrAssertConcluir(index)
        }
    }

    @Test
    fun disabledSystemMotion_keepsCompletedFramesAfterReplay_forEveryStep() {
        // Given
        animatorScale.set(1f)
        openGuide(listOf("80", "2", "60", "1", "2"), advanceToMarking = false)
        val colors = drawingColors()

        // When: the actual system preference changes while the guide is already open.
        animatorScale.set(0f)

        // Then
        STEP_DRAWINGS.forEachIndexed { index, description ->
            // Visible without opening "Detalhes", above the step's lead.
            composeRule.onNodeWithText(MOTION_OFF_NOTE).performScrollTo().assertIsDisplayed()
            // Every pixel: at low density the dashes are one pixel wide and a sampling grid
            // can miss them all.
            val completed = composeRule.awaitRiveFrame(description) {
                it.pixels().count(colors::isInk) > 10
            }.pixels()
            val playLabel = if (index == 3) "Reproduzir marcação" else "Reproduzir etapa"
            composeRule.onNodeWithText(playLabel).performClick()
            composeRule.mainClock.advanceTimeBy(500)
            assertTrue(
                completed.contentEquals(
                    composeRule.onNodeWithContentDescription(description).captureToImage().pixels(),
                ),
                "$description must retain the completed diagram with motion disabled",
            )
            advanceOrAssertConcluir(index)
        }
        animatorScale.set(1f)
        composeRule.onNodeWithText(MOTION_OFF_NOTE).assertDoesNotExist()
        composeRule.onNodeWithText("Reproduzir etapa").assertIsDisplayed()
    }

    @Test
    fun markingPixels_preserveCalculatedRatios_whenLengthChangesAndAllowancesAreZero() {
        // Independently worked examples: waist 80 + ease 2, then 60/120 cm lengths.
        listOf(
            listOf("80", "2", "60", "1", "2") to listOf(0.173892, 0.160568, 0.973352, 1.0),
            listOf("80", "2", "120", "1", "2") to listOf(0.096635, 0.089230, 0.985191, 1.0),
            listOf("80", "2", "60", "0", "0") to listOf(0.178653, 1.0),
        ).forEach { (inputs, expectedRatios) ->
            // Given
            val colors = drawingColors()

            // When
            openGuide(inputs)

            // Then
            composeRule.awaitRiveFrame("Marcação proporcional em Rive") {
                showsMarkingRatios(it, expectedRatios, colors)
            }
            if (inputs[3] == "0") {
                composeRule.onNodeWithText("Margem de cintura zero: as linhas 1 e 2 coincidem.")
                    .performScrollTo().assertIsDisplayed()
                composeRule.onNodeWithText("Margem de barra zero: as linhas 3 e 4 coincidem.")
                    .performScrollTo().assertIsDisplayed()
            }
            composeRule.onNodeWithContentDescription("Fechar guia").performClick()
        }
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
            // Given
            val radii = (calculateSkirtRadii(draft) as SkirtCalculation.Success).radii
            // The drawing paints no sheet of its own, so the dark theme needs its own check.
            (if (index == 0) listOf(false, true) else listOf(false)).forEach { darkTheme ->
                val colors = drawingColors(darkTheme)

                // When/Then
                showDrawing(radii, FoldingStep.Cutting, darkTheme)
                composeRule.awaitRiveFrame("Corte proporcional em Rive") {
                    isCircularDrawing(it, ratios, unfolded = false, colors)
                }
                showDrawing(radii, FoldingStep.Unfolding, darkTheme)
                composeRule.awaitRiveFrame("Círculo desdobrado proporcional em Rive") {
                    isCircularDrawing(it, ratios, unfolded = true, colors)
                }
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
                    GuideAnimation(
                        rememberGuideFile(),
                        radii,
                        step,
                        motionEnabled = false,
                        drawingSize = 288.dp,
                    )
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
                        colors.isFabric(
                            restarted[restarted.width / 4, restarted.height / 2].toArgb(),
                        ),
                        "Replay must restart the fold, not only update its label",
                    )
                }
                replayed = true
            }
            val image = drawing.captureToImage()
            val frame = image.pixels()
            // The histogram boxes and hex-formats every pixel, so build it only on failure.
            if (frame.count(colors::isFabric) <= 20) {
                fail(
                    "$description: fabric must remain visible throughout playback " +
                        "at ${elapsed}ms (${image.width}x${image.height}); " +
                        "most frequent colours: " +
                        frame.toList().groupingBy { Integer.toHexString(it) }.eachCount()
                            .entries.sortedByDescending { it.value }.take(6),
                )
            }
            // Even the longest step has completed two full cycles by 18 seconds.
            if (elapsed > 18_000) lateFrames += frame.contentHashCode()
        }
        assertTrue(lateFrames.size > 3, "$description must keep moving after two cycles")
    }

    /** Whether the marking's quarter shows an ink arc at each of [expectedRatios]. */
    private fun showsMarkingRatios(
        image: ImageBitmap,
        expectedRatios: List<Double>,
        colors: DrawingColors,
    ): Boolean {
        val pixels = image.toPixelMap()
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
        return expectedRatios.all { expected ->
            visibleRadii.count { abs(it - expected) < 0.009 } >
                8
        }
    }

    /**
     * Whether every drawn quarter shows ink arcs at [ratios], the frame through the waist cutout
     * and fabric between the arcs. Partially covered stroke pixels count as ink, since software
     * MSAA downsamples thin lines.
     */
    private fun isCircularDrawing(
        image: ImageBitmap,
        ratios: List<Double>,
        unfolded: Boolean,
        colors: DrawingColors,
    ): Boolean {
        val pixels = image.toPixelMap()
        val scale = pixels.width / 448.0
        val origin = (if (unfolded) 224 else 24) * scale
        val outerRadius = (if (unfolded) 200 else 400) * scale
        val quadrants = if (unfolded) {
            listOf(1 to 1, -1 to 1, -1 to -1, 1 to -1)
        } else {
            listOf(1 to 1)
        }
        return quadrants.all { (horizontal, vertical) ->
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
            ratios.all { expected -> visibleRadii.count { abs(it - expected) < 0.012 } > 3 }
        } &&
            // The waist cutout shows the player's frame through the drawing.
            colors.isFrame(
                pixels[(origin + outerRadius * ratios.min() * 0.5).toInt(), origin.toInt()]
                    .toArgb(),
            ) &&
            colors.isFabric(
                pixels[(origin + outerRadius * 0.5).toInt(), (origin + outerRadius * 0.5).toInt()]
                    .toArgb(),
            )
    }

    private fun ImageBitmap.pixels(): IntArray = IntArray(width * height).also { readPixels(it) }

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
            composeRule.onNodeWithText(field.labelText).performScrollTo()
                .performTextReplacement(value)
        }
        closeSoftKeyboard()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
        composeRule.onNodeWithText("Guia de dobra do tecido").performScrollTo().performClick()
        if (advanceToMarking) {
            repeat(3) { composeRule.onNodeWithText("Próximo").performClick() }
        }
    }

    private fun advanceOrAssertConcluir(index: Int) {
        if (index == STEP_DRAWINGS.lastIndex) {
            composeRule.onNodeWithText("Concluir").assertIsDisplayed()
        } else {
            composeRule.onNodeWithText("Próximo").performClick()
        }
    }

    private companion object {
        const val MOTION_OFF_NOTE =
            "Movimento desativado no sistema. O desenho mostra o resultado da etapa."

        /** Each step's drawing, by its accessible description, in step order. */
        val STEP_DRAWINGS = listOf(
            "Dobra do tecido em Rive: etapa 1",
            "Dobra do tecido em Rive: etapa 2",
            "Dobra do tecido em Rive: etapa 3",
            "Marcação proporcional em Rive",
            "Corte proporcional em Rive",
            "Círculo desdobrado proporcional em Rive",
        )
    }
}
