package com.almeidajr.molda.skirt.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R

// What only the live calculator adds around the shared skirt result.

/**
 * The live calculator's outcome: the non-finite warning, or the shared result with the save and
 * edit actions. [cardModifier] goes on the cutting card, the target of the scroll reveal.
 */
@Composable
internal fun CalculationOutcome(
    state: CalculatorState,
    onOpenGuide: () -> Unit,
    onSaveProject: () -> Unit,
    onSaveAsNewProject: (() -> Unit)?,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    onEditMeasurements: () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (state.hasNonfiniteResult) {
            Text(
                stringResource(R.string.skirt_error_nonfinite_result),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
        }
        state.result?.let { (draft, radii) ->
            SkirtResultContent(
                skirtResult(draft, radii, LocalResources.current::getString),
                onOpenGuide,
                cardModifier = cardModifier,
                actions = {
                    SaveActions(onSaveProject, onSaveAsNewProject)
                    TextButton(
                        onClick = onEditMeasurements,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.skirt_edit_measurements)) }
                },
                measurementsNote = { HonestLimitsNote() },
            )
        }
    }
}

// Editing a saved project offers updating it or keeping it and saving another.
@Composable
private fun SaveActions(onSaveProject: () -> Unit, onSaveAsNewProject: (() -> Unit)?) {
    OutlinedButton(onClick = onSaveProject, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(
            stringResource(
                if (onSaveAsNewProject == null) {
                    R.string.project_save
                } else {
                    R.string.project_save_confirm
                },
            ),
        )
    }
    onSaveAsNewProject?.let {
        OutlinedButton(onClick = it, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.project_save_as_new))
        }
    }
}

@Composable
private fun HonestLimitsNote() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.skirt_limits_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.skirt_limits_body))
        }
    }
}
