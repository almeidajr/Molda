package com.almeidajr.molda.skirt.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.testing.viewModelScenario
import app.cash.turbine.test
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.skirt.domain.SkirtFieldError
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
