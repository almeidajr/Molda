package com.almeidajr.molda.skirt.ui

import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.skirt.domain.SkirtCalculation
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.skirt.domain.calculateSkirt
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

// Robolectric resolves the result's copy from the app's string resources.
@RunWith(RobolectricTestRunner::class)
class SkirtResultTest {
    private val text: (Int) -> String = RuntimeEnvironment.getApplication()::getString

    @Test
    fun formulas_referenceInputs_showEachLineWithTheDraftsValues() {
        // When
        val result = resultFor("80", "2", "60", "1", "2")

        // Then
        assertEquals(
            listOf(
                "Valores em centímetros.",
                "Costura da cintura: r = (cintura + folga) / (2π)",
                "r = (80 + 2) / (2π) = 13,05 cm",
                "Barra acabada = r + comprimento = r + 60 = 73,05 cm",
                "Corte da cintura = r − margem de costura = r − 1 = 12,05 cm",
                "Corte da barra = r + comprimento + margem de barra = r + 60 + 2 = 75,05 cm",
                "Use r sem arredondamento nos cálculos; " +
                    "apenas os resultados exibidos têm duas casas decimais.",
            ),
            result.formulas,
        )
    }

    @Test
    fun allowanceDirections_referenceInputs_stateEachAllowanceAndItsDirection() {
        // When/Then
        assertEquals(
            "A margem de cintura de 1 cm avança para dentro; " +
                "a margem de barra de 2 cm avança para fora.",
            resultFor("80", "2", "60", "1", "2").allowanceDirections,
        )
    }

    @Test
    fun diagramDescription_nonZeroAllowances_statesDirectionsWithoutCoincidence() {
        // When
        val result = resultFor("80", "2", "60", "1", "2,5")

        // Then
        assertTrue(
            "A margem de cintura de 1 cm avança para dentro; " +
                "a margem de barra de 2,5 cm avança para fora." in result.diagramDescription,
        )
        assertFalse("coincidem" in result.diagramDescription)
        assertEquals(emptyList(), result.coincidenceNotes)
    }

    @Test
    fun diagramDescription_zeroAllowances_statesZeroDirectionsAndCoincidences() {
        // When
        val result = resultFor("80", "2", "60", "0", "0")

        // Then
        val notes = listOf(
            "Margem de cintura zero: as linhas 1 e 2 coincidem.",
            "Margem de barra zero: as linhas 3 e 4 coincidem.",
        )
        assertEquals(notes, result.coincidenceNotes)
        (
            notes + "A margem de cintura de 0 cm avança para dentro; " +
                "a margem de barra de 0 cm avança para fora."
            ).forEach { assertTrue(it in result.diagramDescription, it) }
    }

    @Test
    fun skirtResult_paddedSignedOrPointInput_showsValuesAsTyped() {
        // When
        val result = resultFor("80", "1.5", "60", "01", "+1")

        // Then
        assertEquals(
            listOf(
                "r = (80 + 1,5) / (2π) = 12,97 cm",
                "Corte da cintura = r − margem de costura = r − 1 = 11,97 cm",
                "Corte da barra = r + comprimento + margem de barra = r + 60 + 1 = 73,97 cm",
            ),
            listOf(result.formulas[2], result.formulas[4], result.formulas[5]),
        )
        assertEquals(
            "A margem de cintura de 1 cm avança para dentro; " +
                "a margem de barra de 1 cm avança para fora.",
            result.allowanceDirections,
        )
    }

    @Test
    fun measurementSummary_anyInput_keepsTwoDecimals() {
        // When/Then
        assertEquals(
            "Folga de cintura: 1,50",
            resultFor("80", "1.5", "60", "01", "+1").measurementSummary[1],
        )
    }

    @Test
    fun skirtResult_storedDraftAndRadii_readsTheSameAsTheLiveCalculation() {
        // Given: the live screen's calculation, and the project saved from it.
        val live = assertIs<SkirtCalculation.Success>(
            calculateSkirt(
                SkirtDraftField.entries.zip(listOf("80", "2", "60", "01", "+1")).toMap(),
            ),
        )
        val saved = SkirtSnapshot(live.draft, live.radii, source = null)

        // When/Then
        assertEquals(
            skirtResult(live.draft, live.radii, text),
            skirtResult(saved.draft, saved.radii, text),
        )
    }

    private fun resultFor(vararg inputs: String): SkirtResult {
        val calculation = calculateSkirt(SkirtDraftField.entries.zip(inputs).toMap())
        val success = assertIs<SkirtCalculation.Success>(calculation)
        return skirtResult(success.draft, success.radii, text)
    }
}
