package com.almeidajr.molda.skirt.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii

/**
 * A saved skirt result, read-only: the cutting card on the stored [radii] (never recalculated),
 * the guide entry and the measurements it was drafted from.
 */
@Composable
fun SavedSkirtResult(
    draft: SkirtDraft,
    radii: SkirtRadii,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text: (Int) -> String = LocalResources.current::getString
    val card = cuttingCard(radii, text)
    val description = diagramDescription(
        card,
        formatDecimal(draft.waistSeamAllowance.centimetres),
        formatDecimal(draft.hemAllowance.centimetres),
        text,
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CuttingCard(card, description)
        FabricFoldingGuideEntry(onOpenGuide)
        UsedMeasurements(draft)
    }
}

/** The measurements and choices a result was drafted from, under their heading. */
@Composable
internal fun UsedMeasurements(draft: SkirtDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.skirt_used_measurements),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        val summary = measurementSummary(draft, LocalResources.current::getString)
        // One wrapping line; TalkBack hears pauses instead of the visual separators.
        Text(
            summary.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { contentDescription = summary.joinToString("; ") },
        )
    }
}
