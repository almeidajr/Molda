package com.almeidajr.molda.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R

/** Names the result being saved; a storage failure keeps the dialog, the name and the result. */
@Composable
fun SaveProjectDialog(
    saving: ProjectSaving,
    onEditName: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = modifier,
        title = { Text(stringResource(R.string.project_save)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProjectNameField(saving, onEditName, onSave)
                saving.saveError?.let { error ->
                    Text(
                        stringResource(error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = !saving.isSaving,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                val label = if (saving.isSaving) {
                    R.string.status_saving
                } else {
                    R.string.project_save_confirm
                }
                Text(stringResource(label))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                enabled = !saving.isSaving,
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ProjectNameField(
    saving: ProjectSaving,
    onEditName: (String) -> Unit,
    onSave: () -> Unit,
) {
    OutlinedTextField(
        value = saving.name,
        onValueChange = onEditName,
        label = { Text(stringResource(R.string.project_field_name)) },
        isError = saving.hasBlankName,
        enabled = !saving.isSaving,
        supportingText = {
            Text(
                stringResource(
                    if (saving.hasBlankName) {
                        R.string.project_error_blank_name
                    } else {
                        R.string.project_hint_name
                    },
                ),
                modifier = Modifier.semantics {
                    if (saving.hasBlankName) liveRegion = LiveRegionMode.Polite
                },
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onSave() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Shows the saved confirmation once in [snackbar], then calls [onDismiss]. */
@Composable
fun ProjectSaveFeedback(state: ProjectsState, snackbar: SnackbarHostState, onDismiss: () -> Unit) {
    val message = state.savedMessage?.let { stringResource(it) }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            onDismiss()
        }
    }
}
