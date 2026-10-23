package com.almeidajr.molda.skirt.ui

import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.skirt.domain.SkirtRadii

/** The diameter the hem cutting circle fills in the drawing's marking artboards. */
private const val MARKING_DIAMETER = 800.0

/**
 * The drawing's four circle diameters by view-model name: the hem cutting circle fills
 * [MARKING_DIAMETER] and the other three keep their proportion to it.
 */
internal fun markingDiameters(radii: SkirtRadii): Map<String, Float> {
    // Normalize before converting to Float; large valid centimetre values stay finite.
    fun diameter(radius: Length) =
        (MARKING_DIAMETER * (radius.centimetres / radii.hemCutting.centimetres)).toFloat()
    return mapOf(
        "waistStitching" to diameter(radii.waistStitching),
        "waistCutting" to diameter(radii.waistCutting),
        "finishedHem" to diameter(radii.finishedHem),
        "hemCutting" to diameter(radii.hemCutting),
    )
}
