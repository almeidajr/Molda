package com.almeidajr.molda.skirt.ui

import com.almeidajr.molda.measurement.Length
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleInputsTest {
    @Test
    fun exampleCentimetres_dropsDecimalsOnlyFromWholeCentimetres() {
        assertEquals(
            listOf("80", "0", "1,50"),
            listOf(80.0, 0.0, 1.5).map { exampleCentimetres(Length(it)) },
        )
    }
}
