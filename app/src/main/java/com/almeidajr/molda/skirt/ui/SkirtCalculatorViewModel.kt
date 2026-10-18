package com.almeidajr.molda.skirt.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.almeidajr.molda.skirt.domain.SkirtCalculation
import com.almeidajr.molda.skirt.domain.SkirtMeasurements
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.skirt.domain.calculateSkirtRadii
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class MeasurementField(val label: String, val requiresPositive: Boolean = false) {
    Waist("Circunferência da cintura (cm)", true),
    Ease("Folga de cintura (cm)"),
    Length("Comprimento acabado (cm)", true),
    WaistSeamAllowance("Margem de costura da cintura (cm)"),
    HemAllowance("Margem de barra (cm)"),
}

data class CalculatorState(
    val inputs: Map<MeasurementField, String> = MeasurementField.entries.associateWith { "" },
    val errors: Map<MeasurementField, String> = emptyMap(),
    val calculationError: String? = null,
    val radii: SkirtRadii? = null,
    val sourceProfileName: String? = null,
    val hasAttemptedCalculation: Boolean = false,
    val calculationAttempt: Int = 0,
)

/** Accepts a decimal comma or point, without grouping, exponents or partial parsing. */
internal fun parseMeasurement(text: String): Double? {
    val trimmed = text.trim()
    if (!DecimalMeasurement.matches(trimmed)) return null
    return trimmed.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
}

private val DecimalMeasurement = Regex("[+-]?[0-9]+([,.][0-9]+)?")

@HiltViewModel
class SkirtCalculatorViewModel @Inject constructor(private val savedState: SavedStateHandle) :
    ViewModel() {
    private val mutableState = MutableStateFlow(
        CalculatorState(
            inputs = MeasurementField.entries.associateWith { savedState[it.name] ?: "" },
            sourceProfileName = savedState["sourceProfileName"],
            hasAttemptedCalculation = savedState["hasAttemptedCalculation"] ?: false,
        ),
    )
    val state: StateFlow<CalculatorState> = mutableState.asStateFlow()

    fun selectProfile(waist: String, name: String) {
        edit(MeasurementField.Waist, waist)
        savedState["sourceProfileName"] = name
        mutableState.update { it.copy(sourceProfileName = name) }
    }

    fun edit(field: MeasurementField, text: String) {
        savedState[field.name] = text
        if (field == MeasurementField.Waist) savedState["sourceProfileName"] = null
        mutableState.update {
            it.copy(
                inputs = it.inputs + (field to text),
                sourceProfileName = if (field ==
                    MeasurementField.Waist
                ) {
                    null
                } else {
                    it.sourceProfileName
                },
                calculationError = null,
                radii = null,
            )
        }
    }

    fun blur(field: MeasurementField) {
        mutableState.update { current ->
            if (!current.hasAttemptedCalculation) return@update current
            val error =
                measurementError(field, current.inputs.getValue(field))
                    ?: cuttingMarginError(field, current.inputs)
            current.copy(
                errors = if (error ==
                    null
                ) {
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
        )
        val values = current.inputs.mapValues { parseMeasurement(it.value) }
        val errors = MeasurementField.entries.mapNotNull { field ->
            measurementError(field, current.inputs.getValue(field))?.let { field to it }
        }.toMap()
        if (errors.isNotEmpty()) {
            mutableState.value =
                current.copy(errors = errors, radii = null, calculationError = null)
            return
        }
        val measurements = skirtMeasurements(values)
        mutableState.value = when (val result = calculateSkirtRadii(measurements)) {
            is SkirtCalculation.Success -> current.copy(
                errors = emptyMap(),
                calculationError = null,
                radii = result.radii,
            )

            SkirtCalculation.NonpositiveWaistCutting -> current.copy(
                errors = mapOf(
                    MeasurementField.WaistSeamAllowance to
                        "A margem deve ser menor que o raio de costura da cintura.",
                ),
                calculationError = null,
                radii = null,
            )

            SkirtCalculation.NonfiniteResult, SkirtCalculation.InvalidMeasurements -> current.copy(
                errors = emptyMap(),
                calculationError = "Não foi possível calcular raios finitos. Revise as medidas.",
                radii = null,
            )
        }
    }
}

private fun measurementError(field: MeasurementField, text: String): String? {
    val value = parseMeasurement(text)
    return when {
        text.isBlank() -> "Informe um valor, inclusive zero para folgas e margens."
        value == null -> "Informe um número finito, como 80,5, sem separador de milhar."
        field.requiresPositive && value <= 0.0 -> "Informe um valor maior que zero."
        value < 0.0 -> "Informe zero ou um valor positivo."
        else -> null
    }
}

private fun cuttingMarginError(
    field: MeasurementField,
    inputs: Map<MeasurementField, String>,
): String? {
    if (field != MeasurementField.WaistSeamAllowance ||
        inputs.any { (inputField, text) -> measurementError(inputField, text) != null }
    ) {
        return null
    }
    val values = inputs.mapValues { parseMeasurement(it.value) }
    return if (calculateSkirtRadii(skirtMeasurements(values)) ==
        SkirtCalculation.NonpositiveWaistCutting
    ) {
        "A margem deve ser menor que o raio de costura da cintura."
    } else {
        null
    }
}

private fun skirtMeasurements(values: Map<MeasurementField, Double?>): SkirtMeasurements =
    SkirtMeasurements(
        waist = requireNotNull(values[MeasurementField.Waist]),
        ease = requireNotNull(values[MeasurementField.Ease]),
        length = requireNotNull(values[MeasurementField.Length]),
        waistSeamAllowance = requireNotNull(values[MeasurementField.WaistSeamAllowance]),
        hemAllowance = requireNotNull(values[MeasurementField.HemAllowance]),
    )
