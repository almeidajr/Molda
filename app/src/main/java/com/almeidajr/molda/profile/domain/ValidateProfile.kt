package com.almeidajr.molda.profile.domain

import com.almeidajr.molda.measurement.ParsedLength
import com.almeidajr.molda.measurement.parseLength
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

enum class ProfileError { BlankName, InvalidWaist, InvalidDate, FutureDate }

sealed interface ProfileValidation {
    data class Valid(val measurements: ProfileMeasurements) : ProfileValidation

    data class Invalid(val errors: Set<ProfileError>) : ProfileValidation
}

/** How a measurement date is typed and shown: day, month and a year of at least 1. */
val ProfileDateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

/** The date typed as [text] in [ProfileDateFormat], or null when it is not a real date. */
fun parseProfileDate(text: String): LocalDate? = try {
    LocalDate.parse(text.trim(), ProfileDateFormat).takeIf { it.year >= 1 }
} catch (_: DateTimeParseException) {
    null
}

/** Checks a typed profile form; the measurement date may be [today] but not later. */
fun validateProfile(
    name: String,
    waist: String,
    measurementDate: String,
    notes: String,
    today: LocalDate,
): ProfileValidation {
    val trimmedName = name.trim()
    val length = (parseLength(waist) as? ParsedLength.Valid)?.length
        ?.takeIf { it.centimetres > 0.0 }
    val date = parseProfileDate(measurementDate)
    val errors = buildSet {
        if (trimmedName.isEmpty()) add(ProfileError.BlankName)
        if (length == null) add(ProfileError.InvalidWaist)
        when {
            date == null -> add(ProfileError.InvalidDate)
            date.isAfter(today) -> add(ProfileError.FutureDate)
        }
    }
    return if (length == null || date == null || errors.isNotEmpty()) {
        ProfileValidation.Invalid(errors)
    } else {
        ProfileValidation.Valid(ProfileMeasurements(ProfileName(trimmedName), length, date, notes))
    }
}
