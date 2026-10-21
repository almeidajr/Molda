package com.almeidajr.molda.project.domain

import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class ProjectNameTest {
    @Test
    fun defaultProjectName_fromProfile_joinsPatternProfileAndDate() {
        // When/Then
        assertEquals(
            "Saia godê · Ana · 18/10/2026",
            defaultProjectName("Saia godê", profileName = "Ana", LocalDate.of(2026, 10, 18)),
        )
    }

    @Test
    fun defaultProjectName_withoutProfile_omitsTheProfilePart() {
        // When/Then
        assertEquals(
            "Saia godê · 05/01/2027",
            defaultProjectName("Saia godê", profileName = null, LocalDate.of(2027, 1, 5)),
        )
    }

    @Test
    fun readProjectName_withSurroundingWhitespace_returnsTrimmedName() {
        // When/Then
        assertEquals(ProjectName("Saia da formatura"), readProjectName("  Saia da formatura "))
    }

    @Test
    fun readProjectName_blankText_returnsNull() {
        // When/Then
        listOf("", "   ", "\t\n").forEach { assertNull(readProjectName(it), "for \"$it\"") }
    }
}
