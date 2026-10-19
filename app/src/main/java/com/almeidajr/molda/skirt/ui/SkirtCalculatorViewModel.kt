package com.almeidajr.molda.skirt.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import com.almeidajr.molda.measurement.lengthText
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
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/** The calculator state that survives process death. */
@Serializable
data class CalculatorForm(
    val inputs: Map<SkirtDraftField, String> = SkirtDraftField.entries.associateWith { "" },
    val sourceProfileName: String? = null,
    /** The source profile's measurement date as ISO-8601 text, set with [sourceProfileName]. */
    val sourceMeasurementDate: String? = null,
    val hasAttemptedCalculation: Boolean = false,
    /** The values a saved draft was loaded with, or null when none was loaded. */
    val loaded: DraftValues? = null,
)

/** A form's inputs and the profile its waist came from. */
@Serializable
data class DraftValues(
    val inputs: Map<SkirtDraftField, String>,
    val sourceProfileName: String?,
    val sourceMeasurementDate: String?,
)

private val CalculatorForm.values: DraftValues
    get() = DraftValues(inputs, sourceProfileName, sourceMeasurementDate)

/** [draft] and [radii] are both set after a successful calculation, and only then. */
data class CalculatorState(
    val form: CalculatorForm = CalculatorForm(),
    val errors: Map<SkirtDraftField, SkirtFieldError> = emptyMap(),
    val hasNonfiniteResult: Boolean = false,
    val draft: SkirtDraft? = null,
    val radii: SkirtRadii? = null,
    val calculationAttempt: Int = 0,
) {
    /** Whether the inputs or their source differ from the loaded saved draft. */
    val isDirty: Boolean get() = form.loaded?.let { it != form.values } == true
}

@HiltViewModel
class SkirtCalculatorViewModel @Inject constructor(savedState: SavedStateHandle) : ViewModel() {
    private var savedForm by savedState.saved { CalculatorForm() }
    private val mutableState = MutableStateFlow(CalculatorState(form = savedForm))
    val state: StateFlow<CalculatorState> = mutableState.asStateFlow()

    fun selectProfile(waist: String, name: String, measurementDate: LocalDate) =
        transition { it.withProfileWaist(waist, name, measurementDate) }

    /** Replaces the form with a saved [draft] and its source, then calculates it. */
    fun load(draft: SkirtDraft, sourceProfileName: String?, sourceMeasurementDate: LocalDate?) =
        transition {
            val form = CalculatorForm(
                inputs = SkirtDraftField.entries.associateWith { field ->
                    lengthText(draft.lengthOf(field))
                },
                sourceProfileName = sourceProfileName,
                sourceMeasurementDate = sourceMeasurementDate?.toString(),
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
    name: String,
    measurementDate: LocalDate,
): CalculatorState {
    val edited = edited(SkirtDraftField.Waist, waist)
    return edited.copy(
        form = edited.form.copy(
            sourceProfileName = name,
            sourceMeasurementDate = measurementDate.toString(),
        ),
    )
}

private fun CalculatorState.edited(field: SkirtDraftField, text: String): CalculatorState = copy(
    form = form.copy(
        inputs = form.inputs + (field to text),
        sourceProfileName = if (field == SkirtDraftField.Waist) null else form.sourceProfileName,
        sourceMeasurementDate = if (field == SkirtDraftField.Waist) {
            null
        } else {
            form.sourceMeasurementDate
        },
    ),
    hasNonfiniteResult = false,
    draft = null,
    radii = null,
)

/** After the first calculation attempt, revalidates [field] as the user leaves it. */
private fun CalculatorState.blurred(field: SkirtDraftField): CalculatorState {
    if (!form.hasAttemptedCalculation) return this
    val error = skirtFieldError(field, form.inputs.getValue(field))
        ?: seamAllowanceError(field, form.inputs)
    return copy(errors = if (error == null) errors - field else errors + (field to error))
}

private fun CalculatorState.calculated(): CalculatorState {
    val attempt = copy(
        form = form.copy(hasAttemptedCalculation = true),
        calculationAttempt = calculationAttempt + 1,
        errors = emptyMap(),
        hasNonfiniteResult = false,
        draft = null,
        radii = null,
    )
    return when (val reading = readSkirtDraft(form.inputs)) {
        is SkirtDraftReading.Invalid -> attempt.copy(errors = reading.errors)

        is SkirtDraftReading.Valid -> when (val result = calculateSkirtRadii(reading.draft)) {
            is SkirtCalculation.Success -> attempt.copy(draft = reading.draft, radii = result.radii)

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
