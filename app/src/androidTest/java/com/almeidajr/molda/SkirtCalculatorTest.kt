package com.almeidajr.molda

import androidx.activity.compose.setContent
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
import androidx.compose.ui.test.assertIsEnabled
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
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.ui.theme.MoldaTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SkirtCalculatorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun openSkirtCalculator() = composeRule.showSkirtCalculator()

    @Test
    fun garmentChoicesAndAllowances_explainExplicitZeroBeforeCalculation() {
        listOf(
            "Folga é o acréscimo à cintura para vestir; informe zero se não desejar folga.",
            "Tecido reservado para costurar a cintura; informe zero se não houver margem.",
            "Tecido reservado para acabar a barra; informe zero se não houver margem.",
        ).forEach { composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
        composeRule.onNodeWithText("Calcular").performScrollTo().assertIsDisplayed()
        calculate()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assertIsFocused()
    }

    @Test
    fun calculatorForm_spansCalculateAndKeepsWaistSourceWithWaistField() {
        val waist = composeRule.onNodeWithText(SkirtDraftField.Waist.labelText)
            .getUnclippedBoundsInRoot()
        val source = composeRule.onNodeWithText("Medida manual").getUnclippedBoundsInRoot()
        val useProfile = composeRule.onNodeWithText("Usar perfil").assertHeightIsAtLeast(48.dp)
            .getUnclippedBoundsInRoot()
        // The source and its action share one row just above the field, before its guidance.
        assertTrue(source.top >= useProfile.top && source.bottom <= useProfile.bottom)
        assertTrue(useProfile.bottom <= waist.top && waist.top - useProfile.bottom < 16.dp)
        assertTrue(useProfile.right <= waist.right + 1.dp)
        val calculate = composeRule.onNodeWithText("Calcular").performScrollTo()
            .assertIsEnabled().getUnclippedBoundsInRoot()
        // The button spans the form, wider than the field inset inside its group.
        assertTrue(calculate.left < waist.left && calculate.right > waist.right)
        calculate()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assertIsFocused()
    }

    @Test
    fun appearanceSchemes_atDoubleText_keepAllFourRadiiAndSeparatedActionsReachable() {
        listOf(
            false to false,
            true to false,
            false to true,
            true to true,
        ).forEach { (dark, dynamic) ->
            composeRule.runOnUiThread {
                composeRule.activity.setContent {
                    DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                        MoldaTheme(darkTheme = dark, dynamicColor = dynamic) {
                            MoldaRoute()
                        }
                    }
                }
            }
            composeRule.showSkirtCalculator()
            fillInputs(listOf("80", "2", "60", "1", "2"))
            composeRule.onNodeWithText("Calcular").performScrollTo()
                .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            calculate()
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
                node.assertReadableTextContrast("$radius dark=$dark dynamic=$dynamic")
                // A single match: the card's rows are the diagram's only key.
                composeRule.onNodeWithText(
                    radius.substringAfter(". ").substringBefore(":"),
                    useUnmergedTree = true,
                ).assertReadableTextContrast("label $radius dark=$dark dynamic=$dynamic")
            }
            assertResultsAreaContrast("dark=$dark dynamic=$dynamic")
            composeRule.onNodeWithText("Editar medidas").performScrollTo()
                .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick()
            composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assertIsFocused()
        }
    }

    @Test
    fun calculate_revealsGroupedResultsAndInputSummary_withCollapsedFormulasAndEdit() {
        fillInputs(listOf("80", "2", "60", "1", "2"))
        calculate()
        composeRule.onNodeWithText("Raios para o molde").assertIsDisplayed()
        listOf("Linhas de referência", "Linhas de corte", "Medidas utilizadas (cm)").forEach {
            composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        SkirtDraftField.entries.zip(listOf("80", "2", "60", "1", "2")).forEach { (field, value) ->
            composeRule.onNodeWithText(
                "${field.summaryLabelText}: $value,00",
                substring = true,
            ).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithText("(cm):", substring = true).assertDoesNotExist()
        assertResultsReadInOrder()
        composeRule.onNodeWithText("r = (80 + 2) / (2π) = 13,05 cm").assertDoesNotExist()
        composeRule.onNodeWithText("Fórmulas").performScrollTo().performClick()
        composeRule.onNodeWithText("r = (80 + 2) / (2π) = 13,05 cm")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Use r sem arredondamento", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Fórmulas").performScrollTo().performClick()
        composeRule.onNodeWithText("r = (80 + 2) / (2π) = 13,05 cm").assertDoesNotExist()
        composeRule.onNodeWithText("Editar medidas").performScrollTo().performClick()
        composeRule.onNodeWithText(
            SkirtDraftField.Waist.labelText,
        ).assertIsFocused().assertIsDisplayed()
    }

    @Test
    fun cuttingCard_announcesEachRadiusOnceUnderGroupHeadings() {
        fillInputs(listOf("80", "2", "60", "1", "2"))
        calculate()
        listOf("Raios para o molde", "Linhas de referência", "Linhas de corte").forEach {
            composeRule.onNode(hasText(it) and isHeading()).performScrollTo().assertIsDisplayed()
        }
        composeRule.onAllNodes(
            hasText("Raios para o molde") and
                SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion),
        )
            .assertCountEquals(1)
        REFERENCE_RADII.forEach { radius ->
            composeRule.onAllNodes(hasContentDescription(radius)).assertCountEquals(1)
            composeRule.onNodeWithContentDescription(radius).performScrollTo().assertIsDisplayed()
            composeRule.onAllNodes(hasText(radius.substringAfterLast(": "))).assertCountEquals(1)
        }
    }

    @Test
    fun validation_firstSubmitFocusesWaist_thenRevalidatesOnBlur() {
        composeRule.onNodeWithText("Informe um valor", substring = true).assertDoesNotExist()
        calculate()
        composeRule.onNodeWithText(
            SkirtDraftField.Waist.labelText,
        ).assertIsFocused().assertIsDisplayed()
        edit(SkirtDraftField.Waist, "-1")
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).performImeAction()
        composeRule.onNodeWithText("Informe um valor maior que zero.").assertExists()
        edit(SkirtDraftField.Waist, "80,5")
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).performImeAction()
        composeRule.onNodeWithText("Informe um valor maior que zero.").assertDoesNotExist()
        composeRule.onNodeWithText(SkirtDraftField.Ease.labelText).assertIsFocused()
    }

    @Test
    fun keyboard_nextAndDone_acceptDecimalCommaAndExplicitZeros() {
        val values = listOf("80,5", "0", "60", "0", "0")
        SkirtDraftField.entries.zip(values).forEach { (field, value) ->
            edit(field, value)
            composeRule.onNodeWithText(field.labelText).performClick().performImeAction()
            if (field != SkirtDraftField.HemAllowance) {
                composeRule.onNodeWithText(SkirtDraftField.entries[field.ordinal + 1].labelText)
                    .assertIsFocused().assertIsDisplayed()
            }
        }
        composeRule.onNodeWithContentDescription(
            "Linha 4, traço e ponto. Corte da barra: 72,81 cm",
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun untouchedFields_blurWithoutShowingErrors_andEveryEditWithdrawsResults() {
        edit(SkirtDraftField.Waist, "-1")
        composeRule.onNodeWithText(
            SkirtDraftField.Waist.labelText,
        ).performClick().performImeAction()
        composeRule.onNodeWithText("Informe um valor maior que zero.").assertDoesNotExist()
        fillInputs(listOf("80", "0", "60", "0", "0"))
        SkirtDraftField.entries.forEach { field ->
            calculate()
            composeRule.onNodeWithText("Raios para o molde").assertExists()
            edit(field, "1")
            composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
            edit(
                field,
                if (field ==
                    SkirtDraftField.Waist
                ) {
                    "80"
                } else if (field ==
                    SkirtDraftField.FinishedLength
                ) {
                    "60"
                } else {
                    "0"
                },
            )
        }
    }

    @Test
    fun validation_blurRetainsImpossibleCuttingMarginErrorUntilCorrected() {
        fillInputs(listOf("80", "0", "60", "100", "0"))
        calculate()
        edit(SkirtDraftField.WaistSeamAllowance, "90")
        composeRule.onNodeWithText(
            SkirtDraftField.WaistSeamAllowance.labelText,
        ).performClick().performImeAction()
        composeRule.onNodeWithText("A margem deve ser menor", substring = true).assertExists()
        edit(SkirtDraftField.WaistSeamAllowance, "1")
        composeRule.onNodeWithText(
            SkirtDraftField.WaistSeamAllowance.labelText,
        ).performClick().performImeAction()
        composeRule.onNodeWithText("A margem deve ser menor", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
    }

    @Test
    fun keyboard_hardwareTabFollowsMeasurementOrder() {
        composeRule.onNodeWithText(SkirtDraftField.Ease.labelText).performScrollTo().performClick()
            .performKeyInput { pressKey(Key.Tab) }
        composeRule.onNodeWithText(
            SkirtDraftField.FinishedLength.labelText,
        ).assertIsFocused().assertIsDisplayed()
    }

    @Test
    fun calculate_withoutProfile_displaysFourLabelledReferenceRadiiAndBoundaries() {
        fillInputs(listOf("80", "2", "60", "1", "2"))
        calculate()
        REFERENCE_RADII.forEach {
            composeRule.onNodeWithContentDescription(it).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithText(
            "Para saia godê inteira",
            substring = true,
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            "O comprimento acabado exclui a altura do cós.",
            substring = true,
        ).assertIsDisplayed()
    }

    @Test
    fun edit_decimalCommaInput_withdrawsResultAndRecalculates() {
        fillInputs(listOf("80,5", "1,25", "60,75", "0,625", "2,5"))
        calculate()
        composeRule.onNodeWithContentDescription(
            "Linha 2, linha tracejada. Corte da cintura: 12,39 cm",
        ).performScrollTo().assertIsDisplayed()
        edit(SkirtDraftField.Waist, "90")
        composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
        calculate()
        composeRule.onNodeWithContentDescription(
            "Linha 1, linha contínua. Costura da cintura: 14,52 cm",
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun calculate_blankMalformedAndInvalidInputs_showsErrorsWithoutResults() {
        fillInputs(listOf("80", "0", "60", "0", "0"))
        calculate()
        composeRule.onNodeWithContentDescription(
            "Linha 4, traço e ponto. Corte da barra: 72,73 cm",
        ).performScrollTo().assertIsDisplayed()
        listOf(
            "" to "Informe um valor",
            "NaN" to "Informe um número finito",
            "1,2,3" to "Informe um número finito",
            "-1" to "Informe zero ou um valor positivo",
        ).forEach { (text, error) ->
            edit(SkirtDraftField.HemAllowance, text)
            calculate()
            composeRule.onNodeWithText(
                error,
                substring = true,
            ).performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
        }
        edit(SkirtDraftField.HemAllowance, "0")
        edit(SkirtDraftField.WaistSeamAllowance, "100")
        calculate()
        composeRule.onNodeWithText(
            "A margem deve ser menor",
            substring = true,
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun recreate_keepsInProgressMeasurements() {
        fillInputs(listOf("80,5", "0", "60", "1", "2"))
        composeRule.onNodeWithText(SkirtDraftField.HemAllowance.labelText).assertTextContains("2")
        composeRule.activityRule.scenario.recreate()
        SkirtDraftField.entries.zip(
            listOf("80,5", "0", "60", "1", "2"),
        ).forEach { (field, value) ->
            composeRule.onNodeWithText(field.labelText).performScrollTo().assertTextContains(value)
        }
    }

    @Test
    fun keyboard_focusedFieldAndCalculateRemainUsable() {
        fillInputs(listOf("80", "2", "60", "1", "2"))
        composeRule.onNodeWithText(SkirtDraftField.HemAllowance.labelText)
            .performScrollTo().performClick().assertIsDisplayed()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription(REFERENCE_RADII.last())
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun calculateButton_withKeyboardOpen_hidesKeyboardAndShowsWholeCuttingCard() {
        fillInputsWithKeyboardOpen(listOf("80", "2", "60", "1", "2"))
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
        assertCuttingCardOnScreenWithoutKeyboard()
    }

    @Test
    fun doneOnLastField_hidesKeyboardAndShowsWholeCuttingCard() {
        fillInputsWithKeyboardOpen(listOf("80", "2", "60", "1", "2"))
        composeRule.onNodeWithText(SkirtDraftField.HemAllowance.labelText).performImeAction()
        assertCuttingCardOnScreenWithoutKeyboard()
    }

    @Test
    fun calculate_withInvalidWaistAndKeyboardOpen_focusesAndRevealsWaist() {
        fillInputsWithKeyboardOpen(listOf("", "2", "60", "1", "2"))
        composeRule.onNodeWithText(SkirtDraftField.HemAllowance.labelText).performImeAction()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText)
            .assertIsFocused().assertIsDisplayed()
        composeRule.onNodeWithText("Informe um valor", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
    }

    @Test
    fun drafting_explainsCurrentInputsAndAccessibleLines() {
        composeRule.onNodeWithText(
            "Meça a circunferência da cintura",
            substring = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "na posição desejada do cós",
            substring = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "a partir da linha de costura da cintura; exclua a altura do cós.",
            substring = true,
        ).performScrollTo().assertIsDisplayed()
        fillInputs(listOf("80", "2", "60", "1", "2"))
        calculate()
        composeRule.onNodeWithText("Fórmulas").performScrollTo().performClick()
        composeRule.onNodeWithText(
            "r = (80 + 2) / (2π) = 13,05 cm",
        ).performScrollTo().assertIsDisplayed()
        listOf(
            "Linha 1, linha contínua: costura da cintura.",
            "Linha 4, traço e ponto: corte da barra.",
            "Diagrama esquemático",
            "não garante o caimento",
            "A margem de cintura de 1 cm avança para dentro; a margem de barra de 2 cm avança para fora.",
        ).forEach {
            composeRule.onNodeWithContentDescription(it, substring = true)
                .performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithContentDescription("coincidem", substring = true).assertDoesNotExist()
    }

    @Test
    fun drafting_zeroAllowances_explainsCoincidentLines() {
        fillInputs(listOf("80", "0", "60", "0", "0"))
        calculate()
        listOf(
            "Margem de cintura zero: as linhas 1 e 2 coincidem.",
            "Margem de barra zero: as linhas 3 e 4 coincidem.",
        ).forEach {
            composeRule.onNodeWithContentDescription(it, substring = true)
                .performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun drafting_edit_withdrawsExplanationAndUsesNewLength() {
        fillInputs(listOf("80", "2", "60", "1", "2"))
        calculate()
        composeRule.onNodeWithText("Como levar os raios ao papel").performScrollTo().performClick()
        composeRule.onNodeWithText("Fórmulas").performScrollTo().performClick()
        listOf(
            "Barra acabada = r + comprimento = r + 60 = 73,05 cm",
            "Corte da cintura = r − margem de costura = r − 1 = 12,05 cm",
            "Corte da barra = r + comprimento + margem de barra = r + 60 + 2 = 75,05 cm",
            "A margem de cintura de 1 cm avança para dentro; a margem de barra de 2 cm avança para fora.",
        ).forEach { composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
        edit(SkirtDraftField.FinishedLength, "70")
        composeRule.onNodeWithText("Como levar os raios ao papel").assertDoesNotExist()
        composeRule.onNodeWithContentDescription(
            "A margem de cintura",
            substring = true,
        ).assertDoesNotExist()
        calculate()
        composeRule.onNodeWithText("Fórmulas").performScrollTo().performClick()
        composeRule.onNodeWithText("Barra acabada = r + comprimento = r + 70 = 83,05 cm")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun drafting_enlargedText_keepsDiagramAndDisclosuresReachable() {
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                    MoldaTheme { MoldaRoute() }
                }
            }
        }
        composeRule.showSkirtCalculator()
        fillInputs(listOf("80", "2", "60", "1", "2"))
        calculate()
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
    fun results_actionsAndDisclosuresAreTallEnough_andDisclosuresExposeState() {
        fillInputs(listOf("80", "2", "60", "1", "2"))
        calculate()
        listOf("Editar medidas", "Guia de dobra do tecido").forEach {
            composeRule.onNodeWithText(it).performScrollTo().assertHeightIsAtLeast(48.dp)
        }
        listOf("Como levar os raios ao papel", "Fórmulas").forEach { label ->
            val disclosure = composeRule.onNode(hasText(label) and hasClickAction())
            disclosure.performScrollTo().assertHeightIsAtLeast(48.dp)
                .assert(hasStateDescription("Recolhido"))
                .performClick()
            disclosure.assert(hasStateDescription("Expandido"))
        }
    }

    @Test
    fun wideCalculator_placeholderAndResultsStayBesideInputs_andEditsWithdrawDimensions() {
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                val window = LocalWindowInfo.current.containerSize
                val height = maxOf(900f, 1200 * window.height.toFloat() / window.width)
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(DpSize(1200.dp, height.dp)),
                ) {
                    MoldaTheme { MoldaRoute() }
                }
            }
        }
        composeRule.showSkirtCalculator()
        composeRule.onNodeWithText("Preencha as medidas e toque em Calcular.").assertIsDisplayed()
        fillInputs(listOf("80", "2", "60", "1", "2"))
        calculate()
        composeRule.onNodeWithText("Raios para o molde").assertIsDisplayed()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assertIsDisplayed()
        edit(SkirtDraftField.Waist, "90")
        composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
        composeRule.onNodeWithText("Preencha as medidas e toque em Calcular.").assertIsDisplayed()
    }

    @Test
    fun resizingAndLargeText_keepCalculatorDraftAndSameScreenResults() {
        val width = mutableIntStateOf(1200)
        val font = mutableFloatStateOf(1f)
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                val window = LocalWindowInfo.current.containerSize
                val height = maxOf(900f, width.intValue * window.height.toFloat() / window.width)
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.FontScale(font.floatValue),
                ) {
                    DeviceConfigurationOverride(
                        DeviceConfigurationOverride.ForcedSize(
                            DpSize(width.intValue.dp, height.dp),
                        ),
                    ) { MoldaTheme { MoldaRoute() } }
                }
            }
        }
        composeRule.showSkirtCalculator()
        fillInputs(listOf("80", "2", "60", "1", "2"))
        calculate()
        composeRule.runOnUiThread { width.intValue = 400 }
        composeRule.onNodeWithContentDescription(REFERENCE_RADII.last())
            .performScrollTo().assertIsDisplayed()
        composeRule.runOnUiThread {
            width.intValue = 1000
            font.floatValue = 2f
        }
        composeRule.onNodeWithText("Editar medidas").performScrollTo().performClick()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText)
            .assertIsFocused().assertTextContains("80")
        // Android's keyboard can resize the viewport after Compose has become idle.
        composeRule.waitUntil(10_000) { isKeyboardVisible() }
        composeRule.onNodeWithText("Calcular").performScrollTo().assertIsDisplayed()
        composeRule.runOnUiThread {
            width.intValue = 1200
            font.floatValue = 1f
        }
        composeRule.onNodeWithText("Raios para o molde").assertIsDisplayed()
        edit(SkirtDraftField.FinishedLength, "70")
        composeRule.onNodeWithText("Preencha as medidas e toque em Calcular.").assertIsDisplayed()
        composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
    }

    private companion object {
        val REFERENCE_RADII = listOf(
            "Linha 1, linha contínua. Costura da cintura: 13,05 cm",
            "Linha 3, linha pontilhada. Barra acabada: 73,05 cm",
            "Linha 2, linha tracejada. Corte da cintura: 12,05 cm",
            "Linha 4, traço e ponto. Corte da barra: 75,05 cm",
        )
    }

    private fun fillInputs(values: List<String>) {
        SkirtDraftField.entries.zip(values).forEach { (field, value) -> edit(field, value) }
    }

    private fun fillInputsWithKeyboardOpen(values: List<String>) {
        fillInputs(values)
        composeRule.onNodeWithText(SkirtDraftField.HemAllowance.labelText)
            .performScrollTo().performClick()
        composeRule.waitUntil(10_000) { isKeyboardVisible() }
    }

    private fun assertCuttingCardOnScreenWithoutKeyboard() {
        // Android's keyboard can resize the viewport after Compose has become idle.
        composeRule.waitUntil(10_000) { !isKeyboardVisible() }
        composeRule.waitForIdle()
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
        )
            .getUnclippedBoundsInRoot()
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

    private fun isKeyboardVisible(): Boolean =
        ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
            ?.isVisible(WindowInsetsCompat.Type.ime()) == true

    private fun edit(field: SkirtDraftField, value: String) {
        composeRule.onNodeWithText(field.labelText).performScrollTo().performTextReplacement(value)
    }

    private fun calculate() {
        closeSoftKeyboard()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
    }
}
