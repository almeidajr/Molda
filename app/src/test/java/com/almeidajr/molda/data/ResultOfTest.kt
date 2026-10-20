package com.almeidajr.molda.data

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ResultOfTest {
    @Test
    fun resultOf_whenBlockSucceeds_returnsItsValue() = runTest {
        assertEquals(Result.success(42), resultOf { 42 })
    }

    @Test
    fun resultOf_whenBlockFails_returnsTheFailure() = runTest {
        val failure = IllegalStateException("Profile no longer exists")

        assertEquals(Result.failure<Int>(failure), resultOf { throw failure })
    }

    @Test
    fun resultOf_whenBlockIsCancelled_rethrowsTheCancellation() {
        assertFailsWith<CancellationException> {
            runTest { resultOf { throw CancellationException("Left the screen") } }
        }
    }
}
