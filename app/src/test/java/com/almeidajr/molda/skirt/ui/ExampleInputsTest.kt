package com.almeidajr.molda.skirt.ui

import com.almeidajr.molda.measurement.Length
import kotlin.test.assertEquals
import org.junit.Test

class ExampleInputsTest {
    @Test
    fun exampleCentimetres_wholeAndFractionalLengths_dropsDecimalsOnlyFromWholeOnes() {
        // When/Then
        assertEquals(
            listOf("80", "0", "1,50"),
            listOf(80.0, 0.0, 1.5).map { exampleCentimetres(Length(it)) },
        )
    }
}
