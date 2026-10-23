package com.almeidajr.molda.profile.ui

import com.almeidajr.molda.data.LoadResult
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.Test

class PickerRowsTest {
    @Test
    fun pickerRows_withLookalikes_showNotesOnlyForThem() {
        // Given
        val first = profile(1, "Ana", 80.0, "Before pregnancy")
        val second = profile(2, "Ana", 80.0, "After pregnancy")
        val unique = profile(3, "Bia", 80.0, "Alone")

        // When
        val rows = pickerRows(listOf(first, second, unique))

        // Then
        assertEquals(listOf(true, true, false), rows.map(PickerRow::showNotes))
    }

    @Test
    fun pickerRows_differingInOneOfNameWaistOrDate_doNotShowNotes() {
        // Given
        val ana = profile(1, "Ana", 80.0)
        val profiles = listOf(
            ana,
            profile(2, "Bia", 80.0),
            profile(3, "Ana", 81.0),
            profile(4, "Ana", 80.0, date = LocalDate.of(2026, 10, 13)),
        )

        // When
        val rows = pickerRows(profiles)

        // Then
        assertEquals(listOf(false, false, false, false), rows.map(PickerRow::showNotes))
    }

    @Test
    fun pickerRows_keepsEachProfileInOrder() {
        // Given
        val profiles = listOf(profile(2, "Ana", 80.0), profile(1, "Ana", 80.0))

        // When
        val rows = pickerRows(profiles)

        // Then
        assertEquals(profiles, rows.map(PickerRow::profile))
    }

    @Test
    fun pickerRows_ofState_followsItsLoad() {
        // Given
        val ana = profile(1, "Ana", 80.0)

        // When
        val state = ProfilesState(load = LoadResult.Success(listOf(ana)))

        // Then
        assertEquals(
            LoadResult.Success(listOf(PickerRow(ana, showNotes = false))),
            state.pickerRows,
        )
        assertEquals(LoadResult.Loading, ProfilesState().pickerRows)
    }

    private fun profile(
        id: Long,
        name: String,
        waist: Double,
        notes: String = "",
        date: LocalDate = LocalDate.of(2026, 10, 12),
    ) = MeasurementProfile(
        ProfileId(id),
        ProfileMeasurements(ProfileName(name), Length(waist), date, notes),
    )
}
