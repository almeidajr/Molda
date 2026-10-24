package com.almeidajr.molda

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.ui.theme.MoldaTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/** Large windows, large text and hinges keep the cutting card and guide bar whole. */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AdaptiveResultsTest {
    private val hiltRule = HiltAndroidRule(this)
    private val publisher = WindowLayoutInfoPublisherRule()
    private val composeRule = createLaunchLaterComposeRule()
    private val animatorScale = AnimatorDurationScaleRule(composeRule)

    // The publisher must replace the window tracker before the activity starts.
    @get:Rule
    val rules: TestRule = RuleChain.outerRule(hiltRule)
        .around(publisher)
        .around(composeRule)
        .around(animatorScale)

    // ForcedSize can shrink density to fit, so pixel checks use the density the content sees.
    private var pixelsPerDp = 1f

    // ForcedSize may leave part of the window empty, so folds are placed across the content.
    private var contentBounds = Rect.Zero

    // Each test shows its own frame or content over the app's, as it did under the eager rule.
    @Before
    fun launchOnTheAppsOwnContent() = composeRule.launchApp(frame = null)

    @Test
    fun forcedSizes_keepCuttingCardAndItsDiagramInReadableResultsPane_andGuideBarWhole() {
        listOf(
            ResultsLayout(DpSize(1600.dp, 1000.dp), 1f, isTwoPane = true, isDiagramBeside = true),
            // The 96dp navigation rail leaves the card too narrow for two 260dp columns.
            ResultsLayout(DpSize(700.dp, 1000.dp), 1f, isTwoPane = false, isDiagramBeside = false),
            ResultsLayout(DpSize(800.dp, 1000.dp), 1f, isTwoPane = false, isDiagramBeside = true),
            ResultsLayout(DpSize(1600.dp, 1000.dp), 2f, isTwoPane = true, isDiagramBeside = false),
            ResultsLayout(DpSize(1000.dp, 1000.dp), 2f, isTwoPane = false, isDiagramBeside = false),
            // A landscape phone: too short to pin the drawing above the instructions.
            ResultsLayout(DpSize(800.dp, 360.dp), 2f, isTwoPane = false, isDiagramBeside = false),
        ).forEach { layout ->
            // Given
            val context = "size=${layout.size} fontScale=${layout.fontScale}"
            showCalculator(layout.size, layout.fontScale)

            // When
            calculateReferenceCase()

            // Then
            val card = cuttingCard().layoutBounds()
            assertTrue(card.width <= 640.dp.value * pixelsPerDp + 1f, "$context card width")
            val waist = composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).layoutBounds()
            if (layout.isTwoPane) {
                assertTrue(card.left >= waist.right, "$context card beside inputs")
            } else {
                assertTrue(card.left < waist.right, "$context card below inputs")
            }
            assertDiagramInCard(card, layout.isDiagramBeside, context)
            openGuide()
            assertGuideBarWhole(context)
            // The instructions still have room to scroll to their end, the details disclosure.
            composeRule.onNodeWithText("Detalhes").performScrollTo().fullyVisibleBounds(context)
            // Saved state would otherwise reopen the player under the next size.
            composeRule.onNodeWithContentDescription("Fechar guia").performClick()
        }
    }

    @Test
    fun doubleText_appBarCardDisclosuresAndGuideBar_neitherClipNorOverlap() {
        // Given
        showCalculator(size = null, fontScale = 2f)

        // When/Then: the app bar first, then each part of the results as it appears.
        val title = composeRule.onNodeWithText("Saia godê inteira").assertTextFits("title")
            .fullyVisibleBounds("title")
        val up = composeRule.onNodeWithContentDescription("Voltar").fullyVisibleBounds("up")
        // One pixel of slack for dp-to-pixel rounding.
        assertTrue(up.right <= title.left + 1f, "app bar title $title overlaps up arrow $up")
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
            assertTrue(upper.bottom <= lower.top, "radii overlap: $upper $lower")
        }
        // The action row wraps rather than squeezing either label.
        val actionLabels = listOf("Guia de dobra do tecido", "Editar medidas")
        actionLabels.forEach {
            composeRule.onNodeWithText(it).performScrollTo().assertTextFits(it)
                .fullyVisibleBounds(it)
        }
        // Both read at one scroll position.
        val (guide, edit) = actionLabels.map { composeRule.onNodeWithText(it).layoutBounds() }
        assertFalse(guide.overlaps(edit), "actions overlap: $guide $edit")
        listOf("Como levar os raios ao papel", "Fórmulas").forEach { label ->
            val disclosure = composeRule.onNode(hasText(label) and hasClickAction())
            disclosure.performScrollTo().performClick()
            val bounds = disclosure.fullyVisibleBounds(label)
            val text = composeRule.onNodeWithText(label, useUnmergedTree = true)
                .assertTextFits(label).fullyVisibleBounds(label)
            // The 24dp chevron and the button's 12dp end padding stay whole beside the label.
            assertTrue(
                bounds.right - text.right >= 36.dp.value * pixelsPerDp - 1f,
                "$label squeezes the chevron",
            )
        }
        openGuide()
        assertGuideBarWhole("double text")
    }

    @Test
    fun tabletopHinge_keepsCuttingCardAndGuideBarAboveFold() {
        // Given
        showCalculator(size = null, fontScale = 1f)
        val hinge = publishHinge(FoldingFeature.Orientation.HORIZONTAL)

        // When
        calculateReferenceCase()

        // Then
        listOf(
            composeRule.onNodeWithText("Raios para o molde"),
            composeRule.onNodeWithContentDescription(REFERENCE_RADII.last()),
        ).forEach {
            val bounds = it.performScrollTo().fullyVisibleBounds("result")
            assertTrue(bounds.bottom <= hinge.top, "result crosses the fold: $bounds")
        }
        openGuide()
        listOf("Anterior", "Etapa 1 de 6", "Próximo").forEach {
            val bounds = composeRule.onNodeWithText(it).fullyVisibleBounds(it)
            assertTrue(bounds.bottom <= hinge.top, "$it crosses the fold")
        }
    }

    @Test
    fun halfOpenedVerticalHinge_splitsPanesAtFold_andKeepsGuidePlayerOnOneSide() {
        // Given: wide enough on any device for both sides of an off-centre fold to hold a pane.
        showCalculator(DpSize(1000.dp, 800.dp), fontScale = 1f)
        // Off centre, so panes that ignored the fold would put the card across it.
        val hinge = publishHinge(FoldingFeature.Orientation.VERTICAL, centerFraction = 0.55f)

        // When
        calculateReferenceCase()

        // Then
        val waist = composeRule.onNodeWithText(SkirtDraftField.Waist.labelText)
            .performScrollTo().fullyVisibleBounds("waist")
        val card = cuttingCard().layoutBounds()
        assertTrue(waist.right <= hinge.left, "inputs cross the fold")
        assertTrue(card.left >= hinge.right, "card crosses the fold")
        openGuide()
        assertPlayerOnOneSide(hinge)
    }

    @Test
    fun flatVerticalHinge_guidePlayerSpansFullWidth() {
        // Given: whether the calculator behind it has one column or two, a flat fold splits
        // neither.
        showCalculator(size = null, fontScale = 1f)
        calculateReferenceCase()
        openGuide()

        // When
        val hinge = publishHinge(
            FoldingFeature.Orientation.VERTICAL,
            state = FoldingFeature.State.FLAT,
        )

        // Then
        val back = composeRule.onNodeWithText("Anterior").fullyVisibleBounds("Anterior")
        val forward = composeRule.onNodeWithText("Próximo").fullyVisibleBounds("Próximo")
        assertTrue(
            back.right <= hinge.left && forward.left >= hinge.right,
            "guide player does not span the flat fold: $back $forward",
        )
    }

    @Test
    fun halfOpenedVerticalHinge_keepsSinglePaneExampleGuideOnOneSide() {
        // Given: too narrow for the library beside the guide, so the example guide is a single
        // pane.
        showCalculator(DpSize(700.dp, 800.dp), fontScale = 1f)
        val hinge = publishHinge(FoldingFeature.Orientation.VERTICAL, centerFraction = 0.55f)
        composeRule.selectDestination("Guias")

        // When
        composeRule.onNode(hasText("Guia de dobra do tecido") and !isHeading()).performClick()

        // Then
        assertPlayerOnOneSide(hinge)
    }

    @Test
    fun flatVerticalHinge_singlePaneExampleGuideSpansFullWidth() {
        // Given
        showCalculator(DpSize(700.dp, 800.dp), fontScale = 1f)
        composeRule.selectDestination("Guias")
        composeRule.onNode(hasText("Guia de dobra do tecido") and !isHeading()).performClick()

        // When
        val hinge = publishHinge(
            FoldingFeature.Orientation.VERTICAL,
            state = FoldingFeature.State.FLAT,
        )

        // Then
        val back = composeRule.onNodeWithText("Anterior").fullyVisibleBounds("Anterior")
        val forward = composeRule.onNodeWithText("Próximo").fullyVisibleBounds("Próximo")
        assertTrue(
            back.right <= hinge.left && forward.left >= hinge.right,
            "guide player does not span the flat fold: $back $forward",
        )
    }

    private fun assertPlayerOnOneSide(hinge: Rect) {
        val player = listOf("Anterior", "Etapa 1 de 6", "Próximo", "Preparar o tecido")
            .map { composeRule.onNodeWithText(it).fullyVisibleBounds(it) } +
            composeRule.onNodeWithContentDescription("Fechar guia").fullyVisibleBounds("close")
        assertTrue(
            player.all { it.right <= hinge.left } || player.all { it.left >= hinge.right },
            "guide player crosses the fold: $player",
        )
    }

    private fun showCalculator(size: DpSize?, fontScale: Float) {
        // The guide's drawings hold still, so Compose goes idle.
        animatorScale.set(0f)
        composeRule.showRoute(size) { route ->
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                pixelsPerDp = LocalDensity.current.density
                Box(
                    Modifier.fillMaxSize()
                        .onGloballyPositioned { contentBounds = it.boundsInWindow() },
                ) { MoldaTheme(content = route) }
            }
        }
        composeRule.showSkirtCalculator()
    }

    /** Publishes a fold across the content, half-opened by default, and returns its bounds. */
    private fun publishHinge(
        orientation: FoldingFeature.Orientation,
        centerFraction: Float = 0.5f,
        state: FoldingFeature.State = FoldingFeature.State.HALF_OPENED,
    ): Rect {
        // The publisher does not replay, so the content must be collecting first.
        composeRule.waitForIdle()
        val content = contentBounds
        val feature = FoldingFeature(
            activity = composeRule.activity,
            center = if (orientation == FoldingFeature.Orientation.VERTICAL) {
                (content.left + content.width * centerFraction).toInt()
            } else {
                (content.top + content.height * centerFraction).toInt()
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
        SkirtDraftField.entries.zip(listOf("80", "2", "60", "1", "2")).forEach { (field, value) ->
            composeRule.onNodeWithText(
                field.labelText,
            ).performScrollTo().performTextReplacement(value)
        }
        closeSoftKeyboard()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
    }

    // The card's Surface is the semantics parent of its heading.
    private fun cuttingCard(): SemanticsNodeInteraction =
        composeRule.onNode(hasAnyChild(hasText("Raios para o molde")))

    /** The diagram sits on the card's own surface, beside the radii when both columns fit. */
    private fun assertDiagramInCard(card: Rect, isBeside: Boolean, context: String) {
        val diagram = composeRule.onNodeWithContentDescription(
            "Diagrama esquemático",
            substring = true,
        )
            .layoutBounds()
        assertTrue(
            card.contains(diagram.topLeft) && card.contains(diagram.bottomRight - Offset(1f, 1f)),
            "$context diagram outside the card",
        )
        val radii = REFERENCE_RADII.map {
            composeRule.onNodeWithContentDescription(it).layoutBounds()
        }
        if (isBeside) {
            assertTrue(
                radii.all { it.right <= diagram.left } && diagram.top < radii.last().bottom,
                "$context diagram not beside the radii",
            )
        } else {
            assertTrue(
                radii.all {
                    it.bottom <= diagram.top
                },
                "$context diagram not below the radii",
            )
        }
    }

    private fun openGuide() {
        composeRule.onNodeWithText("Guia de dobra do tecido").performScrollTo().performClick()
        composeRule.onNodeWithText("Preparar o tecido").fullyVisibleBounds("step title")
    }

    private fun assertGuideBarWhole(context: String) {
        listOf("Anterior", "Etapa 1 de 6", "Próximo").forEach {
            composeRule.onNodeWithText(it).assertTextFits("$context $it")
                .fullyVisibleBounds("$context $it")
        }
        val forward = composeRule.onNode(hasText("Próximo") and hasClickAction())
            .fullyVisibleBounds("$context Próximo")
        assertTrue(
            forward.height >= 48.dp.value * pixelsPerDp - 1f,
            "$context Próximo is shorter than 48dp",
        )
        composeRule.onNode(hasContentDescription("Fechar guia"))
            .fullyVisibleBounds("$context close")
    }

    private fun SemanticsNodeInteraction.assertTextFits(context: String): SemanticsNodeInteraction {
        val layouts = mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(layouts)) }
        val layout = layouts.single()
        // Text bounds can differ from rounded pixel size by a fraction of a pixel.
        assertTrue(
            // Line width, not line right: centred text is laid out wider than it measures.
            (0 until layout.lineCount).all {
                layout.getLineRight(it) - layout.getLineLeft(it) <= layout.size.width + 1f &&
                    layout.getLineBottom(it) <= layout.size.height + 1f
            },
            "$context text overflows",
        )
        return this
    }

    private data class ResultsLayout(
        val size: DpSize,
        val fontScale: Float,
        val isTwoPane: Boolean,
        val isDiagramBeside: Boolean,
    )

    private companion object {
        val REFERENCE_RADII = listOf(
            "Linha 1, linha contínua. Costura da cintura: 13,05 cm",
            "Linha 3, linha pontilhada. Barra acabada: 73,05 cm",
            "Linha 2, linha tracejada. Corte da cintura: 12,05 cm",
            "Linha 4, traço e ponto. Corte da barra: 75,05 cm",
        )
    }
}
