package com.almeidajr.molda.profile.ui

import androidx.lifecycle.SavedStateHandle
import com.almeidajr.molda.profile.navigation.EditedProfile
import com.almeidajr.molda.testing.FakeProfileRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import com.almeidajr.molda.testing.October13Clock
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** What each profile ViewModel shows and blocks while its own command is still running. */
// Robolectric supplies the real Bundle that the editor's saved state is parceled through.
@RunWith(RobolectricTestRunner::class)
class ProfileOperationsInFlightTest {
    private val storage = FakeProfileRepository(listOf(ana, bia))

    // Commands stay in flight until the test advances the dispatcher.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun delete_whileInFlight_marksItsRowAndBlocksEveryRow() = runTest {
        // Given
        val profiles = ProfileListViewModel(storage)
        advanceUntilIdle()

        // When
        profiles.delete(ana.id)

        // Then
        val deleting = profiles.state.value
        assertTrue(deleting.isDeleting(ana.id))
        assertFalse(deleting.isDeleting(bia.id))
        assertFalse(deleting.canChangeProfiles)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun delete_whileItsOwnDeleteRuns_isIgnored() = runTest {
        // Given
        val profiles = ProfileListViewModel(storage)
        advanceUntilIdle()
        profiles.delete(ana.id)

        // When
        profiles.delete(bia.id)
        advanceUntilIdle()

        // Then
        assertEquals(listOf(bia), storage.current)
        assertTrue(profiles.state.value.canChangeProfiles)
    }

    @Test
    fun save_whileInFlight_isUnavailable() = runTest {
        // Given
        val editor = newProfileEditor()

        // When
        editor.save()

        // Then
        val saving = editor.state.value
        assertTrue(saving.isSaving)
        assertFalse(saving.canSave)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun save_whileItsOwnSaveRuns_storesOnce() = runTest {
        // Given
        val editor = newProfileEditor()
        editor.save()

        // When
        editor.save()
        advanceUntilIdle()

        // Then
        assertEquals(3, storage.current.size)
    }

    private fun newProfileEditor() =
        ProfileEditorViewModel(storage, SavedStateHandle(), October13Clock, EditedProfile.New)
            .apply {
                edit(ProfileField.Name, "Cleo")
                edit(ProfileField.Waist, "70")
                edit(ProfileField.Date, "11/10/2026")
            }
}
