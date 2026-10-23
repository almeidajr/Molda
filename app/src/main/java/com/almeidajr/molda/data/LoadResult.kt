package com.almeidajr.molda.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * A stored list as it loads, named so it does not shadow Kotlin's [Result], which commands return.
 */
sealed interface LoadResult<out T> {
    data object Loading : LoadResult<Nothing>

    data class Success<T>(val value: T) : LoadResult<T>

    data class Error(val failure: Throwable) : LoadResult<Nothing>
}

/**
 * Follows [this] flow as a [LoadResult]: [LoadResult.Loading] first, then a [LoadResult.Success]
 * for each emission, or a final [LoadResult.Error] when the flow fails as storage expects.
 *
 * Unlike Now in Android's `asResult`, which catches every throwable, it catches only the failures
 * [resultOf] expects, so cancellation and bugs propagate.
 */
internal fun <T> Flow<T>.asLoadResult(): Flow<LoadResult<T>> =
    map<T, LoadResult<T>> { LoadResult.Success(it) }
        .onStart { emit(LoadResult.Loading) }
        .catch { failure ->
            if (!failure.isExpectedStorageFailure) throw failure
            emit(LoadResult.Error(failure))
        }
