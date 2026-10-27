package com.almeidajr.molda.project.ui

import com.almeidajr.molda.R
import com.almeidajr.molda.domain.NoLongerExistsException
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** The storage command every project dialog and the project editor's update run through. */
class ProjectCommandTest {
    private var runs = 0

    /** A command that counts its runs and finishes with [outcome] once it completes. */
    private fun commandOf(
        outcome: CompletableDeferred<Result<Unit>> = CompletableDeferred(Result.success(Unit)),
    ): suspend () -> Result<Unit> = {
        runs++
        outcome.await()
    }

    private val failing = CompletableDeferred(Result.failure<Unit>(IllegalStateException("Disk")))

    /**
     * A command that runs its work at once, until the work suspends, as `viewModelScope` does on
     * the main thread. Its failure message is each owner's own, which the ViewModel tests check.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.newCommand() = ProjectCommand<Unit>(
        CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        R.string.project_save_failed,
    )

    @Test
    fun submit_whileRunning_isBusyAndIgnoresTheRepeat() = runTest {
        // Given
        val outcome = CompletableDeferred<Result<Unit>>()
        val command = newCommand()
        command.submit(commandOf(outcome))

        // When
        command.submit(commandOf(outcome))

        // Then
        assertEquals(ProjectCommandState.Running, command.state.value)
        outcome.complete(Result.success(Unit))
        assertEquals(ProjectCommandState.Done(Unit), command.state.value)
        assertEquals(1, runs)
    }

    @Test
    fun submit_afterSuccess_isIgnored() = runTest {
        // Given
        val command = newCommand()
        command.submit(commandOf())

        // When
        command.submit(commandOf())

        // Then
        assertEquals(ProjectCommandState.Done(Unit), command.state.value)
        assertEquals(1, runs)
    }

    @Test
    fun submit_afterTheOutcomeIsAcknowledged_staysSpent() = runTest {
        // Given
        val command = newCommand()
        command.submit(commandOf())
        command.acknowledge()

        // When
        command.submit(commandOf())

        // Then
        assertEquals(ProjectCommandState.Spent, command.state.value)
        assertEquals(1, runs)
    }

    @Test
    fun submit_afterAFailure_runsAgain() = runTest {
        // Given
        val command = newCommand()
        command.submit(commandOf(failing))

        // When
        command.submit(commandOf())

        // Then
        assertEquals(ProjectCommandState.Done(Unit), command.state.value)
        assertEquals(2, runs)
    }

    @Test
    fun submit_ofProjectDeletedMeanwhile_reportsItDeletedWithoutAFailure() = runTest {
        // Given
        val command = newCommand()
        val gone = Result.failure<Unit>(NoLongerExistsException("No project 1"))

        // When
        command.submit(commandOf(CompletableDeferred(gone)))

        // Then
        assertEquals(ProjectCommandState.Deleted, command.state.value)
        assertNull(command.state.value.error)
    }

    @Test
    fun markProjectGone_withNoCommandRunning_reportsItDeleted() = runTest {
        // Given
        val command = newCommand()

        // When
        command.markProjectGone()

        // Then
        assertEquals(ProjectCommandState.Deleted, command.state.value)
    }

    @Test
    fun markProjectGone_afterAFailure_reportsItDeleted() = runTest {
        // Given
        val command = newCommand()
        command.submit(commandOf(failing))

        // When
        command.markProjectGone()

        // Then
        assertEquals(ProjectCommandState.Deleted, command.state.value)
    }

    @Test
    fun markProjectGone_whileRunning_leavesTheOutcomeToTheCommand() = runTest {
        // Given: the command may be what deleted the project
        val outcome = CompletableDeferred<Result<Unit>>()
        val command = newCommand()
        command.submit(commandOf(outcome))

        // When
        command.markProjectGone()

        // Then
        assertEquals(ProjectCommandState.Running, command.state.value)
        outcome.complete(Result.success(Unit))
        assertEquals(ProjectCommandState.Done(Unit), command.state.value)
    }

    @Test
    fun markProjectGone_whileRunningACommandThatThenFails_reportsItDeleted() = runTest {
        // Given
        val outcome = CompletableDeferred<Result<Unit>>()
        val command = newCommand()
        command.submit(commandOf(outcome))
        command.markProjectGone()

        // When
        outcome.complete(Result.failure(IllegalStateException("Disk")))

        // Then
        assertEquals(ProjectCommandState.Deleted, command.state.value)
        assertNull(command.state.value.error)
    }

    @Test
    fun acknowledge_afterTheProjectWasFoundDeleted_spendsTheCommand() = runTest {
        // Given
        val command = newCommand()
        command.markProjectGone()

        // When
        command.acknowledge()

        // Then
        assertEquals(ProjectCommandState.Spent, command.state.value)
    }

    @Test
    fun acknowledgeError_afterAFailure_forgetsTheFailure() = runTest {
        // Given
        val command = newCommand()
        command.submit(commandOf(failing))

        // When
        command.acknowledgeError()

        // Then
        assertEquals(ProjectCommandState.Idle, command.state.value)
    }

    @Test
    fun acknowledgeError_afterSuccess_keepsTheOutcome() = runTest {
        // Given: a retry succeeded before the failure's reaction acknowledged it
        val command = newCommand()
        command.submit(commandOf())

        // When
        command.acknowledgeError()

        // Then
        assertEquals(ProjectCommandState.Done(Unit), command.state.value)
    }
}
