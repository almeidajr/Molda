package com.almeidajr.molda.profile.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.testing.viewModelScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.R
import com.almeidajr.molda.data.LoadResult
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.Ana
import com.almeidajr.molda.profile.Bia
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileError
import com.almeidajr.molda.profile.navigation.EditedProfile
import com.almeidajr.molda.profile.profile
import com.almeidajr.molda.testing.FakeProfileRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import com.almeidajr.molda.testing.October13Clock
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Robolectric supplies the real Bundle that saved state is parceled through.
@RunWith(AndroidJUnit4::class)
class ProfileEditorViewModelTest {
    private val storage = FakeProfileRepository(listOf(Ana))

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun create_forNewProfile_startsBlankAndEditable() = runTest {
        // When
        val state = editor(EditedProfile.New).state.value

        // Then
        assertNull(state.editingId)
        assertEquals("", state.inputs[ProfileField.Name])
        assertEquals("", state.inputs[ProfileField.Date])
        assertEquals(R.string.profile_editor_new, state.title)
        assertEquals(R.string.profile_save_new, state.saveLabel)
        assertTrue(state.showsLocalOnlyNote)
        assertFalse(state.canRetryLoad)
        assertTrue(state.canEdit)
        assertTrue(state.canSave)
    }

    @Test
    fun create_forExistingProfile_loadsItsValuesEditable() = runTest {
        // When
        val state = editor(EditedProfile.Existing(Ana.id)).state.value

        // Then
        assertEquals(Ana.id, state.editingId)
        assertEquals("Ana", state.inputs[ProfileField.Name])
        assertEquals("80", state.inputs[ProfileField.Waist])
        assertFalse(state.isDirty)
        assertEquals(R.string.profile_editor_edit, state.title)
        assertEquals(R.string.profile_save_changes, state.saveLabel)
        assertFalse(state.showsLocalOnlyNote)
        assertFalse(state.canRetryLoad)
        assertTrue(state.canEdit)
    }

    @Test
    fun edit_whileTheProfileLoads_isRefused() = runTest {
        // Given
        storage.loadGate = CompletableDeferred()
        val editor = editor(EditedProfile.Existing(Ana.id))

        // When
        editor.edit(ProfileField.Name, "Ana Maria")

        // Then
        val loading = editor.state.value
        assertFalse(loading.canEdit)
        assertFalse(loading.canRetryLoad)
        assertEquals("", loading.inputs[ProfileField.Name])
    }

    @Test
    fun edit_afterLoadFailure_isRefused() = runTest {
        // Given
        storage.failsLoad = true
        val editor = editor(EditedProfile.Existing(Ana.id))

        // When
        editor.edit(ProfileField.Name, "Ana Maria")

        // Then
        val failed = editor.state.value
        assertEquals(R.string.profile_save_changes, failed.saveLabel)
        assertFalse(failed.showsLocalOnlyNote)
        assertTrue(failed.canRetryLoad)
        assertFalse(failed.canEdit)
        assertEquals("", failed.inputs[ProfileField.Name])
        assertFalse(failed.isDirty)
    }

    @Test
    fun edit_whileSaving_isRefused() = runTest {
        // Given
        storage.commandGate = CompletableDeferred()
        val editor = editor(EditedProfile.Existing(Ana.id))
        editor.save()

        // When
        editor.edit(ProfileField.Name, "Ana Maria")

        // Then
        val saving = editor.state.value
        assertEquals(R.string.status_saving, saving.saveLabel)
        assertFalse(saving.canEdit)
        assertEquals("Ana", saving.inputs[ProfileField.Name])
    }

    @Test
    fun create_forProfileNoLongerStored_reportsItDeletedWithoutAFailure() = runTest {
        // When
        val editor = editor(EditedProfile.Existing(Bia.id))

        // Then
        val deleted = editor.state.value
        assertEquals(Bia.id, deleted.deleted)
        assertFalse(deleted.canRetryLoad)
    }

    @Test
    fun create_whenStorageFailsToLoad_showsTheFailureAndNoDeletion() = runTest {
        // Given
        storage.failsLoad = true

        // When
        val editor = editor(EditedProfile.Existing(Ana.id))

        // Then
        val failed = editor.state.value
        assertIs<LoadResult.Error>(failed.load)
        assertNull(failed.deleted)
    }

    @Test
    fun load_retryAfterLoadFailure_showsTheProfile() = runTest {
        // Given
        storage.failsLoad = true
        val editor = editor(EditedProfile.Existing(Ana.id))
        storage.failsLoad = false

        // When
        editor.load()

        // Then
        val retried = editor.state.value
        assertTrue(retried.canSave)
        assertTrue(retried.canEdit)
        assertFalse(retried.canRetryLoad)
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
    fun save_whileInFlight_isUnavailableAndStoresOnce() = runTest {
        // Given
        val gate = CompletableDeferred<Unit>()
        storage.commandGate = gate
        val editor = editor(EditedProfile.New)
        editor.fill(name = "Cleo", waist = "70", date = "11/10/2026")
        editor.save()

        // When
        editor.save()

        // Then
        val saving = editor.state.value
        assertTrue(saving.isSaving)
        assertFalse(saving.canSave)
        gate.complete(Unit)
        assertEquals(2, storage.current.size)
    }

    @Test
    fun acknowledge_afterSaving_forgetsTheOutcomeAndStoresNothingMore() = runTest {
        // Given
        val editor = editor(EditedProfile.New)
        editor.fill(name = "Bia", waist = "95", date = "11/10/2026")
        editor.save()

        // When
        editor.acknowledge()
        editor.save()

        // Then
        assertNull(editor.state.value.saved)
        assertFalse(editor.state.value.canSave)
        assertEquals(2, storage.current.size)
    }

    @Test
    fun save_editedProfile_replacesItAndReportsTheUpdate() = runTest {
        // Given
        val editor = editor(EditedProfile.Existing(Ana.id))
        editor.edit(ProfileField.Waist, "82")

        // When
        editor.save()

        // Then
        assertEquals(Length(82.0), storage.current.single().measurements.waist)
        assertEquals(ProfileSave.Updated, editor.state.value.saved)
    }

    @Test
    fun save_profileDeletedMeanwhile_reportsItDeletedWithoutAFailure() = runTest {
        // Given
        val editor = editor(EditedProfile.Existing(Ana.id))
        editor.edit(ProfileField.Waist, "82")
        storage.delete(Ana.id)

        // When
        editor.save()

        // Then
        val deleted = editor.state.value
        assertEquals(Ana.id, deleted.deleted)
        assertNull(deleted.error)
        assertNull(deleted.saved)
        assertFalse(deleted.isSaving)
        assertTrue(storage.current.isEmpty())
    }

    @Test
    fun acknowledgeDeleted_afterTheProfileWasDeleted_forgetsTheOutcome() = runTest {
        // Given
        val editor = editor(EditedProfile.Existing(Bia.id))

        // When
        editor.acknowledgeDeleted()

        // Then
        assertNull(editor.state.value.deleted)
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
        assertEquals(R.string.profile_save_failed, failed.error)
        assertTrue(failed.canSave)
        assertNull(failed.saved)
        assertEquals("Bia", failed.inputs[ProfileField.Name])
        assertEquals(listOf(Ana), storage.current)
    }

    @Test
    fun edit_existingProfile_isDirtyOnlyWhileAValueDiffers() = runTest {
        listOf(
            DirtyCase("name changed", Ana, ProfileField.Name to "Ana Maria", isDirty = true),
            DirtyCase(
                "name changed back",
                Ana,
                ProfileField.Name to "Ana Maria",
                ProfileField.Name to "Ana",
                isDirty = false,
            ),
            DirtyCase(
                "waist retyped as the same length",
                Ana,
                ProfileField.Waist to "80,0",
                isDirty = false,
            ),
            // A name is text: a number written another way is another name.
            DirtyCase(
                "numeric name retyped as the same number",
                profile(1, "80"),
                ProfileField.Name to "80,0",
                isDirty = true,
            ),
        ).forEach { case ->
            // Given
            val storage = FakeProfileRepository(listOf(case.stored))
            val editor = ProfileEditorViewModel(
                storage,
                SavedStateHandle(),
                October13Clock,
                EditedProfile.Existing(case.stored.id),
            )

            // When
            case.edits.forEach { (field, text) -> editor.edit(field, text) }

            // Then
            assertEquals(case.isDirty, editor.state.value.isDirty, case.name)
        }
    }

    @Test
    fun recreate_afterProcessDeath_restoresTheForm() = runTest {
        editorScenario(EditedProfile.Existing(Ana.id)).use { scenario ->
            // Given
            scenario.viewModel.edit(ProfileField.Notes, "Sem folga")
            // A reload would replace the restored draft with the stored values.
            storage.update(Bia.copy(id = Ana.id))

            // When
            scenario.recreate()

            // Then
            val restored = scenario.viewModel.state.value
            assertEquals(Ana.id, restored.editingId)
            assertEquals("Sem folga", restored.inputs[ProfileField.Notes])
            assertEquals("Ana", restored.inputs[ProfileField.Name])
            assertTrue(restored.isDirty)
        }
    }

    @Test
    fun recreate_afterProcessDeathOnceUpdated_reportsTheUpdateAndStoresNothingMore() = runTest {
        editorScenario(EditedProfile.Existing(Ana.id)).use { scenario ->
            // Given
            scenario.viewModel.edit(ProfileField.Waist, "82")
            scenario.viewModel.save()
            val stored = storage.current

            // When
            scenario.recreate()
            scenario.viewModel.save()

            // Then
            assertEquals(ProfileSave.Updated, scenario.viewModel.state.value.saved)
            assertEquals(stored, storage.current)
        }
    }

    @Test
    fun recreate_afterProcessDeathOnceCreated_reportsTheCreatedProfile() = runTest {
        editorScenario(EditedProfile.New).use { scenario ->
            // Given
            scenario.viewModel.fill(name = "Bia", waist = "95", date = "11/10/2026")
            scenario.viewModel.save()
            val created = assertIs<ProfileSave.Created>(scenario.viewModel.state.value.saved)

            // When
            scenario.recreate()
            scenario.viewModel.save()

            // Then
            assertEquals(created, scenario.viewModel.state.value.saved)
            assertEquals(2, storage.current.size)
        }
    }

    /** Editing [stored] with [edits], in order, leaves the form [isDirty]. */
    private class DirtyCase(
        val name: String,
        val stored: MeasurementProfile,
        vararg val edits: Pair<ProfileField, String>,
        val isDirty: Boolean,
    )

    private fun editor(profile: EditedProfile) =
        ProfileEditorViewModel(storage, SavedStateHandle(), October13Clock, profile)

    private fun editorScenario(profile: EditedProfile) = viewModelScenario {
        ProfileEditorViewModel(storage, createSavedStateHandle(), October13Clock, profile)
    }

    private fun ProfileEditorViewModel.fill(name: String, waist: String, date: String) {
        edit(ProfileField.Name, name)
        edit(ProfileField.Waist, waist)
        edit(ProfileField.Date, date)
    }
}
