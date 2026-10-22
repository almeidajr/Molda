package com.almeidajr.molda.data

import androidx.sqlite.SQLiteException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a storage [block] as a [Result], rethrowing cancellation so it keeps propagating.
 *
 * The failures storage expects become a [Result.failure] for the caller: a database error, a
 * value that fails domain validation, or a row that no longer exists. Anything else is a bug and
 * propagates.
 */
internal suspend fun <T> resultOf(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    // A subclass of IllegalStateException, so it must be rethrown before that is caught.
    throw cancelled
} catch (failure: SQLiteException) {
    Result.failure(failure)
} catch (failure: IllegalArgumentException) {
    Result.failure(failure)
} catch (failure: IllegalStateException) {
    Result.failure(failure)
}
