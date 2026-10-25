package com.almeidajr.molda.update.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.almeidajr.molda.R
import com.almeidajr.molda.update.domain.Update
import com.almeidajr.molda.update.domain.UpdateCheck
import com.almeidajr.molda.update.domain.UpdateDownload
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/**
 * Checks for an update once when Molda starts and, when one is available, announces it once
 * through [onShowNotice] (message, action label, action): "Atualizar" starts the download, or
 * "Instalar" goes through [onInstall] when it is already downloaded. A failed check, none on a
 * local build, or an up-to-date Molda announces nothing.
 */
@Composable
fun StartupUpdateNotice(
    onShowNotice: (message: String, action: String, onAction: () -> Unit) -> Unit,
    onInstall: (Update) -> Unit,
) {
    // Owned by the hosting activity, so it checks once per start, not per entry.
    val viewModel = hiltViewModel<UpdateCheckViewModel>()
    val resources = LocalResources.current
    val currentOnShowNotice by rememberUpdatedState(onShowNotice)
    val currentOnInstall by rememberUpdatedState(onInstall)
    // Saved, so recreation never announces again an update already announced.
    var hasAnnounced by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        if (hasAnnounced) return@LaunchedEffect
        val check = viewModel.state.filterNotNull().first()
        if (check !is UpdateCheck.Available) return@LaunchedEffect
        val update = check.update
        // Read once rather than followed, so no download is polled while drafting.
        val isDownloaded =
            viewModel.download.filterNotNull().first().download == UpdateDownload.Downloaded
        hasAnnounced = true
        val message = resources.getString(R.string.update_available, update.version)
        if (isDownloaded) {
            currentOnShowNotice(message, resources.getString(R.string.update_install)) {
                currentOnInstall(update)
            }
        } else {
            currentOnShowNotice(
                message,
                resources.getString(R.string.update_download),
                viewModel::startDownload,
            )
        }
    }
}
