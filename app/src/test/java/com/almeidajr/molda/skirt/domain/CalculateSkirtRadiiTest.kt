package com.almeidajr.molda.skirt.domain

import com.almeidajr.molda.measurement.Length
import kotlin.test.assertEquals
import org.junit.Test

class CalculateSkirtRadiiTest {
    @Test
    fun calculate_referenceDraft_returnsFourUnroundedRadii() {
        val result = calculateSkirtRadii(draft(80.0, 2.0, 60.0, 1.0, 2.0))
        val radii = (result as SkirtCalculation.Success).radii
        assertEquals(13.0507053335354, radii.waistStitching.centimetres, absoluteTolerance = 1e-12)
        assertEquals(73.0507053335354, radii.finishedHem.centimetres, absoluteTolerance = 1e-12)
        assertEquals(12.0507053335354, radii.waistCutting.centimetres, absoluteTolerance = 1e-12)
        assertEquals(75.0507053335354, radii.hemCutting.centimetres, absoluteTolerance = 1e-12)
    }

    @Test
    fun calculate_zeroAllowances_preservesStitchingAndFinishedLines() {
        val radii = successRadii(draft(100.0, 0.0, 50.0, 0.0, 0.0))
        assertEquals(15.9154943091895, radii.waistStitching.centimetres, absoluteTolerance = 1e-12)
        assertEquals(65.9154943091895, radii.finishedHem.centimetres, absoluteTolerance = 1e-12)
        assertEquals(radii.waistStitching, radii.waistCutting)
        assertEquals(radii.finishedHem, radii.hemCutting)
    }

    @Test
    fun calculate_fractionalDraft_keepsIntermediatePrecision() {
        val radii = successRadii(draft(80.5, 1.25, 60.75, 0.625, 2.5))
        assertEquals(13.0109165977624, radii.waistStitching.centimetres, absoluteTolerance = 1e-12)
        assertEquals(73.7609165977624, radii.finishedHem.centimetres, absoluteTolerance = 1e-12)
        assertEquals(12.3859165977624, radii.waistCutting.centimetres, absoluteTolerance = 1e-12)
        assertEquals(76.2609165977624, radii.hemCutting.centimetres, absoluteTolerance = 1e-12)
    }

    @Test
    fun calculate_seamAllowanceNotSmallerThanWaistStitching_rejectsBoundaryAndExcess() {
        val valid = draft(80.0, 2.0, 60.0, 1.0, 2.0)
        val radius = successRadii(valid).waistStitching.centimetres
        listOf(radius, radius + 1.0).forEach {
            assertEquals(
                SkirtCalculation.SeamAllowanceNotSmallerThanWaistStitching,
                calculateSkirtRadii(valid.copy(waistSeamAllowance = Length(it))),
            )
        }
    }

    @Test
    fun calculate_overflow_rejectsNonfiniteResults() {
        assertEquals(
            SkirtCalculation.NonfiniteResult,
            calculateSkirtRadii(draft(Double.MAX_VALUE, Double.MAX_VALUE, 60.0, 0.0, 0.0)),
        )
        assertEquals(
            SkirtCalculation.NonfiniteResult,
            calculateSkirtRadii(draft(80.0, 0.0, Double.MAX_VALUE, 0.0, Double.MAX_VALUE)),
        )
    }

    private fun successRadii(draft: SkirtDraft): SkirtRadii =
        (calculateSkirtRadii(draft) as SkirtCalculation.Success).radii

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
}
