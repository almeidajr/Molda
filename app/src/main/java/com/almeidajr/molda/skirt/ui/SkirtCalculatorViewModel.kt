package com.almeidajr.molda.skirt.ui

import androidx.lifecycle.ViewModel
import com.almeidajr.molda.skirt.domain.SkirtCalculation
import com.almeidajr.molda.skirt.domain.SkirtMeasurements
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.skirt.domain.calculateSkirtRadii
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
)

/** Accepts a decimal comma or point, without grouping, exponents or partial parsing. */
internal fun parseMeasurement(text: String): Double? {
    val trimmed = text.trim()
    if (!DecimalMeasurement.matches(trimmed)) return null
    return trimmed.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
}

private val DecimalMeasurement = Regex("[+-]?[0-9]+([,.][0-9]+)?")

class SkirtCalculatorViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = mutableState.asStateFlow()

    fun edit(field: MeasurementField, text: String) {
        mutableState.update {
            it.copy(
                inputs = it.inputs + (field to text),
                errors = it.errors - field,
                calculationError = null,
                radii = null,
            )
        }
    }

    fun calculate() {
        val current = mutableState.value
        val values = current.inputs.mapValues { parseMeasurement(it.value) }
        val errors = MeasurementField.entries.mapNotNull { field ->
            val value = values[field]
            val error = when {
                current.inputs.getValue(
                    field,
                ).isBlank() -> "Informe um valor, inclusive zero para folgas e margens."

                value == null -> "Informe um número finito, como 80,5, sem separador de milhar."

                field.requiresPositive && value <= 0.0 -> "Informe um valor maior que zero."

                value < 0.0 -> "Informe zero ou um valor positivo."

                else -> null
            }
            error?.let { field to it }
        }.toMap()
        if (errors.isNotEmpty()) {
            mutableState.value =
                current.copy(errors = errors, radii = null, calculationError = null)
            return
        }
        val measurements = SkirtMeasurements(
            waist = requireNotNull(values[MeasurementField.Waist]),
            ease = requireNotNull(values[MeasurementField.Ease]),
            length = requireNotNull(values[MeasurementField.Length]),
            waistSeamAllowance = requireNotNull(values[MeasurementField.WaistSeamAllowance]),
            hemAllowance = requireNotNull(values[MeasurementField.HemAllowance]),
        )
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
