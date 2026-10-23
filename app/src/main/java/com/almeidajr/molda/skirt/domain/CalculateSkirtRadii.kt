package com.almeidajr.molda.skirt.domain

import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.measurement.ParsedLength
import com.almeidajr.molda.measurement.parseLength
import kotlin.math.PI
import kotlinx.serialization.Serializable

/**
 * A full-circle skirt draft: the waist (a body measurement) plus the drafting assumptions.
 * The waist and finished length must be positive; zero ease and allowances are valid.
 */
@Serializable
data class SkirtDraft(
    val waist: Length,
    val ease: Length,
    val finishedLength: Length,
    val waistSeamAllowance: Length,
    val hemAllowance: Length,
) {
    init {
        require(waist.centimetres > 0.0 && finishedLength.centimetres > 0.0) {
            "A skirt draft needs a positive waist and finished length"
        }
    }

    fun lengthOf(field: SkirtDraftField): Length = when (field) {
        SkirtDraftField.Waist -> waist
        SkirtDraftField.Ease -> ease
        SkirtDraftField.FinishedLength -> finishedLength
        SkirtDraftField.WaistSeamAllowance -> waistSeamAllowance
        SkirtDraftField.HemAllowance -> hemAllowance
    }
}

/** The draft's fields, in form order. */
enum class SkirtDraftField(val mustBePositive: Boolean = false) {
    Waist(mustBePositive = true),
    Ease,
    FinishedLength(mustBePositive = true),
    WaistSeamAllowance,
    HemAllowance,
}

enum class SkirtFieldError {
    Blank,
    NotANumber,
    NotPositive,
    Negative,

    /** Only on [SkirtDraftField.WaistSeamAllowance], once every field holds a valid length. */
    SeamAllowanceNotSmallerThanWaistStitching,
}

@Serializable
data class SkirtRadii(
    val waistStitching: Length,
    val finishedHem: Length,
    val waistCutting: Length,
    val hemCutting: Length,
)

/** What a skirt draft's calculation gives the sewist: exactly one of its three outcomes. */
sealed interface SkirtCalculation {
    data class Invalid(val errors: Map<SkirtDraftField, SkirtFieldError>) : SkirtCalculation

    /** The inputs are valid, but the radii overflow. */
    data object NonfiniteResult : SkirtCalculation

    data class Success(val draft: SkirtDraft, val radii: SkirtRadii) : SkirtCalculation
}

/** Calculates the form's field texts (missing fields count as blank). */
fun calculateSkirt(inputs: Map<SkirtDraftField, String>): SkirtCalculation {
    val texts = SkirtDraftField.entries.associateWith { inputs[it].orEmpty() }
    val errors = texts.mapNotNull { (field, text) ->
        skirtFieldError(field, text)?.let { field to it }
    }.toMap()
    if (errors.isNotEmpty()) return SkirtCalculation.Invalid(errors)
    val lengths = texts.mapValues { (parseLength(it.value) as ParsedLength.Valid).length }
    return calculateSkirtRadii(
        SkirtDraft(
            waist = lengths.getValue(SkirtDraftField.Waist),
            ease = lengths.getValue(SkirtDraftField.Ease),
            finishedLength = lengths.getValue(SkirtDraftField.FinishedLength),
            waistSeamAllowance = lengths.getValue(SkirtDraftField.WaistSeamAllowance),
            hemAllowance = lengths.getValue(SkirtDraftField.HemAllowance),
        ),
    )
}

private fun skirtFieldError(field: SkirtDraftField, text: String): SkirtFieldError? {
    val parsed = parseLength(text)
    return when {
        text.isBlank() -> SkirtFieldError.Blank

        parsed is ParsedLength.Malformed -> SkirtFieldError.NotANumber

        field.mustBePositive &&
            (parsed !is ParsedLength.Valid || parsed.length.centimetres <= 0.0) ->
            SkirtFieldError.NotPositive

        parsed is ParsedLength.Negative -> SkirtFieldError.Negative

        else -> null
    }
}

/** Calculates radii in centimetres without rounding intermediate values. */
fun calculateSkirtRadii(draft: SkirtDraft): SkirtCalculation {
    val waistStitching = (draft.waist.centimetres + draft.ease.centimetres) / (2 * PI)
    val finishedHem = waistStitching + draft.finishedLength.centimetres
    val waistCutting = waistStitching - draft.waistSeamAllowance.centimetres
    val hemCutting = finishedHem + draft.hemAllowance.centimetres
    return when {
        listOf(waistStitching, finishedHem, waistCutting, hemCutting).any { !it.isFinite() } ->
            SkirtCalculation.NonfiniteResult

        waistCutting <= 0.0 -> SkirtCalculation.Invalid(
            mapOf(
                SkirtDraftField.WaistSeamAllowance to
                    SkirtFieldError.SeamAllowanceNotSmallerThanWaistStitching,
            ),
        )

        else -> SkirtCalculation.Success(
            draft,
            SkirtRadii(
                waistStitching = Length(waistStitching),
                finishedHem = Length(finishedHem),
                waistCutting = Length(waistCutting),
                hemCutting = Length(hemCutting),
            ),
        )
    }
}
