package com.almeidajr.molda.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.almeidajr.molda.profile.data.LocalProfileRepository
import com.almeidajr.molda.profile.domain.MeasurementProfile
import java.math.BigDecimal

@Composable
fun ProfilesRoute(onSelect: (Double) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val viewModel: ProfilesViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ProfilesViewModel(LocalProfileRepository(context), createSavedStateHandle())
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfilesContent(
        state,
        viewModel::startCreating,
        viewModel::edit,
        viewModel::save,
        viewModel::load,
        onSelect,
        modifier,
    )
}

@Composable
fun ProfilesContent(
    state: ProfilesState,
    onCreate: () -> Unit,
    onEdit: (ProfileField, String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onSelect: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Perfis de medidas (opcional)", style = MaterialTheme.typography.titleLarge)
        Text(
            "Os perfis ficam só neste aparelho, sem backup ou sincronização. " +
                "Você pode perdê-los ao limpar os dados do app ou trocar de aparelho.",
        )
        if (state.isLoading) Text("Carregando perfis…")
        state.loadError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Button(onClick = onRetry, enabled = !state.isLoading) {
                Text("Tentar carregar novamente")
            }
        }
        state.savedMessage?.let { Text(it) }
        state.profiles.forEach { profile ->
            Button(onClick = {
                onSelect(profile.measurements.waist)
            }) { Text(profileLabel(profile)) }
            if (profile.measurements.notes.isNotBlank()) Text(profile.measurements.notes)
        }
        Button(
            onClick = onCreate,
            enabled =
                !state.isLoading && state.loadError == null && !state.isSaving,
        ) { Text("Novo perfil") }
        if (state.isCreating) {
            ProfileForm(state, onEdit)
            state.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = onSave,
                enabled =
                    !state.isLoading && state.loadError == null && !state.isSaving,
            ) {
                Text(if (state.isSaving) "Salvando…" else "Salvar perfil")
            }
        }
    }
}

internal fun profileWaistText(value: Double): String =
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().replace('.', ',')

private fun profileLabel(profile: MeasurementProfile): String =
    "Usar perfil: ${profile.measurements.name} — " +
        "${profileWaistText(profile.measurements.waist)} cm — " +
        profile.measurements.measurementDate.format(ProfileDateFormat) + " (perfil ${profile.id})"

@Composable
private fun ProfileForm(state: ProfilesState, onEdit: (ProfileField, String) -> Unit) {
    ProfileField.entries.forEach { field ->
        val error = state.errors[field]
        OutlinedTextField(
            value = state.inputs.getValue(field), onValueChange = { onEdit(field, it) },
            label = { Text(field.label) }, isError = error != null,
            supportingText = error?.let { { Text(it) } },
            enabled = !state.isSaving,
            singleLine = field != ProfileField.Notes,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (field == ProfileField.Waist) {
                    KeyboardType.Decimal
                } else {
                    KeyboardType.Text
                },
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
