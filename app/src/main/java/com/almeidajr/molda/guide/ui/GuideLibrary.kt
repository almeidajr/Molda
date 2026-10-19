package com.almeidajr.molda.guide.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R
import com.almeidajr.molda.ui.navigation.CatalogEntry
import com.almeidajr.molda.ui.navigation.CatalogScreen
import com.almeidajr.molda.ui.navigation.CatalogSelection
import com.almeidajr.molda.ui.navigation.motifDashEffect

/**
 * The Guias root: the library of guides, of which fabric folding is the first. Beside the guides,
 * [fabricFoldingSelection] marks whether its entry is the one shown.
 */
@Composable
fun GuideLibrary(
    fabricFoldingSelection: CatalogSelection,
    onOpenFabricFolding: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CatalogScreen(
        stringResource(R.string.navigation_guides),
        stringResource(R.string.guide_library_intro),
        onAbout,
        modifier,
    ) {
        item {
            CatalogEntry(
                stringResource(R.string.guide_open),
                stringResource(R.string.guide_fabric_folding_description),
                onOpenFabricFolding,
                selection = fabricFoldingSelection,
            ) { FoldedSquareMotif() }
        }
    }
}

/** The motif's strokes in 48-unit space: points joined in order and a dash pattern,
 * solid when null. */
private val MotifStrokes = listOf(
    // The square, missing the corner folded over.
    listOf(22f to 8f, 40f to 8f, 40f to 40f, 8f to 40f, 8f to 22f) to null,
    // The fold line.
    listOf(8f to 22f, 22f to 8f) to floatArrayOf(3f, 2f),
    // The folded corner, lying on the fabric.
    listOf(8f to 22f, 22f to 22f, 22f to 8f) to null,
)

/** A square of fabric with its top-left corner folded over along a dashed line, decorative. */
@Composable
private fun FoldedSquareMotif() {
    val color = LocalContentColor.current
    Canvas(Modifier.size(48.dp)) {
        val unit = size.width / 48f
        MotifStrokes.forEach { (points, dashes) ->
            val path = Path().apply {
                points.forEachIndexed { index, (x, y) ->
                    if (index == 0) moveTo(x * unit, y * unit) else lineTo(x * unit, y * unit)
                }
            }
            drawPath(
                path,
                color,
                style = Stroke(
                    width = 2 * unit,
                    join = StrokeJoin.Round,
                    pathEffect = motifDashEffect(dashes, unit),
                ),
            )
        }
    }
}
