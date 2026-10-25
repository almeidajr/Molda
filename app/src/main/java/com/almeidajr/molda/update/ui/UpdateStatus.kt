package com.almeidajr.molda.update.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.almeidajr.molda.R
import com.almeidajr.molda.update.domain.Update
import com.almeidajr.molda.update.domain.UpdateCheck
import com.almeidajr.molda.update.domain.UpdateDownload
import com.almeidajr.molda.update.navigation.INSTALL_UPDATE_RESULT_KEY

/**
 * The update on offer and its download, both followed by the hosting entry's own
 * [UpdateCheckViewModel]. Installing, chosen or because the download finished while shown, is
 * requested from the app on [INSTALL_UPDATE_RESULT_KEY].
 */
@Composable
fun UpdateStatus(modifier: Modifier = Modifier) {
    val viewModel = hiltViewModel<UpdateCheckViewModel>()
    val check by viewModel.state.collectAsStateWithLifecycle()
    val watched by viewModel.download.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val results = LocalResultEventBus.current
    val update = (check as? UpdateCheck.Available)?.update
    val onInstall = { installed: Update -> results.sendResult(INSTALL_UPDATE_RESULT_KEY, installed) }
    val hasFinishedWhileWatched = watched?.hasFinishedWhileWatched == true
    LaunchedEffect(update, hasFinishedWhileWatched) {
        if (update != null && hasFinishedWhileWatched) onInstall(update)
    }
    UpdateStatus(
        check = check,
        download = watched?.download,
        onCheck = viewModel::check,
        onOpenPage = uriHandler::openUri,
        onStartDownload = viewModel::startDownload,
        onInstall = { update?.let(onInstall) },
        modifier = modifier,
    )
}

/**
 * The outcome of the last [check] (null while checking); for an available update, its [download]
 * with the action it allows and its release page; and the action that checks again. A local
 * build, which never checks, shows nothing.
 */
@Composable
fun UpdateStatus(
    check: UpdateCheck?,
    download: UpdateDownload?,
    onCheck: () -> Unit,
    onOpenPage: (String) -> Unit,
    onStartDownload: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (check == UpdateCheck.NotChecked) return
    Column(modifier) {
        ListItem(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            supportingContent = if (check == UpdateCheck.Failed) {
                { Text(stringResource(R.string.update_failed_hint)) }
            } else {
                null
            },
        ) { Text(check.headline()) }
        if (check is UpdateCheck.Available) {
            UpdateDownloadStatus(download, onStartDownload, onInstall)
            ListItem(
                onClick = { onOpenPage(check.update.page) },
                modifier = Modifier.heightIn(min = 56.dp),
                trailingContent = {
                    Icon(painterResource(R.drawable.ic_open_in_new), contentDescription = null)
                },
            ) { Text(stringResource(R.string.update_release_page)) }
        }
        ListItem(
            onClick = onCheck,
            modifier = Modifier.heightIn(min = 56.dp),
            enabled = check != null,
            leadingContent = {
                Icon(painterResource(R.drawable.ic_refresh), contentDescription = null)
            },
        ) { Text(stringResource(R.string.update_check)) }
    }
}

/** The offered update's [download] (null until known) and the action it allows. */
@Composable
private fun UpdateDownloadStatus(
    download: UpdateDownload?,
    onStartDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    when (download) {
        null, UpdateDownload.NotStarted -> UpdateAction(
            R.string.update_download,
            R.drawable.ic_download,
            onStartDownload,
        )

        is UpdateDownload.Downloading -> ListItem(
            supportingContent = {
                val progress = download.progress
                val indicatorModifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                if (progress == null) {
                    LinearProgressIndicator(indicatorModifier)
                } else {
                    LinearProgressIndicator(progress = { progress }, modifier = indicatorModifier)
                }
            },
        ) { Text(stringResource(R.string.update_downloading)) }

        UpdateDownload.Downloaded -> UpdateAction(
            R.string.update_install,
            R.drawable.ic_install_mobile,
            onInstall,
        )

        UpdateDownload.VerificationFailed -> {
            ListItem(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                supportingContent = { Text(stringResource(R.string.update_download_failed_hint)) },
            ) { Text(stringResource(R.string.update_download_failed)) }
            UpdateAction(R.string.update_retry, R.drawable.ic_refresh, onStartDownload)
        }
    }
}

@Composable
private fun UpdateAction(@StringRes label: Int, @DrawableRes icon: Int, onClick: () -> Unit) =
    ListItem(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 56.dp),
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
    ) { Text(stringResource(label)) }

@Composable
private fun UpdateCheck?.headline(): String = when (this) {
    null -> stringResource(R.string.update_checking)
    is UpdateCheck.Available -> stringResource(R.string.update_available, update.version)
    UpdateCheck.UpToDate -> stringResource(R.string.update_up_to_date)
    UpdateCheck.Failed, UpdateCheck.NotChecked -> stringResource(R.string.update_failed)
}
