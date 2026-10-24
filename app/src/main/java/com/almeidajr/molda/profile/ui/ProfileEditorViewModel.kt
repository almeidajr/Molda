package com.almeidajr.molda.profile.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.data.LoadResult
import com.almeidajr.molda.data.asLoadResult
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.profile.domain.ProfileValidation
import com.almeidajr.molda.profile.domain.validateProfile
import com.almeidajr.molda.profile.navigation.EditedProfile
import com.almeidajr.molda.ui.navigation.EditorKind
import com.almeidajr.molda.ui.navigation.EditorReport
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One profile editor's form, its validation and its save. It starts blank for a new [profile] or
 * loads an existing one; a form restored after process death wins over both, and one already
 * stored comes back saved, as what its save did, so its entry closes.
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
        savedForm?.let { form ->
            // Nothing to load: the form is what the editor shows.
            ProfileEditorState(form = form, load = LoadResult.Success(Unit), saved = form.saved)
        }
            ?: ProfileEditorState(form = ProfileForm(editingId = editingId)),
    )
    val state: StateFlow<ProfileEditorState> = mutableState.asStateFlow()

    /** This editor's status for the app, which asks before its changes are dropped. */
    val report: StateFlow<EditorReport> = state.map { it.report }
        .stateIn(viewModelScope, SharingStarted.Eagerly, state.value.report)

    val today: LocalDate get() = LocalDate.now(clock)

    init {
        load()
    }

    /** Loads the existing profile; after a load failure, calling it again is the retry. */
    fun load() {
        if (editingId == null || mutableState.value.load is LoadResult.Success) return
        transition { it.copy(load = LoadResult.Loading) }
        viewModelScope.launch {
            val result = repository.profiles()
                .map { profiles ->
                    checkNotNull(profiles.find { it.id == editingId }) {
                        "Profile no longer exists"
                    }
                }
                .asLoadResult()
                .first { it !is LoadResult.Loading }
            transition { it.loadedProfile(result) }
        }
    }

    fun edit(field: ProfileField, text: String) = transition { it.edited(field, text) }

    /** Stores the form unless Save is unavailable; then it is [ProfileEditorState.saved]. */
    fun save() {
        val current = mutableState.value
        if (!current.canSave) return
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
    private fun transition(next: (ProfileEditorState) -> ProfileEditorState) {
        mutableState.value = next(mutableState.value)
        val state = mutableState.value
        if (state.load is LoadResult.Success) savedForm = state.form
    }

    @AssistedFactory
    interface Factory {
        fun create(profile: EditedProfile): ProfileEditorViewModel
    }
}

private val ProfileEditorState.report get() = EditorReport(EditorKind.Profile, isDirty, isSaving)
