package com.almeidajr.molda

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.ui.theme.MoldaTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Large windows and large text keep the cutting card and the guide player whole, on a phone-sized
 * screen unless a test forces another size. Without Rive's native code the guide's frame shows its
 * written fallback at the drawing's size.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@Config(qualifiers = "w411dp-h923dp-xxhdpi")
class AdaptiveResultsTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    // ForcedSize can shrink density to fit, so pixel checks use the density the content sees.
    private var pixelsPerDp = 1f

    private var isLaunched = false

    @Test
    @Config(qualifiers = "w1600dp-h1000dp-xxhdpi")
    fun forcedSizes_keepCuttingCardAndItsDiagramInReadableResultsPane_andGuideBarWhole() {
        listOf(
            ResultsLayout(DpSize(1600.dp, 1000.dp), 1f, isTwoPane = true, isDiagramBeside = true),
            // The 96dp navigation rail leaves the card too narrow for two 260dp columns.
            ResultsLayout(DpSize(700.dp, 1000.dp), 1f, isTwoPane = false, isDiagramBeside = false),
            ResultsLayout(DpSize(800.dp, 1000.dp), 1f, isTwoPane = false, isDiagramBeside = true),
            ResultsLayout(DpSize(1600.dp, 1000.dp), 2f, isTwoPane = true, isDiagramBeside = false),
            // A landscape phone: too short to pin the drawing above the instructions.
            ResultsLayout(DpSize(800.dp, 360.dp), 2f, isTwoPane = false, isDiagramBeside = false),
        ).forEach { layout ->
            // Given
            val context = "size=${layout.size} fontScale=${layout.fontScale}"
            showCalculator(layout.size, layout.fontScale)

            // When
            calculate()

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
            // The instructions still have room to scroll to their end, the "Detalhes" disclosure.
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
        // One pixel of tolerance for dp-to-pixel rounding.
        assertTrue(up.right <= title.left + 1f, "app bar title $title overlaps up arrow $up")
        calculate()
        ReferenceRadii.forEach { radius ->
            composeRule.onNodeWithContentDescription(radius).performScrollTo()
                .fullyVisibleBounds(radius)
            composeRule.onNodeWithText(radius.substringAfterLast(": "), useUnmergedTree = true)
                .assertTextFits(radius)
        }
        // Card order, read in one scroll position.
        val radii = ReferenceRadii.map {
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
    fun guide_atNormalAndDoubleText_keepsLeadAndStepBarInViewOnEveryStep() {
        listOf(1f, 2f).forEach { fontScale ->
            // Given: zero allowances, so the marking and cutting steps note coinciding lines.
            showCalculator(size = null, fontScale)
            calculate(listOf("80", "2", "60", "0", "0"))
            composeRule.click("Guia de dobra do tecido")

            // When/Then: each step is checked, then left through its forward action.
            STEP_LEADS.forEachIndexed { index, lead ->
                val context = "${index + 1} fontScale=$fontScale"
                composeRule.onNodeWithText(STEP_TITLES[index]).assertIsDisplayed()
                    .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
                    .assertTextFits(context)
                assertLeadAndStepBarInView(lead, index, context, fontScale)
                readDetails(isMarkingOrLater = index >= 3)
                // Reading to the end of the details leaves the pinned drawing frame in view.
                composeRule.onNodeWithText(DRAWING_FALLBACK).assertIsDisplayed()
                if (index in 3..4) {
                    composeRule.onNodeWithText("Margem de cintura zero: as linhas 1 e 2 coincidem.")
                        .performScrollTo().assertIsDisplayed()
                }
                composeRule.onNodeWithText("Etapa ${index + 1} de 6").assertIsDisplayed()
                composeRule.onNodeWithText("Anterior").assertIsDisplayed().let {
                    if (index == 0) it.assertIsNotEnabled() else it.assertIsEnabled()
                }
                composeRule.onNodeWithText(if (index == 5) "Concluir" else "Próximo")
                    .assertIsDisplayed()
                    .performClick()
            }
        }
    }

    /**
     * Before any scrolling, the step bar is whole and the step's lead sentence sits under the
     * pinned drawing, so the action to take now is readable without hunting for it. At 100% text
     * the whole lead fits. At 200% the pinned drawing, a wrapped title and the stacked step bar
     * leave fewer lines than the longer leads need: there the lead must start in view with at
     * least its first line whole.
     */
    private fun assertLeadAndStepBarInView(
        lead: String,
        index: Int,
        context: String,
        fontScale: Float,
    ) {
        val leadNode = composeRule.onNodeWithText(lead)
        if (fontScale > 1f) {
            val layouts = mutableListOf<TextLayoutResult>()
            leadNode.performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                assertTrue(it(layouts))
            }
            val unclipped = leadNode.layoutBounds()
            val visible = leadNode.fetchSemanticsNode().boundsInRoot
            assertTrue(
                visible.top <= unclipped.top + 1f &&
                    visible.height >= layouts.single().getLineBottom(0) - 1f,
                "$context lead does not start in view with its first line whole: " +
                    "$visible of $unclipped",
            )
        } else {
            leadNode.fullyVisibleBounds("$context lead")
        }
        listOf("Anterior", "Etapa ${index + 1} de 6", if (index == 5) "Concluir" else "Próximo")
            .forEach { composeRule.onNodeWithText(it).fullyVisibleBounds("$context $it") }
    }

    /** The supporting detail starts collapsed under a labeled disclosure, then reads to the end. */
    private fun readDetails(isMarkingOrLater: Boolean) {
        composeRule.onNodeWithText("Detalhes").performScrollTo()
            .assert(hasStateDescription("Recolhido"))
            .performClick()
            .assert(hasStateDescription("Expandido"))
        if (isMarkingOrLater) {
            composeRule.onNodeWithText("Antes de cortar tecido da peça", substring = true)
                .performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithText("O desenho mostra cada etapa", substring = true)
            .performScrollTo().assertIsDisplayed()
    }

    private fun showCalculator(size: DpSize?, fontScale: Float) {
        val frame: RouteFrame = { route ->
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                pixelsPerDp = LocalDensity.current.density
                MoldaTheme(content = route)
            }
        }
        if (isLaunched) {
            composeRule.showRoute(size, frame)
        } else {
            composeRule.launchApp(size = size, frame = frame)
        }
        isLaunched = true
        composeRule.showSkirtCalculator()
    }

    private fun calculate(values: List<String> = ReferenceInputs) {
        composeRule.fillSkirtDraft(values)
        composeRule.click("Calcular")
    }

    // The card's Surface is the semantics parent of its heading.
    private fun cuttingCard(): SemanticsNodeInteraction =
        composeRule.onNode(hasAnyChild(hasText("Raios para o molde")))

    /** The diagram sits on the card's own surface, beside the radii when both columns fit. */
    private fun assertDiagramInCard(card: Rect, isBeside: Boolean, context: String) {
        val diagram = composeRule.onNodeWithContentDescription(
            "Diagrama esquemático",
            substring = true,
        ).layoutBounds()
        assertTrue(
            card.contains(diagram.topLeft) && card.contains(diagram.bottomRight - Offset(1f, 1f)),
            "$context diagram outside the card",
        )
        val radii = ReferenceRadii.map {
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
        composeRule.click("Guia de dobra do tecido")
        composeRule.onNodeWithText("Preparar o tecido").fullyVisibleBounds("step title")
    }

    private fun assertGuideBarWhole(context: String) {
        listOf("Anterior", "Etapa 1 de 6", "Próximo").forEach {
            composeRule.onNodeWithText(it).assertTextFits("$context $it")
                .fullyVisibleBounds("$context $it")
        }
        composeRule.onNode(hasText("Próximo") and hasClickAction())
            .fullyVisibleBounds("$context Próximo")
        composeRule.onNode(hasContentDescription("Fechar guia"))
            .fullyVisibleBounds("$context close")
    }

    private data class ResultsLayout(
        val size: DpSize,
        val fontScale: Float,
        val isTwoPane: Boolean,
        val isDiagramBeside: Boolean,
    )

    private companion object {
        /** What the drawing frame shows when Rive cannot start, as on the JVM. */
        const val DRAWING_FALLBACK =
            "Não foi possível iniciar a animação. As instruções escritas continuam disponíveis."

        val STEP_TITLES = listOf(
            "Preparar o tecido",
            "Primeira dobra",
            "Segunda dobra",
            "Marcar os raios",
            "Cortar os arcos",
            "Desdobrar o círculo",
        )

        val STEP_LEADS = listOf(
            "Alise o tecido sem esticar e respeite o fio.",
            "Dobre no sentido do comprimento, levando a ourela esquerda até a direita.",
            "Dobre transversalmente, levando as bordas cortadas de cima até as de baixo.",
            "Prenda fita ou régua no encontro das duas dobras fechadas e marque cada arco " +
                "com raio constante.",
            "Corte o arco 2 e depois o arco 4, atravessando as quatro camadas alinhadas.",
            "Desfaça a segunda dobra, abrindo as duas camadas superiores para cima, e depois " +
                "a primeira, levando a camada de cima para a esquerda; alise sem esticar.",
        )
    }
}
