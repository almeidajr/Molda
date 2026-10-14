package com.almeidajr.molda.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun ProfileEditor(
    state: ProfilesState,
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
        ProfileEditorHeading(state.editingId == null)
        ProfileField.entries.forEach { field ->
            ProfileInput(state, field, onEdit)
            if (field == ProfileField.Date) {
                TextButton(onClick = {
                    focus.clearFocus()
                    showCalendar = true
                }, enabled = !state.isSaving) {
                    Text("Escolher data no calendário")
                }
            }
        }
        ProfileMutationError(state.saveError)
        state.loadError?.let {
            ProfileMutationError(it)
            TextButton(onClick = onRetry) { Text("Tentar carregar novamente") }
        }
        Button(onClick = {
            focus.clearFocus()
            onSave()
        }, enabled = !state.isSaving && !state.isLoading && state.loadError == null) {
            Text(
                when {
                    state.isSaving -> "Salvando…"
                    state.editingId != null -> "Salvar alterações"
                    else -> "Salvar perfil"
                },
            )
        }
        TextButton(onClick = {
            focus.clearFocus()
            onCancel()
        }, enabled = !state.isSaving) { Text("Cancelar") }
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
private fun ProfileEditorHeading(isCreating: Boolean) {
    Text(
        if (isCreating) "Novo perfil de medidas" else "Editar perfil de medidas",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.semantics { heading() },
    )
    if (isCreating) {
        Text(
            "Os perfis ficam só neste aparelho, sem backup ou sincronização. " +
                "Você pode perdê-los ao limpar os dados do app ou trocar de aparelho.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileDatePicker(
    text: String,
    today: LocalDate,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // Material calendar timestamps represent dates at UTC midnight, not local midnight.
    val selectable = remember(today) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = !Instant.ofEpochMilli(
                utcTimeMillis,
            ).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)
            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        }
    }
    val initial = parseProfileDate(text)
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = initial?.takeUnless { it.isAfter(today) || it.year < 1 }
            ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        yearRange = 1..maxOf(2100, today.year),
        selectableDates = selectable,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let {
                    onSelect(
                        Instant.ofEpochMilli(
                            it,
                        ).atZone(ZoneOffset.UTC).toLocalDate().format(ProfileDateFormat),
                    )
                }
            }, enabled = picker.selectedDateMillis != null) { Text("Confirmar data") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar calendário") } },
    ) { DatePicker(state = picker, showModeToggle = false) }
}

@Composable
private fun ProfileInput(
    state: ProfilesState,
    field: ProfileField,
    onEdit: (ProfileField, String) -> Unit,
) {
    val focus = LocalFocusManager.current
    val error = state.errors[field]
    OutlinedTextField(
        value = state.inputs.getValue(field),
        onValueChange = { onEdit(field, it) },
        label = { Text(field.label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        enabled = !state.isSaving,
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
private fun ProfileMutationError(error: String?) {
    error?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
