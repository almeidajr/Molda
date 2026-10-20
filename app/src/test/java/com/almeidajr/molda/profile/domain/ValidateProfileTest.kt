package com.almeidajr.molda.profile.domain

import com.almeidajr.molda.measurement.Length
import java.time.LocalDate
import kotlin.test.assertEquals
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
    fun validate_completeForm_returnsTrimmedMeasurements() {
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
    fun validate_measurementTakenToday_isAccepted() {
        assertEquals(
            LocalDate.of(2026, 10, 13),
            (validate(measurementDate = "13/10/2026") as ProfileValidation.Valid)
                .measurements.measurementDate,
        )
    }

    @Test
    fun validate_futureDate_isRejected() {
        assertEquals(
            ProfileValidation.Invalid(setOf(ProfileError.FutureDate)),
            validate(measurementDate = "14/10/2026"),
        )
    }

    @Test
    fun validate_blankName_isRejected() {
        assertEquals(
            ProfileValidation.Invalid(setOf(ProfileError.BlankName)),
            validate(name = "   "),
        )
    }

    @Test
    fun validate_zeroNegativeOrMalformedWaist_isRejected() {
        listOf("0", "-1", "Infinity", "", "80cm").forEach {
            assertEquals(
                ProfileValidation.Invalid(setOf(ProfileError.InvalidWaist)),
                validate(waist = it),
                it,
            )
        }
    }

    @Test
    fun validate_impossibleOrMalformedDate_isRejected() {
        listOf("32/02/2026", "29/02/2025", "00/00/0000", "12/10/0000", "2026-10-12", "").forEach {
            assertEquals(
                ProfileValidation.Invalid(setOf(ProfileError.InvalidDate)),
                validate(measurementDate = it),
                it,
            )
        }
    }

    @Test
    fun validate_severalInvalidFields_reportsEveryError() {
        assertEquals(
            ProfileValidation.Invalid(
                setOf(ProfileError.BlankName, ProfileError.InvalidWaist, ProfileError.InvalidDate),
            ),
            validate(name = "", waist = "0", measurementDate = "32/02/2026"),
        )
    }
}
