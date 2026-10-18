package com.almeidajr.molda.measurement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LengthParsingTest {
    @Test
    fun parse_decimalCommaAndPoint_consumesTheWholeNumber() {
        assertEquals(ParsedLength.Valid(Length(80.5)), parseLength(" 80,5 "))
        assertEquals(ParsedLength.Valid(Length(80.5)), parseLength("80.5"))
        assertEquals(ParsedLength.Valid(Length(0.0)), parseLength("0"))
    }

    @Test
    fun parse_negativeNumber_isRecognisedAsNegative() {
        assertEquals(ParsedLength.Negative, parseLength("-1,25"))
    }

    @Test
    fun parse_missingMalformedAndNonfiniteInput_rejectsWithoutPartialParsing() {
        listOf(
            "", " ", "NaN", "Infinity", "1e309", "1e2", "80cm", "80,", "1.000,5",
            "1,2,3", "0x10", "9".repeat(400),
        )
            .forEach { assertEquals(it, ParsedLength.Malformed, parseLength(it)) }
    }

    @Test
    fun length_negativeOrNonfiniteCentimetres_isRejected() {
        listOf(-0.5, Double.NaN, Double.POSITIVE_INFINITY)
            .forEach { assertThrows(IllegalArgumentException::class.java) { Length(it) } }
    }
}
