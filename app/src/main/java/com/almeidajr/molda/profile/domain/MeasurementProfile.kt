package com.almeidajr.molda.profile.domain

import java.time.LocalDate

data class ProfileMeasurements(
    val name: String,
    val waist: Double,
    val measurementDate: LocalDate,
    val notes: String = "",
) {
    init {
        require(name.isNotBlank()) { "Profile name is required" }
        require(waist.isFinite() && waist > 0) { "Waist must be positive and finite" }
    }
}

data class MeasurementProfile(val id: Long, val measurements: ProfileMeasurements) {
    init {
        require(id > 0) { "Profile identity must be positive" }
    }
}

interface ProfileRepository {
    suspend fun list(): List<MeasurementProfile>
    suspend fun create(measurements: ProfileMeasurements): MeasurementProfile
}
