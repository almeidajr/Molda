package com.almeidajr.molda

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import java.util.Locale
import kotlin.test.assertTrue

private fun contrastRatio(first: Color, second: Color): Float {
    val (darker, lighter) = listOf(first.luminance(), second.luminance()).sorted()
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun assertContrast(context: String, contrast: Float, minimum: Float) {
    // Printed (logcat tag System.out) so each run's measured pairs can be read back per scheme.
    println("MoldaContrast $context contrast=${"%.2f".format(Locale.ROOT, contrast)}")
    assertTrue(contrast >= minimum, "$context contrast=$contrast")
}

/**
 * A grouped surface's fill, sampled inside its end padding, against what shows through its
 * rounded top-start corner, so the group reads as its own item on the surface below it.
 */
internal fun SemanticsNodeInteraction.assertSurfaceStandsOut(context: String) {
    val pixels = captureToImage().toPixelMap()
    val fill = pixels[pixels.width - 4, pixels.height / 2]
    // Tonal neighbors sit near 1.1:1; equal tones, the regression, measure exactly 1.
    assertContrast(context, contrastRatio(fill, pixels[0, 0]), minimum = 1.05f)
}
