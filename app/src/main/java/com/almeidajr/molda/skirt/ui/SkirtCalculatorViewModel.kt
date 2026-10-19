package com.almeidajr.molda.skirt.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import com.almeidajr.molda.measurement.MeasurementSource
import com.almeidajr.molda.measurement.ParsedLength
import com.almeidajr.molda.measurement.lengthText
import com.almeidajr.molda.measurement.parseLength
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.skirt.domain.SkirtCalculation
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.skirt.domain.SkirtDraftReading
import com.almeidajr.molda.skirt.domain.SkirtFieldError
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.skirt.domain.calculateSkirtRadii
import com.almeidajr.molda.skirt.domain.readSkirtDraft
import com.almeidajr.molda.skirt.domain.skirtFieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/** The calculator state that survives process death. */
@Serializable
data class CalculatorForm(
    val inputs: Map<SkirtDraftField, String> = SkirtDraftField.entries.associateWith { "" },
    /** The profile the waist came from, or null when it was typed by hand. */
    val source: MeasurementSource? = null,
    val hasAttemptedCalculation: Boolean = false,
    /** Whether a result for these inputs is shown, so a restored form recalculates it. */
    val isCalculated: Boolean = false,
    /** The values a saved draft was loaded with, or null when none was loaded. */
    val loaded: DraftValues? = null,
)

/** A form's inputs and the profile its waist came from. */
@Serializable
data class DraftValues(val inputs: Map<SkirtDraftField, String>, val source: MeasurementSource?)

private val CalculatorForm.values: DraftValues
    get() = DraftValues(inputs, source)

/** [draft] and [radii] are both set after a successful calculation, and only then. */
data class CalculatorState(
    val form: CalculatorForm = CalculatorForm(),
    val errors: Map<SkirtDraftField, SkirtFieldError> = emptyMap(),
    val hasNonfiniteResult: Boolean = false,
    val draft: SkirtDraft? = null,
    val radii: SkirtRadii? = null,
    val calculationAttempt: Int = 0,
) {
    /** Whether the inputs' values or their source differ from the loaded saved draft. */
    val isDirty: Boolean get() = form.loaded?.let { !it.hasSameValues(form.values) } == true
}

// "80" and "80,0" are the same length; text that is not a length compares as typed.
private fun DraftValues.hasSameValues(other: DraftValues): Boolean = source == other.source &&
    SkirtDraftField.entries.all { field ->
        val text = inputs[field].orEmpty()
        val otherText = other.inputs[field].orEmpty()
        when (val length = parseLength(text)) {
            is ParsedLength.Valid -> length == parseLength(otherText)
            else -> text == otherText
        }
    }

/** The calculated result as a project snapshot, or null before a successful calculation. */
fun CalculatorState.snapshot(): SkirtSnapshot? {
    val draft = draft
    val radii = radii
    if (draft == null || radii == null) return null
    return SkirtSnapshot(draft, radii, form.source)
}

@HiltViewModel
class SkirtCalculatorViewModel @Inject constructor(savedState: SavedStateHandle) : ViewModel() {
    private var savedForm by savedState.saved { CalculatorForm() }
    private val mutableState = MutableStateFlow(CalculatorState(form = savedForm).restored())
    val state: StateFlow<CalculatorState> = mutableState.asStateFlow()

    /** Uses the [waist] taken from the profile [source]. */
    fun selectProfile(waist: String, source: MeasurementSource) =
        transition { it.withProfileWaist(waist, source) }

    /** Replaces the form with a saved [draft] and its [source], then calculates it. */
    fun load(draft: SkirtDraft, source: MeasurementSource?) = transition {
        val form = CalculatorForm(
            inputs = SkirtDraftField.entries.associateWith { field ->
                lengthText(draft.lengthOf(field))
            },
            source = source,
        )
        CalculatorState(form.copy(loaded = form.values)).calculated()
    }

    fun edit(field: SkirtDraftField, text: String) = transition { it.edited(field, text) }

    fun blur(field: SkirtDraftField) = transition { it.blurred(field) }

    fun calculate() = transition { it.calculated() }

    // Main-thread only, so reading back the new value cannot race another transition.
    private fun transition(next: (CalculatorState) -> CalculatorState) {
        mutableState.value = next(mutableState.value)
        savedForm = mutableState.value.form
    }
}

private fun CalculatorState.withProfileWaist(
    waist: String,
    source: MeasurementSource,
): CalculatorState {
    val edited = edited(SkirtDraftField.Waist, waist)
    return edited.copy(form = edited.form.copy(source = source))
}

private fun CalculatorState.edited(field: SkirtDraftField, text: String): CalculatorState = copy(
    form = form.copy(
        inputs = form.inputs + (field to text),
        isCalculated = false,
        source = form.source.takeIf { field != SkirtDraftField.Waist },
    ),
    hasNonfiniteResult = false,
    draft = null,
    radii = null,
)

/** The state restored from a saved [CalculatorState.form], with its result recalculated. */
private fun CalculatorState.restored(): CalculatorState =
    if (form.isCalculated) calculated().copy(calculationAttempt = calculationAttempt) else this

/** After the first calculation attempt, revalidates [field] as the user leaves it. */
private fun CalculatorState.blurred(field: SkirtDraftField): CalculatorState {
    if (!form.hasAttemptedCalculation) return this
    val error = skirtFieldError(field, form.inputs.getValue(field))
        ?: seamAllowanceError(field, form.inputs)
    return copy(errors = if (error == null) errors - field else errors + (field to error))
}

private fun CalculatorState.calculated(): CalculatorState {
    val attempt = copy(
        form = form.copy(hasAttemptedCalculation = true, isCalculated = false),
        calculationAttempt = calculationAttempt + 1,
        errors = emptyMap(),
        hasNonfiniteResult = false,
        draft = null,
        radii = null,
    )
    return when (val reading = readSkirtDraft(form.inputs)) {
        is SkirtDraftReading.Invalid -> attempt.copy(errors = reading.errors)

        is SkirtDraftReading.Valid -> when (val result = calculateSkirtRadii(reading.draft)) {
            is SkirtCalculation.Success -> attempt.copy(
                form = attempt.form.copy(isCalculated = true),
                draft = reading.draft,
                radii = result.radii,
            )

            SkirtCalculation.SeamAllowanceNotSmallerThanWaistStitching -> attempt.copy(
                errors = mapOf(
                    SkirtDraftField.WaistSeamAllowance to
                        SkirtFieldError.SeamAllowanceNotSmallerThanWaistStitching,
                ),
            )

            SkirtCalculation.NonfiniteResult -> attempt.copy(hasNonfiniteResult = true)
        }
    }
}

private fun seamAllowanceError(
    field: SkirtDraftField,
    inputs: Map<SkirtDraftField, String>,
): SkirtFieldError? {
    val reading = readSkirtDraft(inputs)
    return SkirtFieldError.SeamAllowanceNotSmallerThanWaistStitching.takeIf {
        field == SkirtDraftField.WaistSeamAllowance &&
            reading is SkirtDraftReading.Valid &&
            calculateSkirtRadii(reading.draft) ==
            SkirtCalculation.SeamAllowanceNotSmallerThanWaistStitching
    }
}
