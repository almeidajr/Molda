package com.almeidajr.molda.project.ui

import androidx.annotation.StringRes
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
    ProjectNameDialog(
        title = stringResource(R.string.project_save),
        confirmLabel = stringResource(R.string.project_save_confirm),
        name = saving.name,
        hasBlankName = saving.hasBlankName,
        isBusy = saving.isBusy,
        error = saving.error,
        onEditName = onEditName,
        onConfirm = onSave,
        onCancel = onCancel,
        modifier = modifier,
    )
}

/** Renames a stored project; a storage failure keeps the dialog and the typed name. */
@Composable
fun RenameProjectDialog(
    renaming: ProjectRenaming,
    onEditName: (String) -> Unit,
    onRename: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProjectNameDialog(
        title = stringResource(R.string.project_rename),
        confirmLabel = stringResource(R.string.project_rename_confirm),
        name = renaming.name,
        hasBlankName = renaming.hasBlankName,
        isBusy = renaming.isBusy,
        error = renaming.error,
        onEditName = onEditName,
        onConfirm = onRename,
        onCancel = onCancel,
        modifier = modifier,
    )
}

/** Asks before deleting a stored project; a storage failure keeps it and says so. */
@Composable
fun DeleteProjectDialog(
    deleting: ProjectDeleting,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = modifier,
        title = {
            Text(stringResource(R.string.project_delete_title, deleting.name))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.project_delete_body))
                deleting.error?.let { ErrorText(it) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDelete,
                enabled = !deleting.isBusy,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                val label = if (deleting.isBusy) {
                    R.string.project_deleting
                } else {
                    R.string.action_confirm_delete
                }
                Text(stringResource(label))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                enabled = !deleting.isBusy,
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Suppress("LongParameterList") // The dialog's whole state, shared by saving and renaming.
@Composable
private fun ProjectNameDialog(
    title: String,
    confirmLabel: String,
    name: String,
    hasBlankName: Boolean,
    isBusy: Boolean,
    @StringRes error: Int?,
    onEditName: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = modifier,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProjectNameField(name, hasBlankName, isBusy, onEditName, onConfirm)
                error?.let { ErrorText(it) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isBusy,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(if (isBusy) stringResource(R.string.status_saving) else confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                enabled = !isBusy,
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ErrorText(@StringRes error: Int) {
    Text(
        stringResource(error),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
    )
}

@Composable
private fun ProjectNameField(
    name: String,
    hasBlankName: Boolean,
    isBusy: Boolean,
    onEditName: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    OutlinedTextField(
        value = name,
        onValueChange = onEditName,
        label = { Text(stringResource(R.string.project_field_name)) },
        isError = hasBlankName,
        enabled = !isBusy,
        supportingText = {
            Text(
                stringResource(
                    if (hasBlankName) {
                        R.string.project_error_blank_name
                    } else {
                        R.string.project_hint_name
                    },
                ),
                modifier = Modifier.semantics {
                    if (hasBlankName) liveRegion = LiveRegionMode.Polite
                },
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onConfirm() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Shows the latest command's outcome once in [snackbar], then calls [onDismiss]. */
@Composable
fun ProjectFeedback(state: ProjectsState, snackbar: SnackbarHostState, onDismiss: () -> Unit) {
    val message = state.message?.let { stringResource(it) }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            onDismiss()
        }
    }
}
