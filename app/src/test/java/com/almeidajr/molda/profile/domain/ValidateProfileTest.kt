package com.almeidajr.molda.profile.domain

import com.almeidajr.molda.measurement.Length
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.junit.Test

class ValidateProfileTest {
    private val today = LocalDate.of(2026, 10, 13)

    private fun validate(
        name: String = "Ana",
        waist: String = "80,5",
        measurementDate: String = "12/10/2026",
        notes: String = "",
    ) = validateProfile(name, waist, measurementDate, notes, today)

    @Test
    fun validate_completeForm_trimsTheNameAndKeepsNotesAsTyped() {
        // When/Then
        assertEquals(
            ProfileValidation.Valid(
                ProfileMeasurements(
                    ProfileName("Ana"),
                    Length(80.5),
                    LocalDate.of(2026, 10, 12),
                    " no cós ",
                ),
            ),
            validate(name = "  Ana ", notes = " no cós "),
        )
    }

    @Test
    fun validate_measurementDateToday_isValid() {
        // When
        val validation = validate(measurementDate = "13/10/2026")

        // Then
        assertEquals(
            today,
            assertIs<ProfileValidation.Valid>(validation).measurements.measurementDate,
        )
    }

    @Test
    fun validate_futureDate_reportsFutureDate() {
        // When/Then
        assertEquals(
            ProfileValidation.Invalid(setOf(ProfileError.FutureDate)),
            validate(measurementDate = "14/10/2026"),
        )
    }

    @Test
    fun validate_blankName_reportsBlankName() {
        // When/Then
        assertEquals(
            ProfileValidation.Invalid(setOf(ProfileError.BlankName)),
            validate(name = "   "),
        )
    }

    @Test
    fun validate_zeroNegativeOrMalformedWaist_reportsInvalidWaist() {
        // Given
        val waists = listOf("0", "-1", "Infinity", "", "80cm")

        // When/Then
        waists.forEach { waist ->
            assertEquals(
                ProfileValidation.Invalid(setOf(ProfileError.InvalidWaist)),
                validate(waist = waist),
                waist,
            )
        }
    }

    @Test
    fun validate_impossibleOrMalformedDate_reportsInvalidDate() {
        // Given
        val dates = listOf("32/02/2026", "29/02/2025", "00/00/0000", "12/10/0000", "2026-10-12", "")

        // When/Then
        dates.forEach { date ->
            assertEquals(
                ProfileValidation.Invalid(setOf(ProfileError.InvalidDate)),
                validate(measurementDate = date),
                date,
            )
        }
    }

    @Test
    fun validate_severalInvalidFields_reportsEveryError() {
        // When/Then
        assertEquals(
            ProfileValidation.Invalid(
                setOf(ProfileError.BlankName, ProfileError.InvalidWaist, ProfileError.InvalidDate),
            ),
            validate(name = "", waist = "0", measurementDate = "32/02/2026"),
        )
    }
}
