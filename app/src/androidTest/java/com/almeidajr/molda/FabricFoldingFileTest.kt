package com.almeidajr.molda

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.platform.app.InstrumentationRegistry
import app.rive.Artboard
import app.rive.Fit
import app.rive.RenderBackend
import app.rive.RiveFile
import app.rive.RiveFileSource
import app.rive.SoftwareRenderBuffer
import app.rive.StateMachine
import app.rive.ViewModelInstance
import app.rive.ViewModelSource
import app.rive.core.RiveWorker
import app.rive.runtime.kotlin.core.Rive
import app.rive.runtime.kotlin.core.ViewModel.PropertyDataType
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.skirt.domain.SkirtCalculation
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.skirt.domain.calculateSkirtRadii
import com.almeidajr.molda.skirt.ui.FoldingStep
import com.almeidajr.molda.skirt.ui.guidePalette
import com.almeidajr.molda.skirt.ui.markingDiameters
import com.almeidajr.molda.ui.theme.moldaColorScheme
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The shipped `fabric_folding.riv`, bound as the guide binds it and rendered offscreen into a
 * bitmap on Rive's own clock, as Rive recommends for snapshot tests
 * (https://rive.app/docs/runtimes/android/rendering-to-a-bitmap). No screen, no wall-clock frames.
 */
class FabricFoldingFileTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun guideFile_declaresEveryStepArtboard_andEveryBoundProperty() = runWithGuideFile { file ->
        // Given: what the guide binds, by name and type.
        val bound = guidePalette(scheme(isDark = false)).keys
            .associateWith { PropertyDataType.COLOR } +
            markingDiameters(radii(ReferenceDraft)).keys
                .associateWith { PropertyDataType.NUMBER } +
            ("play" to PropertyDataType.BOOLEAN)

        // When
        val artboards = file.getArtboardNames()
        // The guide binds an instance of the default artboard's default view model.
        val viewModel = Artboard.create(file).use { file.getDefaultViewModelInfo(it).viewModelName }
        val properties = file.getViewModelProperties(viewModel).associate { it.name to it.type }

        // Then
        assertTrue(
            artboards.containsAll(FoldingStep.entries.map { it.artboard }),
            "Artboards: $artboards",
        )
        assertEquals(bound, properties.filterKeys(bound::containsKey))
    }

    @Test
    fun stillDrawing_everyStepInBothThemes_showsFabricOnAnUnpaintedFrame_andHoldsStill() =
        runWithGuideFile { file ->
            FoldingStep.entries.forEach { step ->
                listOf(false, true).forEach { isDark ->
                    // When: motion off, as the guide shows it, and again much later.
                    val (completed, later) =
                        file.render(
                            step,
                            scheme(isDark),
                            play = false,
                            listOf(0.seconds, 3.seconds),
                        )

                    // Then
                    val label = "$step (dark: $isDark)"
                    val colors = DrawingColors(scheme(isDark))
                    assertTrue(completed.count(colors::isFabric) > MIN_FABRIC, "$label fabric")
                    // The host's frame shows through: the file paints no background.
                    assertTrue(completed.corners().all { Color.alpha(it) == 0 }, "$label corners")
                    assertTrue(completed.contentEquals(later), "$label must hold still")
                }
            }
        }

    @Test
    fun measuredDrawings_placeEachLineAtItsBoundDiameter_inBothThemes() = runWithGuideFile { file ->
        // Given: normal and longer skirts, and zero allowances, whose lines coincide.
        listOf(
            ReferenceDraft,
            ReferenceDraft.copy(finishedLength = Length(120.0)),
            ReferenceDraft.copy(waistSeamAllowance = Length(0.0), hemAllowance = Length(0.0)),
        ).forEach { draft ->
            val diameters = markingDiameters(radii(draft))
            val ratios = diameters.values.map { it / diameters.getValue("hemCutting").toDouble() }
            listOf(false, true).forEach { isDark ->
                val colors = DrawingColors(scheme(isDark))
                listOf(FoldingStep.Marking, FoldingStep.Cutting, FoldingStep.Unfolding).forEach {
                    // When
                    val pixels = file.render(
                        it,
                        scheme(isDark),
                        play = false,
                        draft = draft,
                    ).single()

                    // Then
                    val label = "$it for $draft (dark: $isDark)"
                    assertTrue(pixels.showsArcsAt(ratios, it, colors), "$label arcs")
                    if (it != FoldingStep.Marking) {
                        assertTrue(pixels.showsCutQuarters(ratios.min(), it, colors), label)
                    }
                }
            }
        }
    }

    @Test
    fun playingDrawing_everyStep_keepsMovingWithoutBlankFrames() = runWithGuideFile { file ->
        FoldingStep.entries.forEach { step ->
            // When: mid-sweep, then well past two full cycles of the longest step.
            val frames = file.render(step, scheme(isDark = false), play = true, PlayingTimes)

            // Then
            val colors = DrawingColors(scheme(isDark = false))
            assertTrue(frames.all { it.count(colors::isFabric) > MIN_FABRIC }, "$step went blank")
            assertFalse(frames[0].contentEquals(frames[1]), "$step must move")
            assertTrue(
                frames.drop(2).map { it.contentHashCode() }.distinct().size > 1,
                "$step must keep moving after two cycles",
            )
        }
    }

    /** Loads the shipped file on a Rive worker of its own, polled on the main thread. */
    private fun runWithGuideFile(block: suspend TestScope.(RiveFile) -> Unit) = runTest {
        Rive.init(context)
        // The guide's backend: OpenGL renders blank frames on the emulator's software driver.
        val worker = RiveWorker(RenderBackend.Vulkan)
        // Rive confirms loads and queries only while the main thread polls it.
        backgroundScope.launch { worker.beginPolling(Resumed.lifecycle) }
        try {
            RiveFile.load(RiveFileSource.RawRes(R.raw.fabric_folding, context.resources), worker)
                .use { block(it) }
        } finally {
            worker.release("FabricFoldingFileTest", "Test finished")
        }
    }

    /**
     * Renders [step]'s pixels bound as `GuideDrawing` binds it, at each of [times] after it starts.
     */
    private suspend fun RiveFile.render(
        step: FoldingStep,
        colors: ColorScheme,
        play: Boolean,
        times: List<Duration> = listOf(Duration.ZERO),
        draft: SkirtDraft = ReferenceDraft,
    ): List<IntArray> = Artboard.create(this).use { defaultArtboard ->
        ViewModelInstance.create(
            this,
            ViewModelSource.DefaultForArtboard(defaultArtboard).defaultInstance(),
        ).use { viewModel ->
            guidePalette(colors).forEach { (name, color) ->
                viewModel.setColor(name, color.toArgb())
            }
            markingDiameters(radii(draft)).forEach { (name, diameter) ->
                viewModel.setNumber(name, diameter)
            }
            viewModel.setBoolean("play", play)
            Artboard.create(this, step.artboard).use { artboard ->
                StateMachine.create(artboard).use { stateMachine ->
                    riveWorker.setMainViewModelInstance(
                        stateMachine.stateMachineHandle,
                        viewModel.instanceHandle,
                    )
                    riveWorker.bind(stateMachine.stateMachineHandle)
                    renderFrames(artboard, stateMachine, times)
                }
            }
        }
    }

    /**
     * Renders [artboard]'s pixels at each of [times] after [stateMachine] starts, advancing one
     * 60 fps frame at a time as a display does.
     */
    private fun RiveFile.renderFrames(
        artboard: Artboard,
        stateMachine: StateMachine,
        times: List<Duration>,
    ): List<IntArray> = SoftwareRenderBuffer(SIZE, SIZE, riveWorker).use { buffer ->
        stateMachine.advance(Duration.ZERO)
        var elapsed = Duration.ZERO
        times.map { time ->
            while (elapsed < time) {
                stateMachine.advance(FRAME)
                elapsed += FRAME
            }
            val bitmap = createBitmap(SIZE, SIZE)
            buffer.renderInto(bitmap, artboard, stateMachine, Fit.Contain(), Color.TRANSPARENT)
                .pixels().also { bitmap.recycle() }
        }
    }

    /**
     * Whether every drawn quarter shows ink arcs at each of [ratios] of the hem cutting radius: one
     * quarter from the marking origin, or all four of the unfolded circle.
     */
    private fun IntArray.showsArcsAt(
        ratios: List<Double>,
        step: FoldingStep,
        colors: DrawingColors,
    ): Boolean {
        val (origin, outerRadius) = step.circle()
        val quarters = if (step == FoldingStep.Unfolding) {
            listOf(1 to 1, -1 to 1, -1 to -1, 1 to -1)
        } else {
            listOf(1 to 1)
        }
        return quarters.all { (horizontal, vertical) ->
            val inkRadii = indices.filter { colors.isInk(this[it]) }.mapNotNull { index ->
                val dx = (index % SIZE - origin) * horizontal
                val dy = (index / SIZE - origin) * vertical
                // Away from the fold edges and the line numbers at the arc ends.
                if (atan2(dy, dx) in 0.2..1.35) hypot(dx, dy) / outerRadius else null
            }
            ratios.all { ratio ->
                inkRadii.count { abs(it - ratio) < ARC_TOLERANCE * SCALE / outerRadius } > 8
            }
        }
    }

    /** Whether the frame shows through the waist cutout, with fabric between the arcs. */
    private fun IntArray.showsCutQuarters(
        waistRatio: Double,
        step: FoldingStep,
        colors: DrawingColors,
    ): Boolean {
        val (origin, outerRadius) = step.circle()
        val cutout = (origin + outerRadius * waistRatio * 0.5).toInt()
        val skirt = (origin + outerRadius * 0.5).toInt()
        return Color.alpha(this[origin.toInt() * SIZE + cutout]) == 0 &&
            colors.isFabric(this[skirt * SIZE + skirt])
    }

    /** The step's circle in pixels: its center and its hem cutting radius. */
    private fun FoldingStep.circle(): Pair<Double, Double> = if (this == FoldingStep.Unfolding) {
        224.0 * SCALE to 200.0 * SCALE
    } else {
        24.0 * SCALE to 400.0 * SCALE
    }

    private fun IntArray.corners(): List<Int> =
        listOf(0, SIZE - 1, size - SIZE, size - 1).map { this[it] }

    /**
     * The bitmap's pixels, top row first. shortcut: Rive 11.15 hands Vulkan renders back bottom
     * row first, so this reads the rows in reverse; drop that once the drawings fail upside down.
     */
    private fun Bitmap.pixels(): IntArray = IntArray(width * height).also {
        getPixels(it, (height - 1) * width, -width, 0, 0, width, height)
    }

    private fun scheme(isDark: Boolean): ColorScheme = moldaColorScheme(context, isDark)

    private fun radii(draft: SkirtDraft): SkirtRadii =
        (calculateSkirtRadii(draft) as SkirtCalculation.Success).radii

    /** Classifies rendered pixels as the drawing's ink or fabric under [scheme]'s palette. */
    private class DrawingColors(scheme: ColorScheme) {
        private val palette = guidePalette(scheme).mapValues { it.value.toArgb() }
        private val ink = palette.getValue("ink")
        private val fabric = palette.getValue("fabric")

        // Summed channel difference allowed for rasterized solid fills.
        private val tolerance = 12

        fun isFabric(pixel: Int): Boolean = Color.alpha(pixel) == 255 &&
            (
                distance(pixel, fabric) <= tolerance ||
                    distance(pixel, palette.getValue("fabricFold")) <= tolerance
                )

        // Anti-aliased strokes blend into the fabric; count those nearer the ink.
        fun isInk(pixel: Int): Boolean =
            Color.alpha(pixel) > 0 && distance(pixel, ink) * 2 < distance(ink, fabric)

        private fun distance(first: Int, second: Int): Int =
            abs(Color.red(first) - Color.red(second)) +
                abs(Color.green(first) - Color.green(second)) +
                abs(Color.blue(first) - Color.blue(second))
    }

    /** Keeps Rive polling for the whole test: no activity owns these offscreen drawings. */
    private object Resumed : LifecycleOwner {
        override val lifecycle: Lifecycle =
            LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
    }

    private companion object {
        /** The artboards' 448 units at two pixels each, under contain fitting. */
        const val SCALE = 2
        const val SIZE = 448 * SCALE

        /** How far, in artboard units, a drawn arc may sit from its bound radius. */
        const val ARC_TOLERANCE = 1.5

        /** A tenth of the frame: every step's sheet of fabric covers more. */
        const val MIN_FABRIC = SIZE * SIZE / 10

        val FRAME = (1.0 / 60).seconds

        /** An 80 cm waist with 2 cm ease, 60 cm long, with 1 and 2 cm allowances. */
        val ReferenceDraft =
            SkirtDraft(Length(80.0), Length(2.0), Length(60.0), Length(1.0), Length(2.0))

        /** Two mid-sweep frames, then four once the longest step (7 s a cycle) looped twice. */
        val PlayingTimes = listOf(500.milliseconds, 1500.milliseconds) +
            listOf(20, 21, 22, 23).map { it.seconds }
    }
}
