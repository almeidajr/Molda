package com.almeidajr.molda.data

import androidx.sqlite.SQLiteException
import com.almeidajr.molda.domain.NoLongerExistsException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a storage [block] as a [Result], rethrowing cancellation so it keeps propagating.
 *
 * The failures storage expects become a [Result.failure] for the caller: a database error, a
 * value that fails domain validation, or a row that no longer exists. Anything else is a bug and
 * propagates.
 */
internal suspend fun <T> resultOf(block: suspend () -> T): Result<T> =
    runCatching { block() }.onFailure { if (!it.isExpectedStorageFailure) throw it }

/** Whether [resultOf] turns [this] failure into a [Result.failure] rather than propagating it. */
internal val Throwable.isExpectedStorageFailure: Boolean
    get() = when (this) {
        // A subclass of IllegalStateException, so it must be excluded before that is matched.
        is CancellationException -> false

        is NoLongerExistsException,
        is SQLiteException,
        is IllegalArgumentException,
        is IllegalStateException,
        -> true

        else -> false
    }
