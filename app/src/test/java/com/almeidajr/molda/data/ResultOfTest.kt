package com.almeidajr.molda.data

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ResultOfTest {
    @Test
    fun resultOf_whenBlockSucceeds_returnsItsValue() = runTest {
        // When/Then
        assertEquals(Result.success(42), resultOf { 42 })
    }

    @Test
    fun resultOf_whenBlockFails_returnsTheFailure() = runTest {
        // Given
        val failure = IllegalStateException("Profile no longer exists")

        // When
        val result = resultOf<Int> { throw failure }

        // Then
        assertEquals(Result.failure(failure), result)
    }

    @Test
    fun resultOf_whenBlockIsCancelled_rethrowsTheCancellation() = runTest {
        // When/Then
        assertFailsWith<CancellationException> {
            resultOf { throw CancellationException("Left the screen") }
        }
    }
}
