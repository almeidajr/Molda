package com.almeidajr.molda.update.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** The outcome of checking for an [Update] on the installed version's channel. */
sealed interface UpdateCheck {
    data class Available(val update: Update) : UpdateCheck

    data object UpToDate : UpdateCheck

    /** Offline, or GitHub failed: never shown as an error. */
    data object Failed : UpdateCheck

    /** A debug or local build, which never checks. */
    data object NotChecked : UpdateCheck
}

/** The state of an [Update]'s download, which the system download service runs. */
sealed interface UpdateDownload {
    data object NotStarted : UpdateDownload

    /** Downloading, [progress] done from 0 to 1; null while the size is unknown. */
    data class Downloading(val progress: Float?) : UpdateDownload

    /** Downloaded, its APK matching the digest GitHub publishes: ready to install. */
    data object Downloaded : UpdateDownload

    /**
     * The download failed, or its APK had no digest or didn't match it; the file is gone and the
     * download can start again.
     */
    data object VerificationFailed : UpdateDownload
}

/** The updates on offer to the installed version; the only seam the UI depends on. */
interface UpdateRepository {
    /**
     * Checks GitHub for an update, unless the installed version [checksForUpdates] is false; an
     * available one becomes [offered].
     */
    suspend fun check(): UpdateCheck

    /**
     * The update the latest successful check offered, null until one does; kept for the app's
     * lifetime, so every screen follows the same download.
     */
    val offered: StateFlow<Update?>

    /**
     * The state of [update]'s download, found by its APK's file name (even after process death)
     * and followed while collected.
     */
    fun download(update: Update): Flow<UpdateDownload>

    /** Starts downloading [update]'s APK, replacing an earlier or failed download of it. */
    suspend fun startDownload(update: Update)

    /**
     * Installs [update]'s downloaded APK through the system installer, which asks the person to
     * confirm and restarts Molda; [UpdateInstall.NotAllowed] until Molda may install apps.
     */
    suspend fun install(update: Update): UpdateInstall
}

/** The outcome of installing an [Update]. */
enum class UpdateInstall {
    /** The system installer took the APK and asks the person to confirm. */
    Started,

    /** The person hasn't allowed Molda to install unknown apps: nothing was installed. */
    NotAllowed,
}

/**
 * The [offered][UpdateRepository.offered] update each time its download finishes while followed,
 * so the install can start; a download found already finished, after Molda was in the background
 * or restarted, never counts, so Molda never restarts without the person's choice.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun UpdateRepository.finishedDownloads(): Flow<Update> = offered
    .filterNotNull()
    .flatMapLatest { update -> download(update).finishes().map { update } }

private fun Flow<UpdateDownload>.finishes(): Flow<Unit> = flow {
    var previous: UpdateDownload? = null
    collect { download ->
        if (previous is UpdateDownload.Downloading && download == UpdateDownload.Downloaded) {
            emit(Unit)
        }
        previous = download
    }
}
