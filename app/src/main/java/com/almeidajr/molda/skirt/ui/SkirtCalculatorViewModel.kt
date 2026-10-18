package com.almeidajr.molda.skirt.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.flow.update

/** [draft] and [radii] are both set after a successful calculation, and only then. */
data class CalculatorState(
    val inputs: Map<SkirtDraftField, String> = SkirtDraftField.entries.associateWith { "" },
    val errors: Map<SkirtDraftField, SkirtFieldError> = emptyMap(),
    val hasNonfiniteResult: Boolean = false,
    val draft: SkirtDraft? = null,
    val radii: SkirtRadii? = null,
    val sourceProfileName: String? = null,
    val hasAttemptedCalculation: Boolean = false,
    val calculationAttempt: Int = 0,
)

@HiltViewModel
class SkirtCalculatorViewModel @Inject constructor(private val savedState: SavedStateHandle) :
    ViewModel() {
    private val mutableState = MutableStateFlow(
        CalculatorState(
            inputs = SkirtDraftField.entries.associateWith { savedState[it.name] ?: "" },
            sourceProfileName = savedState["sourceProfileName"],
            hasAttemptedCalculation = savedState["hasAttemptedCalculation"] ?: false,
        ),
    )
    val state: StateFlow<CalculatorState> = mutableState.asStateFlow()

    fun selectProfile(waist: String, name: String) {
        edit(SkirtDraftField.Waist, waist)
        savedState["sourceProfileName"] = name
        mutableState.update { it.copy(sourceProfileName = name) }
    }

    fun edit(field: SkirtDraftField, text: String) {
        savedState[field.name] = text
        val isWaist = field == SkirtDraftField.Waist
        if (isWaist) savedState["sourceProfileName"] = null
        mutableState.update {
            it.copy(
                inputs = it.inputs + (field to text),
                sourceProfileName = if (isWaist) null else it.sourceProfileName,
                hasNonfiniteResult = false,
                draft = null,
                radii = null,
            )
        }
    }

    fun blur(field: SkirtDraftField) {
        mutableState.update { current ->
            if (!current.hasAttemptedCalculation) return@update current
            val error = skirtFieldError(field, current.inputs.getValue(field))
                ?: seamAllowanceError(field, current.inputs)
            current.copy(
                errors = if (error == null) {
                    current.errors - field
                } else {
                    current.errors + (field to error)
                },
            )
        }
    }

    fun calculate() {
        savedState["hasAttemptedCalculation"] = true
        val current = mutableState.value.copy(
            hasAttemptedCalculation = true,
            calculationAttempt = mutableState.value.calculationAttempt + 1,
            errors = emptyMap(),
            hasNonfiniteResult = false,
            draft = null,
            radii = null,
        )
        mutableState.value = when (val reading = readSkirtDraft(current.inputs)) {
            is SkirtDraftReading.Invalid -> current.copy(errors = reading.errors)

            is SkirtDraftReading.Valid -> when (val result = calculateSkirtRadii(reading.draft)) {
                is SkirtCalculation.Success ->
                    current.copy(draft = reading.draft, radii = result.radii)

                SkirtCalculation.SeamAllowanceNotSmallerThanWaistStitching -> current.copy(
                    errors = mapOf(
                        SkirtDraftField.WaistSeamAllowance to
                            SkirtFieldError.SeamAllowanceNotSmallerThanWaistStitching,
                    ),
                )

                SkirtCalculation.NonfiniteResult -> current.copy(hasNonfiniteResult = true)
            }
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
