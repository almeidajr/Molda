package com.almeidajr.molda

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.testing.October18Clock
import com.almeidajr.molda.ui.theme.MoldaTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SkirtCalculatorTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Before
    fun openSkirtCalculator() {
        composeRule.launchApp(clock = October18Clock)
        composeRule.showSkirtCalculator()
    }

    @Test
    fun form_beforeCalculation_explainsMeasuringAndExplicitZeros() {
        // Then
        listOf(
            "Meça a circunferência da cintura",
            "na posição desejada do cós",
            "Folga é o acréscimo à cintura para vestir; informe zero se não desejar folga.",
            "a partir da linha de costura da cintura; exclua a altura do cós.",
            "Tecido reservado para costurar a cintura; informe zero se não houver margem.",
            "Tecido reservado para acabar a barra; informe zero se não houver margem.",
        ).forEach {
            composeRule.onNodeWithText(it, substring = true).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun waistSource_onForm_sharesRowWithUseProfileJustAboveWaist() {
        // When
        val waist = composeRule.onNodeWithText(SkirtDraftField.Waist.labelText)
            .getUnclippedBoundsInRoot()
        val source = composeRule.onNodeWithText("Medida manual").getUnclippedBoundsInRoot()
        val useProfile = composeRule.onNodeWithText("Usar perfil").assertHeightIsAtLeast(48.dp)
            .getUnclippedBoundsInRoot()

        // Then
        assertTrue(source.top >= useProfile.top && source.bottom <= useProfile.bottom)
        assertTrue(useProfile.bottom <= waist.top && waist.top - useProfile.bottom < 16.dp)
        assertTrue(useProfile.right <= waist.right + 1.dp)
    }

    @Test
    fun calculateButton_onForm_spansFormAndIsTouchSized() {
        // When
        val waist = composeRule.onNodeWithText(SkirtDraftField.Waist.labelText)
            .getUnclippedBoundsInRoot()
        val calculate = composeRule.onNodeWithText("Calcular").performScrollTo()
            .assertHeightIsAtLeast(48.dp).getUnclippedBoundsInRoot()

        // Then: wider than the field inset inside its group.
        assertTrue(calculate.left < waist.left && calculate.right > waist.right)
    }

    @Test
    fun results_everySchemeAtDoubleText_keepRadiiLargeAndReadable() {
        listOf(false, true).forEach { isDarkTheme ->
            // Given
            showCalculator { route ->
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                    MoldaTheme(isDarkTheme = isDarkTheme, content = route)
                }
            }
            fillInputs(REFERENCE_INPUTS)

            // When
            calculate()

            // Then
            REFERENCE_RADII.forEach { radius ->
                composeRule.onNodeWithContentDescription(radius).performScrollTo()
                val value = radius.substringAfterLast(": ")
                val node = composeRule.onNodeWithText(value, useUnmergedTree = true)
                    .assertIsDisplayed()
                val layouts = mutableListOf<TextLayoutResult>()
                node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                    assertTrue(it(layouts))
                }
                val style = layouts.single().layoutInput.style
                assertEquals(24.sp, style.fontSize)
                assertEquals(32.sp, style.lineHeight)
                node.assertReadableTextContrast("$radius dark=$isDarkTheme")
                // A single match: the card's rows are the diagram's only key.
                composeRule.onNodeWithText(
                    radius.substringAfter(". ").substringBefore(":"),
                    useUnmergedTree = true,
                ).assertReadableTextContrast("label $radius dark=$isDarkTheme")
            }
            assertResultsAreaContrast("dark=$isDarkTheme")
        }
    }

    @Test
    fun calculate_validDraft_showsGroupedRadiiSummaryAndLimitsInReadingOrder() {
        // Given
        fillInputs(REFERENCE_INPUTS)

        // When
        calculate()

        // Then
        composeRule.onNodeWithText("Raios para o molde").assertIsDisplayed()
        REFERENCE_RADII.forEach {
            composeRule.onNodeWithContentDescription(it).performScrollTo().assertIsDisplayed()
        }
        listOf("Linhas de referência", "Linhas de corte", "Medidas utilizadas (cm)").forEach {
            composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        SkirtDraftField.entries.zip(REFERENCE_INPUTS).forEach { (field, value) ->
            composeRule.onNodeWithText("${field.summaryLabelText}: $value,00", substring = true)
                .performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithText("(cm):", substring = true).assertDoesNotExist()
        listOf("Para saia godê inteira", "O comprimento acabado exclui a altura do cós.").forEach {
            composeRule.onNodeWithText(it, substring = true).performScrollTo().assertIsDisplayed()
        }
        assertResultsReadInOrder()
    }

    @Test
    fun cuttingCard_afterCalculation_announcesEachRadiusOnceUnderGroupHeadings() {
        // Given
        fillInputs(REFERENCE_INPUTS)

        // When
        calculate()

        // Then
        listOf("Raios para o molde", "Linhas de referência", "Linhas de corte").forEach {
            composeRule.onNode(hasText(it) and isHeading()).performScrollTo().assertIsDisplayed()
        }
        composeRule.onAllNodes(
            hasText("Raios para o molde") and
                SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion),
        ).assertCountEquals(1)
        REFERENCE_RADII.forEach { radius ->
            composeRule.onAllNodes(hasContentDescription(radius)).assertCountEquals(1)
            composeRule.onNodeWithContentDescription(radius).performScrollTo().assertIsDisplayed()
            composeRule.onAllNodes(hasText(radius.substringAfterLast(": "))).assertCountEquals(1)
        }
    }

    @Test
    fun diagram_afterCalculation_describesLinesAndLimitsWithoutCoincidence() {
        // Given
        fillInputs(REFERENCE_INPUTS)

        // When
        calculate()

        // Then
        listOf(
            "Linha 1, linha contínua: costura da cintura.",
            "Linha 4, traço e ponto: corte da barra.",
            "Diagrama esquemático",
            "não garante o caimento",
            "A margem de cintura de 1 cm avança para dentro; " +
                "a margem de barra de 2 cm avança para fora.",
        ).forEach {
            composeRule.onNodeWithContentDescription(it, substring = true)
                .performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithContentDescription("coincidem", substring = true).assertDoesNotExist()
    }

    @Test
    fun disclosures_expanded_showTheirContent() {
        // Given
        fillInputs(REFERENCE_INPUTS)
        calculate()

        mapOf(
            "Como levar os raios ao papel" to "Transfira os raios ao papel",
            "Fórmulas" to "r = (80 + 2) / (2π)",
        ).forEach { (label, content) ->
            // When
            composeRule.onNodeWithText(label).performScrollTo().performClick()

            // Then
            composeRule.onNodeWithText(content, substring = true).performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun disclosures_clicked_exposeExpandedState() {
        // Given
        fillInputs(REFERENCE_INPUTS)
        calculate()

        listOf("Como levar os raios ao papel", "Fórmulas").forEach { label ->
            val disclosure = composeRule.onNode(hasText(label) and hasClickAction())
            disclosure.performScrollTo().assert(hasStateDescription("Recolhido"))

            // When
            disclosure.performClick()

            // Then
            disclosure.assert(hasStateDescription("Expandido"))
        }
    }

    @Test
    fun resultActions_afterCalculation_areTouchSized() {
        // Given
        fillInputs(REFERENCE_INPUTS)

        // When
        calculate()

        // Then
        listOf("Editar medidas", "Guia de dobra do tecido").forEach {
            composeRule.onNodeWithText(it).performScrollTo()
                .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        }
        listOf("Como levar os raios ao papel", "Fórmulas").forEach {
            composeRule.onNode(hasText(it) and hasClickAction()).performScrollTo()
                .assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun editMeasurements_afterCalculation_focusesWaist() {
        // Given
        fillInputs(REFERENCE_INPUTS)
        calculate()

        // When
        composeRule.onNodeWithText("Editar medidas").performScrollTo().performClick()

        // Then
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText)
            .assertIsFocused().assertIsDisplayed()
    }

    @Test
    fun edit_afterCalculation_withdrawsResultsUntilRecalculated() {
        // Given
        fillInputs(REFERENCE_INPUTS)
        calculate()

        // When
        edit(SkirtDraftField.Waist, "90")

        // Then
        composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
        calculate()
        composeRule.onNodeWithContentDescription(
            "Linha 1, linha contínua. Costura da cintura: 14,64 cm",
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun calculate_invalidHemAllowance_showsItsMessageWithoutResults() {
        // Given
        fillInputs(REFERENCE_INPUTS)

        listOf(
            "" to "Informe um valor",
            "NaN" to "Informe um número finito",
            "1,2,3" to "Informe um número finito",
            "-1" to "Informe zero ou um valor positivo",
        ).forEach { (text, error) ->
            edit(SkirtDraftField.HemAllowance, text)

            // When
            calculate()

            // Then
            composeRule.onNodeWithText(
                error,
                substring = true,
            ).performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
        }
    }

    @Test
    fun calculate_seamAllowanceBeyondWaistStitching_showsMarginMessage() {
        // Given
        fillInputs(listOf("80", "2", "60", "100", "2"))

        // When
        calculate()

        // Then
        composeRule.onNodeWithText("A margem deve ser menor", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
    }

    @Test
    fun imeAction_afterFirstCalculation_revalidatesTheFieldLeft() {
        // Given
        calculate()
        edit(SkirtDraftField.Waist, "-1")

        // When
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).performImeAction()

        // Then
        composeRule.onNodeWithText("Informe um valor maior que zero.").assertExists()
        edit(SkirtDraftField.Waist, "80,5")
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).performImeAction()
        composeRule.onNodeWithText("Informe um valor maior que zero.").assertDoesNotExist()
    }

    @Test
    fun imeNext_onEachField_focusesTheNextField() {
        SkirtDraftField.entries.zipWithNext().forEach { (field, next) ->
            // When: the keyboard opening after the action would hide the next field for a frame.
            val input = composeRule.onNodeWithText(field.labelText).performScrollTo().performClick()
            composeRule.awaitKeyboardVisible()
            input.performImeAction()

            // Then
            composeRule.onNodeWithText(next.labelText).assertIsFocused().assertIsDisplayed()
        }
    }

    @Test
    fun tab_onEase_focusesFinishedLength() {
        // Given
        val ease = composeRule.onNodeWithText(SkirtDraftField.Ease.labelText)
            .performScrollTo().performClick()
        // A keyboard arriving after Tab hides the next field for a frame Compose cannot see coming.
        composeRule.awaitKeyboardVisible()

        // When
        ease.performKeyInput { pressKey(Key.Tab) }

        // Then
        composeRule.onNodeWithText(SkirtDraftField.FinishedLength.labelText)
            .assertIsFocused().assertIsDisplayed()
    }

    @Test
    fun calculateButton_withKeyboardOpen_hidesKeyboardAndShowsWholeCuttingCard() {
        // Given
        fillInputsWithKeyboardOpen(REFERENCE_INPUTS)

        // When
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()

        // Then
        assertCuttingCardOnScreenWithoutKeyboard()
    }

    @Test
    fun imeDone_onLastField_hidesKeyboardAndShowsWholeCuttingCard() {
        // Given
        fillInputsWithKeyboardOpen(REFERENCE_INPUTS)

        // When
        composeRule.onNodeWithText(SkirtDraftField.HemAllowance.labelText).performImeAction()

        // Then
        assertCuttingCardOnScreenWithoutKeyboard()
    }

    @Test
    fun imeDone_withInvalidWaistAndKeyboardOpen_focusesAndRevealsWaist() {
        // Given
        fillInputsWithKeyboardOpen(listOf("", "2", "60", "1", "2"))

        // When
        composeRule.onNodeWithText(SkirtDraftField.HemAllowance.labelText).performImeAction()

        // Then
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText)
            .assertIsFocused().assertIsDisplayed()
        composeRule.onNodeWithText("Informe um valor", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
    }

    @Test
    fun recreate_withMeasurementsInProgress_keepsThem() {
        // Given
        val values = listOf("80,5", "0", "60", "1", "2")
        fillInputs(values)

        // When
        composeRule.recreate()

        // Then
        SkirtDraftField.entries.zip(values).forEach { (field, value) ->
            composeRule.onNodeWithText(field.labelText).performScrollTo().assertTextContains(value)
        }
    }

    @Test
    fun drafting_doubleText_keepsDiagramAndDisclosuresReachable() {
        // Given
        showCalculator { route ->
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                MoldaTheme(content = route)
            }
        }
        fillInputs(REFERENCE_INPUTS)

        // When
        calculate()

        // Then
        composeRule.onNodeWithContentDescription("Diagrama esquemático", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Como levar os raios ao papel").performScrollTo().performClick()
        composeRule.onNodeWithText("Transfira os raios ao papel", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Fórmulas").performScrollTo().performClick()
        composeRule.onNodeWithText("Use r sem arredondamento", substring = true)
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun wideCalculator_editAfterCalculation_bringsBackPlaceholderBesideInputs() {
        // Given
        showCalculator { route ->
            val window = LocalWindowInfo.current.containerSize
            val height = maxOf(900f, 1200 * window.height.toFloat() / window.width)
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(1200.dp, height.dp)),
            ) { MoldaTheme(content = route) }
        }
        composeRule.onNodeWithText("Preencha as medidas e toque em Calcular.").assertIsDisplayed()
        fillInputs(REFERENCE_INPUTS)
        calculate()
        composeRule.onNodeWithText("Raios para o molde").assertIsDisplayed()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assertIsDisplayed()

        // When
        edit(SkirtDraftField.Waist, "90")

        // Then
        composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
        composeRule.onNodeWithText("Preencha as medidas e toque em Calcular.").assertIsDisplayed()
    }

    @Test
    fun resizingAndLargeText_afterCalculation_keepDraftAndResults() {
        // Given
        val width = mutableIntStateOf(1200)
        val font = mutableFloatStateOf(1f)
        showCalculator { route ->
            val window = LocalWindowInfo.current.containerSize
            val height = maxOf(900f, width.intValue * window.height.toFloat() / window.width)
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(font.floatValue)) {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(DpSize(width.intValue.dp, height.dp)),
                ) { MoldaTheme(content = route) }
            }
        }
        fillInputs(REFERENCE_INPUTS)
        calculate()

        // When
        composeRule.runOnIdle { width.intValue = 400 }

        // Then
        composeRule.onNodeWithContentDescription(REFERENCE_RADII.last())
            .performScrollTo().assertIsDisplayed()

        // When
        composeRule.runOnIdle {
            width.intValue = 1000
            font.floatValue = 2f
        }

        // Then
        composeRule.onNodeWithText("Editar medidas").performScrollTo().performClick()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText)
            .assertIsFocused().assertTextContains("80")
    }

    private companion object {
        val REFERENCE_INPUTS = listOf("80", "2", "60", "1", "2")

        /** The cutting card's radii for [REFERENCE_INPUTS], as each row announces itself. */
        val REFERENCE_RADII = listOf(
            "Linha 1, linha contínua. Costura da cintura: 13,05 cm",
            "Linha 3, linha pontilhada. Barra acabada: 73,05 cm",
            "Linha 2, linha tracejada. Corte da cintura: 12,05 cm",
            "Linha 4, traço e ponto. Corte da barra: 75,05 cm",
        )
    }

    /** Shows the app inside [frame], on the calculator. */
    private fun showCalculator(frame: RouteFrame) {
        composeRule.showRoute(frame = frame)
        composeRule.showSkirtCalculator()
    }

    private fun fillInputs(values: List<String>) {
        SkirtDraftField.entries.zip(values).forEach { (field, value) -> edit(field, value) }
    }

    private fun fillInputsWithKeyboardOpen(values: List<String>) {
        fillInputs(values)
        composeRule.onNodeWithText(SkirtDraftField.HemAllowance.labelText)
            .performScrollTo().performClick()
        composeRule.awaitKeyboardVisible()
    }

    private fun assertCuttingCardOnScreenWithoutKeyboard() {
        composeRule.awaitKeyboardHidden()
        // No performScrollTo: the radii must already be on screen.
        REFERENCE_RADII.forEach { composeRule.onNodeWithContentDescription(it).assertIsDisplayed() }
    }

    /** Card with its diagram, the action row, summary, limits, then the two disclosures. */
    private fun assertResultsReadInOrder() {
        val card = composeRule.onNode(hasAnyChild(hasText("Raios para o molde")))
            .getUnclippedBoundsInRoot()
        val diagram = composeRule.onNodeWithContentDescription(
            "Diagrama esquemático",
            substring = true,
        ).getUnclippedBoundsInRoot()
        assertTrue(
            diagram.top >= card.top && diagram.bottom <= card.bottom,
            "diagram outside the card",
        )
        val guide = composeRule.onNodeWithText("Guia de dobra do tecido")
            .getUnclippedBoundsInRoot()
        val edit = composeRule.onNodeWithText("Editar medidas").getUnclippedBoundsInRoot()
        assertTrue(guide.top >= card.bottom && edit.top >= card.bottom, "actions above the card")
        val below = listOf(
            "Medidas utilizadas (cm)",
            "Limites do cálculo",
            "Como levar os raios ao papel",
            "Fórmulas",
        ).map { composeRule.onNodeWithText(it).getUnclippedBoundsInRoot() }
        (listOf(maxOf(guide.bottom, edit.bottom)) + below.map { it.top })
            .zipWithNext().forEach { (upper, lower) ->
                assertTrue(upper <= lower, "results out of order: $upper > $lower")
            }
    }

    /** Every other text and meaningful graphic in the results area, against its own surface. */
    private fun assertResultsAreaContrast(scheme: String) {
        listOf(
            "Raios para o molde",
            "Linhas de referência",
            "Linhas de corte",
            "Diagrama das linhas",
            "Guia de dobra do tecido",
            "Editar medidas",
            "Medidas utilizadas (cm)",
            "Folga de cintura:",
            "Limites do cálculo",
            "Para saia godê inteira",
            "Como levar os raios ao papel",
            "Fórmulas",
        ).forEach { text ->
            composeRule.onNodeWithText(text, substring = true, useUnmergedTree = true)
                .performScrollTo().assertReadableTextContrast("$text $scheme")
        }
        // The arcs, number discs and the not-to-scale note share the card's ink.
        composeRule.onNodeWithContentDescription("Diagrama esquemático", substring = true)
            .performScrollTo().assertGraphicContrast("diagram $scheme")
        composeRule.onNode(hasText("Fórmulas") and hasClickAction())
            .performScrollTo().assertGraphicContrast("disclosure chevron $scheme")
    }

    private fun edit(field: SkirtDraftField, value: String) {
        composeRule.onNodeWithText(field.labelText).performScrollTo().performTextReplacement(value)
    }

    private fun calculate() {
        composeRule.closeKeyboard()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
    }
}
