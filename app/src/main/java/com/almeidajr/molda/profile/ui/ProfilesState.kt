package com.almeidajr.molda.profile.ui

import androidx.annotation.StringRes
import com.almeidajr.molda.R
import com.almeidajr.molda.data.LoadResult
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileError
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
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

/**
 * The profile editor state that survives process death; [editingId] is null for a new profile.
 * [isStored] once the editor's save has stored it, so a restored editor never stores it again.
 */
@Serializable
data class ProfileForm(
    val inputs: Map<ProfileField, String> = blankInputs,
    val originalInputs: Map<ProfileField, String> = blankInputs,
    val editingId: ProfileId? = null,
    val isStored: Boolean = false,
)

/** What a successful save did to the stored profiles. */
sealed interface ProfileSave {
    data class Created(val profile: MeasurementProfile) : ProfileSave

    data object Updated : ProfileSave
}

/**
 * A profile list's or a profile editor's state: the list uses [load] and deletion, the editor
 * its [form] and its save.
 */
data class ProfilesState(
    val form: ProfileForm = ProfileForm(),
    /**
     * The stored profiles: the list's rows, or for an editor the profile its form was filled from,
     * none when the form did not come from storage.
     */
    val load: LoadResult<List<MeasurementProfile>> = LoadResult.Loading,
    val errors: Map<ProfileField, ProfileError> = emptyMap(),
    val isSaving: Boolean = false,
    @param:StringRes val saveError: Int? = null,
    /** What the editor's save stored, until its entry has shown it and closed the editor. */
    val saved: ProfileSave? = null,
    /** The profile the list deleted, until its entry has shown it. */
    val deleted: ProfileId? = null,
    val deleteId: ProfileId? = null,
) {
    val inputs: Map<ProfileField, String> get() = form.inputs
    val isDirty: Boolean get() = form.inputs != form.originalInputs
    val editingId: ProfileId? get() = form.editingId
    val canMutate: Boolean get() = load is LoadResult.Success && !isSaving

    /** The picker's rows: the stored profiles, each with whether its notes are shown. */
    val pickerRows: LoadResult<List<PickerRow>>
        get() = when (load) {
            is LoadResult.Success -> LoadResult.Success(pickerRows(load.value))
            LoadResult.Loading -> LoadResult.Loading
            is LoadResult.Error -> load
        }
}

/** A profile in the picker; [showNotes] when its notes tell it apart from lookalikes. */
data class PickerRow(val profile: MeasurementProfile, val showNotes: Boolean)

/**
 * Each of [profiles] as a picker row, shown with notes when another profile has the same name,
 * waist and measurement date; the notes themselves do not count towards being alike.
 */
fun pickerRows(profiles: List<MeasurementProfile>): List<PickerRow> {
    val sizes = profiles.groupingBy { it.measurements.lookalikeKey() }.eachCount()
    return profiles.map {
        PickerRow(it, showNotes = sizes.getValue(it.measurements.lookalikeKey()) > 1)
    }
}

private fun ProfileMeasurements.lookalikeKey() = Triple(name, waist, measurementDate)
