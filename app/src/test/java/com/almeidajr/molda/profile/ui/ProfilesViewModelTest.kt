package com.almeidajr.molda.profile.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.testing.viewModelScenario
import app.cash.turbine.test
import com.almeidajr.molda.R
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileError
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.testing.FakeProfileRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric supplies the real Bundle that saved state is parcelled through.
@RunWith(RobolectricTestRunner::class)
class ProfilesViewModelTest {
    private val storage = FakeProfileRepository(listOf(ana))

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun profiles_followTheRepositoryWithoutReloading() = runTest {
        val profiles = profilesViewModel()
        assertEquals(listOf(ana), profiles.state.value.profiles)

        val added = storage.create(bia.measurements).getOrThrow()

        assertEquals(listOf(ana, added), profiles.state.value.profiles)
    }

    @Test
    fun load_afterLoadFailure_retryShowsStoredProfiles() = runTest {
        storage.failsLoad = true
        val profiles = profilesViewModel()

        profiles.state.test {
            assertEquals(R.string.profile_load_failed, awaitItem().loadError)

            storage.failsLoad = false
            profiles.load()

            val retried = expectMostRecentItem()
            assertNull(retried.loadError)
            assertEquals(listOf(ana), retried.profiles)
        }
    }

    @Test
    fun load_whileStillLoading_keepsOneRepositoryCollector() = runTest {
        storage.loadGate = CompletableDeferred()
        val counted = CollectorCounting(storage)
        val profiles = ProfilesViewModel(counted, SavedStateHandle(), clock)

        profiles.load()
        profiles.load()
        storage.loadGate?.complete(Unit)

        assertEquals(1, counted.activeCollectors)
        assertEquals(listOf(ana), profiles.state.value.profiles)
    }

    @Test
    fun save_newProfile_closesTheFormAndListsIt() = runTest {
        val profiles = profilesViewModel()

        profiles.startCreating()
        profiles.fill(name = "Bia", waist = "95", date = "11/10/2026")
        profiles.save()

        val saved = profiles.state.value
        assertEquals(listOf("Ana", "Bia"), saved.profiles.map { it.measurements.name.text })
        assertFalse(saved.isEditorOpen)
        assertEquals(R.string.profile_saved, saved.savedMessage)
        assertEquals(saved.profiles.last(), saved.createdProfile)
    }

    @Test
    fun save_editedProfile_replacesItInTheList() = runTest {
        val profiles = profilesViewModel()

        profiles.startEditing(ana.id)
        profiles.edit(ProfileField.Waist, "82")
        profiles.save()

        assertEquals(Length(82.0), profiles.state.value.profiles.single().measurements.waist)
    }

    @Test
    fun save_withInvalidInputs_showsFieldErrors() = runTest {
        val profiles = profilesViewModel()

        profiles.startCreating()
        profiles.fill(name = " ", waist = "0", date = "14/10/2026")
        profiles.save()

        assertEquals(
            mapOf(
                ProfileField.Name to ProfileError.BlankName,
                ProfileField.Waist to ProfileError.InvalidWaist,
                ProfileField.Date to ProfileError.FutureDate,
            ),
            profiles.state.value.errors,
        )
    }

    @Test
    fun save_whenStorageFails_keepsTheFormAndShowsTheFailure() = runTest {
        storage.failsCommands = true
        val profiles = profilesViewModel()

        profiles.startCreating()
        profiles.fill(name = "Bia", waist = "95", date = "11/10/2026")
        profiles.save()

        val failed = profiles.state.value
        assertEquals(R.string.profile_save_failed, failed.saveError)
        assertTrue(failed.isEditorOpen)
        assertEquals("Bia", failed.inputs[ProfileField.Name])
        assertEquals(listOf(ana), failed.profiles)
    }

    @Test
    fun delete_profileBeingEdited_removesItAndClosesTheForm() = runTest {
        val profiles = profilesViewModel()

        profiles.startEditing(ana.id)
        profiles.delete(ana.id)

        val deleted = profiles.state.value
        assertEquals(emptyList<MeasurementProfile>(), deleted.profiles)
        assertFalse(deleted.isEditorOpen)
        assertEquals(R.string.profile_deleted, deleted.savedMessage)
    }

    @Test
    fun delete_whenStorageFails_keepsTheProfileAndShowsTheFailure() = runTest {
        storage.failsCommands = true
        val profiles = profilesViewModel()

        profiles.delete(ana.id)

        val failed = profiles.state.value
        assertEquals(R.string.profile_delete_failed, failed.saveError)
        assertEquals(ana.id, failed.deleteId)
        assertEquals(listOf(ana), failed.profiles)
    }

    @Test
    fun edit_isDirtyOnlyWhileTheFormDiffersFromTheProfile() = runTest {
        val profiles = profilesViewModel()
        profiles.startEditing(ana.id)

        profiles.edit(ProfileField.Name, "Ana Maria")
        assertTrue(profiles.state.value.isDirty)

        profiles.edit(ProfileField.Name, "Ana")
        assertFalse(profiles.state.value.isDirty)
    }

    @Test
    fun recreate_afterProcessDeath_restoresTheForm() = runTest {
        viewModelScenario { ProfilesViewModel(storage, createSavedStateHandle(), clock) }
            .use { scenario ->
                scenario.viewModel.startEditing(ana.id)
                scenario.viewModel.edit(ProfileField.Notes, "Sem folga")

                scenario.recreate()

                val restored = scenario.viewModel.state.value
                assertEquals(ana.id, restored.editingId)
                assertEquals("Sem folga", restored.inputs[ProfileField.Notes])
                assertEquals("80", restored.inputs[ProfileField.Waist])
                assertTrue(restored.isDirty)
            }
    }

    private fun profilesViewModel() = ProfilesViewModel(storage, SavedStateHandle(), clock)

    private fun ProfilesViewModel.fill(name: String, waist: String, date: String) {
        edit(ProfileField.Name, name)
        edit(ProfileField.Waist, waist)
        edit(ProfileField.Date, date)
    }

    private val clock = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC)
}

private val ana = profile(1, "Ana", 80.0)
private val bia = profile(2, "Bia", 95.0)

private fun profile(id: Long, name: String, waist: Double) = MeasurementProfile(
    ProfileId(id),
    ProfileMeasurements(ProfileName(name), Length(waist), LocalDate.of(2026, 10, 12)),
)

/** Counts the collectors of [profiles], to show a retry replaces the pending one. */
private class CollectorCounting(private val profiles: ProfileRepository) :
    ProfileRepository by profiles {
    var activeCollectors = 0
        private set

    override fun profiles(): Flow<List<MeasurementProfile>> = profiles.profiles()
        .onStart { activeCollectors++ }
        .onCompletion { activeCollectors-- }
}
