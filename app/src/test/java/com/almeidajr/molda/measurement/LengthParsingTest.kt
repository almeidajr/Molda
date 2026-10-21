package com.almeidajr.molda.measurement

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

class LengthParsingTest {
    @Test
    fun parseLength_withDecimalCommaOrPoint_returnsTheWholeNumber() {
        // Given
        val cases = mapOf(" 80,5 " to 80.5, "80.5" to 80.5, "0" to 0.0)

        // When/Then
        cases.forEach { (text, centimetres) ->
            assertEquals(ParsedLength.Valid(Length(centimetres)), parseLength(text), text)
        }
    }

    @Test
    fun parseLength_withNegativeNumber_returnsNegative() {
        // When/Then
        assertEquals(ParsedLength.Negative, parseLength("-1,25"))
    }

    @Test
    fun parseLength_withBlankPartialOrNonfiniteText_returnsMalformed() {
        // Given
        val cases = listOf(
            "", " ", "NaN", "Infinity", "1e309", "1e2", "80cm", "80,", "1.000,5",
            "1,2,3", "0x10", "9".repeat(400),
        )

        // When/Then
        cases.forEach { text -> assertEquals(ParsedLength.Malformed, parseLength(text), text) }
    }

    @Test
    fun length_withNegativeOrNonfiniteCentimetres_throws() {
        // Given
        val cases = listOf(-0.5, Double.NaN, Double.POSITIVE_INFINITY)

        // When/Then
        cases.forEach { centimetres ->
            assertFailsWith<IllegalArgumentException>("$centimetres") { Length(centimetres) }
        }
    }
}
