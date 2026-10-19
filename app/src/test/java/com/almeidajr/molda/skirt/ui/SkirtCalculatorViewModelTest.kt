package com.almeidajr.molda.skirt.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.testing.viewModelScenario
import app.cash.turbine.test
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.skirt.domain.SkirtFieldError
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric supplies the real Bundle that saved state is parcelled through.
@RunWith(RobolectricTestRunner::class)
class SkirtCalculatorViewModelTest {
    @Test
    fun calculate_withValidDraft_showsRadii() = runTest {
        val calculator = calculatorWith(validDraft)

        calculator.calculate()

        calculator.state.test {
            val radii = awaitItem().radii
            assertEquals(11.4591559, radii?.waistStitching?.centimetres ?: 0.0, 1e-6)
            assertEquals(63.4591559, radii?.hemCutting?.centimetres ?: 0.0, 1e-6)
        }
    }

    @Test
    fun blur_beforeFirstCalculation_showsNoError() = runTest {
        val calculator = calculatorWith(validDraft + (SkirtDraftField.Ease to ""))

        calculator.blur(SkirtDraftField.Ease)

        calculator.state.test {
            assertEquals(emptyMap<SkirtDraftField, SkirtFieldError>(), awaitItem().errors)
        }
    }

    @Test
    fun blur_afterFirstCalculation_showsFieldError() = runTest {
        val calculator = calculatorWith(validDraft)
        calculator.calculate()

        calculator.edit(SkirtDraftField.Ease, "")
        calculator.blur(SkirtDraftField.Ease)

        calculator.state.test {
            assertEquals(mapOf(SkirtDraftField.Ease to SkirtFieldError.Blank), awaitItem().errors)
        }
    }

    @Test
    fun blur_onSeamAllowanceReachingWaistStitching_showsSeamAllowanceError() = runTest {
        val calculator = calculatorWith(validDraft)
        calculator.calculate()

        calculator.edit(SkirtDraftField.WaistSeamAllowance, "12")
        calculator.blur(SkirtDraftField.WaistSeamAllowance)

        calculator.state.test {
            assertEquals(
                mapOf(
                    SkirtDraftField.WaistSeamAllowance to
                        SkirtFieldError.SeamAllowanceNotSmallerThanWaistStitching,
                ),
                awaitItem().errors,
            )
        }
    }

    @Test
    fun selectProfile_usesTheProfileWaistUntilTheWaistIsEdited() = runTest {
        val calculator = SkirtCalculatorViewModel(SavedStateHandle())

        calculator.selectProfile(
            waist = "68,5",
            name = "Ana",
            measurementDate = LocalDate.of(2026, 10, 12),
        )

        calculator.state.test {
            val selected = awaitItem().form
            assertEquals("68,5", selected.inputs[SkirtDraftField.Waist])
            assertEquals("Ana", selected.sourceProfileName)

            calculator.edit(SkirtDraftField.Waist, "70")
            val edited = awaitItem().form
            assertNull(edited.sourceProfileName)
            assertNull(edited.sourceMeasurementDate)
        }
    }

    @Test
    fun recreate_afterProcessDeath_restoresTheForm() = runTest {
        viewModelScenario { SkirtCalculatorViewModel(createSavedStateHandle()) }.use { scenario ->
            scenario.viewModel.selectProfile(waist = "70", name = "Ana", LocalDate.of(2026, 10, 12))
            scenario.viewModel.edit(SkirtDraftField.Ease, "2")
            scenario.viewModel.calculate()

            scenario.recreate()

            scenario.viewModel.state.test {
                assertEquals(
                    CalculatorForm(
                        inputs = SkirtDraftField.entries.associateWith { "" } +
                            mapOf(SkirtDraftField.Waist to "70", SkirtDraftField.Ease to "2"),
                        sourceProfileName = "Ana",
                        sourceMeasurementDate = "2026-10-12",
                        hasAttemptedCalculation = true,
                    ),
                    awaitItem().form,
                )
            }
        }
    }

    @Test
    fun load_savedDraft_fillsEveryInputAndItsSourceAndCalculates() = runTest {
        val calculator = SkirtCalculatorViewModel(SavedStateHandle())

        calculator.load(savedDraft, "Ana", LocalDate.of(2026, 10, 12))

        calculator.state.test {
            val loaded = awaitItem()
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
            assertEquals("Ana", loaded.form.sourceProfileName)
            assertEquals("2026-10-12", loaded.form.sourceMeasurementDate)
            assertEquals(savedDraft, loaded.draft)
            assertFalse(loaded.isDirty)
        }
    }

    @Test
    fun isDirty_followsWhetherTheLoadedDraftDiffers() = runTest {
        val calculator = SkirtCalculatorViewModel(SavedStateHandle())
        calculator.load(savedDraft, sourceProfileName = null, sourceMeasurementDate = null)

        calculator.state.test {
            assertFalse(awaitItem().isDirty)
            calculator.edit(SkirtDraftField.Ease, "3")
            assertTrue(awaitItem().isDirty)
            calculator.edit(SkirtDraftField.Ease, "2")
            assertFalse(awaitItem().isDirty)
            calculator.selectProfile("70,125", "Ana", LocalDate.of(2026, 10, 12))
            assertTrue(awaitItem().isDirty)
        }
    }

    @Test
    fun isDirty_withoutALoadedDraft_isFalse() = runTest {
        val calculator = calculatorWith(validDraft)

        calculator.state.test { assertFalse(awaitItem().isDirty) }
    }

    @Test
    fun recreate_afterProcessDeath_keepsTheLoadedDraftsChanges() = runTest {
        viewModelScenario { SkirtCalculatorViewModel(createSavedStateHandle()) }.use { scenario ->
            scenario.viewModel.load(savedDraft, "Ana", LocalDate.of(2026, 10, 12))
            scenario.viewModel.edit(SkirtDraftField.Ease, "3")

            scenario.recreate()

            scenario.viewModel.state.test { assertTrue(awaitItem().isDirty) }
        }
    }

    private val savedDraft = SkirtDraft(
        waist = Length(70.125),
        ease = Length(2.0),
        finishedLength = Length(50.0),
        waistSeamAllowance = Length(1.5),
        hemAllowance = Length(0.0),
    )

    private fun calculatorWith(inputs: Map<SkirtDraftField, String>) =
        SkirtCalculatorViewModel(SavedStateHandle()).apply {
            inputs.forEach { (field, text) -> edit(field, text) }
        }

    private val validDraft = mapOf(
        SkirtDraftField.Waist to "70",
        SkirtDraftField.Ease to "2",
        SkirtDraftField.FinishedLength to "50",
        SkirtDraftField.WaistSeamAllowance to "1",
        SkirtDraftField.HemAllowance to "2",
    )
}
