package com.almeidajr.molda.profile.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.almeidajr.molda.R
import com.almeidajr.molda.profile.domain.parseProfileDate
import java.time.LocalDate

@Composable
fun ProfileEditor(
    state: ProfileEditorState,
    onEdit: (ProfileField, String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    onRetry: () -> Unit = {},
) {
    val focus = LocalFocusManager.current
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.showsLocalOnlyNote) {
            Text(stringResource(R.string.profile_local_only))
        }
        ProfileMeasurementFields(state, onEdit) {
            focus.clearFocus()
            showCalendar = true
        }
        ProfileMutationError(state.error)
        if (state.canRetryLoad) {
            ProfileMutationError(R.string.profile_load_failed)
            TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.action_retry_load))
            }
        }
        Button(
            onClick = {
                focus.clearFocus()
                onSave()
            },
            enabled = state.canSave,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(stringResource(state.saveLabel))
        }
        TextButton(
            onClick = {
                focus.clearFocus()
                onCancel()
            },
            enabled = !state.isSaving,
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.action_cancel)) }
    }
    if (showCalendar) {
        ProfileDatePicker(
            state.inputs.getValue(ProfileField.Date),
            today,
            onSelect = {
                onEdit(ProfileField.Date, it)
                showCalendar = false
            },
            onDismiss = { showCalendar = false },
        )
    }
}

@Composable
private fun ProfileMeasurementFields(
    state: ProfileEditorState,
    onEdit: (ProfileField, String) -> Unit,
    onCalendar: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                stringResource(R.string.profile_editor_section),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            ProfileField.entries.forEach { field ->
                if (field == ProfileField.Date) {
                    ProfileDateField(state, onClick = onCalendar)
                } else {
                    ProfileInput(state, field, onEdit)
                }
            }
        }
    }
}

@Composable
private fun ProfileDatePicker(
    text: String,
    today: LocalDate,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectable = remember(today) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                isSelectableDate(calendarMillisToDate(utcTimeMillis), today)
            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        }
    }
    val current = parseProfileDate(text)
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = initialCalendarSelection(current, today)
            ?.let(::dateToCalendarMillis),
        initialDisplayedMonthMillis = dateToCalendarMillis(initialCalendarDate(current, today)),
        yearRange = 1..maxOf(2100, today.year),
        selectableDates = selectable,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    picker.selectedDateMillis?.let {
                        onSelect(profileDateText(calendarMillisToDate(it)))
                    }
                },
                enabled = picker.selectedDateMillis != null,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.profile_confirm_date))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.profile_cancel_calendar))
            }
        },
        // A window that fits the keyboard is moved by the system when it resizes, so the calendar
        // returning from text mode jumped to the top. Edge to edge, only text mode makes room for
        // the keyboard, while the calendar ignores the keyboard still closing behind it.
        modifier = Modifier.windowInsetsPadding(
            if (picker.displayMode == DisplayMode.Input) {
                WindowInsets.safeDrawing
            } else {
                WindowInsets.safeDrawing.exclude(WindowInsets.ime)
            },
        ),
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) { DatePicker(state = picker, modifier = Modifier.verticalScroll(rememberScrollState())) }
}

@Composable
private fun ProfileDateField(state: ProfileEditorState, onClick: () -> Unit) {
    val error = state.errors[ProfileField.Date]?.message
    val enabled = state.canEdit
    val openCalendar = stringResource(R.string.profile_open_calendar)
    // Dates are typed only in the calendar's text mode, so the field opens it on any tap.
    OutlinedTextField(
        value = state.inputs.getValue(ProfileField.Date),
        onValueChange = {},
        label = { Text(stringResource(ProfileField.Date.label)) },
        readOnly = true,
        enabled = enabled,
        singleLine = true,
        isError = error != null,
        supportingText = {
            Text(
                stringResource(error ?: R.string.profile_hint_date),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        trailingIcon = {
            IconButton(onClick = onClick, enabled = enabled) {
                Icon(painterResource(R.drawable.ic_calendar), openCalendar)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                if (enabled) {
                    onClick(openCalendar) {
                        onClick()
                        true
                    }
                }
            }
            .pointerInput(enabled) {
                if (enabled) {
                    awaitEachGesture {
                        // Modifier.clickable doesn't work for text fields: observe the tap in the
                        // Initial pass, before the text field consumes it.
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        if (waitForUpOrCancellation(pass = PointerEventPass.Initial) != null) {
                            onClick()
                        }
                    }
                }
            },
    )
}

@Composable
private fun ProfileInput(
    state: ProfileEditorState,
    field: ProfileField,
    onEdit: (ProfileField, String) -> Unit,
) {
    val focus = LocalFocusManager.current
    val error = state.errors[field]?.message
    OutlinedTextField(
        value = state.inputs.getValue(field),
        onValueChange = { onEdit(field, it) },
        label = { Text(stringResource(field.label)) },
        isError = error != null,
        supportingText = {
            Text(
                stringResource(
                    error ?: when (field) {
                        ProfileField.Name -> R.string.profile_hint_name
                        ProfileField.Waist -> R.string.profile_hint_waist
                        ProfileField.Notes -> R.string.profile_hint_notes
                        ProfileField.Date -> R.string.profile_hint_date
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        enabled = state.canEdit,
        singleLine = field != ProfileField.Notes,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (field ==
                ProfileField.Waist
            ) {
                KeyboardType.Decimal
            } else {
                KeyboardType.Text
            },
            imeAction = if (field == ProfileField.Notes) ImeAction.Done else ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ProfileMutationError(@StringRes error: Int?) {
    error?.let {
        Text(
            stringResource(it),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
