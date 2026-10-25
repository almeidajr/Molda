package com.almeidajr.molda.update.domain

import app.cash.turbine.test
import com.almeidajr.molda.testing.Beta2
import com.almeidajr.molda.testing.FakeUpdateRepository
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FinishedDownloadsTest {
    @Test
    fun finishedDownloads_whenTheOfferedUpdatesDownloadFinishesWhileFollowed_emitsTheUpdate() =
        runTest {
            // Given
            val updates = FakeUpdateRepository(
                UpdateCheck.Available(Beta2),
                UpdateDownload.Downloading(progress = 0.9f),
            )
            updates.check()

            updates.finishedDownloads().test {
                // When
                updates.download = UpdateDownload.Downloaded

                // Then
                assertEquals(Beta2, awaitItem())
            }
        }

    @Test
    fun finishedDownloads_withTheDownloadFinishedBeforeFollowing_emitsNothing() = runTest {
        // Given
        val updates = FakeUpdateRepository(UpdateCheck.Available(Beta2), UpdateDownload.Downloaded)
        updates.check()

        // When/Then
        updates.finishedDownloads().test { expectNoEvents() }
    }

    @Test
    fun finishedDownloads_whenTheDownloadFails_emitsNothing() = runTest {
        // Given
        val updates = FakeUpdateRepository(
            UpdateCheck.Available(Beta2),
            UpdateDownload.Downloading(progress = 0.9f),
        )
        updates.check()

        updates.finishedDownloads().test {
            // When
            updates.download = UpdateDownload.VerificationFailed

            // Then
            expectNoEvents()
        }
    }

    @Test
    fun finishedDownloads_beforeACheckOffersAnUpdate_emitsNothing() = runTest {
        // Given
        val updates = FakeUpdateRepository(
            UpdateCheck.Available(Beta2),
            UpdateDownload.Downloading(progress = 0.9f),
        )

        updates.finishedDownloads().test {
            // When
            updates.download = UpdateDownload.Downloaded

            // Then
            expectNoEvents()
        }
    }
}
