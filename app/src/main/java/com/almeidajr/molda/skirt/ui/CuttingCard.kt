package com.almeidajr.molda.skirt.ui

import androidx.annotation.StringRes
import com.almeidajr.molda.R
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.skirt.domain.SkirtRadii
import java.util.Locale

/** Stroke patterns that tell the four drafting lines apart without relying on color. */
enum class LineStyle(@param:StringRes val description: Int) {
    Solid(R.string.skirt_line_style_solid),
    Dashed(R.string.skirt_line_style_dashed),
    Dotted(R.string.skirt_line_style_dotted),
    DashDot(R.string.skirt_line_style_dash_dot),
}

/**
 * One radius on the cutting card, keyed to the diagram by [number] and [lineStyle].
 *
 * [value] keeps its unit behind a non-breaking space so they never wrap apart.
 * [coincidentLine] names the line drawn at the same radius when an allowance is zero.
 */
data class CuttingCardEntry(
    val number: Int,
    val lineStyle: LineStyle,
    val label: String,
    val value: String,
    val coincidentLine: Int? = null,
)

data class CuttingCardGroup(val title: String, val entries: List<CuttingCardEntry>)

/**
 * Maps calculated radii to the reference-line group followed by the cutting-line group,
 * resolving its titles and labels with [text].
 */
@Suppress("MagicNumber") // Diagram numbers 1–4 are the fixed key shared with the diagram.
fun cuttingCard(radii: SkirtRadii, text: (Int) -> String): List<CuttingCardGroup> {
    val waistCoincides = radii.waistCutting == radii.waistStitching
    val hemCoincides = radii.hemCutting == radii.finishedHem
    return listOf(
        CuttingCardGroup(
            text(R.string.skirt_reference_lines),
            listOf(
                CuttingCardEntry(
                    1,
                    LineStyle.Solid,
                    text(R.string.skirt_line_waist_stitching),
                    formatCentimetres(radii.waistStitching),
                    2.takeIf { waistCoincides },
                ),
                CuttingCardEntry(
                    3,
                    LineStyle.Dotted,
                    text(R.string.skirt_line_finished_hem),
                    formatCentimetres(radii.finishedHem),
                    4.takeIf { hemCoincides },
                ),
            ),
        ),
        CuttingCardGroup(
            text(R.string.skirt_cutting_lines),
            listOf(
                CuttingCardEntry(
                    2,
                    LineStyle.Dashed,
                    text(R.string.skirt_line_waist_cutting),
                    formatCentimetres(radii.waistCutting),
                    1.takeIf { waistCoincides },
                ),
                CuttingCardEntry(
                    4,
                    LineStyle.DashDot,
                    text(R.string.skirt_line_hem_cutting),
                    formatCentimetres(radii.hemCutting),
                    3.takeIf { hemCoincides },
                ),
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
    .filter { entry -> entry.coincidentLine?.let { it > entry.number } == true }
    .map { entry ->
        text(
            if (entry.number == 1) {
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
    val key = card.flatMap { it.entries }.sortedBy { it.number }.map { entry ->
        text(R.string.skirt_diagram_line_key).format(
            PtBr,
            entry.number,
            text(entry.lineStyle.description),
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
private val PtBr = Locale.forLanguageTag("pt-BR")

/**
 * "Label: value" items for the inputs used, whose unit the summary heading states once.
 * A non-breaking space keeps each value on its label's line when the summary wraps.
 */
fun measurementSummary(draft: SkirtDraft, text: (Int) -> String): List<String> =
    SkirtDraftField.entries.map { field ->
        "${text(field.summaryLabel)}: ${formatDecimal(draft.lengthOf(field).centimetres)}"
    }
