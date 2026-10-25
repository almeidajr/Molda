package com.almeidajr.molda.update.ui

import com.almeidajr.molda.update.domain.Update
import com.almeidajr.molda.update.domain.UpdateCheck
import com.almeidajr.molda.update.domain.UpdateDownload
import com.almeidajr.molda.update.domain.UpdateRepository
import kotlinx.coroutines.flow.first

/** What Molda announces about an available [update] when it starts. */
internal sealed interface StartupNotice {
    val update: Update

    /** "Atualizar": the update isn't downloaded yet. */
    data class Download(override val update: Update) : StartupNotice

    /** "Instalar": the update is downloaded and verified. */
    data class Install(override val update: Update) : StartupNotice
}

/**
 * Checks for an update and reads its download once, rather than following it, so no download is
 * polled while drafting; null when there is nothing to announce: no update, a failed check, or a
 * local build.
 */
internal suspend fun UpdateRepository.startupNotice(): StartupNotice? {
    val update = (check() as? UpdateCheck.Available)?.update ?: return null
    return when (download(update).first()) {
        UpdateDownload.Downloaded -> StartupNotice.Install(update)
        else -> StartupNotice.Download(update)
    }
}
