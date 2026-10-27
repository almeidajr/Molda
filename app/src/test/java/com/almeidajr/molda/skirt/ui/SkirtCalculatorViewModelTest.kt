package com.almeidajr.molda.skirt.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.testing.viewModelScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.measurement.MeasurementSource
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.skirt.domain.SkirtFieldError
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// The calculator sets its state synchronously and launches no coroutines, so tests read
// `state.value` directly. Robolectric supplies the real Bundle that saved state is parceled
// through.
@RunWith(AndroidJUnit4::class)
class SkirtCalculatorViewModelTest {
    @Test
    fun calculate_validDraft_showsItsResult() {
        // Given
        val calculator = calculatorWith(validDraft)

        // When
        calculator.calculate()

        // Then
        assertEquals(
            SkirtDraft(Length(70.0), Length(2.0), Length(50.0), Length(1.0), Length(2.0)),
            calculator.state.value.result?.draft,
        )
    }

    @Test
    fun edit_afterCalculation_withdrawsResult() {
        // Given
        val calculator = calculatorWith(validDraft)
        calculator.calculate()

        // When
        calculator.edit(SkirtDraftField.HemAllowance, "3")

        // Then
        assertNull(calculator.state.value.result)
    }

    @Test
    fun edit_afterInvalidCalculation_keepsEveryErrorUntilItsFieldIsLeft() {
        // Given
        val calculator =
            calculatorWith(
                validDraft + mapOf(SkirtDraftField.Ease to "", SkirtDraftField.HemAllowance to ""),
            )
        calculator.calculate()

        // When
        calculator.edit(SkirtDraftField.Ease, "2")

        // Then
        assertEquals(
            mapOf(
                SkirtDraftField.Ease to SkirtFieldError.Blank,
                SkirtDraftField.HemAllowance to SkirtFieldError.Blank,
            ),
            calculator.state.value.errors,
        )
    }

    @Test
    fun blur_afterSuccessfulCalculation_keepsTheResult() {
        // Given
        val calculator = calculatorWith(validDraft)
        calculator.calculate()

        // When
        calculator.blur(SkirtDraftField.Ease)

        // Then
        assertNotNull(calculator.state.value.result)
    }

    @Test
    fun blur_beforeFirstCalculation_showsNoError() {
        // Given
        val calculator = calculatorWith(validDraft + (SkirtDraftField.Ease to ""))

        // When
        calculator.blur(SkirtDraftField.Ease)

        // Then
        assertEquals(emptyMap(), calculator.state.value.errors)
    }

    @Test
    fun blur_afterFirstCalculation_showsFieldError() {
        // Given
        val calculator = calculatorWith(validDraft)
        calculator.calculate()
        calculator.edit(SkirtDraftField.Ease, "")

        // When
        calculator.blur(SkirtDraftField.Ease)

        // Then
        assertEquals(
            mapOf(SkirtDraftField.Ease to SkirtFieldError.Blank),
            calculator.state.value.errors,
        )
    }

    @Test
    fun blur_correctedFieldAfterFirstCalculation_clearsItsError() {
        // Given
        val calculator = calculatorWith(validDraft + (SkirtDraftField.Ease to ""))
        calculator.calculate()
        calculator.edit(SkirtDraftField.Ease, "2")

        // When
        calculator.blur(SkirtDraftField.Ease)

        // Then
        assertEquals(emptyMap(), calculator.state.value.errors)
    }

    @Test
    fun blur_seamAllowanceReachingWaistStitching_showsSeamAllowanceError() {
        // Given
        val calculator = calculatorWith(validDraft)
        calculator.calculate()
        calculator.edit(SkirtDraftField.WaistSeamAllowance, "12")

        // When
        calculator.blur(SkirtDraftField.WaistSeamAllowance)

        // Then
        assertEquals(
            mapOf(
                SkirtDraftField.WaistSeamAllowance to
                    SkirtFieldError.SeamAllowanceNotSmallerThanWaistStitching,
            ),
            calculator.state.value.errors,
        )
    }

    @Test
    fun selectProfile_withItsWaist_fillsTheExactWaistAndSource() {
        listOf(
            68.5 to "68,5",
            80.123456789 to "80,123456789",
            80.0 to "80",
        ).forEach { (waist, text) ->
            // Given
            val calculator = calculator()

            // When
            calculator.selectProfile(Length(waist), ana)

            // Then
            val form = calculator.state.value.form
            assertEquals(text, form.inputs[SkirtDraftField.Waist], "waist $waist")
            assertEquals(ana, form.source)
        }
    }

    @Test
    fun selectProfile_afterCalculating_clearsTheResult() {
        // Given
        val calculator = calculatorWith(validDraft)
        calculator.calculate()

        // When
        calculator.selectProfile(Length(68.5), ana)

        // Then
        assertNull(calculator.state.value.result)
    }

    @Test
    fun edit_waistAfterSelectingProfile_dropsTheSource() {
        // Given
        val calculator = calculator()
        calculator.selectProfile(Length(68.5), ana)

        // When
        calculator.edit(SkirtDraftField.Waist, "70")

        // Then
        assertNull(calculator.state.value.form.source)
    }

    @Test
    fun recreate_afterProcessDeath_restoresTheForm() {
        viewModelScenario { calculator(savedState = createSavedStateHandle()) }.use { scenario ->
            // Given
            scenario.viewModel.selectProfile(Length(70.0), ana)
            scenario.viewModel.edit(SkirtDraftField.Ease, "2")
            scenario.viewModel.calculate()

            // When
            scenario.recreate()

            // Then
            assertEquals(
                CalculatorForm(
                    inputs = SkirtDraftField.entries.associateWith { "" } +
                        mapOf(SkirtDraftField.Waist to "70", SkirtDraftField.Ease to "2"),
                    source = ana,
                    hasAttemptedCalculation = true,
                ),
                scenario.viewModel.state.value.form,
            )
        }
    }

    @Test
    fun create_withAStartingDraft_fillsEveryInputAndItsSourceAndCalculates() {
        // When
        val calculator = calculator(savedDraft, ana)

        // Then
        val started = calculator.state.value
        assertEquals(
            mapOf(
                SkirtDraftField.Waist to "70,125",
                SkirtDraftField.Ease to "2",
                SkirtDraftField.FinishedLength to "50",
                SkirtDraftField.WaistSeamAllowance to "1,5",
                SkirtDraftField.HemAllowance to "0",
            ),
            started.form.inputs,
        )
        assertEquals(ana, started.form.source)
        assertEquals(savedDraft, started.result?.draft)
        assertFalse(started.isDirty)
    }

    @Test
    fun isDirty_startingDraftWithChangedValue_isTrue() {
        // Given
        val calculator = calculator(savedDraft)

        // When
        calculator.edit(SkirtDraftField.Ease, "3")

        // Then
        assertTrue(calculator.state.value.isDirty)
    }

    @Test
    fun isDirty_startingDraftWithValueChangedBack_isFalse() {
        // Given
        val calculator = calculator(savedDraft)
        calculator.edit(SkirtDraftField.Ease, "3")

        // When
        calculator.edit(SkirtDraftField.Ease, "2")

        // Then
        assertFalse(calculator.state.value.isDirty)
    }

    @Test
    fun isDirty_startingDraftWithNewSourceForTheSameWaist_isTrue() {
        // Given
        val calculator = calculator(savedDraft)

        // When
        calculator.selectProfile(Length(70.125), ana)

        // Then
        assertTrue(calculator.state.value.isDirty)
    }

    @Test
    fun isDirty_sameValuesWrittenDifferently_isFalse() {
        // Given
        val calculator = calculator(savedDraft)

        // When
        calculator.edit(SkirtDraftField.FinishedLength, "50,0")
        calculator.edit(SkirtDraftField.WaistSeamAllowance, "1.50")

        // Then
        assertFalse(calculator.state.value.isDirty)
    }

    @Test
    fun isDirty_withoutAStartingDraft_isFalse() {
        // When
        val calculator = calculatorWith(validDraft)

        // Then
        assertFalse(calculator.state.value.isDirty)
    }

    @Test
    fun recreate_afterProcessDeath_keepsTheUncalculatedChangesWithoutAResult() {
        viewModelScenario {
            calculator(savedDraft, ana, createSavedStateHandle())
        }.use { scenario ->
            // Given
            scenario.viewModel.edit(SkirtDraftField.Ease, "3")

            // When
            scenario.recreate()

            // Then
            val state = scenario.viewModel.state.value
            assertTrue(state.isDirty)
            assertNull(state.result)
        }
    }

    @Test
    fun recreate_afterProcessDeath_recalculatesTheShownResult() {
        viewModelScenario {
            calculator(savedDraft, ana, createSavedStateHandle())
        }.use { scenario ->
            // Given
            scenario.viewModel.edit(SkirtDraftField.Ease, "3")
            scenario.viewModel.calculate()

            // When
            scenario.recreate()

            // Then
            val state = scenario.viewModel.state.value
            assertEquals(savedDraft.copy(ease = Length(3.0)), state.result?.draft)
            assertEquals(ana, state.form.source)
        }
    }

    private val ana = MeasurementSource("Ana", LocalDate.of(2026, 10, 12))

    private val savedDraft = SkirtDraft(
        waist = Length(70.125),
        ease = Length(2.0),
        finishedLength = Length(50.0),
        waistSeamAllowance = Length(1.5),
        hemAllowance = Length(0.0),
    )

    private val validDraft = mapOf(
        SkirtDraftField.Waist to "70",
        SkirtDraftField.Ease to "2",
        SkirtDraftField.FinishedLength to "50",
        SkirtDraftField.WaistSeamAllowance to "1",
        SkirtDraftField.HemAllowance to "2",
    )

    private fun calculator(
        startingDraft: SkirtDraft? = null,
        startingSource: MeasurementSource? = null,
        savedState: SavedStateHandle = SavedStateHandle(),
    ) = SkirtCalculatorViewModel(savedState, startingDraft, startingSource)

    private fun calculatorWith(inputs: Map<SkirtDraftField, String>) = calculator().apply {
        inputs.forEach { (field, text) -> edit(field, text) }
    }
}
