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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almeidajr.molda.AdaptiveContent
import com.almeidajr.molda.MoldaRoute
import com.almeidajr.molda.skirt.domain.SkirtRadii
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun SkirtCalculatorRoute(
    modifier: Modifier = Modifier,
    viewModel: SkirtCalculatorViewModel = viewModel(),
) {
    MoldaRoute(modifier = modifier, calculator = viewModel)
}

// `profiles` is a named slot for the profile picker, not the screen's content.
@Suppress("ComposableLambdaParameterNaming")
@Composable
fun SkirtCalculatorScreen(
    state: CalculatorState,
    onEdit: (MeasurementField, String) -> Unit,
    onCalculate: () -> Unit,
    modifier: Modifier = Modifier,
    onBlur: (MeasurementField) -> Unit = {},
    onUseProfile: () -> Unit = {},
    profiles: @Composable () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val focusRequesters = remember { MeasurementField.entries.associateWith { FocusRequester() } }
    val bringRequesters =
        remember { MeasurementField.entries.associateWith { BringIntoViewRequester() } }
    val scope = rememberCoroutineScope()
    val resultRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(state.calculationAttempt) {
        if (state.radii != null) resultRequester.bringIntoView()
        MeasurementField.entries.firstOrNull { it in state.errors }?.let { field ->
            focusRequesters.getValue(field).requestFocus()
            bringRequesters.getValue(field).bringIntoView()
        }
    }
    val form: @Composable () -> Unit = {
        profiles()
        CalculatorHeading()
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
        Button(onClick = {
            focusManager.clearFocus()
            onCalculate()
        }) { Text("Calcular") }
    }
    val outcome: @Composable () -> Unit = {
        CalculationOutcome(state, Modifier.bringIntoViewRequester(resultRequester)) {
            focusRequesters.getValue(MeasurementField.Waist).requestFocus()
            scope.launch { bringRequesters.getValue(MeasurementField.Waist).bringIntoView() }
        }
    }
    AdaptiveCalculator(state, modifier, form, outcome)
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

@Composable
private fun CalculatorHeading() {
    Text(
        "Saia godê inteira",
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        "Medidas em centímetros. Informe folga e margens explicitamente; " +
            "zero é válido.",
    )
}

@Composable
private fun CalculationOutcome(
    state: CalculatorState,
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
        SkirtResults(it, state.inputs, onEditMeasurements, modifier)
        FabricFoldingTutorial(it)
        DraftingExplanation(state.inputs, it)
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
    if (field == MeasurementField.Waist) {
        TextButton(onClick = onUseProfile) { Text("Usar perfil") }
        Text(
            state.sourceProfileName?.let { "Cintura copiada do perfil: $it" } ?: "Medida manual",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SkirtResults(
    radii: SkirtRadii,
    inputs: Map<MeasurementField, String>,
    onEditMeasurements: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        "Raios para o molde",
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.semantics {
            heading()
            liveRegion = LiveRegionMode.Polite
        },
    )
    RadiusGroup(
        "Linhas de referência",
        "Raio da linha de costura da cintura: ${formatRadius(radii.waistStitching)}",
        "Raio da barra acabada: ${formatRadius(radii.finishedHem)}",
    )
    RadiusGroup(
        "Linhas de corte",
        "Raio de corte da cintura: ${formatRadius(radii.waistCutting)}",
        "Raio de corte da barra: ${formatRadius(radii.hemCutting)}",
    )
    TextButton(onClick = onEditMeasurements) { Text("Editar medidas") }
    Text(
        "Medidas utilizadas",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() },
    )
    MeasurementField.entries.forEach { field ->
        Text(
            "${field.label}: ${formatRadius(
                requireNotNull(parseMeasurement(inputs.getValue(field))),
            )}",
        )
    }
    Text(
        "Para saia godê inteira em tecido plano, com cós e fechamento. " +
            "Não inclui divisão em painéis, margens do fechamento, molde do cós " +
            "ou metragem de tecido. O comprimento acabado exclui a altura do cós.",
    )
}

@Composable
private fun RadiusGroup(title: String, first: String, second: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(first, style = MaterialTheme.typography.headlineSmall)
            Text(second, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
internal fun formatRadius(value: Double): String =
    String.format(Locale.forLanguageTag("pt-BR"), "%.2f cm", value)

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
