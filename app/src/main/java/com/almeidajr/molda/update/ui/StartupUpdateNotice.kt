package com.almeidajr.molda.update.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.navigation3.runtime.result.ResultEventBus
import com.almeidajr.molda.R
import com.almeidajr.molda.update.domain.UpdateRepository
import com.almeidajr.molda.update.navigation.INSTALL_UPDATE_RESULT_KEY
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

/**
 * Checks for an update once when Molda starts and, when one is available, announces it once
 * through [onShowNotice] (message, action label, action): "Atualizar" starts the download, and
 * "Instalar", when it is already downloaded, requests the install on [results]. Placed once,
 * outside the entries; a failed check, none on a local build, or an up-to-date Molda announces
 * nothing.
 */
@Composable
fun StartupUpdateNotice(
    results: ResultEventBus,
    onShowNotice: (message: String, action: String, onAction: () -> Unit) -> Unit,
) {
    val updates = rememberUpdateRepository()
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val currentOnShowNotice by rememberUpdatedState(onShowNotice)
    // Saved, so recreation never checks or announces again.
    var hasChecked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(updates) {
        if (hasChecked) return@LaunchedEffect
        val notice = updates.startupNotice()
        hasChecked = true
        if (notice == null) return@LaunchedEffect
        val update = notice.update
        val message = resources.getString(R.string.update_available, update.version)
        when (notice) {
            is StartupNotice.Download ->
                currentOnShowNotice(message, resources.getString(R.string.update_download)) {
                    scope.launch { updates.startDownload(update) }
                }

            is StartupNotice.Install ->
                currentOnShowNotice(message, resources.getString(R.string.update_install)) {
                    results.sendResult(INSTALL_UPDATE_RESULT_KEY, update)
                }
        }
    }
}

/** The app's [UpdateRepository], for the update UI that lives outside every entry. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface UpdatesEntryPoint {
    fun updates(): UpdateRepository
}

@Composable
internal fun rememberUpdateRepository(): UpdateRepository {
    val application = LocalContext.current.applicationContext
    return remember(application) { application.updates() }
}

private fun Context.updates(): UpdateRepository =
    EntryPointAccessors.fromApplication(this, UpdatesEntryPoint::class.java).updates()
