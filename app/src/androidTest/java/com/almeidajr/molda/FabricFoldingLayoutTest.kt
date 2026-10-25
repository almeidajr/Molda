package com.almeidajr.molda

import androidx.activity.BackEventCompat
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
import androidx.compose.ui.test.hasStateDescription
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
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.ui.theme.MoldaTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The guide player's layout: what stays in view on every step, at normal and double text. */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class FabricFoldingLayoutTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @get:Rule(order = 2)
    val animatorScale = AnimatorDurationScaleRule(composeRule)

    private var isLaunched = false

    @Test
    fun guide_atNormalAndDoubleText_keepsLeadAndStepBarInViewOnEveryStep() {
        listOf(false to 1f, true to 2f).forEach { (isDarkTheme, fontScale) ->
            // Given
            openGuide(isDarkTheme, fontScale)

            // When/Then: each step is checked, then left through its forward action.
            listOf(
                "Preparar o tecido",
                "Primeira dobra",
                "Segunda dobra",
                "Marcar os raios",
                "Cortar os arcos",
                "Desdobrar o círculo",
            ).forEachIndexed { index, title ->
                val context = "$title dark=$isDarkTheme fontScale=$fontScale"
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
                    (0 until layout.lineCount).all {
                        layout.getLineRight(it) <= layout.size.width + 1f &&
                            layout.getLineBottom(it) <= layout.size.height + 1f
                    },
                    "$context must wrap within the available width",
                )
                // The drawing can change the pinned area's height once Rive loads.
                composeRule.awaitRiveFrame(STEP_DRAWINGS[index])
                assertLeadAndStepBarInView(STEP_LEADS[index], index, context, fontScale)
                readDetails(isMarkingOrLater = index >= 3)
                // Reading to the end of the details leaves the drawing, its control, in view.
                composeRule.onNodeWithContentDescription(STEP_DRAWINGS[index]).assertIsDisplayed()
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
                    .assertHeightIsAtLeast(48.dp)
                    .assertWidthIsAtLeast(48.dp)
                    .performClick()
            }
        }
    }

    @Test
    fun closeAction_atDoubleText_isTallEnough_andReturnsToResults() {
        // Given
        openGuide(isDarkTheme = false, fontScale = 2f)

        // When
        composeRule.onNodeWithContentDescription("Fechar guia")
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick()

        // Then
        composeRule.onNodeWithText("Preparar o tecido").assertDoesNotExist()
        composeRule.onNodeWithText("Guia de dobra do tecido").assertIsDisplayed()
    }

    @Test
    fun predictiveBack_onGuide_previewsResultsBeneath_andCancelKeepsGuide() {
        // Given
        openGuide(isDarkTheme = false, fontScale = 1f)
        val dispatcher = composeRule.activity.onBackPressedDispatcher
        // Dispatching on the UI thread bypasses Compose's synchronization.
        composeRule.waitForIdle()

        // When
        composeRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(
                BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_LEFT),
            )
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(200f, 400f, 0.5f, BackEventCompat.EDGE_LEFT),
            )
        }

        // Then
        composeRule.onNodeWithText("Preparar o tecido").assertExists()
        composeRule.onNodeWithText("Guia de dobra do tecido").assertExists()

        // When
        composeRule.runOnUiThread { dispatcher.dispatchOnBackCancelled() }

        // Then
        composeRule.onNodeWithText("Guia de dobra do tecido").assertDoesNotExist()
        composeRule.onNodeWithText("Preparar o tecido").assertIsDisplayed()
    }

    private companion object {
        val STEP_DRAWINGS = listOf(
            "Dobra do tecido em Rive: etapa 1",
            "Dobra do tecido em Rive: etapa 2",
            "Dobra do tecido em Rive: etapa 3",
            "Marcação proporcional em Rive",
            "Corte proporcional em Rive",
            "Círculo desdobrado proporcional em Rive",
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

    /**
     * Before any scrolling, the step bar is whole and the step's lead sentence sits under the
     * pinned drawing, so the action to take now is readable without hunting for it. At 100% text
     * the whole lead fits. At 200% on a compact phone (320x640 dp) the pinned drawing, a wrapped
     * title and the stacked step bar leave about three lead lines, fewer than the longer leads
     * need. The guide opens with system animations off, so the motion note sits above the lead and
     * takes that room at 200%: there the note must start in view with at least its first line
     * whole.
     */
    private fun assertLeadAndStepBarInView(
        lead: String,
        index: Int,
        context: String,
        fontScale: Float,
    ) {
        if (fontScale > 1f) {
            val firstInstruction = composeRule.onNodeWithText(
                "Movimento desativado no sistema. O desenho mostra o resultado da etapa.",
            )
            val layouts = mutableListOf<TextLayoutResult>()
            firstInstruction.performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                assertTrue(it(layouts))
            }
            val unclipped = firstInstruction.layoutBounds()
            val visible = firstInstruction.fetchSemanticsNode().boundsInRoot
            // Printed (logcat tag System.out) so each window's visible share can be read back.
            println("MoldaLead $context visible=${visible.height} of ${unclipped.height}")
            assertTrue(
                visible.top <= unclipped.top + 1f &&
                    visible.height >= layouts.single().getLineBottom(0) - 1f,
                "$context first instruction does not start in view with its first line whole: " +
                    "$visible of $unclipped",
            )
        } else {
            composeRule.onNodeWithText(lead).fullyVisibleBounds("$context lead")
        }
        listOf("Anterior", "Etapa ${index + 1} de 6", if (index == 5) "Concluir" else "Próximo")
            .forEach { composeRule.onNodeWithText(it).fullyVisibleBounds("$context $it") }
    }

    /**
     * The supporting detail starts collapsed under a labelled 48dp disclosure, then reads to the
     * end.
     */
    private fun readDetails(isMarkingOrLater: Boolean) {
        composeRule.onNodeWithText("Detalhes").performScrollTo()
            .assertHeightIsAtLeast(48.dp)
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

    private fun openGuide(isDarkTheme: Boolean, fontScale: Float) {
        val frame: RouteFrame = { route ->
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                MoldaTheme(isDarkTheme = isDarkTheme, content = route)
            }
        }
        if (isLaunched) {
            composeRule.showRoute(frame = frame)
        } else {
            composeRule.launchApp(frame = frame)
        }
        isLaunched = true
        // The guide's drawings hold still, so Compose goes idle.
        animatorScale.set(0f)
        composeRule.showSkirtCalculator()
        SkirtDraftField.entries.zip(listOf("80", "2", "60", "0", "0"))
            .forEach { (field, value) ->
                composeRule.onNodeWithText(field.labelText).performScrollTo()
                    .performTextReplacement(value)
            }
        closeSoftKeyboard()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
        composeRule.onNodeWithText("Guia de dobra do tecido")
            .performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
    }
}
