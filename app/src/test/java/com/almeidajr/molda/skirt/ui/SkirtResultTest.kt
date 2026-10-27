package com.almeidajr.molda.skirt.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.skirt.domain.SkirtCalculation
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.skirt.domain.calculateSkirt
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment

// Robolectric resolves the result's copy from the app's string resources.
@RunWith(AndroidJUnit4::class)
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
        // When
        val result = resultFor("80", "2", "60", "1", "2")

        // Then
        assertEquals(
            "A margem de cintura de 1 cm avança para dentro; " +
                "a margem de barra de 2 cm avança para fora.",
            result.allowanceDirections,
        )
        assertTrue(result.allowanceDirections in result.diagramDescription)
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

    private fun resultFor(vararg inputs: String): SkirtResult {
        val calculation = calculateSkirt(SkirtDraftField.entries.zip(inputs).toMap())
        val success = assertIs<SkirtCalculation.Success>(calculation)
        return skirtResult(success.draft, success.radii, text)
    }
}
