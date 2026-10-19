package com.almeidajr.molda.profile.data

import com.almeidajr.molda.data.resultOf
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
class LocalProfileRepository(private val dao: ProfileDao) : ProfileRepository {
    override fun profiles(): Flow<List<MeasurementProfile>> =
        dao.profiles().map { rows -> rows.map { it.toProfile() } }

    override suspend fun create(measurements: ProfileMeasurements): Result<MeasurementProfile> =
        resultOf {
            MeasurementProfile(ProfileId(dao.insert(measurements.toEntity())), measurements)
        }

    override suspend fun update(profile: MeasurementProfile): Result<Unit> = resultOf {
        check(dao.update(profile.measurements.toEntity(profile.id.value)) == 1) {
            "Profile no longer exists"
        }
    }

    override suspend fun delete(id: ProfileId): Result<Unit> = resultOf {
        check(dao.delete(id.value) == 1) { "Profile no longer exists" }
    }
}

// Domain constructors validate every loaded row, so a malformed row fails the load.
private fun ProfileEntity.toProfile() = MeasurementProfile(
    ProfileId(id),
    ProfileMeasurements(ProfileName(name), Length(waist), LocalDate.parse(measurementDate), notes),
)

private fun ProfileMeasurements.toEntity(id: Long = 0) = ProfileEntity(
    id = id,
    name = name.text,
    waist = waist.centimetres,
    measurementDate = measurementDate.toString(),
    notes = notes,
)
