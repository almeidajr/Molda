package com.almeidajr.molda.skirt.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.skirt.domain.SkirtFieldError
import com.almeidajr.molda.ui.adaptive.AdaptiveContent
import kotlinx.coroutines.launch

@Composable
fun SkirtCalculatorScreen(
    state: CalculatorState,
    onEdit: (SkirtDraftField, String) -> Unit,
    onCalculate: () -> Unit,
    modifier: Modifier = Modifier,
    onBlur: (SkirtDraftField) -> Unit = {},
    onUseProfile: () -> Unit = {},
    onOpenTutorial: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val focusRequesters = remember { SkirtDraftField.entries.associateWith { FocusRequester() } }
    val bringRequesters =
        remember { SkirtDraftField.entries.associateWith { BringIntoViewRequester() } }
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
                        SkirtDraftField.entries[field.ordinal + 1],
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
            focusRequesters.getValue(SkirtDraftField.Waist).requestFocus()
            scope.launch { bringRequesters.getValue(SkirtDraftField.Waist).bringIntoView() }
        }
    }
    AdaptiveCalculator(state, modifier, form, outcome)
}

/** After each Calculate, shows the cutting card or focuses the first invalid field. */
@Composable
private fun RevealCalculationOutcome(
    state: CalculatorState,
    resultRequester: BringIntoViewRequester,
    focusRequesters: Map<SkirtDraftField, FocusRequester>,
    bringRequesters: Map<SkirtDraftField, BringIntoViewRequester>,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    // Saved so returning from the tutorial keeps the scroll position instead of revealing again.
    var revealedAttempt by rememberSaveable { mutableIntStateOf(state.calculationAttempt) }
    LaunchedEffect(state.calculationAttempt) {
        if (state.calculationAttempt == revealedAttempt) return@LaunchedEffect
        revealedAttempt = state.calculationAttempt
        if (state.radii != null) {
            // Clearing focus alone leaves the keyboard covering the radii after IME Done.
            keyboard?.hide()
            resultRequester.bringIntoView()
        }
        SkirtDraftField.entries.firstOrNull { it in state.errors }?.let { field ->
            focusRequesters.getValue(field).requestFocus()
            bringRequesters.getValue(field).bringIntoView()
        }
    }
}

@Composable
private fun MeasurementGroups(input: @Composable (SkirtDraftField) -> Unit) {
    listOf(
        "Medida do corpo" to listOf(SkirtDraftField.Waist),
        "Escolhas da peça" to listOf(SkirtDraftField.Ease, SkirtDraftField.FinishedLength),
        "Margens de corte" to
            listOf(SkirtDraftField.WaistSeamAllowance, SkirtDraftField.HemAllowance),
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

/** The form label of each draft field. */
val SkirtDraftField.label: String
    get() = when (this) {
        SkirtDraftField.Waist -> "Circunferência da cintura (cm)"
        SkirtDraftField.Ease -> "Folga de cintura (cm)"
        SkirtDraftField.FinishedLength -> "Comprimento acabado (cm)"
        SkirtDraftField.WaistSeamAllowance -> "Margem de costura da cintura (cm)"
        SkirtDraftField.HemAllowance -> "Margem de barra (cm)"
    }

@StringRes
private fun errorMessage(error: SkirtFieldError): Int = when (error) {
    SkirtFieldError.Blank -> R.string.skirt_error_blank

    SkirtFieldError.NotANumber -> R.string.skirt_error_not_a_number

    SkirtFieldError.NotPositive -> R.string.skirt_error_not_positive

    SkirtFieldError.Negative -> R.string.skirt_error_negative

    SkirtFieldError.SeamAllowanceNotSmallerThanWaistStitching ->
        R.string.skirt_error_seam_allowance_too_large
}

private fun measurementGuidance(field: SkirtDraftField): String = when (field) {
    SkirtDraftField.Waist ->
        "Meça a circunferência da cintura na posição desejada do cós, " +
            "sem acrescentar folga ou margens."

    SkirtDraftField.FinishedLength ->
        "Meça o comprimento até a barra acabada, " +
            "a partir da linha de costura da cintura; exclua a altura do cós. As margens são informadas separadamente."

    SkirtDraftField.Ease ->
        "Folga é o acréscimo à cintura para vestir; informe zero se não desejar folga."

    SkirtDraftField.WaistSeamAllowance ->
        "Tecido reservado para costurar a cintura; informe zero se não houver margem."

    SkirtDraftField.HemAllowance ->
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
    if (state.hasNonfiniteResult) {
        Text(
            stringResource(R.string.skirt_error_nonfinite_result),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
        )
    }
    val draft = state.draft
    val radii = state.radii
    if (draft != null && radii != null) {
        val card = cuttingCard(radii)
        val description = diagramDescription(
            card,
            enteredValue(state.inputs, SkirtDraftField.WaistSeamAllowance),
            enteredValue(state.inputs, SkirtDraftField.HemAllowance),
        )
        CuttingCard(card, description, modifier)
        SkirtResults(draft, onOpenTutorial, onEditMeasurements)
        DraftingExplanation(state.inputs, radii)
    }
}

@Composable
private fun MeasurementInput(
    field: SkirtDraftField,
    state: CalculatorState,
    onEdit: (SkirtDraftField, String) -> Unit,
    onBlur: (SkirtDraftField) -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit,
    onFocused: () -> Unit,
    onUseProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = state.errors[field]
    var wasFocused by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (field == SkirtDraftField.Waist) WaistSource(state.sourceProfileName, onUseProfile)
        OutlinedTextField(
            value = state.inputs.getValue(field),
            onValueChange = { onEdit(field, it) },
            label = { Text(field.label) },
            isError = error != null,
            supportingText = {
                Text(
                    error?.let { stringResource(errorMessage(it)) } ?: measurementGuidance(field),
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
                    SkirtDraftField.HemAllowance
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
private fun SkirtResults(
    draft: SkirtDraft,
    onOpenTutorial: () -> Unit,
    onEditMeasurements: () -> Unit,
) {
    // One action row under the card; at large text the second button wraps below the first.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FabricFoldingTutorialEntry(onOpenTutorial)
        TextButton(onClick = onEditMeasurements, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Editar medidas")
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Medidas utilizadas (cm)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        val summary = measurementSummary(draft)
        // One wrapping line; TalkBack hears pauses instead of the visual separators.
        Text(
            summary.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { contentDescription = summary.joinToString("; ") },
        )
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
