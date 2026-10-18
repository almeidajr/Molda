package com.almeidajr.molda.profile.ui

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.R
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileDateFormat
import com.almeidajr.molda.profile.domain.ProfileError
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.profile.domain.ProfileValidation
import com.almeidajr.molda.profile.domain.validateProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

data class ProfilesState(
    val profiles: List<MeasurementProfile> = emptyList(),
    val inputs: Map<ProfileField, String> = ProfileField.entries.associateWith { "" },
    val errors: Map<ProfileField, ProfileError> = emptyMap(),
    val isCreating: Boolean = false,
    val isDirty: Boolean = false,
    val editingId: ProfileId? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    @param:StringRes val loadError: Int? = null,
    @param:StringRes val saveError: Int? = null,
    @param:StringRes val savedMessage: Int? = null,
    val deleteId: ProfileId? = null,
)

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val savedState: SavedStateHandle,
    private val clock: Clock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        ProfilesState(
            inputs = ProfileField.entries.associateWith { savedState[it.name] ?: "" },
            isCreating = savedState["isCreating"] ?: false,
            isDirty = savedState["isDirty"] ?: false,
            editingId = savedState.get<Long>("editingId")?.let(::ProfileId),
        ),
    )
    val state: StateFlow<ProfilesState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (!mutableState.value.isLoading && mutableState.value.loadError == null) return
        mutableState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            try {
                val profiles = repository.profiles().first()
                mutableState.update { it.copy(profiles = profiles.toList(), isLoading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(
                        isLoading = false,
                        loadError = R.string.profile_load_failed,
                    )
                }
            }
        }
    }

    fun startCreating() {
        if (!canMutate()) return
        clearSavedForm()
        savedState["isCreating"] = true
        savedState["editingId"] = null
        mutableState.update {
            it.copy(
                inputs = ProfileField.entries.associateWith {
                    ""
                },
                isCreating = true,
                isDirty = false,
                editingId = null,
                errors = emptyMap(),
                savedMessage = null,
                saveError = null,
                deleteId = null,
            )
        }
    }

    fun startEditing(id: ProfileId) {
        if (!canMutate()) return
        val profile = mutableState.value.profiles.find { it.id == id } ?: return
        val measurements = profile.measurements
        val inputs = mapOf(
            ProfileField.Name to measurements.name.text,
            ProfileField.Waist to profileWaistText(measurements.waist),
            ProfileField.Date to measurements.measurementDate.format(ProfileDateFormat),
            ProfileField.Notes to measurements.notes,
        )
        inputs.forEach { (field, text) -> savedState[field.name] = text }
        savedState["isCreating"] = true
        savedState["editingId"] = id.value
        savedState["originalInputs"] = HashMap(inputs.mapKeys { it.key.name })
        savedState["isDirty"] = false
        mutableState.update {
            it.copy(
                inputs = inputs,
                isCreating = true,
                editingId = id,
                isDirty = false,
                errors = emptyMap(),
                savedMessage = null,
                saveError = null,
                deleteId = null,
            )
        }
    }

    fun edit(field: ProfileField, text: String) {
        if (mutableState.value.isSaving) return
        savedState[field.name] = text
        val inputs = mutableState.value.inputs + (field to text)
        val original: HashMap<String, String>? = savedState["originalInputs"]
        val isDirty = inputs.any { (key, value) -> value != original?.get(key.name).orEmpty() }
        savedState["isDirty"] = isDirty
        mutableState.update {
            it.copy(
                inputs = inputs,
                isDirty = isDirty,
                errors = it.errors - field,
                saveError = null,
                deleteId = null,
            )
        }
    }

    fun save() {
        val current = mutableState.value
        if (!canMutate() || !current.isCreating) return
        val validation = validateProfile(
            name = current.inputs.getValue(ProfileField.Name),
            waist = current.inputs.getValue(ProfileField.Waist),
            measurementDate = current.inputs.getValue(ProfileField.Date),
            notes = current.inputs.getValue(ProfileField.Notes),
            today = today,
        )
        val measurements = when (validation) {
            is ProfileValidation.Invalid -> {
                val errors = validation.errors.associateBy { error -> error.field }
                mutableState.update { it.copy(errors = errors) }
                return
            }

            is ProfileValidation.Valid -> validation.measurements
        }
        mutableState.update { it.copy(isSaving = true, saveError = null, savedMessage = null) }
        viewModelScope.launch {
            try {
                val profile = current.editingId?.let { id ->
                    MeasurementProfile(id, measurements).also { repository.update(it).getOrThrow() }
                } ?: repository.create(measurements).getOrThrow()
                clearSavedForm()
                mutableState.update {
                    it.copy(
                        profiles = it.profiles.withSaved(profile),
                        inputs = ProfileField.entries.associateWith { "" },
                        isCreating = false,
                        isDirty = false,
                        editingId = null,
                        isSaving = false,
                        errors = emptyMap(),
                        savedMessage = R.string.profile_saved,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        saveError = R.string.profile_save_failed,
                    )
                }
            }
        }
    }

    fun delete(id: ProfileId) {
        if (!canMutate() || mutableState.value.profiles.none { it.id == id }) return
        mutableState.update {
            it.copy(isSaving = true, saveError = null, savedMessage = null, deleteId = id)
        }
        viewModelScope.launch {
            try {
                repository.delete(id).getOrThrow()
                if (mutableState.value.editingId == id) {
                    clearSavedForm()
                    mutableState.update {
                        it.copy(
                            inputs = ProfileField.entries.associateWith { "" },
                            isCreating = false,
                            isDirty = false,
                            editingId = null,
                            errors = emptyMap(),
                        )
                    }
                }
                mutableState.update {
                    it.copy(
                        profiles = it.profiles.filterNot { profile -> profile.id == id },
                        isSaving = false,
                        savedMessage = R.string.profile_deleted,
                        deleteId = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        saveError = R.string.profile_delete_failed,
                    )
                }
            }
        }
    }

    val today: LocalDate get() = LocalDate.now(clock)

    fun cancel() {
        if (mutableState.value.isSaving) return
        clearSavedForm()
        mutableState.update {
            it.copy(
                inputs = ProfileField.entries.associateWith { "" },
                isCreating = false,
                isDirty = false,
                editingId = null,
                errors = emptyMap(),
                saveError = null,
                deleteId = null,
            )
        }
    }

    fun dismissMessage() {
        mutableState.update { it.copy(savedMessage = null) }
    }
    private fun canMutate(): Boolean = mutableState.value.let {
        !it.isLoading && !it.isSaving && it.loadError == null
    }

    private fun clearSavedForm() {
        ProfileField.entries.forEach { savedState[it.name] = "" }
        savedState["originalInputs"] = null
        savedState["isDirty"] = false
        savedState["isCreating"] = false
        savedState["editingId"] = null
    }
}

private fun List<MeasurementProfile>.withSaved(profile: MeasurementProfile) =
    if (any { it.id == profile.id }) {
        map {
            if (it.id ==
                profile.id
            ) {
                profile
            } else {
                it
            }
        }
    } else {
        this + profile
    }
