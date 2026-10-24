package com.almeidajr.molda.testing

import com.almeidajr.molda.data.resultOf
import com.almeidajr.molda.domain.NoLongerExistsException
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory profiles, seeded with [profiles], that emit again after each change like Room. */
class FakeProfileRepository(profiles: List<MeasurementProfile> = emptyList()) : ProfileRepository {
    private val stored = MutableStateFlow(profiles)

    /** The stored profiles right now, in creation order. */
    val current: List<MeasurementProfile> get() = stored.value

    /** Makes loading the profiles throw. */
    @Volatile var failsLoad = false

    /** Makes every command fail without changing the stored profiles. */
    @Volatile var failsCommands = false

    /** When set, loading waits for it to complete, so tests can observe the pending state. */
    @Volatile var loadGate: CompletableDeferred<Unit>? = null

    /** When set, every command waits for it to complete, so tests can observe the pending state. */
    @Volatile var commandGate: CompletableDeferred<Unit>? = null

    override fun profiles(): Flow<List<MeasurementProfile>> = flow {
        loadGate?.await()
        check(!failsLoad) { "Controlled load failure" }
        emitAll(stored)
    }

    override fun profile(id: ProfileId): Flow<MeasurementProfile> = profiles().map { profiles ->
        profiles.find { it.id == id } ?: throw NoLongerExistsException("No profile $id")
    }

    override suspend fun create(measurements: ProfileMeasurements) = command {
        val id = ProfileId((current.maxOfOrNull { it.id.value } ?: 0) + 1)
        MeasurementProfile(id, measurements).also { created -> stored.update { it + created } }
    }

    override suspend fun update(profile: MeasurementProfile) = command {
        requireStored(profile.id)
        stored.update { profiles -> profiles.map { if (it.id == profile.id) profile else it } }
    }

    override suspend fun delete(id: ProfileId) = command {
        requireStored(id)
        stored.update { profiles -> profiles.filterNot { it.id == id } }
    }

    private fun requireStored(id: ProfileId) {
        if (current.none { it.id == id }) throw NoLongerExistsException("No profile $id")
    }

    private suspend fun <T> command(block: () -> T): Result<T> {
        commandGate?.await()
        return if (failsCommands) {
            Result.failure(IllegalStateException("Controlled storage failure"))
        } else {
            resultOf { block() }
        }
    }
}
