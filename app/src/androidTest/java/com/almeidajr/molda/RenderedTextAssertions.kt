package com.almeidajr.molda

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import org.junit.Assert.assertTrue

internal fun SemanticsNodeInteraction.assertReadableTextContrast(context: String) {
    val layouts = mutableListOf<TextLayoutResult>()
    performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
    val foreground = layouts.single().layoutInput.style.color.luminance()
    val background = captureToImage().toPixelMap()[0, 0].luminance()
    val contrast = (maxOf(foreground, background) + 0.05f) /
        (minOf(foreground, background) + 0.05f)
    assertTrue("$context contrast=$contrast", contrast >= 4.5f)
}
