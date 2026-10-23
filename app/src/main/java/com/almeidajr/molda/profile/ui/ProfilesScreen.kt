package com.almeidajr.molda.profile.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R
import com.almeidajr.molda.data.LoadResult
import com.almeidajr.molda.measurement.lengthText
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileDateFormat
import com.almeidajr.molda.profile.domain.ProfileId
import java.time.LocalDate

@Composable
fun ProfilesContent(
    state: ProfilesState,
    onRetry: () -> Unit,
    onStartEdit: (ProfileId) -> Unit,
    onDelete: (ProfileId) -> Unit,
    modifier: Modifier = Modifier,
    /** False while the open editor saves, which no row may replace. */
    enabled: Boolean = true,
) {
    val canMutate = enabled && state.canMutate
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.profile_management_local_only))
        when (val load = state.load) {
            LoadResult.Loading -> {
                CircularProgressIndicator()
                Text(stringResource(R.string.profile_loading))
            }

            is LoadResult.Error -> {
                Text(
                    stringResource(R.string.profile_load_failed),
                    color = MaterialTheme.colorScheme.error,
                )
                Button(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.action_retry_load))
                }
            }

            is LoadResult.Success if load.value.isEmpty() ->
                Text(stringResource(R.string.profile_empty_management))

            is LoadResult.Success -> load.value.forEach { profile ->
                ProfileManagementRow(
                    profile,
                    canMutate,
                    onStartEdit,
                    onDelete,
                    isDeleting = state.isSaving && state.deleteId == profile.id,
                    deleteError = state.saveError.takeIf { state.deleteId == profile.id },
                )
            }
        }
    }
}

/** Opens a new profile's editor; not [enabled] while the open editor saves. */
@Composable
fun ProfileCreateAction(onCreate: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.profile_new)
    // Material 3 FABs have no enabled state, so a disabled one ignores taps and says so.
    ExtendedFloatingActionButton(
        text = { Text(label) },
        // This overload hides the text from accessibility and names the FAB by its icon.
        icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = label) },
        onClick = { if (enabled) onCreate() },
        modifier = modifier.semantics { if (!enabled) disabled() },
    )
}

internal fun profileDateText(date: LocalDate): String = date.format(ProfileDateFormat)

@Composable
private fun ProfileManagementRow(
    profile: MeasurementProfile,
    enabled: Boolean,
    onStartEdit: (ProfileId) -> Unit,
    onDelete: (ProfileId) -> Unit,
    isDeleting: Boolean,
    @StringRes deleteError: Int?,
) {
    var confirmDelete by remember(profile.id) { mutableStateOf(false) }
    if (confirmDelete) {
        ConfirmProfileDeletion(profile, enabled, { confirmDelete = false }) {
            confirmDelete = false
            onDelete(profile.id)
        }
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            ListItem(
                onClick = { onStartEdit(profile.id) },
                enabled = enabled,
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(
                                R.string.profile_summary,
                                lengthText(profile.measurements.waist),
                                profileDateText(profile.measurements.measurementDate),
                            ),
                        )
                        if (profile.measurements.notes.isNotBlank()) {
                            Text(profile.measurements.notes)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    profile.measurements.name.text,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            ProfileRowActions(profile, enabled, onStartEdit) { confirmDelete = true }
        }
    }
    if (isDeleting) Text(stringResource(R.string.profile_deleting, profile.measurements.name.text))
    deleteError?.let {
        Text(
            stringResource(it),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun ProfileRowActions(
    profile: MeasurementProfile,
    enabled: Boolean,
    onStartEdit: (ProfileId) -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        TextButton(onClick = {
            expanded = true
        }, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.action_menu_for, profile.measurements.name.text))
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_edit)) },
                onClick = {
                    expanded = false
                    onStartEdit(profile.id)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_delete)) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun ConfirmProfileDeletion(
    profile: MeasurementProfile,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.profile_delete_title, profile.measurements.name.text))
        },
        text = { Text(stringResource(R.string.profile_delete_body)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = enabled,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.action_confirm_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePicker(
    state: ProfilesState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onSelect: (MeasurementProfile) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.profile_picker_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.profile_picker_cancel))
            }
            when (val rows = state.pickerRows) {
                LoadResult.Loading -> {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.profile_loading))
                }

                is LoadResult.Error -> {
                    Text(
                        stringResource(R.string.profile_load_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.action_retry_load))
                    }
                }

                is LoadResult.Success if rows.value.isEmpty() ->
                    Text(stringResource(R.string.profile_empty_picker))

                is LoadResult.Success -> rows.value.forEach { (profile, showNotes) ->
                    PickerRow(profile, showNotes, onSelect)
                }
            }
            Button(
                onClick = onCreate,
                enabled = state.canMutate,
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.profile_new)) }
        }
    }
}

@Composable
private fun PickerRow(
    profile: MeasurementProfile,
    showNotes: Boolean,
    onSelect: (MeasurementProfile) -> Unit,
) {
    // Two tonal steps above the sheet's surfaceContainerLow, so each row reads as its own item.
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        ListItem(
            onClick = { onSelect(profile) },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            supportingContent = if (showNotes && profile.measurements.notes.isNotBlank()) {
                { Text(profile.measurements.notes) }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    R.string.profile_use_label,
                    profile.measurements.name.text,
                    lengthText(profile.measurements.waist),
                    profileDateText(profile.measurements.measurementDate),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
