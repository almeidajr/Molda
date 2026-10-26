package com.almeidajr.molda.update.ui

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.testing.ViewModelScenario
import androidx.lifecycle.viewmodel.testing.viewModelScenario
import com.almeidajr.molda.testing.Beta2
import com.almeidajr.molda.testing.FakeUpdateRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import com.almeidajr.molda.update.domain.UpdateCheck
import com.almeidajr.molda.update.domain.UpdateDownload
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric supplies the real Bundle that saved state is parcelled through.
@RunWith(RobolectricTestRunner::class)
class UpdateInstallViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val updates = FakeUpdateRepository(
        UpdateCheck.Available(Beta2),
        UpdateDownload.Downloaded,
        canInstall = false,
    )

    @Test
    fun create_withThePermission_startsTheInstall() = runTest {
        // Given
        updates.canInstall = true

        // When
        install().use { scenario ->
            // Then
            assertEquals(UpdateInstallState.Started, scenario.viewModel.state.value)
            assertEquals(listOf(Beta2), updates.installed)
        }
    }

    @Test
    fun create_withoutThePermission_asksForIt() = runTest {
        install().use { scenario ->
            // When/Then
            assertEquals(UpdateInstallState.NeedsPermission, scenario.viewModel.state.value)
            assertEquals(emptyList(), updates.installed)
        }
    }

    @Test
    fun settingClosed_withThePermissionGranted_continuesTheInstall() = runTest {
        install().use { scenario ->
            // Given
            scenario.viewModel.onSettingOpened()
            updates.canInstall = true

            // When
            scenario.viewModel.onSettingClosed()

            // Then
            assertEquals(UpdateInstallState.Started, scenario.viewModel.state.value)
            assertEquals(listOf(Beta2), updates.installed)
        }
    }

    @Test
    fun settingClosed_withoutThePermission_explains() = runTest {
        install().use { scenario ->
            // Given
            scenario.viewModel.onSettingOpened()

            // When
            scenario.viewModel.onSettingClosed()

            // Then
            assertEquals(UpdateInstallState.Refused, scenario.viewModel.state.value)
            assertEquals(emptyList(), updates.installed)
        }
    }

    @Test
    fun retry_afterTheExplanation_asksForThePermissionAgain() = runTest {
        install().use { scenario ->
            // Given
            scenario.viewModel.onSettingOpened()
            scenario.viewModel.onSettingClosed()

            // When
            scenario.viewModel.retry()

            // Then
            assertEquals(UpdateInstallState.NeedsPermission, scenario.viewModel.state.value)
        }
    }

    @Test
    fun recreate_afterProcessDeathInTheSetting_continuesOnceGranted() = runTest {
        install().use { scenario ->
            // Given
            scenario.viewModel.onSettingOpened()
            updates.canInstall = true

            // When
            scenario.recreate()

            // Then
            assertEquals(UpdateInstallState.Started, scenario.viewModel.state.value)
            assertEquals(listOf(Beta2), updates.installed)
        }
    }

    @Test
    fun recreate_afterProcessDeathInTheSetting_explainsWithoutReopeningIt() = runTest {
        install().use { scenario ->
            // Given
            scenario.viewModel.onSettingOpened()

            // When
            scenario.recreate()

            // Then
            assertEquals(UpdateInstallState.Refused, scenario.viewModel.state.value)
            assertEquals(emptyList(), updates.installed)
        }
    }

    private fun install(): ViewModelScenario<UpdateInstallViewModel> = viewModelScenario {
        UpdateInstallViewModel(updates, createSavedStateHandle(), Beta2)
    }
}
