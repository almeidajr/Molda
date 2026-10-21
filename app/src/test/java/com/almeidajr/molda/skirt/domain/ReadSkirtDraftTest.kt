package com.almeidajr.molda.skirt.domain

import com.almeidajr.molda.measurement.Length
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

class ReadSkirtDraftTest {
    private val valid = SkirtDraftField.entries.zip(listOf("80", "2", "60", "1", "2")).toMap()

    @Test
    fun readSkirtDraft_paddedDecimalCommaText_returnsDraftOfLengths() {
        // When/Then
        assertEquals(
            SkirtDraftReading.Valid(
                SkirtDraft(Length(80.5), Length(2.0), Length(60.0), Length(1.0), Length(2.0)),
            ),
            readSkirtDraft(valid + (SkirtDraftField.Waist to " 80,5 ")),
        )
    }

    @Test
    fun readSkirtDraft_zeroEaseAndAllowances_returnsValidDraft() {
        // Given
        val zeros = valid + listOf(
            SkirtDraftField.Ease to "0",
            SkirtDraftField.WaistSeamAllowance to "0",
            SkirtDraftField.HemAllowance to "0",
        )

        // When/Then
        assertEquals(
            SkirtDraftReading.Valid(
                SkirtDraft(Length(80.0), Length(0.0), Length(60.0), Length(0.0), Length(0.0)),
            ),
            readSkirtDraft(zeros),
        )
    }

    @Test
    fun readSkirtDraft_invalidTextInAnyField_reportsItsErrorOnThatField() {
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
                // When/Then
                assertEquals(
                    SkirtDraftReading.Invalid(mapOf(field to error)),
                    readSkirtDraft(valid + (field to text)),
                    "$field '$text'",
                )
            }
        }
    }

    @Test
    fun readSkirtDraft_zeroWaistOrFinishedLength_reportsNotPositive() {
        listOf(SkirtDraftField.Waist, SkirtDraftField.FinishedLength).forEach { field ->
            // When/Then
            assertEquals(
                SkirtDraftReading.Invalid(mapOf(field to SkirtFieldError.NotPositive)),
                readSkirtDraft(valid + (field to "0")),
                "$field",
            )
        }
    }

    @Test
    fun readSkirtDraft_severalInvalidFields_reportsEachOne() {
        // Given
        val inputs = valid + listOf(SkirtDraftField.Waist to "", SkirtDraftField.Ease to "-2")

        // When/Then
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

    @Test
    fun skirtDraft_zeroWaist_isRejected() {
        // When/Then
        assertFailsWith<IllegalArgumentException> {
            SkirtDraft(Length(0.0), Length(2.0), Length(60.0), Length(1.0), Length(2.0))
        }
    }

    @Test
    fun skirtDraft_zeroFinishedLength_isRejected() {
        // When/Then
        assertFailsWith<IllegalArgumentException> {
            SkirtDraft(Length(80.0), Length(2.0), Length(0.0), Length(1.0), Length(2.0))
        }
    }
}
