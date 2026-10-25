package com.almeidajr.molda.update.ui

import com.almeidajr.molda.testing.Beta2
import com.almeidajr.molda.testing.FakeUpdateRepository
import com.almeidajr.molda.update.domain.UpdateCheck
import com.almeidajr.molda.update.domain.UpdateDownload
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

class StartupNoticeTest {
    @Test
    fun startupNotice_withAnUpdateNotDownloaded_offersTheDownload() = runTest {
        // When
        val notice = FakeUpdateRepository(UpdateCheck.Available(Beta2)).startupNotice()

        // Then
        assertEquals(StartupNotice.Download(Beta2), notice)
    }

    @Test
    fun startupNotice_afterAFailedDownload_offersTheDownloadAgain() = runTest {
        // When
        val notice = FakeUpdateRepository(
            UpdateCheck.Available(Beta2),
            UpdateDownload.VerificationFailed,
        ).startupNotice()

        // Then
        assertEquals(StartupNotice.Download(Beta2), notice)
    }

    @Test
    fun startupNotice_withTheUpdateDownloaded_offersTheInstall() = runTest {
        // When
        val notice = FakeUpdateRepository(
            UpdateCheck.Available(Beta2),
            UpdateDownload.Downloaded,
        ).startupNotice()

        // Then
        assertEquals(StartupNotice.Install(Beta2), notice)
    }

    @Test
    fun startupNotice_duringTheUpdatesDownload_announcesNothing() = runTest {
        // When
        val notice = FakeUpdateRepository(
            UpdateCheck.Available(Beta2),
            UpdateDownload.Downloading(progress = 0.4f),
        ).startupNotice()

        // Then
        assertNull(notice)
    }

    @Test
    fun startupNotice_withoutAnUpdate_announcesNothing() = runTest {
        // When/Then
        assertNull(FakeUpdateRepository(UpdateCheck.UpToDate).startupNotice())
    }
}
