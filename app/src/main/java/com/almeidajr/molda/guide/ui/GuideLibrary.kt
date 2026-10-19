package com.almeidajr.molda.guide.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R
import com.almeidajr.molda.ui.navigation.CatalogEntry
import com.almeidajr.molda.ui.navigation.MoldaTopAppBar

/**
 * The Guias root: the library of guides, of which fabric folding is the first. When the library
 * is a list pane beside the guides, [isFabricFoldingOpen] is non-null and selects its entry.
 */
@Composable
fun GuideLibrary(
    isFabricFoldingOpen: Boolean?,
    onOpenFabricFolding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        MoldaTopAppBar(stringResource(R.string.navigation_guias))
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(320.dp),
                modifier = Modifier.widthIn(max = 1040.dp).fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(R.string.guide_library_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    CatalogEntry(
                        stringResource(R.string.guide_open),
                        stringResource(R.string.guide_fabric_folding_description),
                        onOpenFabricFolding,
                        selected = isFabricFoldingOpen,
                    ) { FoldedSquareMotif() }
                }
            }
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
                    pathEffect = dashes?.let {
                        PathEffect.dashPathEffect(it.map { dash -> dash * unit }.toFloatArray())
                    },
                ),
            )
        }
    }
}
