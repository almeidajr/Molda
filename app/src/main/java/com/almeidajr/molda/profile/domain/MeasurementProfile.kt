package com.almeidajr.molda.profile.domain

import com.almeidajr.molda.measurement.Length
import java.time.LocalDate

/** The positive identity of a stored [MeasurementProfile]. */
@JvmInline
value class ProfileId(val value: Long) {
    init {
        require(value > 0) { "Profile identity must be positive: $value" }
    }
}

/** A non-blank profile name without surrounding whitespace. */
@JvmInline
value class ProfileName(val text: String) {
    init {
        require(text.isNotBlank() && text == text.trim()) {
            "Profile name must be non-blank and trimmed"
        }
    }
}

data class ProfileMeasurements(
    val name: ProfileName,
    val waist: Length,
    val measurementDate: LocalDate,
    val notes: String = "",
) {
    init {
        require(waist.centimetres > 0.0) { "Profile waist must be positive" }
    }
}

data class MeasurementProfile(val id: ProfileId, val measurements: ProfileMeasurements)

interface ProfileRepository {
    suspend fun list(): List<MeasurementProfile>
    suspend fun create(measurements: ProfileMeasurements): MeasurementProfile
    suspend fun update(profile: MeasurementProfile)
    suspend fun delete(id: ProfileId)
}
