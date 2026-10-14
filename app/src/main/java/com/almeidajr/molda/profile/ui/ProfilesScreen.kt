package com.almeidajr.molda.profile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.profile.domain.MeasurementProfile
import java.math.BigDecimal

@Composable
fun ProfilesContent(
    state: ProfilesState,
    onRetry: () -> Unit,
    onStartEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val canMutate = !state.isLoading && state.loadError == null && !state.isSaving
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Perfis de medidas (opcional)",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Os perfis ficam só neste aparelho, sem backup ou sincronização. " +
                "Você pode perdê-los ao limpar os dados do app ou trocar de aparelho.",
        )
        if (state.isLoading) {
            CircularProgressIndicator()
            Text("Carregando perfis…")
        }
        state.loadError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Button(onClick = onRetry, enabled = !state.isLoading) {
                Text("Tentar carregar novamente")
            }
        }
        if (!state.isLoading && state.loadError == null && state.profiles.isEmpty()) {
            Text(
                "Nenhum perfil salvo. Os perfis são opcionais; " +
                    "você pode calcular com uma medida manual.",
            )
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
    ) { Text("Novo perfil") }
}

internal fun profileWaistText(value: Double): String =
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().replace('.', ',')

internal fun profileLabel(profile: MeasurementProfile): String =
    "Usar perfil: ${profile.measurements.name} — " +
        "${profileWaistText(profile.measurements.waist)} cm — " +
        profile.measurements.measurementDate.format(ProfileDateFormat)

@Composable
private fun ProfileManagementRow(
    profile: MeasurementProfile,
    enabled: Boolean,
    onStartEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    isDeleting: Boolean,
    deleteError: String?,
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
                headlineContent = { Text(profile.measurements.name) },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "${profileWaistText(profile.measurements.waist)} cm — " +
                                profile.measurements.measurementDate.format(ProfileDateFormat),
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
    if (isDeleting) Text("Excluindo ${profile.measurements.name}…")
    deleteError?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun ProfileRowActions(
    profile: MeasurementProfile,
    enabled: Boolean,
    onStartEdit: (Long) -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        TextButton(onClick = { expanded = true }, enabled = enabled) {
            Text("Ações: ${profile.measurements.name}")
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Editar perfil") },
                onClick = {
                    expanded = false
                    onStartEdit(profile.id)
                },
            )
            DropdownMenuItem(
                text = { Text("Excluir perfil") },
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
        title = { Text("Excluir perfil ${profile.measurements.name}?") },
        text = {
            Text("A medida salva será excluída deste aparelho. O cálculo atual será mantido.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = enabled) { Text("Confirmar exclusão") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
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
                "Usar perfil",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            TextButton(onClick = onDismiss) { Text("Cancelar seleção") }
            when {
                state.isLoading -> {
                    CircularProgressIndicator()
                    Text("Carregando perfis…")
                }

                state.loadError != null -> {
                    Text(state.loadError, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text("Tentar carregar novamente") }
                }

                state.profiles.isEmpty() -> Text(
                    "Nenhum perfil salvo. Você pode calcular com uma medida manual.",
                )

                else -> state.profiles.forEach { profile ->
                    val hasDuplicate = state.profiles.count {
                        it.measurements.name == profile.measurements.name &&
                            it.measurements.waist == profile.measurements.waist &&
                            it.measurements.measurementDate == profile.measurements.measurementDate
                    } > 1
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            headlineContent = { Text(profileLabel(profile)) },
                            supportingContent = if (hasDuplicate &&
                                profile.measurements.notes.isNotBlank()
                            ) {
                                { Text(profile.measurements.notes) }
                            } else {
                                null
                            },
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(profile) },
                        )
                    }
                }
            }
            Button(
                onClick = onCreate,
                enabled = !state.isLoading && state.loadError == null && !state.isSaving,
            ) { Text("Novo perfil") }
        }
    }
}
