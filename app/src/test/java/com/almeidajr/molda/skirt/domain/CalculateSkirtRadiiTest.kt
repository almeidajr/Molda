package com.almeidajr.molda.skirt.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateSkirtRadiiTest {
    @Test
    fun calculate_referenceMeasurements_returnsFourUnroundedRadii() {
        val result = calculateSkirtRadii(SkirtMeasurements(80.0, 2.0, 60.0, 1.0, 2.0))
        val radii = (result as SkirtCalculation.Success).radii
        assertEquals(13.0507053335354, radii.waistStitching, 1e-12)
        assertEquals(73.0507053335354, radii.finishedHem, 1e-12)
        assertEquals(12.0507053335354, radii.waistCutting, 1e-12)
        assertEquals(75.0507053335354, radii.hemCutting, 1e-12)
    }

    @Test
    fun calculate_zeroAllowances_preservesStitchingAndFinishedLines() {
        val radii = (
            calculateSkirtRadii(
                SkirtMeasurements(100.0, 0.0, 50.0, 0.0, 0.0),
            ) as SkirtCalculation.Success
            ).radii
        assertEquals(15.9154943091895, radii.waistStitching, 1e-12)
        assertEquals(65.9154943091895, radii.finishedHem, 1e-12)
        assertEquals(radii.waistStitching, radii.waistCutting, 0.0)
        assertEquals(radii.finishedHem, radii.hemCutting, 0.0)
    }

    @Test
    fun calculate_fractionalMeasurements_keepsIntermediatePrecision() {
        val radii = (
            calculateSkirtRadii(
                SkirtMeasurements(80.5, 1.25, 60.75, 0.625, 2.5),
            ) as SkirtCalculation.Success
            ).radii
        assertEquals(13.0109165977624, radii.waistStitching, 1e-12)
        assertEquals(73.7609165977624, radii.finishedHem, 1e-12)
        assertEquals(12.3859165977624, radii.waistCutting, 1e-12)
        assertEquals(76.2609165977624, radii.hemCutting, 1e-12)
    }

    @Test
    fun calculate_invalidSignsAndNonfiniteMeasurements_rejectsEveryField() {
        val valid = SkirtMeasurements(80.0, 2.0, 60.0, 1.0, 2.0)
        listOf(
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            -1.0,
        ).forEach { invalid ->
            listOf(
                valid.copy(waist = invalid),
                valid.copy(ease = invalid),
                valid.copy(length = invalid),
                valid.copy(waistSeamAllowance = invalid),
                valid.copy(hemAllowance = invalid),
            ).forEach {
                assertEquals(SkirtCalculation.InvalidMeasurements, calculateSkirtRadii(it))
            }
        }
        assertEquals(
            SkirtCalculation.InvalidMeasurements,
            calculateSkirtRadii(valid.copy(waist = 0.0)),
        )
        assertEquals(
            SkirtCalculation.InvalidMeasurements,
            calculateSkirtRadii(valid.copy(length = 0.0)),
        )
    }

    @Test
    fun calculate_nonpositiveCuttingRadius_rejectsBoundaryAndExcessAllowance() {
        val valid = SkirtMeasurements(80.0, 2.0, 60.0, 1.0, 2.0)
        val radius = (calculateSkirtRadii(valid) as SkirtCalculation.Success).radii.waistStitching
        listOf(radius, radius + 1.0).forEach {
            assertEquals(
                SkirtCalculation.NonpositiveWaistCutting,
                calculateSkirtRadii(valid.copy(waistSeamAllowance = it)),
            )
        }
    }

    @Test
    fun calculate_overflow_rejectsNonfiniteResults() {
        assertEquals(
            SkirtCalculation.NonfiniteResult,
            calculateSkirtRadii(
                SkirtMeasurements(Double.MAX_VALUE, Double.MAX_VALUE, 60.0, 0.0, 0.0),
            ),
        )
        assertEquals(
            SkirtCalculation.NonfiniteResult,
            calculateSkirtRadii(
                SkirtMeasurements(80.0, 0.0, Double.MAX_VALUE, 0.0, Double.MAX_VALUE),
            ),
        )
    }
}
