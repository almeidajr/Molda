package com.almeidajr.molda.skirt.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R

/**
 * The answer as one surface: the four radii, then the drafting diagram they key into, so each
 * number and its line are read together. The diagram moves beside the radii when both fit.
 */
@Composable
internal fun CuttingCard(
    groups: List<CuttingCardGroup>,
    diagramDescription: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        BoxWithConstraints(Modifier.padding(24.dp)) {
            val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
            val isSideBySide = maxWidth >= CardColumnMinWidth * fontScale * 2 + 24.dp
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(
                    stringResource(R.string.skirt_card_title),
                    style = MaterialTheme.typography.titleLarge,
                    // The heading appears once per successful calculation, announcing it once.
                    modifier = Modifier.semantics {
                        heading()
                        liveRegion = LiveRegionMode.Polite
                    },
                )
                if (isSideBySide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        // Traversal groups keep TalkBack on all four radii before the diagram
                        // instead of zigzagging between the two columns row by row.
                        Column(
                            Modifier.weight(1f).semantics { isTraversalGroup = true },
                            Arrangement.spacedBy(24.dp),
                        ) {
                            groups.forEach { RadiusGroup(it) }
                        }
                        DiagramSection(
                            groups,
                            diagramDescription,
                            Modifier.weight(1f).semantics { isTraversalGroup = true },
                        )
                    }
                } else {
                    groups.forEach { RadiusGroup(it) }
                    DiagramSection(groups, diagramDescription)
                }
            }
        }
    }
}

/** Narrowest column that keeps a radius row or the diagram readable beside the other. */
private val CardColumnMinWidth = 260.dp

@Composable
private fun RadiusGroup(group: CuttingCardGroup) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            group.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        group.entries.forEach { CuttingCardRadius(it) }
    }
}

/** The card's radius rows are the diagram's key, so the drawing carries numbers only. */
@Composable
private fun DiagramSection(
    groups: List<CuttingCardGroup>,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            stringResource(R.string.skirt_diagram_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        DraftingFigure(groups, description)
    }
}

@Composable
private fun CuttingCardRadius(entry: CuttingCardEntry) {
    val ink = MaterialTheme.colorScheme.onSecondaryContainer
    val description = stringResource(
        R.string.skirt_card_radius_description,
        entry.line.number,
        stringResource(entry.line.style.description),
        entry.label,
        entry.value,
    )
    Column(
        // One merged announcement replaces the separate number, sample, label and value.
        Modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LineKey(entry, ink)
        Text(
            entry.value,
            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
            color = ink,
        )
    }
}

/** A line's diagram number, stroke sample and label: the key between a radius and its arc. */
@Composable
private fun LineKey(entry: CuttingCardEntry, ink: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(entry.line.number.toString(), style = MaterialTheme.typography.labelLarge, color = ink)
        Canvas(Modifier.size(width = 40.dp, height = 16.dp)) {
            drawLine(
                color = ink,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2.dp.toPx(),
                pathEffect = linePathEffect(entry.line.style),
            )
        }
        // Card ink, not onSurfaceVariant: the label is quieter through type, not off-role colour.
        Text(entry.label, style = MaterialTheme.typography.bodyLarge, color = ink)
    }
}
