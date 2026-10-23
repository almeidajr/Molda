package com.almeidajr.molda.skirt.ui

import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.skirt.domain.SkirtCalculation
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.skirt.domain.calculateSkirtRadii
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class MarkingDiametersTest {
    @Test
    fun markingDiameters_referenceRadii_scaleTheHemCuttingToTheMarkingDiameter() {
        // Given: an 80 cm waist with 2 cm ease, 60 cm length and 1 and 2 cm allowances.
        val radii = radii(waistSeamAllowance = 1.0, hemAllowance = 2.0)

        // When
        val diameters = markingDiameters(radii)

        // Then: the worked ratios 0.173892, 0.160568 and 0.973352 of 800.
        assertEquals(139.113f, diameters.getValue("waistStitching"), absoluteTolerance = 0.001f)
        assertEquals(128.454f, diameters.getValue("waistCutting"), absoluteTolerance = 0.001f)
        assertEquals(778.681f, diameters.getValue("finishedHem"), absoluteTolerance = 0.001f)
        assertEquals(800f, diameters.getValue("hemCutting"))
    }

    @Test
    fun markingDiameters_zeroAllowances_makeCuttingAndStitchingCirclesCoincide() {
        // When
        val diameters = markingDiameters(radii(waistSeamAllowance = 0.0, hemAllowance = 0.0))

        // Then
        assertEquals(142.922f, diameters.getValue("waistStitching"), absoluteTolerance = 0.001f)
        assertEquals(diameters.getValue("waistStitching"), diameters.getValue("waistCutting"))
        assertEquals(800f, diameters.getValue("finishedHem"))
        assertEquals(800f, diameters.getValue("hemCutting"))
    }

    @Test
    fun markingDiameters_veryLargeRadii_stayFinite() {
        // Given: radii whose product with the marking diameter, or Float value, would overflow.
        val radii = SkirtRadii(
            waistStitching = Length(4e307),
            finishedHem = Length(1.6e308),
            waistCutting = Length(2e307),
            hemCutting = Length(Double.MAX_VALUE),
        )

        // When
        val diameters = markingDiameters(radii)

        // Then
        assertTrue(diameters.values.all { it.isFinite() }, "$diameters")
        assertEquals(800f, diameters.getValue("hemCutting"))
    }

    private fun radii(waistSeamAllowance: Double, hemAllowance: Double): SkirtRadii {
        val draft = SkirtDraft(
            waist = Length(80.0),
            ease = Length(2.0),
            finishedLength = Length(60.0),
            waistSeamAllowance = Length(waistSeamAllowance),
            hemAllowance = Length(hemAllowance),
        )
        return (calculateSkirtRadii(draft) as SkirtCalculation.Success).radii
    }
}
