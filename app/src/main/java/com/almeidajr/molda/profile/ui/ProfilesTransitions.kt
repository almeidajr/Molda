package com.almeidajr.molda.profile.ui

import com.almeidajr.molda.R
import com.almeidajr.molda.data.LoadResult
import com.almeidajr.molda.measurement.lengthText
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileValidation

/** Fills the editor with the stored profile once [result] has it. */
internal fun ProfilesState.loadedProfile(result: LoadResult<MeasurementProfile>) = when (result) {
    is LoadResult.Success -> {
        val profile = result.value
        val measurements = profile.measurements
        val inputs = mapOf(
            ProfileField.Name to measurements.name.text,
            ProfileField.Waist to lengthText(measurements.waist),
            ProfileField.Date to profileDateText(measurements.measurementDate),
            ProfileField.Notes to measurements.notes,
        )
        copy(
            form = ProfileForm(inputs, inputs, profile.id),
            load = LoadResult.Success(listOf(profile)),
        )
    }

    is LoadResult.Error -> copy(load = result)

    LoadResult.Loading -> copy(load = LoadResult.Loading)
}

internal fun ProfilesState.edited(field: ProfileField, text: String) =
    if (isSaving || load is LoadResult.Loading) {
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
