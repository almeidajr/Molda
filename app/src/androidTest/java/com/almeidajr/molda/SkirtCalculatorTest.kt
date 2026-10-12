package com.almeidajr.molda

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.skirt.ui.MeasurementField
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
