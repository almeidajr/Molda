package com.almeidajr.molda.pattern.ui

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R
import com.almeidajr.molda.ui.navigation.CatalogEntry
import com.almeidajr.molda.ui.navigation.MoldaTopAppBar

/** The Moldes root: the catalog of pattern drafts, of which the full-circle skirt is the first. */
@Composable
fun PatternCatalog(onOpenSkirt: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        MoldaTopAppBar(stringResource(R.string.navigation_moldes))
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
                        stringResource(R.string.pattern_catalog_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    CatalogEntry(
                        stringResource(R.string.skirt_title),
                        stringResource(R.string.pattern_catalog_skirt),
                        onOpenSkirt,
                    ) { QuarterArcMotif() }
                }
            }
        }
    }
}

/** The motif's arcs in 48-unit space: radius and dash pattern, solid when null. */
private val MotifArcs = listOf(
    12f to floatArrayOf(4f, 2f),
    14f to null,
    34f to floatArrayOf(1f, 2.5f),
    38f to floatArrayOf(5f, 2f, 1f, 2f),
)

/** The drafting diagram's motif: four quarter arcs around the pattern's corner, decorative. */
@Composable
private fun QuarterArcMotif() {
    val color = LocalContentColor.current
    Canvas(Modifier.size(48.dp)) {
        val unit = size.width / 48f
        val corner = Offset(6 * unit, 6 * unit)
        MotifArcs.forEach { (radius, dashes) ->
            val scaled = radius * unit
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = corner - Offset(scaled, scaled),
                size = Size(scaled * 2, scaled * 2),
                style = Stroke(
                    width = 2 * unit,
                    pathEffect = dashes?.let {
                        PathEffect.dashPathEffect(it.map { dash -> dash * unit }.toFloatArray())
                    },
                ),
            )
        }
        drawCircle(color, radius = 2 * unit, center = corner)
    }
}
