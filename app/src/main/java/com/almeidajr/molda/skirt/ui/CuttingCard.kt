package com.almeidajr.molda.skirt.ui

import androidx.annotation.StringRes
import com.almeidajr.molda.R
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.skirt.ui.DraftingLine.FinishedHem
import com.almeidajr.molda.skirt.ui.DraftingLine.HemCutting
import com.almeidajr.molda.skirt.ui.DraftingLine.WaistCutting
import com.almeidajr.molda.skirt.ui.DraftingLine.WaistStitching
import java.util.Locale

/** Stroke patterns that tell the four drafting lines apart without relying on color. */
enum class LineStyle(@param:StringRes val description: Int) {
    Solid(R.string.skirt_line_style_solid),
    Dashed(R.string.skirt_line_style_dashed),
    Dotted(R.string.skirt_line_style_dotted),
    DashDot(R.string.skirt_line_style_dash_dot),
}

/** The four drafting lines, numbered as the diagram and the cutting card key them. */
enum class DraftingLine(val number: Int, val style: LineStyle, @param:StringRes val label: Int) {
    WaistStitching(1, LineStyle.Solid, R.string.skirt_line_waist_stitching),
    WaistCutting(2, LineStyle.Dashed, R.string.skirt_line_waist_cutting),
    FinishedHem(3, LineStyle.Dotted, R.string.skirt_line_finished_hem),
    HemCutting(4, LineStyle.DashDot, R.string.skirt_line_hem_cutting),
}

/**
 * One radius on the cutting card, keyed to the diagram by its [line].
 *
 * [value] keeps its unit behind a non-breaking space so they never wrap apart.
 * [coincidentLine] names the line drawn at the same radius when an allowance is zero.
 */
data class CuttingCardEntry(
    val line: DraftingLine,
    val label: String,
    val value: String,
    val coincidentLine: DraftingLine? = null,
)

data class CuttingCardGroup(val title: String, val entries: List<CuttingCardEntry>)

/**
 * Maps calculated radii to the reference-line group followed by the cutting-line group,
 * resolving its titles and labels with [text].
 */
fun cuttingCard(radii: SkirtRadii, text: (Int) -> String): List<CuttingCardGroup> {
    val waistCoincides = radii.waistCutting == radii.waistStitching
    val hemCoincides = radii.hemCutting == radii.finishedHem
    fun entry(line: DraftingLine, radius: Length, partner: DraftingLine, coincides: Boolean) =
        CuttingCardEntry(
            line,
            text(line.label),
            formatCentimetres(radius),
            partner.takeIf { coincides },
        )
    return listOf(
        CuttingCardGroup(
            text(R.string.skirt_reference_lines),
            listOf(
                entry(WaistStitching, radii.waistStitching, WaistCutting, waistCoincides),
                entry(FinishedHem, radii.finishedHem, HemCutting, hemCoincides),
            ),
        ),
        CuttingCardGroup(
            text(R.string.skirt_cutting_lines),
            listOf(
                entry(WaistCutting, radii.waistCutting, WaistStitching, waistCoincides),
                entry(HemCutting, radii.hemCutting, FinishedHem, hemCoincides),
            ),
        ),
    )
}

/** Two pt-BR decimals and "cm" joined by a non-breaking space, e.g. "13,05 cm". */
fun formatCentimetres(length: Length): String = "${formatDecimal(length.centimetres)} cm"

/** Two pt-BR decimals without grouping, e.g. "80,00". */
fun formatDecimal(value: Double): String = String.format(PtBr, "%.2f", value)

/** "Margem de … zero" notes for each pair of lines drawn at the same radius. */
fun coincidenceNotes(card: List<CuttingCardGroup>, text: (Int) -> String): List<String> = card
    .flatMap { it.entries }
    .filter { entry -> entry.coincidentLine?.let { it.number > entry.line.number } == true }
    .map { entry ->
        text(
            if (entry.line == WaistStitching) {
                R.string.skirt_waist_lines_coincide
            } else {
                R.string.skirt_hem_lines_coincide
            },
        )
    }

/**
 * The drafting diagram's text equivalent: its key, line order, coincidences, allowance
 * directions and limits, without repeating the radii already on the card.
 */
fun diagramDescription(
    card: List<CuttingCardGroup>,
    waistSeamAllowance: String,
    hemAllowance: String,
    text: (Int) -> String,
): String {
    val key = card.flatMap { it.entries }.sortedBy { it.line.number }.map { entry ->
        text(R.string.skirt_diagram_line_key).format(
            PtBr,
            entry.line.number,
            text(entry.line.style.description),
            entry.label.lowercase(),
        )
    }
    val allowances =
        text(R.string.skirt_allowance_directions).format(PtBr, waistSeamAllowance, hemAllowance)
    return (
        listOf(text(R.string.skirt_diagram_summary), text(R.string.skirt_diagram_line_order)) +
            key + coincidenceNotes(card, text) + allowances + text(R.string.skirt_diagram_limits)
        ).joinToString(" ")
}

/** The locale the copy is written in. */
internal val PtBr = Locale.forLanguageTag("pt-BR")

/**
 * "Label: value" items for the inputs used, whose unit the summary heading states once.
 * A non-breaking space keeps each value on its label's line when the summary wraps.
 */
fun measurementSummary(draft: SkirtDraft, text: (Int) -> String): List<String> =
    SkirtDraftField.entries.map { field ->
        "${text(field.summaryLabel)}: ${formatDecimal(draft.lengthOf(field).centimetres)}"
    }
