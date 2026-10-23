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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R

// The parts only the live calculator adds around the shared skirt result.

// Editing a saved project offers updating it or keeping it and saving another.
@Composable
internal fun SaveActions(onSaveProject: () -> Unit, onSaveAsNewProject: (() -> Unit)?) {
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
internal fun HonestLimitsNote() {
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
