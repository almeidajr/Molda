package com.almeidajr.molda.profile.ui

import com.almeidajr.molda.R
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileValidation

internal fun ProfilesState.loading() = copy(isLoading = true, loadError = null)

internal fun ProfilesState.loadedWith(result: Result<List<MeasurementProfile>>) = result.fold(
    onSuccess = { copy(profiles = it, isLoading = false) },
    onFailure = { copy(isLoading = false, loadError = R.string.profile_load_failed) },
)

/** Opens the editor on [form] unless storage is busy or unavailable. */
internal fun ProfilesState.opened(form: ProfileForm) = if (canMutate) {
    copy(form = form, errors = emptyMap(), savedMessage = null, saveError = null, deleteId = null)
} else {
    this
}

internal fun ProfilesState.editing(id: ProfileId): ProfilesState {
    val measurements = profiles.find { it.id == id }?.measurements ?: return this
    val inputs = mapOf(
        ProfileField.Name to measurements.name.text,
        ProfileField.Waist to profileWaistText(measurements.waist),
        ProfileField.Date to profileDateText(measurements.measurementDate),
        ProfileField.Notes to measurements.notes,
    )
    return opened(ProfileForm(inputs, inputs, isEditorOpen = true, editingId = id))
}

internal fun ProfilesState.edited(field: ProfileField, text: String) = if (isSaving) {
    this
} else {
    copy(
        form = form.copy(inputs = form.inputs + (field to text)),
        errors = errors - field,
        saveError = null,
        deleteId = null,
    )
}

/** Shows the field errors of an invalid form, or starts storing a valid one. */
internal fun ProfilesState.submitted(validation: ProfileValidation) = when (validation) {
    is ProfileValidation.Invalid -> copy(errors = validation.errors.associateBy { it.field })
    is ProfileValidation.Valid -> mutating()
}

internal fun ProfilesState.mutating(deleteId: ProfileId? = null) =
    copy(isSaving = true, saveError = null, savedMessage = null, deleteId = deleteId)

/** Closes the editor after a save; [result] holds the profile when the save created one. */
internal fun ProfilesState.savedWith(result: Result<MeasurementProfile?>) = result.fold(
    onSuccess = {
        closed().copy(isSaving = false, savedMessage = R.string.profile_saved, createdProfile = it)
    },
    onFailure = { copy(isSaving = false, saveError = R.string.profile_save_failed) },
)

internal fun ProfilesState.deletedWith(id: ProfileId, result: Result<Unit>) = result.fold(
    onSuccess = {
        (if (editingId == id) closed() else this)
            .copy(isSaving = false, savedMessage = R.string.profile_deleted, deleteId = null)
    },
    onFailure = { copy(isSaving = false, saveError = R.string.profile_delete_failed) },
)

internal fun ProfilesState.cancelled() =
    if (isSaving) this else closed().copy(saveError = null, deleteId = null)

private fun ProfilesState.closed() = copy(form = ProfileForm(), errors = emptyMap())
