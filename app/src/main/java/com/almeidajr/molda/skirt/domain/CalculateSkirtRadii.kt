package com.almeidajr.molda.skirt.domain

import kotlin.math.PI

data class SkirtMeasurements(
    val waist: Double,
    val ease: Double,
    val length: Double,
    val waistSeamAllowance: Double,
    val hemAllowance: Double,
)

data class SkirtRadii(
    val waistStitching: Double,
    val finishedHem: Double,
    val waistCutting: Double,
    val hemCutting: Double,
)

sealed interface SkirtCalculation {
    data class Success(val radii: SkirtRadii) : SkirtCalculation

    data object InvalidMeasurements : SkirtCalculation

    data object NonpositiveWaistCutting : SkirtCalculation

    data object NonfiniteResult : SkirtCalculation
}

/** Calculates radii in centimetres without rounding intermediate values. */
fun calculateSkirtRadii(measurements: SkirtMeasurements): SkirtCalculation {
    val values =
        with(measurements) { listOf(waist, ease, length, waistSeamAllowance, hemAllowance) }
    val invalidPositiveMeasurements = measurements.waist <= 0.0 || measurements.length <= 0.0
    if (values.any { !it.isFinite() || it < 0.0 } || invalidPositiveMeasurements) {
        return SkirtCalculation.InvalidMeasurements
    }
    val waistStitching = (measurements.waist + measurements.ease) / (2 * PI)
    val finishedHem = waistStitching + measurements.length
    val radii = SkirtRadii(
        waistStitching = waistStitching,
        finishedHem = finishedHem,
        waistCutting = waistStitching - measurements.waistSeamAllowance,
        hemCutting = finishedHem + measurements.hemAllowance,
    )
    return when {
        listOf(radii.waistStitching, radii.finishedHem, radii.waistCutting, radii.hemCutting)
            .any { !it.isFinite() } -> SkirtCalculation.NonfiniteResult

        radii.waistCutting <= 0.0 -> SkirtCalculation.NonpositiveWaistCutting

        else -> SkirtCalculation.Success(radii)
    }
}
