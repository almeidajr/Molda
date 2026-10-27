package com.almeidajr.molda.profile.data

import com.almeidajr.molda.data.resultOf
import com.almeidajr.molda.domain.NoLongerExistsException
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.profile.domain.ProfileRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed profiles, read and written through [dao]. */
class LocalProfileRepository internal constructor(private val dao: ProfileDao) : ProfileRepository {
    override fun profiles(): Flow<List<MeasurementProfile>> =
        dao.profiles().map { rows -> rows.map { it.toProfile() } }

    override fun profile(id: ProfileId): Flow<MeasurementProfile> =
        dao.profile(id.value).map { row -> row?.toProfile() ?: throw profileMissing() }

    override suspend fun create(measurements: ProfileMeasurements): Result<MeasurementProfile> =
        resultOf {
            MeasurementProfile(ProfileId(dao.insert(measurements.toEntity())), measurements)
        }

    override suspend fun update(profile: MeasurementProfile): Result<Unit> = resultOf {
        if (dao.update(profile.measurements.toEntity(profile.id.value)) == 0) {
            throw profileMissing()
        }
    }

    override suspend fun delete(id: ProfileId): Result<Unit> = resultOf {
        if (dao.delete(id.value) == 0) throw profileMissing()
    }
}

private fun profileMissing() = NoLongerExistsException("Profile no longer exists")

// Domain constructors validate every loaded row, so a malformed row fails the load.
private fun ProfileEntity.toProfile() = MeasurementProfile(
    ProfileId(id),
    ProfileMeasurements(ProfileName(name), Length(waist), LocalDate.parse(measurementDate), notes),
)

private fun ProfileMeasurements.toEntity(id: Long = 0) = ProfileEntity(
    id = id,
    name = name.text,
    waist = waist.centimeters,
    measurementDate = measurementDate.toString(),
    notes = notes,
)
