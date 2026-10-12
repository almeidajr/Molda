package com.almeidajr.molda

import androidx.activity.compose.setContent
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.skirt.ui.MeasurementField
import com.almeidajr.molda.skirt.ui.SkirtCalculatorRoute
import com.almeidajr.molda.ui.theme.MoldaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SkirtCalculatorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

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
        calculate()
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
        ).assertIsDisplayed()
        fillInputs(listOf("80", "2", "60", "1", "2"))
        calculate()
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

    private fun fillInputs(values: List<String>) {
        MeasurementField.entries.zip(values).forEach { (field, value) -> edit(field, value) }
    }

    private fun edit(field: MeasurementField, value: String) {
        composeRule.onNodeWithText(field.label).performScrollTo().performTextReplacement(value)
    }

    private fun calculate() {
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
    }
}
