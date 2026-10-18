package com.almeidajr.molda.profile.data

import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a storage [block] as a [Result], rethrowing cancellation so it keeps propagating.
 *
 * Any other storage failure becomes a [Result.failure] for the caller.
 */
@Suppress("TooGenericExceptionCaught")
internal suspend fun <T> resultOf(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    Result.failure(failure)
}
