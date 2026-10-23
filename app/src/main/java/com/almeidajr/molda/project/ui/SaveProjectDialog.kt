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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.domain.readProjectName

/**
 * Names the result being saved, starting from [defaultName]; a storage failure keeps the dialog,
 * the name and the result.
 */
@Composable
fun SaveProjectDialog(
    defaultName: String,
    isBusy: Boolean,
    @StringRes error: Int?,
    onSave: (ProjectName) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TypedNameDialog(
        title = stringResource(R.string.project_save),
        confirmLabel = stringResource(R.string.project_save_confirm),
        initialName = defaultName,
        isBusy = isBusy,
        error = error,
        onConfirm = onSave,
        onCancel = onCancel,
        modifier = modifier,
    )
}

/**
 * A [ProjectNameDialog] that keeps the name typed from [initialName] through recreation, refuses a
 * blank one and confirms a valid one with [onConfirm].
 */
@Composable
private fun TypedNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    isBusy: Boolean,
    @StringRes error: Int?,
    onConfirm: (ProjectName) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var hasBlankName by rememberSaveable { mutableStateOf(false) }
    ProjectNameDialog(
        title = title,
        confirmLabel = confirmLabel,
        name = name,
        hasBlankName = hasBlankName,
        isBusy = isBusy,
        error = error,
        onEditName = {
            name = it
            hasBlankName = false
        },
        onConfirm = {
            val valid = readProjectName(name)
            if (valid == null) hasBlankName = true else onConfirm(valid)
        },
        onCancel = onCancel,
        modifier = modifier,
    )
}

/** Renames a stored project, starting from [currentName]; a storage failure keeps the dialog. */
@Composable
fun RenameProjectDialog(
    currentName: String,
    isBusy: Boolean,
    @StringRes error: Int?,
    onRename: (ProjectName) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TypedNameDialog(
        title = stringResource(R.string.project_rename),
        confirmLabel = stringResource(R.string.project_rename_confirm),
        initialName = currentName,
        isBusy = isBusy,
        error = error,
        onConfirm = onRename,
        onCancel = onCancel,
        modifier = modifier,
    )
}

/** Asks before deleting the stored project [name]; a storage failure keeps it and says so. */
@Composable
fun DeleteProjectDialog(
    name: String,
    isBusy: Boolean,
    @StringRes error: Int?,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = modifier,
        title = {
            Text(stringResource(R.string.project_delete_title, name))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.project_delete_body))
                error?.let { ErrorText(it) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDelete,
                enabled = !isBusy,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                val label = if (isBusy) {
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
                enabled = !isBusy,
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

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
