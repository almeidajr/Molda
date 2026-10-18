package com.almeidajr.molda.skirt.domain

import com.almeidajr.molda.measurement.Length
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadSkirtDraftTest {
    private val valid = SkirtDraftField.entries.zip(listOf("80", "2", "60", "1", "2")).toMap()

    @Test
    fun readSkirtDraft_validText_returnsDraftOfLengths() {
        assertEquals(
            SkirtDraftReading.Valid(
                SkirtDraft(Length(80.5), Length(2.0), Length(60.0), Length(1.0), Length(2.0)),
            ),
            readSkirtDraft(valid + (SkirtDraftField.Waist to " 80,5 ")),
        )
    }

    @Test
    fun readSkirtDraft_zeroEaseAndAllowances_areValid() {
        val zeros = valid + listOf(
            SkirtDraftField.Ease to "0",
            SkirtDraftField.WaistSeamAllowance to "0",
            SkirtDraftField.HemAllowance to "0",
        )

        assertEquals(
            SkirtDraftReading.Valid(
                SkirtDraft(Length(80.0), Length(0.0), Length(60.0), Length(0.0), Length(0.0)),
            ),
            readSkirtDraft(zeros),
        )
    }

    @Test
    fun readSkirtDraft_eachInvalidText_reportsItsErrorOnEveryField() {
        SkirtDraftField.entries.forEach { field ->
            listOf(
                " " to SkirtFieldError.Blank,
                "abc" to SkirtFieldError.NotANumber,
                "1.000,5" to SkirtFieldError.NotANumber,
                "-1" to if (field.mustBePositive) {
                    SkirtFieldError.NotPositive
                } else {
                    SkirtFieldError.Negative
                },
            ).forEach { (text, error) ->
                assertEquals(
                    "$field '$text'",
                    SkirtDraftReading.Invalid(mapOf(field to error)),
                    readSkirtDraft(valid + (field to text)),
                )
            }
        }
    }

    @Test
    fun readSkirtDraft_zeroWaistOrFinishedLength_isNotPositive() {
        listOf(SkirtDraftField.Waist, SkirtDraftField.FinishedLength).forEach { field ->
            assertEquals(
                SkirtDraftReading.Invalid(mapOf(field to SkirtFieldError.NotPositive)),
                readSkirtDraft(valid + (field to "0")),
            )
        }
    }

    @Test
    fun readSkirtDraft_severalInvalidFields_reportsEachOne() {
        val inputs = valid + listOf(SkirtDraftField.Waist to "", SkirtDraftField.Ease to "-2")

        assertEquals(
            SkirtDraftReading.Invalid(
                mapOf(
                    SkirtDraftField.Waist to SkirtFieldError.Blank,
                    SkirtDraftField.Ease to SkirtFieldError.Negative,
                ),
            ),
            readSkirtDraft(inputs),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun skirtDraft_zeroWaist_cannotExist() {
        SkirtDraft(Length(0.0), Length(2.0), Length(60.0), Length(1.0), Length(2.0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun skirtDraft_zeroFinishedLength_cannotExist() {
        SkirtDraft(Length(80.0), Length(2.0), Length(0.0), Length(1.0), Length(2.0))
    }
}
