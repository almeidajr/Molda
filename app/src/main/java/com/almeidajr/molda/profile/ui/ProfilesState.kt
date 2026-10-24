package com.almeidajr.molda.profile.ui

import androidx.annotation.StringRes
import com.almeidajr.molda.R
import com.almeidajr.molda.data.LoadResult
import com.almeidajr.molda.measurement.isSameLength
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
 * [saved] is what the editor's save did, once it has stored the form, so a restored editor
 * reports that save again and never stores the form again.
 */
@Serializable
data class ProfileForm(
    val inputs: Map<ProfileField, String> = blankInputs,
    val originalInputs: Map<ProfileField, String> = blankInputs,
    val editingId: ProfileId? = null,
    val saved: ProfileSave? = null,
)

/** Whether an input differs from the stored one; a waist retyped as the same length does not. */
private val ProfileForm.isDirty: Boolean
    get() = ProfileField.entries.any { field ->
        val text = inputs.getValue(field)
        val original = originalInputs.getValue(field)
        if (field == ProfileField.Waist) !isSameLength(text, original) else text != original
    }

/** What a successful save did to the stored profiles. */
@Serializable
sealed interface ProfileSave {
    @Serializable
    data class Created(val profile: MeasurementProfile) : ProfileSave

    @Serializable
    data object Updated : ProfileSave
}

/** The list's delete of [id], in flight or failed; a failure stays until the next delete. */
sealed interface ProfileDeletion {
    val id: ProfileId

    data class Running(override val id: ProfileId) : ProfileDeletion

    data class Failed(override val id: ProfileId) : ProfileDeletion
}

/** A list of the stored profiles, Perfis' or a picker's, and the deletions made from it. */
data class ProfileListState(
    val load: LoadResult<List<MeasurementProfile>> = LoadResult.Loading,
    val deletion: ProfileDeletion? = null,
    /** The profile the list deleted, until its entry has reacted to it. */
    val deleted: ProfileId? = null,
) {
    /** Whether the rows can be edited, deleted or added to now: loaded, no delete in flight. */
    val canChangeProfiles: Boolean
        get() = load is LoadResult.Success && deletion !is ProfileDeletion.Running

    fun isDeleting(id: ProfileId): Boolean = deletion == ProfileDeletion.Running(id)

    /** The failure of deleting profile [id], shown on its row only. */
    @StringRes
    fun deleteError(id: ProfileId): Int? =
        R.string.profile_delete_failed.takeIf { deletion == ProfileDeletion.Failed(id) }

    /** The picker's rows: the stored profiles, each with whether its notes are shown. */
    val pickerRows: LoadResult<List<PickerRow>>
        get() = when (load) {
            is LoadResult.Success -> LoadResult.Success(pickerRows(load.value))
            LoadResult.Loading -> LoadResult.Loading
            is LoadResult.Error -> load
        }
}

/** The editor's save, in flight or failed; a failure stays until the form changes or saves. */
enum class ProfileSaving { Running, Failed }

/** One profile editor's form and its save. */
data class ProfileEditorState(
    val form: ProfileForm = ProfileForm(),
    /** Whether the form is ready: a new or restored one is, an existing profile once loaded. */
    val load: LoadResult<Unit> = LoadResult.Loading,
    val errors: Map<ProfileField, ProfileError> = emptyMap(),
    val saving: ProfileSaving? = null,
    /** What the editor's save stored, until its entry has reacted to it and closed the editor. */
    val saved: ProfileSave? = null,
) {
    val inputs: Map<ProfileField, String> get() = form.inputs
    val isDirty: Boolean get() = form.isDirty
    val editingId: ProfileId? get() = form.editingId
    val isSaving: Boolean get() = saving == ProfileSaving.Running

    /** Whether the fields take edits: once the form is ready, and never while saving. */
    val canEdit: Boolean get() = load is LoadResult.Success && !isSaving

    /** Whether Save is available: editable and not stored yet (acknowledged or not). */
    val canSave: Boolean get() = canEdit && form.saved == null

    /** Whether loading the profile failed, so the editor shows the failure and a retry. */
    val canRetryLoad: Boolean get() = load is LoadResult.Error

    /** Whether the "stored on this device only" note shows: on a new profile only. */
    val showsLocalOnlyNote: Boolean get() = editingId == null

    @get:StringRes
    val title: Int
        get() = if (editingId == null) R.string.profile_editor_new else R.string.profile_editor_edit

    @get:StringRes
    val saveLabel: Int
        get() = when {
            isSaving -> R.string.status_saving
            editingId != null -> R.string.profile_save_changes
            else -> R.string.profile_save_new
        }

    /** The editor's failure to store its form. */
    @get:StringRes
    val error: Int? get() = R.string.profile_save_failed.takeIf { saving == ProfileSaving.Failed }
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
