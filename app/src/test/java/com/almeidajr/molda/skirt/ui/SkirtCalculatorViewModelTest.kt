package com.almeidajr.molda.skirt.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.testing.viewModelScenario
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.measurement.MeasurementSource
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.skirt.domain.SkirtFieldError
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// The calculator sets its state synchronously and launches no coroutines, so tests read
// `state.value` directly. Robolectric supplies the real Bundle that saved state is parcelled
// through.
@RunWith(RobolectricTestRunner::class)
class SkirtCalculatorViewModelTest {
    @Test
    fun calculate_validDraft_showsRadii() {
        // Given
        val calculator = calculatorWith(validDraft)

        // When
        calculator.calculate()

        // Then
        val radii = calculator.state.value.radii
        assertEquals(
            11.4591559,
            radii?.waistStitching?.centimetres ?: 0.0,
            absoluteTolerance = 1e-6,
        )
        assertEquals(63.4591559, radii?.hemCutting?.centimetres ?: 0.0, absoluteTolerance = 1e-6)
    }

    @Test
    fun edit_afterCalculation_withdrawsResult() {
        // Given
        val calculator = calculatorWith(validDraft)
        calculator.calculate()

        // When
        calculator.edit(SkirtDraftField.HemAllowance, "3")

        // Then
        assertNull(calculator.state.value.radii)
        assertNull(calculator.state.value.snapshot())
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
    fun selectProfile_withItsWaist_fillsWaistAndSource() {
        // Given
        val calculator = SkirtCalculatorViewModel(SavedStateHandle())

        // When
        calculator.selectProfile(waist = "68,5", ana)

        // Then
        val form = calculator.state.value.form
        assertEquals("68,5", form.inputs[SkirtDraftField.Waist])
        assertEquals(ana, form.source)
    }

    @Test
    fun edit_waistAfterSelectingProfile_dropsTheSource() {
        // Given
        val calculator = SkirtCalculatorViewModel(SavedStateHandle())
        calculator.selectProfile(waist = "68,5", ana)

        // When
        calculator.edit(SkirtDraftField.Waist, "70")

        // Then
        assertNull(calculator.state.value.form.source)
    }

    @Test
    fun recreate_afterProcessDeath_restoresTheForm() {
        viewModelScenario { SkirtCalculatorViewModel(createSavedStateHandle()) }.use { scenario ->
            // Given
            scenario.viewModel.selectProfile(waist = "70", ana)
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
    fun load_savedDraft_fillsEveryInputAndItsSourceAndCalculates() {
        // Given
        val calculator = SkirtCalculatorViewModel(SavedStateHandle())

        // When
        calculator.load(savedDraft, ana)

        // Then
        val loaded = calculator.state.value
        assertEquals(
            mapOf(
                SkirtDraftField.Waist to "70,125",
                SkirtDraftField.Ease to "2",
                SkirtDraftField.FinishedLength to "50",
                SkirtDraftField.WaistSeamAllowance to "1,5",
                SkirtDraftField.HemAllowance to "0",
            ),
            loaded.form.inputs,
        )
        assertEquals(ana, loaded.form.source)
        assertEquals(savedDraft, loaded.draft)
        assertFalse(loaded.isDirty)
    }

    @Test
    fun isDirty_loadedDraftWithChangedValue_isTrue() {
        // Given
        val calculator = SkirtCalculatorViewModel(SavedStateHandle())
        calculator.load(savedDraft, source = null)

        // When
        calculator.edit(SkirtDraftField.Ease, "3")

        // Then
        assertTrue(calculator.state.value.isDirty)
    }

    @Test
    fun isDirty_loadedDraftWithValueChangedBack_isFalse() {
        // Given
        val calculator = SkirtCalculatorViewModel(SavedStateHandle())
        calculator.load(savedDraft, source = null)
        calculator.edit(SkirtDraftField.Ease, "3")

        // When
        calculator.edit(SkirtDraftField.Ease, "2")

        // Then
        assertFalse(calculator.state.value.isDirty)
    }

    @Test
    fun isDirty_loadedDraftWithNewSourceForTheSameWaist_isTrue() {
        // Given
        val calculator = SkirtCalculatorViewModel(SavedStateHandle())
        calculator.load(savedDraft, source = null)

        // When
        calculator.selectProfile("70,125", ana)

        // Then
        assertTrue(calculator.state.value.isDirty)
    }

    @Test
    fun isDirty_sameValuesWrittenDifferently_isFalse() {
        // Given
        val calculator = SkirtCalculatorViewModel(SavedStateHandle())
        calculator.load(savedDraft, source = null)

        // When
        calculator.edit(SkirtDraftField.FinishedLength, "50,0")
        calculator.edit(SkirtDraftField.WaistSeamAllowance, "1.50")

        // Then
        assertFalse(calculator.state.value.isDirty)
    }

    @Test
    fun isDirty_withoutALoadedDraft_isFalse() {
        // When
        val calculator = calculatorWith(validDraft)

        // Then
        assertFalse(calculator.state.value.isDirty)
    }

    @Test
    fun recreate_afterProcessDeath_keepsTheLoadedDraftsChanges() {
        viewModelScenario { SkirtCalculatorViewModel(createSavedStateHandle()) }.use { scenario ->
            // Given
            scenario.viewModel.load(savedDraft, ana)
            scenario.viewModel.edit(SkirtDraftField.Ease, "3")

            // When
            scenario.recreate()

            // Then
            assertTrue(scenario.viewModel.state.value.isDirty)
        }
    }

    @Test
    fun recreate_afterProcessDeath_recalculatesTheShownResult() {
        viewModelScenario { SkirtCalculatorViewModel(createSavedStateHandle()) }.use { scenario ->
            // Given
            scenario.viewModel.load(savedDraft, ana)
            scenario.viewModel.edit(SkirtDraftField.Ease, "3")
            scenario.viewModel.calculate()

            // When
            scenario.recreate()

            // Then
            val snapshot = scenario.viewModel.state.value.snapshot()
            assertEquals(savedDraft.copy(ease = Length(3.0)), snapshot?.draft)
            assertEquals(
                11.6382052,
                snapshot?.radii?.waistStitching?.centimetres ?: 0.0,
                absoluteTolerance = 1e-6,
            )
            assertEquals(ana, snapshot?.source)
        }
    }

    @Test
    fun recreate_afterProcessDeath_showsNoResultForUncalculatedChanges() {
        viewModelScenario { SkirtCalculatorViewModel(createSavedStateHandle()) }.use { scenario ->
            // Given
            scenario.viewModel.load(savedDraft, ana)
            scenario.viewModel.edit(SkirtDraftField.Ease, "3")

            // When
            scenario.recreate()

            // Then
            assertNull(scenario.viewModel.state.value.radii)
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

    private fun calculatorWith(inputs: Map<SkirtDraftField, String>) =
        SkirtCalculatorViewModel(SavedStateHandle()).apply {
            inputs.forEach { (field, text) -> edit(field, text) }
        }
}
