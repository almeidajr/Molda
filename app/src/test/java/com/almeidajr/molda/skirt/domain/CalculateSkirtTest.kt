package com.almeidajr.molda.skirt.domain

import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.measurement.lengthText
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import org.junit.Test

class CalculateSkirtTest {
    private val valid = SkirtDraftField.entries.zip(listOf("80", "2", "60", "1", "2")).toMap()

    @Test
    fun calculateSkirt_paddedDecimalCommaText_returnsItsDraft() {
        // When
        val calculation = calculateSkirt(valid + (SkirtDraftField.Waist to " 80,0 "))

        // Then
        assertEquals(
            SkirtDraft(Length(80.0), Length(2.0), Length(60.0), Length(1.0), Length(2.0)),
            assertIs<SkirtCalculation.Success>(calculation).draft,
        )
    }

    @Test
    fun calculateSkirt_zeroEaseAndAllowances_returnsDraft() {
        // Given
        val zeros = valid + listOf(
            SkirtDraftField.Ease to "0",
            SkirtDraftField.WaistSeamAllowance to "0",
            SkirtDraftField.HemAllowance to "0",
        )

        // When/Then
        assertEquals(
            SkirtDraft(Length(80.0), Length(0.0), Length(60.0), Length(0.0), Length(0.0)),
            assertIs<SkirtCalculation.Success>(calculateSkirt(zeros)).draft,
        )
    }

    @Test
    fun calculateSkirt_invalidTextInAnyField_reportsItsErrorOnThatField() {
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
                    SkirtCalculation.Invalid(mapOf(field to error)),
                    calculateSkirt(valid + (field to text)),
                    "$field '$text'",
                )
            }
        }
    }

    @Test
    fun calculateSkirt_zeroWaistOrFinishedLength_reportsNotPositive() {
        listOf(SkirtDraftField.Waist, SkirtDraftField.FinishedLength).forEach { field ->
            // When/Then
            assertEquals(
                SkirtCalculation.Invalid(mapOf(field to SkirtFieldError.NotPositive)),
                calculateSkirt(valid + (field to "0")),
                "$field",
            )
        }
    }

    @Test
    fun calculateSkirt_severalInvalidFields_reportsEachOne() {
        // Given
        val inputs = valid + listOf(SkirtDraftField.Waist to "", SkirtDraftField.Ease to "-2")

        // When/Then
        assertEquals(
            SkirtCalculation.Invalid(
                mapOf(
                    SkirtDraftField.Waist to SkirtFieldError.Blank,
                    SkirtDraftField.Ease to SkirtFieldError.Negative,
                ),
            ),
            calculateSkirt(inputs),
        )
    }

    @Test
    fun calculateSkirt_missingFields_reportsThemBlank() {
        // When/Then
        assertEquals(
            SkirtCalculation.Invalid(
                mapOf(
                    SkirtDraftField.Ease to SkirtFieldError.Blank,
                    SkirtDraftField.HemAllowance to SkirtFieldError.Blank,
                ),
            ),
            calculateSkirt(valid - SkirtDraftField.Ease - SkirtDraftField.HemAllowance),
        )
    }

    @Test
    fun calculateSkirt_seamAllowanceAtOrAboveWaistStitching_reportsItOnTheSeamAllowance() {
        // Given
        val radius = assertIs<SkirtCalculation.Success>(calculateSkirt(valid)).radii.waistStitching

        listOf(lengthText(radius), "14").forEach { allowance ->
            // When/Then
            assertEquals(
                SkirtCalculation.Invalid(
                    mapOf(
                        SkirtDraftField.WaistSeamAllowance to
                            SkirtFieldError.SeamAllowanceNotSmallerThanWaistStitching,
                    ),
                ),
                calculateSkirt(valid + (SkirtDraftField.WaistSeamAllowance to allowance)),
                "allowance $allowance",
            )
        }
    }

    @Test
    fun calculateSkirt_overflowingLengths_reportsNonfiniteResult() {
        // Given
        val huge = "9".repeat(308)

        listOf(
            listOf(SkirtDraftField.Waist to huge, SkirtDraftField.Ease to huge),
            listOf(SkirtDraftField.FinishedLength to huge, SkirtDraftField.HemAllowance to huge),
        ).forEach { overflowing ->
            // When/Then
            assertEquals(
                SkirtCalculation.NonfiniteResult,
                calculateSkirt(valid + overflowing),
                "$overflowing",
            )
        }
    }

    @Test
    fun skirtDraft_zeroWaistOrFinishedLength_isRejected() {
        listOf(
            "zero waist" to {
                SkirtDraft(Length(0.0), Length(2.0), Length(60.0), Length(1.0), Length(2.0))
            },
            "zero finished length" to {
                SkirtDraft(Length(80.0), Length(2.0), Length(0.0), Length(1.0), Length(2.0))
            },
        ).forEach { (case, draft) ->
            // When/Then
            assertFailsWith<IllegalArgumentException>(case) { draft() }
        }
    }
}
