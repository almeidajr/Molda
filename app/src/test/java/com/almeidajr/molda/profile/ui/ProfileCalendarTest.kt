package com.almeidajr.molda.profile.ui

import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class ProfileCalendarTest {
    @Test
    fun dateToCalendarMillis_forADate_isUtcMidnight() {
        // When/Then
        assertEquals(1_791_763_200_000L, dateToCalendarMillis(LocalDate.of(2026, 10, 12)))
    }

    @Test
    fun calendarMillisToDate_aroundNewYear_keepsEachSideInItsYear() {
        // When/Then
        assertEquals(LocalDate.of(2025, 12, 31), calendarMillisToDate(1_767_225_599_999L))
        assertEquals(LocalDate.of(2026, 1, 1), calendarMillisToDate(1_767_225_600_000L))
    }

    @Test
    fun calendarConversions_beforeTheEpoch_useNegativeMillis() {
        // Given
        val date = LocalDate.of(1965, 3, 9)

        // When
        val millis = dateToCalendarMillis(date)

        // Then
        assertEquals(-151_977_600_000L, millis)
        assertEquals(date, calendarMillisToDate(millis))
    }

    @Test
    fun isSelectableDate_today_isAllowed() {
        // Given
        val today = LocalDate.of(2026, 10, 12)

        // When/Then
        assertTrue(isSelectableDate(today, today))
    }

    @Test
    fun isSelectableDate_tomorrow_isRefused() {
        // When/Then
        assertFalse(isSelectableDate(LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 12)))
    }

    @Test
    fun initialCalendarDate_withoutValue_isToday() {
        // Given
        val today = LocalDate.of(2026, 10, 12)

        // When/Then
        assertEquals(today, initialCalendarDate(null, today))
    }

    @Test
    fun initialCalendarDate_withPastValue_isTheValue() {
        // When/Then
        assertEquals(
            LocalDate.of(2026, 1, 5),
            initialCalendarDate(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 10, 12)),
        )
    }

    @Test
    fun initialCalendarDate_withFutureValue_clampsToToday() {
        // Given
        val today = LocalDate.of(2026, 10, 12)

        // When/Then
        assertEquals(today, initialCalendarDate(today.plusDays(3), today))
    }

    @Test
    fun initialCalendarSelection_withPastValue_selectsIt() {
        // When/Then
        assertEquals(
            LocalDate.of(2026, 1, 5),
            initialCalendarSelection(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 10, 12)),
        )
    }

    @Test
    fun initialCalendarSelection_withFutureValue_selectsNothing() {
        // When/Then
        assertNull(initialCalendarSelection(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 12)))
    }
}
