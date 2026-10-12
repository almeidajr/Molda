package com.almeidajr.molda.skirt.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almeidajr.molda.skirt.domain.SkirtRadii
import java.util.Locale

@Composable
fun SkirtCalculatorRoute(
    modifier: Modifier = Modifier,
    viewModel: SkirtCalculatorViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SkirtCalculatorScreen(state, viewModel::edit, viewModel::calculate, modifier)
}

@Composable
fun SkirtCalculatorScreen(
    state: CalculatorState,
    onEdit: (MeasurementField, String) -> Unit,
    onCalculate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(
            rememberScrollState(),
        ).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Saia godê inteira", style = MaterialTheme.typography.headlineMedium)
        Text("Medidas em centímetros. Informe folga e margens explicitamente; zero é válido.")
        Text(
            "Meça a circunferência da cintura na posição desejada do cós, sem acrescentar folga " +
                "ou margens. Meça o comprimento até a barra acabada, a partir da linha de " +
                "costura da cintura; exclua a altura do cós.",
        )
        MeasurementField.entries.forEach { field ->
            val error = state.errors[field]
            OutlinedTextField(
                value = state.inputs.getValue(field),
                onValueChange = { onEdit(field, it) },
                label = { Text(field.label) },
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
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
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) },
                    onDone = {
                        focusManager.clearFocus()
                        onCalculate()
                    },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Button(onClick = {
            focusManager.clearFocus()
            onCalculate()
        }) { Text("Calcular") }
        state.calculationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.radii?.let {
            SkirtResults(it)
            DraftingExplanation(state.inputs, it)
        }
    }
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
