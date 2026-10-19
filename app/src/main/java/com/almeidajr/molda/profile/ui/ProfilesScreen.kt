package com.almeidajr.molda.profile.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileDateFormat
import com.almeidajr.molda.profile.domain.ProfileId
import java.math.BigDecimal
import java.time.LocalDate

@Composable
fun ProfilesContent(
    state: ProfilesState,
    onRetry: () -> Unit,
    onStartEdit: (ProfileId) -> Unit,
    onDelete: (ProfileId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val canMutate = !state.isLoading && state.loadError == null && !state.isSaving
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.profile_management_local_only))
        if (state.isLoading) {
            CircularProgressIndicator()
            Text(stringResource(R.string.profile_loading))
        }
        state.loadError?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.error)
            Button(
                onClick = onRetry,
                enabled = !state.isLoading,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.profile_retry_load))
            }
        }
        if (!state.isLoading && state.loadError == null && state.profiles.isEmpty()) {
            Text(stringResource(R.string.profile_empty_management))
        }
        state.profiles.forEach { profile ->
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

@Composable
fun ProfileCreateAction(state: ProfilesState, onCreate: () -> Unit, modifier: Modifier = Modifier) {
    val canMutate = !state.isLoading && state.loadError == null && !state.isSaving
    ExtendedFloatingActionButton(
        onClick = { if (canMutate) onCreate() },
        modifier = modifier.semantics { if (!canMutate) disabled() },
    ) { Text(stringResource(R.string.profile_new)) }
}

internal fun profileWaistText(waist: Length): String =
    BigDecimal.valueOf(waist.centimetres).stripTrailingZeros().toPlainString().replace('.', ',')

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
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                headlineContent = {
                    Text(
                        profile.measurements.name.text,
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(
                                R.string.profile_summary,
                                profileWaistText(profile.measurements.waist),
                                profileDateText(profile.measurements.measurementDate),
                            ),
                        )
                        if (profile.measurements.notes.isNotBlank()) {
                            Text(profile.measurements.notes)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) {
                    onStartEdit(profile.id)
                },
            )
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
            Text(stringResource(R.string.profile_actions, profile.measurements.name.text))
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
                Text(stringResource(R.string.profile_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.profile_cancel))
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
            when {
                state.isLoading -> {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.profile_loading))
                }

                state.loadError != null -> {
                    Text(stringResource(state.loadError), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.profile_retry_load))
                    }
                }

                state.profiles.isEmpty() -> Text(stringResource(R.string.profile_empty_picker))

                else -> state.profiles.forEach { profile ->
                    val hasDuplicate = state.profiles.count {
                        it.measurements.name == profile.measurements.name &&
                            it.measurements.waist == profile.measurements.waist &&
                            it.measurements.measurementDate == profile.measurements.measurementDate
                    } > 1
                    PickerRow(profile, showNotes = hasDuplicate, onSelect)
                }
            }
            Button(
                onClick = onCreate,
                enabled = !state.isLoading && state.loadError == null && !state.isSaving,
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
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            headlineContent = {
                Text(
                    stringResource(
                        R.string.profile_use_label,
                        profile.measurements.name.text,
                        profileWaistText(profile.measurements.waist),
                        profileDateText(profile.measurements.measurementDate),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            supportingContent = if (showNotes && profile.measurements.notes.isNotBlank()) {
                { Text(profile.measurements.notes) }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth().clickable { onSelect(profile) },
        )
    }
}
