package com.almeidajr.molda.skirt.ui

import com.almeidajr.molda.R
import com.almeidajr.molda.measurement.lengthText
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii

/** Everything a skirt result shows as text, built from a draft and its radii. */
data class SkirtResult(
    val card: List<CuttingCardGroup>,
    val diagramDescription: String,
    val measurementSummary: List<String>,
    val allowanceDirections: String,
    val formulas: List<String>,
    val coincidenceNotes: List<String>,
)

/**
 * The result of [draft] on [radii], taken as given and never recalculated, with its copy resolved
 * by [text]. The explanation and formulas show each value as a sewist would type it.
 */
fun skirtResult(draft: SkirtDraft, radii: SkirtRadii, text: (Int) -> String): SkirtResult {
    val card = cuttingCard(radii, text)
    val seam = lengthText(draft.waistSeamAllowance)
    val hem = lengthText(draft.hemAllowance)
    val allowanceDirections = text(R.string.skirt_allowance_directions).format(PtBr, seam, hem)
    val coincidenceNotes = coincidenceNotes(card, text)
    return SkirtResult(
        card = card,
        diagramDescription = diagramDescription(card, allowanceDirections, coincidenceNotes, text),
        measurementSummary = measurementSummary(draft, text),
        allowanceDirections = allowanceDirections,
        formulas = formulas(draft, radii, text),
        coincidenceNotes = coincidenceNotes,
    )
}

private fun formulas(draft: SkirtDraft, radii: SkirtRadii, text: (Int) -> String): List<String> {
    fun line(id: Int, vararg values: String) = text(id).format(PtBr, *values)
    val length = lengthText(draft.finishedLength)
    return listOf(
        text(R.string.skirt_formulas_unit),
        text(R.string.skirt_formula_waist_stitching),
        line(
            R.string.skirt_formula_waist_stitching_values,
            lengthText(draft.waist),
            lengthText(draft.ease),
            formatCentimetres(radii.waistStitching),
        ),
        line(R.string.skirt_formula_finished_hem, length, formatCentimetres(radii.finishedHem)),
        line(
            R.string.skirt_formula_waist_cutting,
            lengthText(draft.waistSeamAllowance),
            formatCentimetres(radii.waistCutting),
        ),
        line(
            R.string.skirt_formula_hem_cutting,
            length,
            lengthText(draft.hemAllowance),
            formatCentimetres(radii.hemCutting),
        ),
        text(R.string.skirt_formulas_rounding),
    )
}
