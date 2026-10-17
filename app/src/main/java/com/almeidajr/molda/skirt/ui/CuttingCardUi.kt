package com.almeidajr.molda.skirt.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun CuttingCard(groups: List<CuttingCardGroup>, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(
                "Raios para o molde",
                style = MaterialTheme.typography.titleLarge,
                // The heading appears once per successful calculation, announcing it once.
                modifier = Modifier.semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                },
            )
            groups.forEach { group ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        group.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    group.entries.forEach { CuttingCardRadius(it) }
                }
            }
        }
    }
}

@Composable
private fun CuttingCardRadius(entry: CuttingCardEntry) {
    val ink = MaterialTheme.colorScheme.onSecondaryContainer
    Column(
        // One merged announcement replaces the separate number, sample, label and value.
        Modifier.semantics(mergeDescendants = true) {
            contentDescription =
                "Linha ${entry.number}, ${entry.lineStyle.description}. " +
                "${entry.label}: ${entry.value}"
        },
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

/** A line's diagram number, stroke sample and label, shared by the card and the diagram key. */
@Composable
internal fun LineKey(entry: CuttingCardEntry, ink: Color, modifier: Modifier = Modifier) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(entry.number.toString(), style = MaterialTheme.typography.labelLarge, color = ink)
        Canvas(Modifier.size(width = 40.dp, height = 16.dp)) {
            drawLine(
                color = ink,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2.dp.toPx(),
                pathEffect = linePathEffect(entry.lineStyle),
            )
        }
        Text(
            entry.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
