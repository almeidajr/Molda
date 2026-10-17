package com.almeidajr.molda.skirt.ui

import com.almeidajr.molda.skirt.domain.SkirtRadii
import java.util.Locale

/** Stroke patterns that tell the four drafting lines apart without relying on color. */
enum class LineStyle(val description: String) {
    Solid("linha contínua"),
    Dashed("linha tracejada"),
    Dotted("linha pontilhada"),
    DashDot("traço e ponto"),
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

/** Maps calculated radii to the reference-line group followed by the cutting-line group. */
@Suppress("MagicNumber") // Diagram numbers 1–4 are the fixed key shared with the diagram.
fun cuttingCard(radii: SkirtRadii): List<CuttingCardGroup> {
    val waistCoincides = radii.waistCutting == radii.waistStitching
    val hemCoincides = radii.hemCutting == radii.finishedHem
    return listOf(
        CuttingCardGroup(
            "Linhas de referência",
            listOf(
                CuttingCardEntry(
                    1,
                    LineStyle.Solid,
                    "Costura da cintura",
                    formatCentimetres(radii.waistStitching),
                    2.takeIf { waistCoincides },
                ),
                CuttingCardEntry(
                    3,
                    LineStyle.Dotted,
                    "Barra acabada",
                    formatCentimetres(radii.finishedHem),
                    4.takeIf { hemCoincides },
                ),
            ),
        ),
        CuttingCardGroup(
            "Linhas de corte",
            listOf(
                CuttingCardEntry(
                    2,
                    LineStyle.Dashed,
                    "Corte da cintura",
                    formatCentimetres(radii.waistCutting),
                    1.takeIf { waistCoincides },
                ),
                CuttingCardEntry(
                    4,
                    LineStyle.DashDot,
                    "Corte da barra",
                    formatCentimetres(radii.hemCutting),
                    3.takeIf { hemCoincides },
                ),
            ),
        ),
    )
}

/** Two pt-BR decimals and "cm" joined by a non-breaking space, e.g. "13,05 cm". */
fun formatCentimetres(value: Double): String = "${formatDecimal(value)} cm"

/** Two pt-BR decimals without grouping, e.g. "80,00". */
fun formatDecimal(value: Double): String =
    String.format(Locale.forLanguageTag("pt-BR"), "%.2f", value)

/** "Margem de … zero" notes for each pair of lines drawn at the same radius. */
fun coincidenceNotes(card: List<CuttingCardGroup>): List<String> = card
    .flatMap { it.entries }
    .filter { entry -> entry.coincidentLine?.let { it > entry.number } == true }
    .map { entry ->
        val allowance = if (entry.number == 1) "cintura" else "barra"
        "Margem de $allowance zero: as linhas ${entry.number} e ${entry.coincidentLine} coincidem."
    }

/**
 * The drafting diagram's text equivalent: its key, line order, coincidences, allowance
 * directions and limits, without repeating the radii already on the card.
 */
fun diagramDescription(
    card: List<CuttingCardGroup>,
    waistSeamAllowance: String,
    hemAllowance: String,
): String {
    val key = card.flatMap { it.entries }.sortedBy { it.number }.map { entry ->
        "Linha ${entry.number}, ${entry.lineStyle.description}: ${entry.label.lowercase()}."
    }
    return (
        listOf(
            "Diagrama esquemático de um setor da saia godê inteira, sem escala.",
            "Centro comum no canto superior esquerdo; do centro para fora, linhas 2, 1, 3 e 4.",
        ) + key + coincidenceNotes(card) +
            listOf(
                "A margem de cintura de $waistSeamAllowance cm avança para dentro; " +
                    "a margem de barra de $hemAllowance cm avança para fora.",
                "Não é um molde completo, calibrado ou para impressão. " +
                    "O cálculo não garante o caimento.",
            )
        ).joinToString(" ")
}

/**
 * "Label: value" items for the inputs used, whose unit the summary heading states once.
 * A non-breaking space keeps each value on its label's line when the summary wraps.
 */
fun measurementSummary(inputs: Map<MeasurementField, Double>): List<String> =
    inputs.map { (field, value) -> "${field.label.removeSuffix(" (cm)")}: ${formatDecimal(value)}" }
