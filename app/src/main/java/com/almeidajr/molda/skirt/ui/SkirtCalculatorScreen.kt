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
import androidx.compose.ui.platform.LocalResources
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
        ) { Text(stringResource(R.string.skirt_calculate)) }
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
        R.string.skirt_group_body to listOf(SkirtDraftField.Waist),
        R.string.skirt_group_choices to
            listOf(SkirtDraftField.Ease, SkirtDraftField.FinishedLength),
        R.string.skirt_group_allowances to
            listOf(SkirtDraftField.WaistSeamAllowance, SkirtDraftField.HemAllowance),
    ).forEach { (title, fields) ->
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                fields.forEach { input(it) }
            }
        }
    }
}

/** The form label of each draft field, with its unit. */
@get:StringRes
val SkirtDraftField.label: Int
    get() = when (this) {
        SkirtDraftField.Waist -> R.string.skirt_field_waist
        SkirtDraftField.Ease -> R.string.skirt_field_ease
        SkirtDraftField.FinishedLength -> R.string.skirt_field_finished_length
        SkirtDraftField.WaistSeamAllowance -> R.string.skirt_field_waist_seam_allowance
        SkirtDraftField.HemAllowance -> R.string.skirt_field_hem_allowance
    }

/** Each draft field's name in the measurement summary, whose heading states the unit. */
@get:StringRes
val SkirtDraftField.summaryLabel: Int
    get() = when (this) {
        SkirtDraftField.Waist -> R.string.skirt_summary_waist
        SkirtDraftField.Ease -> R.string.skirt_summary_ease
        SkirtDraftField.FinishedLength -> R.string.skirt_summary_finished_length
        SkirtDraftField.WaistSeamAllowance -> R.string.skirt_summary_waist_seam_allowance
        SkirtDraftField.HemAllowance -> R.string.skirt_summary_hem_allowance
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

@StringRes
private fun measurementGuidance(field: SkirtDraftField): Int = when (field) {
    SkirtDraftField.Waist -> R.string.skirt_guidance_waist
    SkirtDraftField.FinishedLength -> R.string.skirt_guidance_finished_length
    SkirtDraftField.Ease -> R.string.skirt_guidance_ease
    SkirtDraftField.WaistSeamAllowance -> R.string.skirt_guidance_waist_seam_allowance
    SkirtDraftField.HemAllowance -> R.string.skirt_guidance_hem_allowance
}

// The screen title lives in the top app bar.
@Composable
private fun CalculatorIntro() {
    Text(stringResource(R.string.skirt_intro))
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
        val card = cuttingCard(radii, LocalResources.current::getString)
        val description = diagramDescription(
            card,
            enteredValue(state.form.inputs, SkirtDraftField.WaistSeamAllowance),
            enteredValue(state.form.inputs, SkirtDraftField.HemAllowance),
        )
        CuttingCard(card, description, modifier)
        SkirtResults(draft, onOpenTutorial, onEditMeasurements)
        DraftingExplanation(state.form.inputs, radii)
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
        if (field == SkirtDraftField.Waist) WaistSource(state.form.sourceProfileName, onUseProfile)
        OutlinedTextField(
            value = state.form.inputs.getValue(field),
            onValueChange = { onEdit(field, it) },
            label = { Text(stringResource(field.label)) },
            isError = error != null,
            supportingText = {
                Text(
                    error?.let { stringResource(errorMessage(it)) }
                        ?: stringResource(measurementGuidance(field)),
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
            sourceProfileName?.let { stringResource(R.string.skirt_waist_from_profile, it) }
                ?: stringResource(R.string.skirt_waist_manual),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onUseProfile, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.profile_picker_title))
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
            Text(stringResource(R.string.skirt_edit_measurements))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.skirt_used_measurements),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        val summary = measurementSummary(draft, LocalResources.current::getString)
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
                stringResource(R.string.skirt_limits_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.skirt_limits_body))
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
                if (state.radii == null) Text(stringResource(R.string.skirt_fill_and_calculate))
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
