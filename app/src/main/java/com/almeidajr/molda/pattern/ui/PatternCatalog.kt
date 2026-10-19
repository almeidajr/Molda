package com.almeidajr.molda.pattern.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R
import com.almeidajr.molda.ui.navigation.CatalogEntry
import com.almeidajr.molda.ui.navigation.CatalogScreen
import com.almeidajr.molda.ui.navigation.motifDashEffect

/** The Moldes root: the catalog of pattern drafts, of which the full-circle skirt is the first. */
@Composable
fun PatternCatalog(onOpenSkirt: () -> Unit, onAbout: () -> Unit, modifier: Modifier = Modifier) {
    CatalogScreen(
        stringResource(R.string.navigation_patterns),
        stringResource(R.string.pattern_catalog_intro),
        onAbout,
        modifier,
    ) {
        item {
            CatalogEntry(
                stringResource(R.string.skirt_title),
                stringResource(R.string.pattern_catalog_skirt),
                onOpenSkirt,
            ) { QuarterArcMotif() }
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
                    pathEffect = motifDashEffect(dashes, unit),
                ),
            )
        }
        drawCircle(color, radius = 2 * unit, center = corner)
    }
}
