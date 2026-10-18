package com.almeidajr.molda.profile.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ResultOfTest {
    @Test
    fun resultOf_whenBlockSucceeds_returnsItsValue() = runBlocking {
        assertEquals(Result.success(42), resultOf { 42 })
    }

    @Test
    fun resultOf_whenBlockFails_returnsTheFailure() = runBlocking {
        val failure = IllegalStateException("Profile no longer exists")

        assertEquals(Result.failure<Int>(failure), resultOf { throw failure })
    }

    @Test
    fun resultOf_whenBlockIsCancelled_rethrowsTheCancellation() {
        assertThrows(CancellationException::class.java) {
            runBlocking { resultOf { throw CancellationException("Left the screen") } }
        }
    }
}
