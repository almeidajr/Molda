package com.almeidajr.molda

import androidx.activity.compose.setContent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
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
import com.almeidajr.molda.skirt.ui.MeasurementField
import com.almeidajr.molda.skirt.ui.SkirtCalculatorRoute
import com.almeidajr.molda.ui.theme.MoldaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SkirtCalculatorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun garmentChoicesAndAllowances_explainExplicitZeroBeforeCalculation() {
        listOf(
            "Folga é o acréscimo à cintura para vestir; informe zero se não desejar folga.",
            "Tecido reservado para costurar a cintura; informe zero se não houver margem.",
            "Tecido reservado para acabar a barra; informe zero se não houver margem.",
        ).forEach { composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
        composeRule.onNodeWithText("Calcular").performScrollTo().assertIsDisplayed()
        calculate()
        composeRule.onNodeWithText(MeasurementField.Waist.label).assertIsFocused()
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
                            SkirtCalculatorRoute()
                        }
                    }
                }
            }
            fillInputs(listOf("80", "2", "60", "1", "2"))
            composeRule.onNodeWithText("Calcular").performScrollTo()
                .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            calculate()
            listOf(
                "Raio da linha de costura da cintura: 13,05 cm",
                "Raio da barra acabada: 73,05 cm",
                "Raio de corte da cintura: 12,05 cm",
                "Raio de corte da barra: 75,05 cm",
            ).forEach { radius ->
                val node = composeRule.onNodeWithText(radius).performScrollTo().assertIsDisplayed()
                val layouts = mutableListOf<TextLayoutResult>()
                node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                val style = layouts.single().layoutInput.style
                assertEquals(24.sp, style.fontSize)
                assertEquals(32.sp, style.lineHeight)
                node.assertReadableTextContrast("$radius dark=$dark dynamic=$dynamic")
            }
            composeRule.onNodeWithText("Editar medidas").performScrollTo()
                .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick()
            composeRule.onNodeWithText(MeasurementField.Waist.label).assertIsFocused()
        }
    }

    @Test
    fun calculate_revealsGroupedResultsAndInputSummary_withCollapsedFormulasAndEdit() {
        fillInputs(listOf("80", "2", "60", "1", "2"))
        calculate()
        composeRule.onNodeWithText("Raios para o molde").assertIsDisplayed()
        listOf("Linhas de referência", "Linhas de corte", "Medidas utilizadas").forEach {
            composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        MeasurementField.entries.zip(listOf("80", "2", "60", "1", "2")).forEach { (field, value) ->
            composeRule.onNodeWithText(
                "${field.label}: $value,00 cm",
            ).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithText("r = (80 + 2) / (2π) = 13,05 cm").assertDoesNotExist()
        composeRule.onNodeWithText("Mostrar fórmulas").performScrollTo().performClick()
        composeRule.onNodeWithText("r = (80 + 2) / (2π) = 13,05 cm")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Ocultar fórmulas").performScrollTo().performClick()
        composeRule.onNodeWithText("r = (80 + 2) / (2π) = 13,05 cm").assertDoesNotExist()
        composeRule.onNodeWithText("Editar medidas").performScrollTo().performClick()
        composeRule.onNodeWithText(
            MeasurementField.Waist.label,
        ).assertIsFocused().assertIsDisplayed()
    }

    @Test
    fun validation_firstSubmitFocusesWaist_thenRevalidatesOnBlur() {
        composeRule.onNodeWithText("Informe um valor", substring = true).assertDoesNotExist()
        calculate()
        composeRule.onNodeWithText(
            MeasurementField.Waist.label,
        ).assertIsFocused().assertIsDisplayed()
        edit(MeasurementField.Waist, "-1")
        composeRule.onNodeWithText(MeasurementField.Waist.label).performImeAction()
        composeRule.onNodeWithText("Informe um valor maior que zero.").assertExists()
        edit(MeasurementField.Waist, "80,5")
        composeRule.onNodeWithText(MeasurementField.Waist.label).performImeAction()
        composeRule.onNodeWithText("Informe um valor maior que zero.").assertDoesNotExist()
        composeRule.onNodeWithText(MeasurementField.Ease.label).assertIsFocused()
    }

    @Test
    fun keyboard_nextAndDone_acceptDecimalCommaAndExplicitZeros() {
        val values = listOf("80,5", "0", "60", "0", "0")
        MeasurementField.entries.zip(values).forEach { (field, value) ->
            edit(field, value)
            composeRule.onNodeWithText(field.label).performClick().performImeAction()
            if (field != MeasurementField.HemAllowance) {
                composeRule.onNodeWithText(MeasurementField.entries[field.ordinal + 1].label)
                    .assertIsFocused().assertIsDisplayed()
            }
        }
        composeRule.onNodeWithText("Raio de corte da barra: 72,81 cm")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun untouchedFields_blurWithoutShowingErrors_andEveryEditWithdrawsResults() {
        edit(MeasurementField.Waist, "-1")
        composeRule.onNodeWithText(MeasurementField.Waist.label).performClick().performImeAction()
        composeRule.onNodeWithText("Informe um valor maior que zero.").assertDoesNotExist()
        fillInputs(listOf("80", "0", "60", "0", "0"))
        MeasurementField.entries.forEach { field ->
            calculate()
            composeRule.onNodeWithText("Raios para o molde").assertExists()
            edit(field, "1")
            composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
            edit(
                field,
                if (field ==
                    MeasurementField.Waist
                ) {
                    "80"
                } else if (field ==
                    MeasurementField.Length
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
        edit(MeasurementField.WaistSeamAllowance, "90")
        composeRule.onNodeWithText(
            MeasurementField.WaistSeamAllowance.label,
        ).performClick().performImeAction()
        composeRule.onNodeWithText("A margem deve ser menor", substring = true).assertExists()
        edit(MeasurementField.WaistSeamAllowance, "1")
        composeRule.onNodeWithText(
            MeasurementField.WaistSeamAllowance.label,
        ).performClick().performImeAction()
        composeRule.onNodeWithText("A margem deve ser menor", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
    }

    @Test
    fun keyboard_hardwareTabFollowsMeasurementOrder() {
        composeRule.onNodeWithText(MeasurementField.Ease.label).performScrollTo().performClick()
            .performKeyInput { pressKey(Key.Tab) }
        composeRule.onNodeWithText(
            MeasurementField.Length.label,
        ).assertIsFocused().assertIsDisplayed()
    }

    @Test
    fun calculate_withoutProfile_displaysFourLabelledReferenceRadiiAndBoundaries() {
        fillInputs(listOf("80", "2", "60", "1", "2"))
        calculate()
        listOf(
            "Raio da linha de costura da cintura: 13,05 cm",
            "Raio da barra acabada: 73,05 cm",
            "Raio de corte da cintura: 12,05 cm",
            "Raio de corte da barra: 75,05 cm",
        ).forEach { composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
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
        composeRule.onNodeWithText(
            "Raio de corte da cintura: 12,39 cm",
        ).performScrollTo().assertIsDisplayed()
        edit(MeasurementField.Waist, "90")
        composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
        calculate()
        composeRule.onNodeWithText(
            "Raio da linha de costura da cintura: 14,52 cm",
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun calculate_blankMalformedAndInvalidInputs_showsErrorsWithoutResults() {
        fillInputs(listOf("80", "0", "60", "0", "0"))
        calculate()
        composeRule.onNodeWithText(
            "Raio de corte da barra: 72,73 cm",
        ).performScrollTo().assertIsDisplayed()
        listOf(
            "" to "Informe um valor",
            "NaN" to "Informe um número finito",
            "1,2,3" to "Informe um número finito",
            "-1" to "Informe zero ou um valor positivo",
        ).forEach { (text, error) ->
            edit(MeasurementField.HemAllowance, text)
            calculate()
            composeRule.onNodeWithText(
                error,
                substring = true,
            ).performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
        }
        edit(MeasurementField.HemAllowance, "0")
        edit(MeasurementField.WaistSeamAllowance, "100")
        calculate()
        composeRule.onNodeWithText(
            "A margem deve ser menor",
            substring = true,
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun recreate_keepsInProgressMeasurements() {
        fillInputs(listOf("80,5", "0", "60", "1", "2"))
        composeRule.onNodeWithText(MeasurementField.HemAllowance.label).assertTextContains("2")
        composeRule.activityRule.scenario.recreate()
        MeasurementField.entries.zip(
            listOf("80,5", "0", "60", "1", "2"),
        ).forEach { (field, value) ->
            composeRule.onNodeWithText(field.label).performScrollTo().assertTextContains(value)
        }
    }

    @Test
    fun keyboard_focusedFieldAndCalculateRemainUsable() {
        fillInputs(listOf("80", "2", "60", "1", "2"))
        composeRule.onNodeWithText(MeasurementField.HemAllowance.label)
            .performScrollTo().performClick().assertIsDisplayed()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
        composeRule.onNodeWithText("Raio de corte da barra: 75,05 cm")
            .performScrollTo().assertIsDisplayed()
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
        composeRule.onNodeWithText("Mostrar fórmulas").performScrollTo().performClick()
        composeRule.onNodeWithText(
            "r = (80 + 2) / (2π) = 13,05 cm",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            "1. Costura da cintura (linha contínua): 13,05 cm",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            "Diagrama esquemático",
            substring = true,
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "A margem de cintura de 1 cm avança para dentro; a margem de barra de 2 cm avança para fora.",
            substring = true,
        ).assertExists()
    }

    @Test
    fun drafting_zeroAllowances_explainsCoincidentLines() {
        fillInputs(listOf("80", "0", "60", "0", "0"))
        calculate()
        listOf(
            "Margem de cintura zero: as linhas 1 e 2 coincidem.",
            "Margem de barra zero: as linhas 3 e 4 coincidem.",
            "2. Corte da cintura (linha tracejada): 12,73 cm",
            "4. Corte da barra (traço e ponto): 72,73 cm",
        ).forEach { composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
    }

    @Test
    fun drafting_edit_withdrawsExplanationAndUsesNewLength() {
        fillInputs(listOf("80", "2", "60", "1", "2"))
        calculate()
        composeRule.onNodeWithText("Mostrar fórmulas").performScrollTo().performClick()
        listOf(
            "Barra acabada = r + comprimento = r + 60 = 73,05 cm",
            "Corte da cintura = r − margem de costura = r − 1 = 12,05 cm",
            "Corte da barra = r + comprimento + margem de barra = r + 60 + 2 = 75,05 cm",
            "2. Corte da cintura (linha tracejada): 12,05 cm",
            "3. Barra acabada (linha pontilhada): 73,05 cm",
            "4. Corte da barra (traço e ponto): 75,05 cm",
            "A margem de cintura de 1 cm avança para dentro; a margem de barra de 2 cm avança para fora.",
        ).forEach { composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
        edit(MeasurementField.Length, "70")
        composeRule.onNodeWithText("Como levar os raios ao papel").assertDoesNotExist()
        composeRule.onNodeWithContentDescription(
            "A margem de cintura",
            substring = true,
        ).assertDoesNotExist()
        calculate()
        composeRule.onNodeWithText("Mostrar fórmulas").performScrollTo().performClick()
        composeRule.onNodeWithText("Barra acabada = r + comprimento = r + 70 = 83,05 cm")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun drafting_enlargedText_keepsGuidanceAndLegendReachable() {
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                    MoldaTheme { SkirtCalculatorRoute() }
                }
            }
        }
        fillInputs(listOf("80", "2", "60", "1", "2"))
        calculate()
        listOf(
            "1. Costura da cintura (linha contínua): 13,05 cm",
            "2. Corte da cintura (linha tracejada): 12,05 cm",
            "3. Barra acabada (linha pontilhada): 73,05 cm",
            "4. Corte da barra (traço e ponto): 75,05 cm",
        ).forEach { composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
        composeRule.onNodeWithText("Diagrama esquemático", substring = true)
            .performScrollTo().assertIsDisplayed()
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
                    MoldaTheme { SkirtCalculatorRoute() }
                }
            }
        }
        composeRule.onNodeWithText("Preencha as medidas e toque em Calcular.").assertIsDisplayed()
        fillInputs(listOf("80", "2", "60", "1", "2"))
        calculate()
        composeRule.onNodeWithText("Raios para o molde").assertIsDisplayed()
        composeRule.onNodeWithText(MeasurementField.Waist.label).assertIsDisplayed()
        edit(MeasurementField.Waist, "90")
        composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
        composeRule.onNodeWithText("Preencha as medidas e toque em Calcular.").assertIsDisplayed()
    }

    @Test
    fun resizingAndLargeText_keepCalculatorDraftAndSameScreenResults() {
        val width = androidx.compose.runtime.mutableStateOf(1200)
        val font = androidx.compose.runtime.mutableStateOf(1f)
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                val window = LocalWindowInfo.current.containerSize
                val height = maxOf(900f, width.value * window.height.toFloat() / window.width)
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(font.value)) {
                    DeviceConfigurationOverride(
                        DeviceConfigurationOverride.ForcedSize(DpSize(width.value.dp, height.dp)),
                    ) { MoldaTheme { SkirtCalculatorRoute() } }
                }
            }
        }
        fillInputs(listOf("80", "2", "60", "1", "2"))
        calculate()
        composeRule.runOnUiThread { width.value = 400 }
        composeRule.onNodeWithText("Raio de corte da barra: 75,05 cm")
            .performScrollTo().assertIsDisplayed()
        composeRule.runOnUiThread {
            width.value = 1000
            font.value = 2f
        }
        composeRule.onNodeWithText("Editar medidas").performScrollTo().performClick()
        composeRule.onNodeWithText(MeasurementField.Waist.label)
            .assertIsFocused().assertTextContains("80")
        // Android's keyboard can resize the viewport after Compose has become idle.
        composeRule.waitUntil {
            ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        composeRule.onNodeWithText("Calcular").performScrollTo().assertIsDisplayed()
        composeRule.runOnUiThread {
            width.value = 1200
            font.value = 1f
        }
        composeRule.onNodeWithText("Raios para o molde").assertIsDisplayed()
        edit(MeasurementField.Length, "70")
        composeRule.onNodeWithText("Preencha as medidas e toque em Calcular.").assertIsDisplayed()
        composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
    }
    private fun fillInputs(values: List<String>) {
        MeasurementField.entries.zip(values).forEach { (field, value) -> edit(field, value) }
    }

    private fun edit(field: MeasurementField, value: String) {
        composeRule.onNodeWithText(field.label).performScrollTo().performTextReplacement(value)
    }

    private fun calculate() {
        closeSoftKeyboard()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
    }
}
