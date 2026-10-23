package com.almeidajr.molda.measurement

import java.math.BigDecimal
import kotlinx.serialization.Serializable

/** A finite, non-negative distance in centimetres. */
@Serializable
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

/** Whether [text] and [other] are the same length ("80", "80,0"); other text compares as typed. */
fun isSameLength(text: String, other: String): Boolean = when (val length = parseLength(text)) {
    is ParsedLength.Valid -> length == parseLength(other)
    else -> text == other
}

private val DecimalNumber = Regex("[+-]?[0-9]+([,.][0-9]+)?")

/** [length] as a user would type it: a decimal comma and no trailing zeros, e.g. "80,5". */
fun lengthText(length: Length): String =
    BigDecimal.valueOf(length.centimetres).stripTrailingZeros().toPlainString().replace('.', ',')
