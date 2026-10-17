package com.almeidajr.molda

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.toSize
import java.util.Locale
import org.junit.Assert.assertTrue

/** Text colour against the rendered pixel behind the text's top-left corner, at least 4.5:1. */
internal fun SemanticsNodeInteraction.assertReadableTextContrast(context: String) {
    val layouts = mutableListOf<TextLayoutResult>()
    performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(layouts)) }
    val foreground = layouts.single().layoutInput.style.color
    val background = captureToImage().toPixelMap()[0, 0]
    assertContrast(context, contrastRatio(foreground, background), minimum = 4.5f)
}

/**
 * The strongest rendered ink in a graphic against its corner background, at least 3:1, so
 * meaningful strokes such as the diagram's lines stay distinguishable from their surface.
 */
internal fun SemanticsNodeInteraction.assertGraphicContrast(context: String) {
    val pixels = captureToImage().toPixelMap()
    val background = pixels[0, 0]
    val strongest = (0 until pixels.width).maxOf { x ->
        (0 until pixels.height).maxOf { y -> contrastRatio(pixels[x, y], background) }
    }
    assertContrast(context, strongest, minimum = 3f)
}

private fun contrastRatio(first: Color, second: Color): Float {
    val (darker, lighter) = listOf(first.luminance(), second.luminance()).sorted()
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun assertContrast(context: String, contrast: Float, minimum: Float) {
    // Printed (logcat tag System.out) so each run's measured pairs can be read back per scheme.
    println("MoldaContrast $context contrast=${"%.2f".format(Locale.ROOT, contrast)}")
    assertTrue("$context contrast=$contrast", contrast >= minimum)
}

/** The node's laid-out bounds in root pixels, whether or not it is scrolled into view. */
internal fun SemanticsNodeInteraction.layoutBounds(): Rect =
    fetchSemanticsNode().layoutInfo.coordinates.let {
        Rect(it.positionInRoot(), it.size.toSize())
    }

/** Returns the node's bounds in root pixels after checking no ancestor clips any of it. */
internal fun SemanticsNodeInteraction.fullyVisibleBounds(context: String): Rect {
    val unclipped = layoutBounds()
    val visible = fetchSemanticsNode(context).boundsInRoot
    assertTrue("$context has no size", unclipped.width > 0f && unclipped.height > 0f)
    assertTrue(
        "$context is clipped: $visible of $unclipped",
        visible.width >= unclipped.width - 1f && visible.height >= unclipped.height - 1f,
    )
    return unclipped
}
