package com.almeidajr.molda.measurement

/** A finite, non-negative distance in centimetres. */
@JvmInline
value class Length(val centimetres: Double) {
    init {
        require(centimetres.isFinite() && centimetres >= 0.0) {
            "A length must be finite and non-negative: $centimetres"
        }
    }
}

/** The outcome of reading a [Length] from user text. */
sealed interface ParsedLength {
    data class Valid(val length: Length) : ParsedLength

    /** A well-formed but negative number. */
    data object Negative : ParsedLength

    /** Blank, non-numeric, partially numeric or non-finite text. */
    data object Malformed : ParsedLength
}

/** Accepts a decimal comma or point, without grouping, exponents or partial parsing. */
fun parseLength(text: String): ParsedLength {
    val trimmed = text.trim()
    val value = trimmed.takeIf { DecimalNumber.matches(it) }
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        ?.takeIf { it.isFinite() }
    return when {
        value == null -> ParsedLength.Malformed
        value < 0.0 -> ParsedLength.Negative
        else -> ParsedLength.Valid(Length(value))
    }
}

/** The centimetres of the valid [Length] typed as [text], or null when it is malformed or negative. */
fun validCentimetres(text: String): Double? =
    (parseLength(text) as? ParsedLength.Valid)?.length?.centimetres

private val DecimalNumber = Regex("[+-]?[0-9]+([,.][0-9]+)?")
