package com.almeidajr.molda.profile.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.profile.domain.ProfileRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed profiles in [databaseName]; the old `profiles.db` is deliberately not read (docs/adr/0002). */
class LocalProfileRepository(context: Context, databaseName: String = "molda.db") :
    ProfileRepository,
    AutoCloseable {
    private val database = Room
        .databaseBuilder<ProfileDatabase>(context.applicationContext, databaseName)
        .setDriver(AndroidSQLiteDriver())
        .build()
    private val dao = database.profiles()

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

    override fun close() = database.close()
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
