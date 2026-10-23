package com.almeidajr.molda.profile.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.profile.domain.ProfileValidation
import com.almeidajr.molda.profile.domain.validateProfile
import com.almeidajr.molda.profile.navigation.EditedProfile
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * One profile editor's form, its validation and its save. It starts blank for a new [profile] or
 * loads an existing one; a form restored after process death wins over both.
 */
@HiltViewModel(assistedFactory = ProfileEditorViewModel.Factory::class)
internal class ProfileEditorViewModel @AssistedInject constructor(
    private val repository: ProfileRepository,
    savedState: SavedStateHandle,
    private val clock: Clock,
    // The edited profile, not its ProfileId: Dagger cannot pass a value class to an assisted
    // constructor.
    @Assisted profile: EditedProfile,
) : ViewModel() {
    private val editingId = (profile as? EditedProfile.Existing)?.id

    // Null until an existing profile has loaded.
    private var savedForm: ProfileForm? by savedState.saved {
        ProfileForm().takeIf {
            editingId ==
                null
        }
    }
    private val mutableState = MutableStateFlow(
        savedForm?.let { ProfilesState(form = it, isLoading = false) }
            ?: ProfilesState(form = ProfileForm(editingId = editingId)),
    )
    val state: StateFlow<ProfilesState> = mutableState.asStateFlow()

    val today: LocalDate get() = LocalDate.now(clock)

    init {
        load()
    }

    /** Loads the existing profile; after a load failure, calling it again is the retry. */
    fun load() {
        val current = mutableState.value
        val isLoaded = !current.isLoading && current.loadError == null
        if (editingId == null || isLoaded) return
        transition { it.loading() }
        viewModelScope.launch {
            val result = repository.profiles()
                .map { Result.success(it) }
                .catch { failure -> emit(Result.failure(failure)) }
                .first()
            transition { it.loadedProfile(editingId, result) }
        }
    }

    fun edit(field: ProfileField, text: String) = transition { it.edited(field, text) }

    /** Stores the form unless it is busy or already stored; then it is [ProfilesState.saved]. */
    fun save() {
        val current = mutableState.value
        if (!current.canMutate || current.saved != null) return
        val validation = validateProfile(
            name = current.inputs.getValue(ProfileField.Name),
            waist = current.inputs.getValue(ProfileField.Waist),
            measurementDate = current.inputs.getValue(ProfileField.Date),
            notes = current.inputs.getValue(ProfileField.Notes),
            today = today,
        )
        transition { it.submitted(validation) }
        if (validation !is ProfileValidation.Valid) return
        viewModelScope.launch {
            val measurements = validation.measurements
            val result = editingId
                ?.let { id ->
                    repository.update(MeasurementProfile(id, measurements)).map {
                        ProfileSave.Updated
                    }
                }
                ?: repository.create(measurements).map(ProfileSave::Created)
            transition { it.savedWith(result) }
        }
    }

    // Main-thread only, so reading back the new value cannot race another transition.
    private fun transition(next: (ProfilesState) -> ProfilesState) {
        mutableState.value = next(mutableState.value)
        val state = mutableState.value
        if (!state.isLoading && state.loadError == null) savedForm = state.form
    }

    @AssistedFactory
    interface Factory {
        fun create(profile: EditedProfile): ProfileEditorViewModel
    }
}
