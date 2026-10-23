package com.almeidajr.molda.profile.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.testing.viewModelScenario
import com.almeidajr.molda.R
import com.almeidajr.molda.data.LoadResult
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileError
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.profile.navigation.EditedProfile
import com.almeidajr.molda.testing.FakeProfileRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import com.almeidajr.molda.testing.October13Clock
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric supplies the real Bundle that saved state is parcelled through.
@RunWith(RobolectricTestRunner::class)
class ProfileEditorViewModelTest {
    private val storage = FakeProfileRepository(listOf(ana))

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun create_forNewProfile_startsBlankAndReady() = runTest {
        // When
        val editor = editor(EditedProfile.New)

        // Then
        val state = editor.state.value
        assertNull(state.editingId)
        assertEquals("", state.inputs[ProfileField.Name])
        assertTrue(state.canMutate)
    }

    @Test
    fun create_forExistingProfile_loadsItsValues() = runTest {
        // When
        val editor = editor(EditedProfile.Existing(ana.id))

        // Then
        val state = editor.state.value
        assertEquals(ana.id, state.editingId)
        assertEquals("Ana", state.inputs[ProfileField.Name])
        assertEquals("80", state.inputs[ProfileField.Waist])
        assertFalse(state.isDirty)
    }

    @Test
    fun create_forProfileNoLongerStored_showsLoadFailure() = runTest {
        // When
        val editor = editor(EditedProfile.Existing(bia.id))

        // Then
        assertIs<LoadResult.Error>(editor.state.value.load)
    }

    @Test
    fun load_retryAfterLoadFailure_showsTheProfile() = runTest {
        // Given
        storage.failsLoad = true
        val editor = editor(EditedProfile.Existing(ana.id))
        storage.failsLoad = false

        // When
        editor.load()

        // Then
        val retried = editor.state.value
        assertEquals(LoadResult.Success(listOf(ana)), retried.load)
        assertEquals("Ana", retried.inputs[ProfileField.Name])
    }

    @Test
    fun save_newProfile_storesItAndReportsTheCreatedOne() = runTest {
        // Given
        val editor = editor(EditedProfile.New)
        editor.fill(name = "Bia", waist = "95", date = "11/10/2026")

        // When
        editor.save()

        // Then
        val created = storage.current.last()
        assertEquals("Bia", created.measurements.name.text)
        assertEquals(ProfileSave.Created(created), editor.state.value.saved)
        assertFalse(editor.state.value.isDirty)
    }

    @Test
    fun save_afterStoring_storesNothingMore() = runTest {
        // Given
        val editor = editor(EditedProfile.New)
        editor.fill(name = "Bia", waist = "95", date = "11/10/2026")
        editor.save()

        // When
        editor.save()

        // Then
        assertEquals(2, storage.current.size)
    }

    @Test
    fun save_editedProfile_replacesItAndReportsTheUpdate() = runTest {
        // Given
        val editor = editor(EditedProfile.Existing(ana.id))
        editor.edit(ProfileField.Waist, "82")

        // When
        editor.save()

        // Then
        assertEquals(Length(82.0), storage.current.single().measurements.waist)
        assertEquals(ProfileSave.Updated, editor.state.value.saved)
    }

    @Test
    fun save_profileDeletedMeanwhile_keepsTheFormAndShowsTheFailure() = runTest {
        // Given
        val editor = editor(EditedProfile.Existing(ana.id))
        editor.edit(ProfileField.Waist, "82")
        storage.delete(ana.id)

        // When
        editor.save()

        // Then
        val failed = editor.state.value
        assertEquals(R.string.profile_save_failed, failed.saveError)
        assertNull(failed.saved)
        assertEquals("82", failed.inputs[ProfileField.Waist])
    }

    @Test
    fun save_withInvalidInputs_showsEachErrorOnItsField() = runTest {
        // Given
        val editor = editor(EditedProfile.New)
        editor.fill(name = " ", waist = "0", date = "14/10/2026")

        // When
        editor.save()

        // Then
        assertEquals(
            mapOf(
                ProfileField.Name to ProfileError.BlankName,
                ProfileField.Waist to ProfileError.InvalidWaist,
                ProfileField.Date to ProfileError.FutureDate,
            ),
            editor.state.value.errors,
        )
    }

    @Test
    fun save_whenStorageFails_keepsTheFormAndShowsTheFailure() = runTest {
        // Given
        storage.failsCommands = true
        val editor = editor(EditedProfile.New)
        editor.fill(name = "Bia", waist = "95", date = "11/10/2026")

        // When
        editor.save()

        // Then
        val failed = editor.state.value
        assertEquals(R.string.profile_save_failed, failed.saveError)
        assertNull(failed.saved)
        assertEquals("Bia", failed.inputs[ProfileField.Name])
        assertEquals(listOf(ana), storage.current)
    }

    @Test
    fun edit_awayFromTheProfile_makesTheFormDirty() = runTest {
        // Given
        val editor = editor(EditedProfile.Existing(ana.id))

        // When
        editor.edit(ProfileField.Name, "Ana Maria")

        // Then
        assertTrue(editor.state.value.isDirty)
    }

    @Test
    fun edit_backToTheProfileValue_makesTheFormClean() = runTest {
        // Given
        val editor = editor(EditedProfile.Existing(ana.id))
        editor.edit(ProfileField.Name, "Ana Maria")

        // When
        editor.edit(ProfileField.Name, "Ana")

        // Then
        assertFalse(editor.state.value.isDirty)
    }

    @Test
    fun edit_waistRetypedAsTheSameLength_keepsTheFormClean() = runTest {
        // Given
        val editor = editor(EditedProfile.Existing(ana.id))

        // When
        editor.edit(ProfileField.Waist, "80,0")

        // Then
        assertFalse(editor.state.value.isDirty)
    }

    @Test
    fun edit_waistToAnotherLength_makesTheFormDirty() = runTest {
        // Given
        val editor = editor(EditedProfile.Existing(ana.id))

        // When
        editor.edit(ProfileField.Waist, "81")

        // Then
        assertTrue(editor.state.value.isDirty)
    }

    @Test
    fun edit_dateOrNotes_makesTheFormDirty() = runTest {
        // Given
        val editor = editor(EditedProfile.Existing(ana.id))

        // When/Then
        editor.edit(ProfileField.Notes, "Left side")
        assertTrue(editor.state.value.isDirty)
        editor.edit(ProfileField.Notes, "")
        assertFalse(editor.state.value.isDirty)
        editor.edit(ProfileField.Date, "2025-01-01")
        assertTrue(editor.state.value.isDirty)
    }

    @Test
    fun recreate_afterProcessDeath_restoresTheForm() = runTest {
        viewModelScenario {
            ProfileEditorViewModel(
                storage,
                createSavedStateHandle(),
                October13Clock,
                EditedProfile.Existing(ana.id),
            )
        }.use { scenario ->
            // Given
            scenario.viewModel.edit(ProfileField.Notes, "Sem folga")
            // A reload would replace the restored draft with the stored values.
            storage.update(bia.copy(id = ana.id))

            // When
            scenario.recreate()

            // Then
            val restored = scenario.viewModel.state.value
            assertEquals(ana.id, restored.editingId)
            assertEquals("Sem folga", restored.inputs[ProfileField.Notes])
            assertEquals("Ana", restored.inputs[ProfileField.Name])
            assertTrue(restored.isDirty)
        }
    }

    @Test
    fun recreate_afterProcessDeathOnceStored_reportsTheSaveAndStoresNothingMore() = runTest {
        viewModelScenario {
            ProfileEditorViewModel(
                storage,
                createSavedStateHandle(),
                October13Clock,
                EditedProfile.New,
            )
        }.use { scenario ->
            // Given
            scenario.viewModel.fill(name = "Bia", waist = "95", date = "11/10/2026")
            scenario.viewModel.save()

            // When
            scenario.recreate()
            scenario.viewModel.save()

            // Then
            assertEquals(ProfileSave.Updated, scenario.viewModel.state.value.saved)
            assertEquals(2, storage.current.size)
        }
    }

    private fun editor(profile: EditedProfile) =
        ProfileEditorViewModel(storage, SavedStateHandle(), October13Clock, profile)

    private fun ProfileEditorViewModel.fill(name: String, waist: String, date: String) {
        edit(ProfileField.Name, name)
        edit(ProfileField.Waist, waist)
        edit(ProfileField.Date, date)
    }
}

internal val ana = profile(1, "Ana", 80.0)
internal val bia = profile(2, "Bia", 95.0)

private fun profile(id: Long, name: String, waist: Double) = MeasurementProfile(
    ProfileId(id),
    ProfileMeasurements(ProfileName(name), Length(waist), LocalDate.of(2026, 10, 12)),
)
