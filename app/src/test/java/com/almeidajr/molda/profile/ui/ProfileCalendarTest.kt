package com.almeidajr.molda.profile.ui

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class ProfileCalendarTest {
    private val zones = listOf("UTC", "Pacific/Kiritimati", "Pacific/Pago_Pago").map(ZoneId::of)
    private val instant = ZonedDateTime.of(2026, 10, 12, 23, 30, 0, 0, ZoneId.of("UTC")).toInstant()

    @Test
    fun calendarConversions_inAnyZone_roundTripTheDatePicked() {
        zones.forEach { zone ->
            // Given
            val today = instant.atZone(zone).toLocalDate()

            // When/Then
            assertEquals(today, calendarMillisToDate(dateToCalendarMillis(today)), zone.id)
        }
    }

    @Test
    fun dateToCalendarMillis_forADate_isUtcMidnight() {
        // When/Then
        assertEquals(1_791_763_200_000L, dateToCalendarMillis(LocalDate.of(2026, 10, 12)))
    }

    @Test
    fun isSelectableDate_afterToday_isRefusedInEveryZone() {
        zones.forEach { zone ->
            // Given
            val today = instant.atZone(zone).toLocalDate()

            // When/Then
            assertTrue(isSelectableDate(today, today), zone.id)
            assertFalse(isSelectableDate(today.plusDays(1), today), zone.id)
        }
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
}
