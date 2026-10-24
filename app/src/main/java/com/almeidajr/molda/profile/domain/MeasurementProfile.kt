package com.almeidajr.molda.profile.domain

import com.almeidajr.molda.domain.NoLongerExistsException
import com.almeidajr.molda.measurement.IsoLocalDateSerializer
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.measurement.MeasurementSource
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** The positive identity of a stored [MeasurementProfile]. */
@Serializable
@JvmInline
value class ProfileId(val value: Long) {
    init {
        require(value > 0) { "Profile identity must be positive: $value" }
    }
}

/** A non-blank profile name without surrounding whitespace. */
@Serializable
@JvmInline
value class ProfileName(val text: String) {
    init {
        require(text.isNotBlank() && text == text.trim()) {
            "Profile name must be non-blank and trimmed"
        }
    }
}

@Serializable
data class ProfileMeasurements(
    val name: ProfileName,
    val waist: Length,
    @Serializable(with = IsoLocalDateSerializer::class) val measurementDate: LocalDate,
    val notes: String = "",
) {
    init {
        require(waist.centimetres > 0.0) { "Profile waist must be positive" }
    }

    /** This profile as the source of a measurement copied from it. */
    val source: MeasurementSource get() = MeasurementSource(name.text, measurementDate)
}

@Serializable
data class MeasurementProfile(val id: ProfileId, val measurements: ProfileMeasurements)

/**
 * Stored measurement profiles; commands report storage failures as [Result.failure], and a profile
 * that is no longer stored as [NoLongerExistsException].
 */
interface ProfileRepository {
    /** Every stored profile in creation order, emitted again after each change. */
    fun profiles(): Flow<List<MeasurementProfile>>

    /**
     * The stored profile [id], emitted again after each change; fails with
     * [NoLongerExistsException] once it is no longer stored.
     */
    fun profile(id: ProfileId): Flow<MeasurementProfile>
    suspend fun create(measurements: ProfileMeasurements): Result<MeasurementProfile>
    suspend fun update(profile: MeasurementProfile): Result<Unit>
    suspend fun delete(id: ProfileId): Result<Unit>
}
