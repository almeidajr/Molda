package com.almeidajr.molda

import android.content.pm.ActivityInfo
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import com.almeidajr.molda.skirt.ui.MeasurementField
import com.almeidajr.molda.skirt.ui.SkirtCalculatorRoute
import com.almeidajr.molda.ui.theme.MoldaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/** Large windows, large text and hinges keep the cutting card and tutorial bar whole. */
@RunWith(AndroidJUnit4::class)
class AdaptiveResultsTest {
    private val publisher = WindowLayoutInfoPublisherRule()
    private val composeRule = createAndroidComposeRule<MainActivity>()

    // The publisher must replace the window tracker before the activity starts.
    @get:Rule
    val rules: TestRule = RuleChain.outerRule(publisher).around(composeRule)

    // ForcedSize can shrink density to fit, so pixel checks use the density the content sees.
    private var pixelsPerDp = 1f

    @Test
    fun forcedSizes_keepCuttingCardInReadableResultsPane_andTutorialBarWhole() {
        listOf(
            Triple(DpSize(1600.dp, 1000.dp), 1f, true),
            Triple(DpSize(700.dp, 1000.dp), 1f, false),
            Triple(DpSize(1600.dp, 1000.dp), 2f, true),
            Triple(DpSize(1000.dp, 1000.dp), 2f, false),
            // A landscape phone: too short to pin the drawing above the instructions.
            Triple(DpSize(800.dp, 360.dp), 2f, false),
        ).forEach { (size, fontScale, isTwoPane) ->
            val context = "size=$size fontScale=$fontScale"
            showCalculator(size, fontScale)
            calculateReferenceCase()
            val card = cuttingCard().layoutBounds()
            assertTrue("$context card width", card.width <= 640.dp.value * pixelsPerDp + 1f)
            val waist = composeRule.onNodeWithText(MeasurementField.Waist.label).layoutBounds()
            if (isTwoPane) {
                assertTrue("$context card beside inputs", card.left >= waist.right)
            } else {
                assertTrue("$context card below inputs", card.left < waist.right)
            }
            openTutorial()
            assertTutorialBarWhole(context)
            // The instructions still have room to scroll into view.
            composeRule.onNodeWithText("Comece com papel ou retalho", substring = true)
                .performScrollTo().fullyVisibleBounds(context)
            // Saved state would otherwise reopen the player under the next size.
            composeRule.onNodeWithContentDescription("Fechar tutorial").performClick()
        }
    }

    @Test
    fun doubleText_appBarCardDisclosuresAndTutorialBar_neitherClipNorOverlap() {
        showCalculator(size = null, fontScale = 2f)
        val title = composeRule.onNodeWithText("Saia godê inteira").assertTextFits("title")
            .fullyVisibleBounds("title")
        val action = composeRule.onNodeWithText("Perfis de medidas")
            .assertTextFits("action").fullyVisibleBounds("action")
        // One pixel of slack for dp-to-pixel rounding.
        assertTrue("app bar title $title overlaps action $action", title.right <= action.left + 1f)
        calculateReferenceCase()
        REFERENCE_RADII.forEach { radius ->
            composeRule.onNodeWithContentDescription(radius).performScrollTo()
                .fullyVisibleBounds(radius)
            composeRule.onNodeWithText(radius.substringAfterLast(": "), useUnmergedTree = true)
                .assertTextFits(radius)
        }
        // Card order, read in one scroll position.
        val radii = REFERENCE_RADII.map {
            composeRule.onNodeWithContentDescription(it).layoutBounds()
        }
        radii.zipWithNext().forEach { (upper, lower) ->
            assertTrue("radii overlap: $upper $lower", upper.bottom <= lower.top)
        }
        listOf("Como levar os raios ao papel", "Fórmulas").forEach { label ->
            val disclosure = composeRule.onNode(hasText(label) and hasClickAction())
            disclosure.performScrollTo().performClick()
            val bounds = disclosure.fullyVisibleBounds(label)
            val text = composeRule.onNodeWithText(label, useUnmergedTree = true)
                .assertTextFits(label).fullyVisibleBounds(label)
            // The 24dp chevron and the button's 12dp end padding stay whole beside the label.
            assertTrue(
                "$label squeezes the chevron",
                bounds.right - text.right >= 36.dp.value * pixelsPerDp - 1f,
            )
        }
        openTutorial()
        assertTutorialBarWhole("double text")
    }

    @Test
    fun tabletopHinge_keepsCuttingCardAndTutorialBarAboveFold() {
        showCalculator(size = null, fontScale = 1f)
        val hinge = publishHinge(FoldingFeature.Orientation.HORIZONTAL)
        calculateReferenceCase()
        listOf(
            composeRule.onNodeWithText("Raios para o molde"),
            composeRule.onNodeWithContentDescription(REFERENCE_RADII.last()),
        ).forEach {
            val bounds = it.performScrollTo().fullyVisibleBounds("result")
            assertTrue("result crosses the fold: $bounds", bounds.bottom <= hinge.top)
        }
        openTutorial()
        listOf("Anterior", "Etapa 1 de 6", "Próximo").forEach {
            val bounds = composeRule.onNodeWithText(it).fullyVisibleBounds(it)
            assertTrue("$it crosses the fold", bounds.bottom <= hinge.top)
        }
    }

    @Test
    fun halfOpenedVerticalHinge_splitsPanesAtFold_andKeepsTutorialPlayerOnOneSide() {
        // A landscape phone is wide enough for both sides of an off-centre fold to hold a pane.
        useLandscape()
        showCalculator(size = null, fontScale = 1f)
        // Off centre, so panes that ignored the fold would put the card across it.
        val hinge = publishHinge(FoldingFeature.Orientation.VERTICAL, centerFraction = 0.55f)
        calculateReferenceCase()
        val waist = composeRule.onNodeWithText(MeasurementField.Waist.label)
            .performScrollTo().fullyVisibleBounds("waist")
        val card = cuttingCard().layoutBounds()
        assertTrue("inputs cross the fold", waist.right <= hinge.left)
        assertTrue("card crosses the fold", card.left >= hinge.right)
        openTutorial()
        // The player's new hinge collector starts empty: the test publisher, unlike a device,
        // does not replay the current fold.
        publishHinge(FoldingFeature.Orientation.VERTICAL, centerFraction = 0.55f)
        val player = listOf("Anterior", "Etapa 1 de 6", "Próximo", "Preparar o tecido")
            .map { composeRule.onNodeWithText(it).fullyVisibleBounds(it) } +
            composeRule.onNodeWithContentDescription("Fechar tutorial").fullyVisibleBounds("close")
        assertTrue(
            "tutorial player crosses the fold: $player",
            player.all { it.right <= hinge.left } || player.all { it.left >= hinge.right },
        )
    }

    @Test
    fun flatVerticalHinge_tutorialPlayerSpansFullWidth() {
        // Wide enough for the calculator's panes to fit beside the fold, so the window stays whole.
        useLandscape()
        showCalculator(size = null, fontScale = 1f)
        calculateReferenceCase()
        openTutorial()
        val hinge = publishHinge(
            FoldingFeature.Orientation.VERTICAL,
            state = FoldingFeature.State.FLAT,
        )
        val back = composeRule.onNodeWithText("Anterior").fullyVisibleBounds("Anterior")
        val forward = composeRule.onNodeWithText("Próximo").fullyVisibleBounds("Próximo")
        assertTrue(
            "tutorial player does not span the flat fold: $back $forward",
            back.right <= hinge.left && forward.left >= hinge.right,
        )
    }

    private fun useLandscape() {
        composeRule.runOnUiThread {
            composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        composeRule.waitUntil(10_000) {
            composeRule.activity.window.decorView.run { width > height }
        }
    }

    private fun showCalculator(size: DpSize?, fontScale: Float) {
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                    val content: @Composable () -> Unit = {
                        pixelsPerDp = LocalDensity.current.density
                        MoldaTheme { SkirtCalculatorRoute() }
                    }
                    if (size == null) {
                        content()
                    } else {
                        DeviceConfigurationOverride(
                            DeviceConfigurationOverride.ForcedSize(size),
                            content,
                        )
                    }
                }
            }
        }
    }

    /** Publishes a fold across the window, half-opened by default, and returns its bounds. */
    private fun publishHinge(
        orientation: FoldingFeature.Orientation,
        centerFraction: Float = 0.5f,
        state: FoldingFeature.State = FoldingFeature.State.HALF_OPENED,
    ): Rect {
        // The publisher does not replay, so the content must be collecting first.
        composeRule.waitForIdle()
        val window = composeRule.activity.window.decorView
        val feature = FoldingFeature(
            activity = composeRule.activity,
            center = if (orientation == FoldingFeature.Orientation.VERTICAL) {
                (window.width * centerFraction).toInt()
            } else {
                (window.height * centerFraction).toInt()
            },
            state = state,
            orientation = orientation,
        )
        publisher.overrideWindowLayoutInfo(TestWindowLayoutInfo(listOf(feature)))
        composeRule.waitForIdle()
        return feature.bounds.let {
            Rect(it.left.toFloat(), it.top.toFloat(), it.right.toFloat(), it.bottom.toFloat())
        }
    }

    private fun calculateReferenceCase() {
        MeasurementField.entries.zip(listOf("80", "2", "60", "1", "2")).forEach { (field, value) ->
            composeRule.onNodeWithText(field.label).performScrollTo().performTextReplacement(value)
        }
        closeSoftKeyboard()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
    }

    // The card's Surface is the semantics parent of its heading.
    private fun cuttingCard(): SemanticsNodeInteraction =
        composeRule.onNode(hasAnyChild(hasText("Raios para o molde")))

    private fun openTutorial() {
        composeRule.onNodeWithText("Tutorial de dobra do tecido").performScrollTo().performClick()
        composeRule.onNodeWithText("Preparar o tecido").fullyVisibleBounds("step title")
    }

    private fun assertTutorialBarWhole(context: String) {
        listOf("Anterior", "Etapa 1 de 6", "Próximo").forEach {
            composeRule.onNodeWithText(it).assertTextFits("$context $it")
                .fullyVisibleBounds("$context $it")
        }
        val forward = composeRule.onNode(hasText("Próximo") and hasClickAction())
            .fullyVisibleBounds("$context Próximo")
        assertTrue(
            "$context Próximo is shorter than 48dp",
            forward.height >= 48.dp.value * pixelsPerDp - 1f,
        )
        composeRule.onNode(hasContentDescription("Fechar tutorial"))
            .fullyVisibleBounds("$context close")
    }

    private fun SemanticsNodeInteraction.assertTextFits(context: String): SemanticsNodeInteraction {
        val layouts = mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(layouts)) }
        val layout = layouts.single()
        // Text bounds can differ from rounded pixel size by a fraction of a pixel.
        assertTrue(
            "$context text overflows",
            // Line width, not line right: centred text is laid out wider than it measures.
            (0 until layout.lineCount).all {
                layout.getLineRight(it) - layout.getLineLeft(it) <= layout.size.width + 1f &&
                    layout.getLineBottom(it) <= layout.size.height + 1f
            },
        )
        return this
    }

    /** The node's laid-out bounds in root pixels, whether or not it is scrolled into view. */
    private fun SemanticsNodeInteraction.layoutBounds(): Rect =
        fetchSemanticsNode().layoutInfo.coordinates.let {
            Rect(it.positionInRoot(), it.size.toSize())
        }

    /** Returns the node's bounds in root pixels after checking no ancestor clips any of it. */
    private fun SemanticsNodeInteraction.fullyVisibleBounds(context: String): Rect {
        val unclipped = layoutBounds()
        val visible = fetchSemanticsNode(context).boundsInRoot
        assertTrue("$context has no size", unclipped.width > 0f && unclipped.height > 0f)
        assertTrue(
            "$context is clipped: $visible of $unclipped",
            visible.width >= unclipped.width - 1f && visible.height >= unclipped.height - 1f,
        )
        return unclipped
    }

    private companion object {
        val REFERENCE_RADII = listOf(
            "Linha 1, linha contínua. Costura da cintura: 13,05 cm",
            "Linha 3, linha pontilhada. Barra acabada: 73,05 cm",
            "Linha 2, linha tracejada. Corte da cintura: 12,05 cm",
            "Linha 4, traço e ponto. Corte da barra: 75,05 cm",
        )
    }
}
