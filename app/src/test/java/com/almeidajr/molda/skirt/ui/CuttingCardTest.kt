package com.almeidajr.molda.skirt.ui

import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.skirt.domain.SkirtCalculation
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.skirt.domain.calculateSkirtRadii
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

// Robolectric resolves the card's copy from the app's string resources.
@RunWith(RobolectricTestRunner::class)
class CuttingCardTest {
    private val text: (Int) -> String = RuntimeEnvironment.getApplication()::getString

    @Test
    fun cuttingCard_referenceRadii_groupsNumberedStyledRadii() {
        // When
        val card = cuttingCard(radiiFor(2.0, 1.0, 2.0), text)

        // Then
        assertEquals(listOf("Linhas de referência", "Linhas de corte"), card.map { it.title })
        assertEquals(
            listOf(
                listOf(
                    CuttingCardEntry(DraftingLine.WaistStitching, "Costura da cintura", "13,05 cm"),
                    CuttingCardEntry(DraftingLine.FinishedHem, "Barra acabada", "73,05 cm"),
                ),
                listOf(
                    CuttingCardEntry(DraftingLine.WaistCutting, "Corte da cintura", "12,05 cm"),
                    CuttingCardEntry(DraftingLine.HemCutting, "Corte da barra", "75,05 cm"),
                ),
            ),
            card.map { it.entries },
        )
    }

    @Test
    fun cuttingCard_zeroWaistAllowance_flagsOnlyWaistLinesAsCoincident() {
        // When
        val card = cuttingCard(radiiFor(0.0, 0.0, 2.0), text)

        // Then
        assertEquals(
            mapOf(1 to 2, 2 to 1, 3 to null, 4 to null),
            card.flatMap { it.entries }.associate { it.line.number to it.coincidentLine?.number },
        )
    }

    @Test
    fun cuttingCard_zeroHemAllowance_flagsOnlyHemLinesAsCoincident() {
        // When
        val card = cuttingCard(radiiFor(0.0, 1.0, 0.0), text)

        // Then
        assertEquals(
            mapOf(1 to null, 2 to null, 3 to 4, 4 to 3),
            card.flatMap { it.entries }.associate { it.line.number to it.coincidentLine?.number },
        )
    }

    @Test
    fun formatCentimetres_anyLength_roundsToTwoCommaDecimalsWithoutGrouping() {
        // When/Then
        assertEquals("12,39 cm", formatCentimetres(Length(12.3859165977624)))
        assertEquals("0,50 cm", formatCentimetres(Length(0.499)))
        assertEquals("1234567,89 cm", formatCentimetres(Length(1_234_567.891)))
    }

    @Test
    fun diagramDescription_referenceCard_keysEveryLineAndStatesLimitsWithoutRadii() {
        // Given
        val card = cuttingCard(radiiFor(2.0, 1.0, 2.0), text)

        // When
        val description = diagramDescription(card, "Margens dadas.", emptyList(), text)

        // Then
        listOf(
            "Linha 1, linha contínua: costura da cintura.",
            "Linha 2, linha tracejada: corte da cintura.",
            "Linha 3, linha pontilhada: barra acabada.",
            "Linha 4, traço e ponto: corte da barra.",
            "esquemático",
            "sem escala",
            "para impressão",
            "não garante o caimento",
            "Margens dadas.",
        ).forEach { assertTrue(it in description, it) }
        card.flatMap { it.entries }.forEach { assertFalse(it.value in description, it.value) }
        assertFalse("coincidem" in description)
    }

    @Test
    fun coincidenceNotes_zeroAllowances_noteEachCoincidentPairOnce() {
        // Given
        val card = cuttingCard(radiiFor(0.0, 0.0, 0.0), text)

        // When/Then
        assertEquals(
            listOf(
                "Margem de cintura zero: as linhas 1 e 2 coincidem.",
                "Margem de barra zero: as linhas 3 e 4 coincidem.",
            ),
            coincidenceNotes(card, text),
        )
    }

    @Test
    fun diagramDescription_zeroAllowances_includesCoincidenceNotes() {
        // Given
        val card = cuttingCard(radiiFor(0.0, 0.0, 0.0), text)

        // When
        val description = diagramDescription(card, "Margens.", coincidenceNotes(card, text), text)

        // Then
        coincidenceNotes(card, text).forEach { assertTrue(it in description, it) }
    }

    @Test
    fun measurementSummary_draft_labelsEachValueWithoutRepeatingTheUnit() {
        // Given
        val draft = draft(80.0, 2.0, 60.5, 1.0, 0.0)

        // When/Then
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

    /** The radii of an 80 cm waist and 60 cm finished length with the given allowances. */
    private fun radiiFor(
        ease: Double,
        waistSeamAllowance: Double,
        hemAllowance: Double,
    ): SkirtRadii = (
        calculateSkirtRadii(draft(80.0, ease, 60.0, waistSeamAllowance, hemAllowance))
            as SkirtCalculation.Success
        ).radii
}
