package com.almeidajr.molda.testing

import com.almeidajr.molda.update.domain.Update
import com.almeidajr.molda.update.domain.UpdateCheck
import com.almeidajr.molda.update.domain.UpdateDownload
import com.almeidajr.molda.update.domain.UpdateInstall
import com.almeidajr.molda.update.domain.UpdateRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Updates without GitHub or the system services: every check answers [result], and the offered
 * update's download is in [download]; a test may change any of them later.
 */
class FakeUpdateRepository(
    @Volatile var result: UpdateCheck = UpdateCheck.UpToDate,
    download: UpdateDownload = UpdateDownload.NotStarted,
    /** Whether Molda may install apps; while false, installs are refused as not allowed. */
    @Volatile var canInstall: Boolean = true,
) : UpdateRepository {
    private val downloadState = MutableStateFlow(download)
    private val installs = MutableStateFlow(emptyList<Update>())

    /** When set, every check waits for it to complete, so tests can observe the pending state. */
    @Volatile var checkGate: CompletableDeferred<Unit>? = null

    /** The download right now; setting it acts as the system download service progressing. */
    var download: UpdateDownload
        get() = downloadState.value
        set(value) {
            downloadState.value = value
        }

    /** The updates whose install started, oldest first. */
    val installed: List<Update> get() = installs.value

    private val offeredUpdate = MutableStateFlow<Update?>(null)

    override val offered: StateFlow<Update?> = offeredUpdate.asStateFlow()

    override suspend fun check(): UpdateCheck {
        checkGate?.await()
        return result.also { if (it is UpdateCheck.Available) offeredUpdate.value = it.update }
    }

    override fun download(update: Update): Flow<UpdateDownload> = downloadState

    override suspend fun startDownload(update: Update) {
        download = UpdateDownload.Downloading(progress = null)
    }

    override suspend fun install(update: Update): UpdateInstall {
        if (!canInstall) return UpdateInstall.NotAllowed
        installs.update { it + update }
        return UpdateInstall.Started
    }
}
