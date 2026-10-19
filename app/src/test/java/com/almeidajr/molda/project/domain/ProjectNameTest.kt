package com.almeidajr.molda.project.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectNameTest {
    private val savedOn = LocalDate.of(2026, 10, 18)

    @Test
    fun defaultProjectName_fromProfile_joinsPatternProfileAndDate() {
        assertEquals(
            "Saia godê · Ana · 18/10/2026",
            defaultProjectName("Saia godê", profileName = "Ana", savedOn),
        )
    }

    @Test
    fun defaultProjectName_withoutProfile_omitsTheProfilePart() {
        assertEquals(
            "Saia godê · 05/01/2027",
            defaultProjectName("Saia godê", profileName = null, LocalDate.of(2027, 1, 5)),
        )
    }

    @Test
    fun readProjectName_trimsSurroundingWhitespace() {
        assertEquals(ProjectName("Saia da formatura"), readProjectName("  Saia da formatura "))
    }

    @Test
    fun readProjectName_blankText_isRefused() {
        listOf("", "   ", "\t\n").forEach { assertNull(readProjectName(it)) }
    }
}
