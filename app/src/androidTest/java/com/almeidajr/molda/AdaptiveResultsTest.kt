package com.almeidajr.molda

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.ui.theme.MoldaTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/** Hinges keep the cutting card and guide player whole; folds need window-testing on a device. */
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

    // ForcedSize may leave part of the window empty, so folds are placed across the content.
    private var contentBounds = Rect.Zero

    private var isLaunched = false

    @Test
    fun results_onATabletopHinge_keepCuttingCardAndGuideBarAboveTheFold() {
        // Given
        showCalculator(size = null)
        val hinge = publishHinge(FoldingFeature.Orientation.HORIZONTAL)

        // When
        calculateReferenceCase()

        // Then
        listOf(
            composeRule.onNodeWithText("Raios para o molde"),
            composeRule.onNodeWithContentDescription(ReferenceRadii.last()),
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
        // Given: wide enough on any device for both sides of an off-center fold to hold a pane.
        showCalculator(DpSize(1000.dp, 800.dp))
        // Off center, so panes that ignored the fold would put the card across it.
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
    fun guidePlayer_onAFlatVerticalHinge_spansTheFullWidth() {
        // Given: whether the calculator behind it has one column or two, a flat fold splits
        // neither.
        showCalculator(size = null)
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
    fun exampleGuide_onAHalfOpenedVerticalHinge_staysOnOneSide() {
        // Given: too narrow for the library beside the guide, so the example guide is a single
        // pane.
        showCalculator(DpSize(700.dp, 800.dp))
        val hinge = publishHinge(FoldingFeature.Orientation.VERTICAL, centerFraction = 0.55f)
        composeRule.selectDestination("Guias")

        // When
        composeRule.onNode(hasText("Guia de dobra do tecido") and !isHeading()).performClick()

        // Then
        assertPlayerOnOneSide(hinge)
    }

    @Test
    fun exampleGuide_onAFlatVerticalHinge_spansTheFullWidth() {
        // Given
        showCalculator(DpSize(700.dp, 800.dp))
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

    private fun showCalculator(size: DpSize?) {
        val frame: RouteFrame = { route ->
            Box(
                Modifier.fillMaxSize().onGloballyPositioned { contentBounds = it.boundsInWindow() },
            ) { MoldaTheme(content = route) }
        }
        if (isLaunched) {
            composeRule.showRoute(size, frame)
        } else {
            composeRule.launchApp(size = size, frame = frame)
        }
        isLaunched = true
        // The guide's drawings hold still, so Compose goes idle.
        animatorScale.set(0f)
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
        composeRule.fillSkirtDraft()
        composeRule.closeKeyboard()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
    }

    // The card's Surface is the semantics parent of its heading.
    private fun cuttingCard(): SemanticsNodeInteraction =
        composeRule.onNode(hasAnyChild(hasText("Raios para o molde")))

    private fun openGuide() {
        composeRule.onNodeWithText("Guia de dobra do tecido").performScrollTo().performClick()
        composeRule.onNodeWithText("Preparar o tecido").fullyVisibleBounds("step title")
    }
}
