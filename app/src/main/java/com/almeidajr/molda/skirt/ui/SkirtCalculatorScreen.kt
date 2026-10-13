package com.almeidajr.molda.skirt.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almeidajr.molda.profile.ui.ProfilesRoute
import com.almeidajr.molda.profile.ui.profileWaistText
import com.almeidajr.molda.skirt.domain.SkirtRadii
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun SkirtCalculatorRoute(
    modifier: Modifier = Modifier,
    viewModel: SkirtCalculatorViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SkirtCalculatorScreen(state, viewModel::edit, viewModel::calculate, modifier, viewModel::blur) {
        ProfilesRoute(onSelect = { viewModel.edit(MeasurementField.Waist, profileWaistText(it)) })
    }
}

@Composable
fun SkirtCalculatorScreen(
    state: CalculatorState,
    onEdit: (MeasurementField, String) -> Unit,
    onCalculate: () -> Unit,
    modifier: Modifier = Modifier,
    onBlur: (MeasurementField) -> Unit = {},
    profiles: @Composable () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val focusRequesters = remember { MeasurementField.entries.associateWith { FocusRequester() } }
    val bringRequesters =
        remember { MeasurementField.entries.associateWith { BringIntoViewRequester() } }
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.calculationAttempt) {
        MeasurementField.entries.firstOrNull { it in state.errors }?.let { field ->
            focusRequesters.getValue(field).requestFocus()
            bringRequesters.getValue(field).bringIntoView()
        }
    }
    Column(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(
            rememberScrollState(),
        ).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Saia godê inteira", style = MaterialTheme.typography.headlineMedium)
        Text("Medidas em centímetros. Informe folga e margens explicitamente; zero é válido.")
        MeasurementField.entries.forEach { field ->
            MeasurementGroupHeading(field)
            MeasurementInput(
                field = field,
                state = state,
                onEdit = onEdit,
                onBlur = onBlur,
                onNext = {
                    focusRequesters.getValue(
                        MeasurementField.entries[field.ordinal + 1],
                    ).requestFocus()
                },
                onDone = {
                    focusManager.clearFocus()
                    onCalculate()
                },
                modifier = Modifier.focusRequester(focusRequesters.getValue(field))
                    .bringIntoViewRequester(bringRequesters.getValue(field)),
                onFocused = { scope.launch { bringRequesters.getValue(field).bringIntoView() } },
            )
        }
        Button(onClick = {
            focusManager.clearFocus()
            onCalculate()
        }) { Text("Calcular") }
        profiles()
        state.calculationError?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
        }
        state.radii?.let {
            SkirtResults(it)
            DraftingExplanation(state.inputs, it)
        }
    }
}

@Composable
private fun MeasurementGroupHeading(field: MeasurementField) {
    val group = when (field) {
        MeasurementField.Waist -> "Medida do corpo"
        MeasurementField.Ease -> "Escolhas da peça"
        MeasurementField.WaistSeamAllowance -> "Margens de corte"
        else -> null
    }
    group?.let {
        Text(
            it,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics {
                heading()
            },
        )
    }
}

@Composable
private fun MeasurementInput(
    field: MeasurementField,
    state: CalculatorState,
    onEdit: (MeasurementField, String) -> Unit,
    onBlur: (MeasurementField) -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val guidance = when (field) {
        MeasurementField.Waist ->
            "Meça a circunferência da cintura na posição desejada do cós, " +
                "sem acrescentar folga ou margens."

        MeasurementField.Length ->
            "Meça o comprimento até a barra acabada, " +
                "a partir da linha de costura da cintura; exclua a altura do cós. As margens são informadas separadamente."

        else -> null
    }
    val error = state.errors[field]
    var wasFocused by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = state.inputs.getValue(field),
        onValueChange = { onEdit(field, it) },
        label = { Text(field.label) },
        isError = error != null,
        supportingText = (error ?: guidance)?.let { message ->
            {
                Text(
                    message,
                    modifier = Modifier.semantics {
                        if (error != null) liveRegion = LiveRegionMode.Polite
                    },
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = if (field ==
                MeasurementField.HemAllowance
            ) {
                ImeAction.Done
            } else {
                ImeAction.Next
            },
        ),
        keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onDone() }),
        modifier = modifier.fillMaxWidth().onFocusChanged { focus ->
            if (wasFocused && !focus.isFocused) onBlur(field)
            wasFocused = focus.isFocused
            if (focus.isFocused) onFocused()
        },
    )
}

@Composable
private fun SkirtResults(radii: SkirtRadii) {
    Text("Raios para o molde", style = MaterialTheme.typography.titleLarge)
    Text("Raio da linha de costura da cintura: ${formatRadius(radii.waistStitching)}")
    Text("Raio da barra acabada: ${formatRadius(radii.finishedHem)}")
    Text("Raio de corte da cintura: ${formatRadius(radii.waistCutting)}")
    Text("Raio de corte da barra: ${formatRadius(radii.hemCutting)}")
    Text(
        "Para saia godê inteira em tecido plano, com cós e fechamento. " +
            "Não inclui divisão em painéis, margens do fechamento, molde do cós " +
            "ou metragem de tecido. O comprimento acabado exclui a altura do cós.",
    )
}

internal fun formatRadius(value: Double): String =
    String.format(Locale.forLanguageTag("pt-BR"), "%.2f cm", value)
