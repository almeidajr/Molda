package com.almeidajr.molda

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.toSize
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import kotlin.test.assertTrue

/** An 80 cm waist with 2 cm ease, 60 cm finished length and 1 and 2 cm allowances. */
val ReferenceInputs = listOf("80", "2", "60", "1", "2")

/**
 * The cutting card's radii for [ReferenceInputs], as each row announces itself; a no-break space
 * keeps each value with its unit.
 */
val ReferenceRadii = listOf(
    "Linha 1, linha contínua. Costura da cintura: 13,05 cm",
    "Linha 3, linha pontilhada. Barra acabada: 73,05 cm",
    "Linha 2, linha tracejada. Corte da cintura: 12,05 cm",
    "Linha 4, traço e ponto. Corte da barra: 75,05 cm",
)

/** Types [values] into the skirt calculator's fields, in form order. */
fun SemanticsNodeInteractionsProvider.fillSkirtDraft(values: List<String> = ReferenceInputs) {
    SkirtDraftField.entries.zip(values).forEach { (field, value) ->
        onNodeWithText(field.labelText).performScrollTo().performTextReplacement(value)
    }
}

/** The guide drawing frame's loading indicator. */
val isLoadingIndicator = hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)

/** The node's laid-out bounds in root pixels, whether it is scrolled into view. */
fun SemanticsNodeInteraction.layoutBounds(): Rect =
    fetchSemanticsNode().layoutInfo.coordinates.let {
        Rect(it.positionInRoot(), it.size.toSize())
    }

/** Returns the node's bounds in root pixels after checking no ancestor clips any of it. */
fun SemanticsNodeInteraction.fullyVisibleBounds(context: String): Rect {
    val unclipped = layoutBounds()
    val visible = fetchSemanticsNode(context).boundsInRoot
    assertTrue(unclipped.width > 0f && unclipped.height > 0f, "$context has no size")
    assertTrue(
        visible.width >= unclipped.width - 1f && visible.height >= unclipped.height - 1f,
        "$context is clipped: $visible of $unclipped",
    )
    return unclipped
}

/** Checks that every line of the node's text fits the size the text was laid out in. */
fun SemanticsNodeInteraction.assertTextFits(context: String): SemanticsNodeInteraction {
    val layouts = mutableListOf<TextLayoutResult>()
    performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(layouts)) }
    val layout = layouts.single()
    // Text bounds can differ from rounded pixel size by a fraction of a pixel.
    assertTrue(
        // Line width, not line right: centered text is laid out wider than it measures.
        (0 until layout.lineCount).all {
            layout.getLineRight(it) - layout.getLineLeft(it) <= layout.size.width + 1f &&
                layout.getLineBottom(it) <= layout.size.height + 1f
        },
        "$context text overflows",
    )
    return this
}
