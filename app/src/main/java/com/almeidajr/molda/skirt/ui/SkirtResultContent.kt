package com.almeidajr.molda.skirt.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R

/**
 * The read-only skirt [result] shown by the live calculator and a saved project alike: the cutting
 * card, the guide entry, the measurements used, then the explanation and formulas.
 *
 * A screen adds its own buttons after the guide entry in [actions], a note under the measurements
 * in [measurementsNote], and a [cardModifier] on the card.
 */
@Composable
internal fun SkirtResultContent(
    result: SkirtResult,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    actions: @Composable FlowRowScope.() -> Unit = {},
    measurementsNote: @Composable () -> Unit = {},
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CuttingCard(result.card, result.diagramDescription, result.coincidenceNotes, cardModifier)
        // One action row under the card; at large text the buttons wrap onto further lines.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FabricFoldingGuideEntry(onOpenGuide)
            actions()
        }
        UsedMeasurements(result.measurementSummary)
        measurementsNote()
        DraftingExplanation(result)
    }
}

/** The measurements and choices a result was drafted from, under their heading. */
@Composable
private fun UsedMeasurements(summary: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.skirt_used_measurements),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        // One wrapping line; TalkBack hears pauses instead of the visual separators.
        Text(
            summary.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { contentDescription = summary.joinToString("; ") },
        )
    }
}
