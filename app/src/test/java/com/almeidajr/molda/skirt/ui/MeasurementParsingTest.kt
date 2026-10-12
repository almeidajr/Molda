package com.almeidajr.molda.skirt.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeasurementParsingTest {
    @Test
    fun parse_decimalCommaAndPoint_consumesTheWholeNumber() {
        assertEquals(80.5, requireNotNull(parseMeasurement(" 80,5 ")), 0.0)
        assertEquals(80.5, requireNotNull(parseMeasurement("80.5")), 0.0)
        assertEquals(0.0, requireNotNull(parseMeasurement("0")), 0.0)
        assertEquals(-1.25, requireNotNull(parseMeasurement("-1,25")), 0.0)
    }

    @Test
    fun parse_missingMalformedAndNonfiniteInput_rejectsWithoutPartialParsing() {
        listOf(
            "", " ", "NaN", "Infinity", "1e309", "1e2", "80cm", "80,", "1.000,5",
            "1,2,3", "0x10", "9".repeat(400),
        )
            .forEach { assertNull(it, parseMeasurement(it)) }
    }
}
