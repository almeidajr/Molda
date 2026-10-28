package com.almeidajr.molda.data

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Which failures count as expected storage failures is [ResultOfTest]'s to check. */
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
    fun asLoadResult_whenStorageFails_endsWithTheError() = runTest {
        // Given
        val failure = IllegalStateException("Stored pattern draft kind is not the snapshot's")

        // When/Then
        assertEquals(listOf(LoadResult.Loading, LoadResult.Error(failure)), failing(failure))
    }

    @Test
    fun asLoadResult_whenFlowHasABug_propagatesIt() = runTest {
        // When/Then
        assertFailsWith<IndexOutOfBoundsException> {
            flow<Int> { throw IndexOutOfBoundsException("A bug") }.asLoadResult().toList()
        }
    }

    @Test
    fun asLoadResult_whenFlowIsCanceled_rethrowsTheCancellation() = runTest {
        // When/Then
        assertFailsWith<CancellationException> {
            failing(CancellationException("Left the screen"))
        }
    }

    private suspend fun failing(failure: Throwable) =
        flow<Int> { throw failure }.asLoadResult().toList()
}
