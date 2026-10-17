package com.almeidajr.molda.skirt.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almeidajr.molda.AdaptiveContent
import com.almeidajr.molda.MoldaRoute
import kotlinx.coroutines.launch

@Composable
fun SkirtCalculatorRoute(
    modifier: Modifier = Modifier,
    viewModel: SkirtCalculatorViewModel = viewModel(),
) {
    MoldaRoute(modifier = modifier, calculator = viewModel)
}

@Composable
fun SkirtCalculatorScreen(
    state: CalculatorState,
    onEdit: (MeasurementField, String) -> Unit,
    onCalculate: () -> Unit,
    modifier: Modifier = Modifier,
    onBlur: (MeasurementField) -> Unit = {},
    onUseProfile: () -> Unit = {},
    onOpenTutorial: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val focusRequesters = remember { MeasurementField.entries.associateWith { FocusRequester() } }
    val bringRequesters =
        remember { MeasurementField.entries.associateWith { BringIntoViewRequester() } }
    val scope = rememberCoroutineScope()
    val resultRequester = remember { BringIntoViewRequester() }
    RevealCalculationOutcome(state, resultRequester, focusRequesters, bringRequesters)
    val form: @Composable () -> Unit = {
        CalculatorIntro()
        MeasurementGroups { field ->
            MeasurementInput(
                field = field,
                state = state,
                onEdit = onEdit,
                onBlur = onBlur,
                onUseProfile = onUseProfile,
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
                onFocused = {
                    scope.launch { bringRequesters.getValue(field).bringIntoView() }
                },
            )
        }
        Button(
            onClick = {
                focusManager.clearFocus()
                onCalculate()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text("Calcular") }
    }
    val outcome: @Composable () -> Unit = {
        CalculationOutcome(
            state,
            onOpenTutorial,
            Modifier.bringIntoViewRequester(resultRequester),
        ) {
            focusRequesters.getValue(MeasurementField.Waist).requestFocus()
            scope.launch { bringRequesters.getValue(MeasurementField.Waist).bringIntoView() }
        }
    }
    AdaptiveCalculator(state, modifier, form, outcome)
}

/** After each Calculate, shows the cutting card or focuses the first invalid field. */
@Composable
private fun RevealCalculationOutcome(
    state: CalculatorState,
    resultRequester: BringIntoViewRequester,
    focusRequesters: Map<MeasurementField, FocusRequester>,
    bringRequesters: Map<MeasurementField, BringIntoViewRequester>,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(state.calculationAttempt) {
        if (state.radii != null) {
            // Clearing focus alone leaves the keyboard covering the radii after IME Done.
            keyboard?.hide()
            resultRequester.bringIntoView()
        }
        MeasurementField.entries.firstOrNull { it in state.errors }?.let { field ->
            focusRequesters.getValue(field).requestFocus()
            bringRequesters.getValue(field).bringIntoView()
        }
    }
}

@Composable
private fun MeasurementGroups(input: @Composable (MeasurementField) -> Unit) {
    listOf(
        "Medida do corpo" to listOf(MeasurementField.Waist),
        "Escolhas da peça" to listOf(MeasurementField.Ease, MeasurementField.Length),
        "Margens de corte" to
            listOf(MeasurementField.WaistSeamAllowance, MeasurementField.HemAllowance),
    ).forEach { (title, fields) ->
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                fields.forEach { input(it) }
            }
        }
    }
}

private fun measurementGuidance(field: MeasurementField): String = when (field) {
    MeasurementField.Waist ->
        "Meça a circunferência da cintura na posição desejada do cós, " +
            "sem acrescentar folga ou margens."

    MeasurementField.Length ->
        "Meça o comprimento até a barra acabada, " +
            "a partir da linha de costura da cintura; exclua a altura do cós. As margens são informadas separadamente."

    MeasurementField.Ease ->
        "Folga é o acréscimo à cintura para vestir; informe zero se não desejar folga."

    MeasurementField.WaistSeamAllowance ->
        "Tecido reservado para costurar a cintura; informe zero se não houver margem."

    MeasurementField.HemAllowance ->
        "Tecido reservado para acabar a barra; informe zero se não houver margem."
}

// The screen title lives in the top app bar.
@Composable
private fun CalculatorIntro() {
    Text(
        "Medidas em centímetros. Informe folga e margens explicitamente; " +
            "zero é válido.",
    )
}

@Composable
private fun CalculationOutcome(
    state: CalculatorState,
    onOpenTutorial: () -> Unit,
    modifier: Modifier = Modifier,
    onEditMeasurements: () -> Unit,
) {
    state.calculationError?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
        )
    }
    state.radii?.let {
        val card = cuttingCard(it)
        CuttingCard(card, modifier)
        FabricFoldingTutorialEntry(onOpenTutorial)
        SkirtResults(state.inputs, onEditMeasurements)
        DraftingExplanation(state.inputs, it, card)
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
    onUseProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = state.errors[field]
    var wasFocused by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (field == MeasurementField.Waist) WaistSource(state.sourceProfileName, onUseProfile)
        OutlinedTextField(
            value = state.inputs.getValue(field),
            onValueChange = { onEdit(field, it) },
            label = { Text(field.label) },
            isError = error != null,
            supportingText = {
                Text(
                    error ?: measurementGuidance(field),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics {
                        if (error != null) liveRegion = LiveRegionMode.Polite
                    },
                )
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
}

/** States where the waist value came from, beside the action that copies it from a profile. */
@Composable
private fun WaistSource(sourceProfileName: String?, onUseProfile: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            sourceProfileName?.let { "Cintura copiada do perfil: $it" } ?: "Medida manual",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onUseProfile, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Usar perfil")
        }
    }
}

@Composable
private fun SkirtResults(inputs: Map<MeasurementField, String>, onEditMeasurements: () -> Unit) {
    TextButton(onClick = onEditMeasurements, modifier = Modifier.heightIn(min = 48.dp)) {
        Text("Editar medidas")
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Medidas utilizadas (cm)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        measurementSummary(
            inputs.mapValues { requireNotNull(parseMeasurement(it.value)) },
        ).forEach { Text(it) }
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Limites do cálculo",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "Para saia godê inteira em tecido plano, com cós e fechamento. " +
                    "Não inclui divisão em painéis, margens do fechamento, molde do cós " +
                    "ou metragem de tecido. O comprimento acabado exclui a altura do cós.",
            )
        }
    }
}

@Composable
private fun AdaptiveCalculator(
    state: CalculatorState,
    modifier: Modifier,
    form: @Composable () -> Unit,
    outcome: @Composable () -> Unit,
) {
    AdaptiveContent(
        modifier = modifier.imePadding(),
        detailOpen = false,
        primary = {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) { form() }
        },
        detail = {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (state.radii == null) Text("Preencha as medidas e toque em Calcular.")
                outcome()
            }
        },
        compact = {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                form()
                outcome()
            }
        },
    )
}
