package com.almeidajr.molda.skirt.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R
import kotlin.math.cos
import kotlin.math.sin

/** The [result]'s transfer explanation and formulas, each behind its own disclosure. */
@Composable
internal fun DraftingExplanation(result: SkirtResult) {
    // The pair reads as one set of toggles: 8dp between 48dp targets, not the page's 16dp.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Disclosure(stringResource(R.string.skirt_explanation_title)) {
            Text(stringResource(R.string.skirt_explanation_terms))
            Text(stringResource(R.string.skirt_explanation_transfer))
            Text(result.allowanceDirections)
        }
        Disclosure(stringResource(R.string.skirt_formulas_title)) {
            result.formulas.forEach { Text(it) }
        }
    }
}

/** The diagram, coincidences and limits as one figure that the text equivalent announces once. */
@Composable
internal fun DraftingFigure(
    card: List<CuttingCardGroup>,
    description: String,
    coincidenceNotes: List<String>,
) {
    Column(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SkirtDiagram(card.flatMap { it.entries }.sortedBy { it.line.number })
        coincidenceNotes.forEach { Text(it) }
        Text(
            stringResource(R.string.skirt_diagram_caption),
            style = MaterialTheme.typography.bodyMedium,
            // Drawn on the cutting card: its ink, quieter through the smaller type.
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

/** A labeled show/hide toggle that starts collapsed and exposes its state. */
@Composable
internal fun Disclosure(label: String, content: @Composable () -> Unit) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val state = stringResource(
        if (isExpanded) R.string.skirt_disclosure_expanded else R.string.skirt_disclosure_collapsed,
    )
    TextButton(
        onClick = { isExpanded = !isExpanded },
        modifier = Modifier.heightIn(min = 48.dp).semantics { stateDescription = state },
    ) {
        // Without the weight a wrapping label squeezes the chevron at large text sizes.
        Text(label, Modifier.weight(1f, fill = false))
        Icon(
            painterResource(R.drawable.ic_expand_more),
            contentDescription = null,
            // Flipped vertically, the chevron points up as "expand less".
            modifier = Modifier.graphicsLayer { scaleY = if (isExpanded) -1f else 1f },
        )
    }
    if (isExpanded) content()
}

@Composable
private fun SkirtDiagram(entries: List<CuttingCardEntry>) {
    // Drawn on the cutting card, so it takes the card's own ink and paper roles.
    val ink = MaterialTheme.colorScheme.onSecondaryContainer
    val paper = MaterialTheme.colorScheme.secondaryContainer
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge.copy(color = ink)
    // Fills the width up to a cap so the drawing stays a sketch, not a full-screen poster.
    Canvas(Modifier.widthIn(max = 400.dp).fillMaxWidth().aspectRatio(1f)) {
        val center = Offset(16.dp.toPx(), 16.dp.toPx())
        val extent = minOf(size.width - center.x, size.height - center.y) - 8.dp.toPx()
        entries.forEach { entry ->
            val radius = extent * diagramFraction(entry)
            drawArc(
                color = ink,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(2.dp.toPx(), pathEffect = linePathEffect(entry.line.style)),
            )
        }
        drawCircle(ink, 3.dp.toPx(), center)
        entries.forEach { entry ->
            // Waist and hem lines sit at opposite ends of the arc so 1 and 2 never collide.
            val angle = if (entry.line.number % 2 == 1) 0.35f else 1.2f
            val radius = extent * diagramFraction(entry)
            val label = textMeasurer.measure(entry.line.number.toString(), labelStyle)
            val labelCenter = center + Offset(radius * cos(angle), radius * sin(angle))
            // A paper-colored disc keeps the number legible where it crosses its line.
            drawCircle(
                paper,
                maxOf(label.size.width, label.size.height) / 2f + 2.dp.toPx(),
                labelCenter,
            )
            drawText(
                label,
                topLeft = labelCenter - Offset(label.size.width / 2f, label.size.height / 2f),
            )
        }
    }
}

/** Schematic radius as a fraction of the drawing, so even small allowances stay visible. */
private fun diagramFraction(entry: CuttingCardEntry): Float = DiagramFractions.getValue(
    minOf(
        entry.line.number,
        entry.coincidentLine?.number ?: entry.line.number,
    ),
)

// Waist arcs sit wider than to scale so lines 1 and 2 and their numbers stay legible.
private val DiagramFractions = mapOf(1 to 0.42f, 2 to 0.3f, 3 to 0.84f, 4 to 0.96f)

/** The stroke pattern for [style], shared by the diagram and the cutting card. */
internal fun DrawScope.linePathEffect(style: LineStyle): PathEffect? = when (style) {
    LineStyle.Solid -> null

    LineStyle.Dashed -> PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 6.dp.toPx()))

    LineStyle.Dotted -> PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx()))

    LineStyle.DashDot -> PathEffect.dashPathEffect(
        floatArrayOf(12.dp.toPx(), 5.dp.toPx(), 2.dp.toPx(), 5.dp.toPx()),
    )
}
