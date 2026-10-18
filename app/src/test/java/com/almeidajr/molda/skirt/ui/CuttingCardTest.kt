package com.almeidajr.molda.skirt.ui

import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.skirt.domain.SkirtCalculation
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.skirt.domain.calculateSkirtRadii
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

// Robolectric resolves the card's copy from the app's string resources.
@RunWith(RobolectricTestRunner::class)
class CuttingCardTest {
    private val text: (Int) -> String = RuntimeEnvironment.getApplication()::getString

    private fun draft(
        waist: Double,
        ease: Double,
        finishedLength: Double,
        waistSeamAllowance: Double,
        hemAllowance: Double,
    ) = SkirtDraft(
        Length(waist),
        Length(ease),
        Length(finishedLength),
        Length(waistSeamAllowance),
        Length(hemAllowance),
    )

    private fun radiiFor(
        waist: Double,
        ease: Double,
        finishedLength: Double,
        waistSeamAllowance: Double,
        hemAllowance: Double,
    ): SkirtRadii = (
        calculateSkirtRadii(
            draft(waist, ease, finishedLength, waistSeamAllowance, hemAllowance),
        ) as SkirtCalculation.Success
        ).radii

    @Test
    fun cuttingCard_referenceMeasurements_groupsNumberedStyledRadii() {
        val card = cuttingCard(radiiFor(80.0, 2.0, 60.0, 1.0, 2.0), text)

        assertEquals(listOf("Linhas de referência", "Linhas de corte"), card.map { it.title })
        assertEquals(
            listOf(
                listOf(
                    CuttingCardEntry(1, LineStyle.Solid, "Costura da cintura", "13,05 cm"),
                    CuttingCardEntry(3, LineStyle.Dotted, "Barra acabada", "73,05 cm"),
                ),
                listOf(
                    CuttingCardEntry(2, LineStyle.Dashed, "Corte da cintura", "12,05 cm"),
                    CuttingCardEntry(4, LineStyle.DashDot, "Corte da barra", "75,05 cm"),
                ),
            ),
            card.map { it.entries },
        )
    }

    @Test
    fun cuttingCard_zeroWaistAllowance_flagsOnlyWaistLinesAsCoincident() {
        val card = cuttingCard(radiiFor(80.0, 0.0, 60.0, 0.0, 2.0), text)

        assertEquals(
            mapOf(1 to 2, 2 to 1, 3 to null, 4 to null),
            card.flatMap { it.entries }.associate { it.number to it.coincidentLine },
        )
    }

    @Test
    fun cuttingCard_zeroHemAllowance_flagsOnlyHemLinesAsCoincident() {
        val card = cuttingCard(radiiFor(80.0, 0.0, 60.0, 1.0, 0.0), text)

        assertEquals(
            mapOf(1 to null, 2 to null, 3 to 4, 4 to 3),
            card.flatMap { it.entries }.associate { it.number to it.coincidentLine },
        )
    }

    @Test
    fun formatCentimetres_roundsToTwoDecimalCommaPlacesWithoutGrouping() {
        assertEquals("12,39 cm", formatCentimetres(Length(12.3859165977624)))
        assertEquals("0,50 cm", formatCentimetres(Length(0.499)))
        assertEquals("1234567,89 cm", formatCentimetres(Length(1_234_567.891)))
    }

    @Test
    fun cuttingCard_largeMeasurements_keepFullValueWithUnit() {
        val card = cuttingCard(radiiFor(10_000.0, 0.0, 5_000.0, 0.0, 0.0), text)

        assertEquals(
            listOf("1591,55 cm", "6591,55 cm", "1591,55 cm", "6591,55 cm"),
            card.flatMap { it.entries }.map { it.value },
        )
    }

    @Test
    fun diagramDescription_keysEveryLineAndStatesLimits_withoutRepeatingRadii() {
        val card = cuttingCard(radiiFor(80.0, 2.0, 60.0, 1.0, 2.0), text)

        val description = diagramDescription(card, "1", "2,5", text)

        listOf(
            "Linha 1, linha contínua: costura da cintura.",
            "Linha 2, linha tracejada: corte da cintura.",
            "Linha 3, linha pontilhada: barra acabada.",
            "Linha 4, traço e ponto: corte da barra.",
            "esquemático",
            "sem escala",
            "para impressão",
            "não garante o caimento",
            "A margem de cintura de 1 cm avança para dentro; " +
                "a margem de barra de 2,5 cm avança para fora.",
        ).forEach { assertTrue(it, it in description) }
        card.flatMap { it.entries }.forEach { assertFalse(it.value, it.value in description) }
        assertFalse("coincidem" in description)
    }

    @Test
    fun coincidenceNotes_zeroAllowances_noteEachCoincidentPairOnce() {
        val card = cuttingCard(radiiFor(80.0, 0.0, 60.0, 0.0, 0.0), text)

        val notes = listOf(
            "Margem de cintura zero: as linhas 1 e 2 coincidem.",
            "Margem de barra zero: as linhas 3 e 4 coincidem.",
        )
        assertEquals(notes, coincidenceNotes(card, text))
        notes.forEach { assertTrue(it in diagramDescription(card, "0", "0", text)) }
    }

    @Test
    fun measurementSummary_statesValuesWithoutRepeatingTheUnit_andKeepsEachValueWithItsLabel() {
        val draft = draft(80.0, 2.0, 60.5, 1.0, 0.0)

        assertEquals(
            listOf(
                "Circunferência da cintura: 80,00",
                "Folga de cintura: 2,00",
                "Comprimento acabado: 60,50",
                "Margem de costura da cintura: 1,00",
                "Margem de barra: 0,00",
            ),
            measurementSummary(draft, text),
        )
    }
}
