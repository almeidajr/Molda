package com.almeidajr.molda.data

import androidx.sqlite.SQLiteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.domain.NoLongerExistsException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

// Robolectric supplies the Android SQLException behind androidx.sqlite's SQLiteException.
@RunWith(AndroidJUnit4::class)
class ResultOfTest {
    @Test
    fun resultOf_whenBlockSucceeds_returnsItsValue() = runTest {
        // When/Then
        assertEquals(Result.success(42), resultOf { 42 })
    }

    @Test
    fun resultOf_whenStorageFails_returnsTheFailure() = runTest {
        listOf(
            NoLongerExistsException("Profile no longer exists"),
            IllegalStateException("Stored pattern draft kind is not the snapshot's"),
            SQLiteException("disk I/O error"),
            IllegalArgumentException("Project identity must be positive: 0"),
        ).forEach { failure ->
            // When/Then
            assertEquals(
                Result.failure(failure),
                resultOf<Int> { throw failure },
                failure::class.simpleName,
            )
        }
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
