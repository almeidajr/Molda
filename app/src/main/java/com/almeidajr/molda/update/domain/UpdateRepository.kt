package com.almeidajr.molda.update.domain

import kotlinx.coroutines.flow.Flow

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
    /** Checks GitHub for an update, unless the installed version [checksForUpdates] is false. */
    suspend fun check(): UpdateCheck

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
