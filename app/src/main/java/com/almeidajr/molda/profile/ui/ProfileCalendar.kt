package com.almeidajr.molda.profile.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// Material calendar timestamps represent a date at UTC midnight in every timezone, so these
// conversions never use the device zone; only [today], which the caller derives from it, does.

internal fun calendarMillisToDate(utcTimeMillis: Long): LocalDate =
    Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()

internal fun dateToCalendarMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun isSelectableDate(date: LocalDate, today: LocalDate): Boolean = !date.isAfter(today)

/** The date the calendar opens on: the current value, otherwise [today], never after [today]. */
internal fun initialCalendarDate(current: LocalDate?, today: LocalDate): LocalDate =
    minOf(current ?: today, today)
