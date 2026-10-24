package com.almeidajr.molda.data

import androidx.sqlite.SQLiteException
import com.almeidajr.molda.domain.NoLongerExistsException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric supplies the Android SQLException behind androidx.sqlite's SQLiteException.
@RunWith(RobolectricTestRunner::class)
class LoadResultTest {
    @Test
    fun asLoadResult_whenFlowEmits_startsLoadingThenSucceedsWithEachValue() = runTest {
        // When/Then
        assertEquals(
            listOf(LoadResult.Loading, LoadResult.Success(1), LoadResult.Success(2)),
            flowOf(1, 2).asLoadResult().toList(),
        )
    }

    @Test
    fun asLoadResult_whenDatabaseFails_endsWithTheError() = runTest {
        // Given
        val failure = SQLiteException("disk I/O error")

        // When/Then
        assertEquals(listOf(LoadResult.Loading, LoadResult.Error(failure)), failing(failure))
    }

    @Test
    fun asLoadResult_whenValidationFails_endsWithTheError() = runTest {
        // Given
        val failure = IllegalArgumentException("Waist must be positive: 0.0")

        // When/Then
        assertEquals(listOf(LoadResult.Loading, LoadResult.Error(failure)), failing(failure))
    }

    @Test
    fun asLoadResult_whenRowNoLongerExists_endsWithTheError() = runTest {
        // Given
        val failure = NoLongerExistsException("Profile no longer exists")

        // When/Then
        assertEquals(listOf(LoadResult.Loading, LoadResult.Error(failure)), failing(failure))
    }

    @Test
    fun asLoadResult_whenStateIsInvalid_endsWithTheError() = runTest {
        // Given
        val failure = IllegalStateException("Stored pattern draft kind is not the snapshot's")

        // When/Then
        assertEquals(listOf(LoadResult.Loading, LoadResult.Error(failure)), failing(failure))
    }

    @Test
    fun asLoadResult_whenFlowHasABug_propagatesIt() = runTest {
        // When/Then
        assertFailsWith<IndexOutOfBoundsException> {
            flow { emit(emptyList<Int>()[0]) }.asLoadResult().toList()
        }
    }

    @Test
    fun asLoadResult_whenFlowIsCancelled_rethrowsTheCancellation() = runTest {
        // When/Then
        assertFailsWith<CancellationException> {
            failing(CancellationException("Left the screen"))
        }
    }

    private suspend fun failing(failure: Throwable) =
        flow<Int> { throw failure }.asLoadResult().toList()
}
