package com.almeidajr.molda.profile.ui

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.R
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileError
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.profile.domain.ProfileValidation
import com.almeidajr.molda.profile.domain.validateProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

enum class ProfileField(@param:StringRes val label: Int) {
    Name(R.string.profile_field_name),
    Waist(R.string.profile_field_waist),
    Date(R.string.profile_field_date),
    Notes(R.string.profile_field_notes),
}

val ProfileError.field: ProfileField
    get() = when (this) {
        ProfileError.BlankName -> ProfileField.Name
        ProfileError.InvalidWaist -> ProfileField.Waist
        ProfileError.InvalidDate, ProfileError.FutureDate -> ProfileField.Date
    }

@get:StringRes
val ProfileError.message: Int
    get() = when (this) {
        ProfileError.BlankName -> R.string.profile_error_blank_name
        ProfileError.InvalidWaist -> R.string.profile_error_invalid_waist
        ProfileError.InvalidDate -> R.string.profile_error_invalid_date
        ProfileError.FutureDate -> R.string.profile_error_future_date
    }

private val blankInputs = ProfileField.entries.associateWith { "" }

/** The profile editor state that survives process death; [editingId] is null for a new profile. */
@Serializable
data class ProfileForm(
    val inputs: Map<ProfileField, String> = blankInputs,
    val originalInputs: Map<ProfileField, String> = blankInputs,
    val isCreating: Boolean = false,
    val editingId: Long? = null,
)

data class ProfilesState(
    val form: ProfileForm = ProfileForm(),
    val profiles: List<MeasurementProfile> = emptyList(),
    val errors: Map<ProfileField, ProfileError> = emptyMap(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    @param:StringRes val loadError: Int? = null,
    @param:StringRes val saveError: Int? = null,
    @param:StringRes val savedMessage: Int? = null,
    val deleteId: ProfileId? = null,
) {
    val inputs: Map<ProfileField, String> get() = form.inputs
    val isCreating: Boolean get() = form.isCreating
    val isDirty: Boolean get() = form.inputs != form.originalInputs
    val editingId: ProfileId? get() = form.editingId?.let(::ProfileId)
    val canMutate: Boolean get() = !isLoading && !isSaving && loadError == null
}

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val repository: ProfileRepository,
    savedState: SavedStateHandle,
    private val clock: Clock,
) : ViewModel() {
    private var savedForm by savedState.saved { ProfileForm() }
    private val mutableState = MutableStateFlow(ProfilesState(form = savedForm))
    val state: StateFlow<ProfilesState> = mutableState.asStateFlow()

    val today: LocalDate get() = LocalDate.now(clock)

    init {
        load()
    }

    /** Follows the stored profiles; after a load failure, calling it again is the retry. */
    fun load() {
        val current = mutableState.value
        if (!current.isLoading && current.loadError == null) return
        transition { it.loading() }
        viewModelScope.launch {
            repository.profiles()
                .map { Result.success(it) }
                .catch { failure -> emit(Result.failure(failure)) }
                .collect { result -> transition { it.loadedWith(result) } }
        }
    }

    fun startCreating() = transition { it.opened(ProfileForm(isCreating = true)) }

    fun startEditing(id: ProfileId) = transition { it.editing(id) }

    fun edit(field: ProfileField, text: String) = transition { it.edited(field, text) }

    fun save() {
        val current = mutableState.value
        if (!current.canMutate || !current.isCreating) return
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
            val result = current.editingId
                ?.let { id -> repository.update(MeasurementProfile(id, measurements)) }
                ?: repository.create(measurements).map {}
            transition { it.savedWith(result) }
        }
    }

    fun delete(id: ProfileId) {
        val current = mutableState.value
        if (!current.canMutate || current.profiles.none { it.id == id }) return
        transition { it.mutating(deleteId = id) }
        viewModelScope.launch {
            val result = repository.delete(id)
            transition { it.deletedWith(id, result) }
        }
    }

    fun cancel() = transition { it.cancelled() }

    fun dismissMessage() = transition { it.copy(savedMessage = null) }

    // Main-thread only, so reading back the new value cannot race another transition.
    private fun transition(next: (ProfilesState) -> ProfilesState) {
        mutableState.value = next(mutableState.value)
        savedForm = mutableState.value.form
    }
}
