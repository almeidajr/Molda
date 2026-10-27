package com.almeidajr.molda.data

import androidx.sqlite.SQLiteException
import com.almeidajr.molda.domain.NoLongerExistsException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric supplies the Android SQLException behind androidx.sqlite's SQLiteException.
@RunWith(RobolectricTestRunner::class)
class ResultOfTest {
    @Test
    fun resultOf_whenBlockSucceeds_returnsItsValue() = runTest {
        // When/Then
        assertEquals(Result.success(42), resultOf { 42 })
    }

    @Test
    fun resultOf_whenRowNoLongerExists_returnsTheFailure() = runTest {
        // Given
        val failure = NoLongerExistsException("Profile no longer exists")

        // When
        val result = resultOf<Int> { throw failure }

        // Then
        assertEquals(Result.failure(failure), result)
    }

    @Test
    fun resultOf_whenStateIsInvalid_returnsTheFailure() = runTest {
        // Given
        val failure = IllegalStateException("Stored pattern draft kind is not the snapshot's")

        // When
        val result = resultOf<Int> { throw failure }

        // Then
        assertEquals(Result.failure(failure), result)
    }

    @Test
    fun resultOf_whenDatabaseFails_returnsTheFailure() = runTest {
        // Given
        val failure = SQLiteException("disk I/O error")

        // When
        val result = resultOf<Int> { throw failure }

        // Then
        assertEquals(Result.failure(failure), result)
    }

    @Test
    fun resultOf_whenValidationFails_returnsTheFailure() = runTest {
        // Given
        val failure = IllegalArgumentException("Project identity must be positive: 0")

        // When
        val result = resultOf<Int> { throw failure }

        // Then
        assertEquals(Result.failure(failure), result)
    }

    @Test
    fun resultOf_whenBlockHasABug_propagatesIt() = runTest {
        // When/Then
        assertFailsWith<IndexOutOfBoundsException> {
            resultOf { throw IndexOutOfBoundsException("A bug") }
        }
    }

    @Test
    fun resultOf_whenBlockIsCanceled_rethrowsTheCancellation() = runTest {
        // When/Then
        assertFailsWith<CancellationException> {
            resultOf { throw CancellationException("Left the screen") }
        }
    }
}
