package com.almeidajr.molda.profile.ui

import com.almeidajr.molda.R
import com.almeidajr.molda.measurement.lengthText
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileValidation

internal fun ProfilesState.loading() = copy(isLoading = true, loadError = null)

internal fun ProfilesState.loadedWith(result: Result<List<MeasurementProfile>>) = result.fold(
    onSuccess = { copy(profiles = it, isLoading = false) },
    onFailure = { copy(isLoading = false, loadError = R.string.profile_load_failed) },
)

/** Fills the editor with stored profile [id] from [result], or fails when it is not there. */
internal fun ProfilesState.loadedProfile(id: ProfileId, result: Result<List<MeasurementProfile>>) =
    when (val profile = result.getOrNull()?.find { it.id == id }) {
        null -> copy(isLoading = false, loadError = R.string.profile_load_failed)

        else -> {
            val measurements = profile.measurements
            val inputs = mapOf(
                ProfileField.Name to measurements.name.text,
                ProfileField.Waist to lengthText(measurements.waist),
                ProfileField.Date to profileDateText(measurements.measurementDate),
                ProfileField.Notes to measurements.notes,
            )
            copy(form = ProfileForm(inputs, inputs, id), isLoading = false)
        }
    }

internal fun ProfilesState.edited(field: ProfileField, text: String) = if (isSaving || isLoading) {
    this
} else {
    copy(
        form = form.copy(inputs = form.inputs + (field to text)),
        errors = errors - field,
        saveError = null,
    )
}

/** Shows the field errors of an invalid form, or starts storing a valid one. */
internal fun ProfilesState.submitted(validation: ProfileValidation) = when (validation) {
    is ProfileValidation.Invalid -> copy(errors = validation.errors.associateBy { it.field })
    is ProfileValidation.Valid -> mutating()
}

internal fun ProfilesState.mutating(deleteId: ProfileId? = null) =
    copy(isSaving = true, saveError = null, deleteId = deleteId)

/** Keeps the stored form as the clean one and reports what the save did. */
internal fun ProfilesState.savedWith(result: Result<ProfileSave>) = result.fold(
    onSuccess = {
        val stored = form.copy(originalInputs = form.inputs, isStored = true)
        copy(form = stored, isSaving = false, saved = it)
    },
    onFailure = { copy(isSaving = false, saveError = R.string.profile_save_failed) },
)

internal fun ProfilesState.deletedWith(id: ProfileId, result: Result<Unit>) = result.fold(
    onSuccess = { copy(isSaving = false, deleteId = null, deleted = id) },
    onFailure = { copy(isSaving = false, saveError = R.string.profile_delete_failed) },
)
