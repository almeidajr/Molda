package com.almeidajr.molda.update.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.almeidajr.molda.R

/**
 * The installation [viewModel] runs: opens Molda's own "install unknown apps" setting when it
 * needs the permission, explains once, with a retry, when the person returns without allowing it,
 * and [onClose]s once the installation started or the explanation is dismissed. Shows nothing
 * otherwise.
 */
@Composable
internal fun UpdateInstall(viewModel: UpdateInstallViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    val packageName = LocalContext.current.packageName
    // The setting answers no result; returning from it is the cue to try again.
    val setting = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.onSettingClosed() }
    LaunchedEffect(state) {
        when (state) {
            UpdateInstallState.Started -> currentOnClose()

            UpdateInstallState.NeedsPermission -> {
                setting.launch(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        "package:$packageName".toUri(),
                    ),
                )
                viewModel.onSettingOpened()
            }

            UpdateInstallState.Installing,
            UpdateInstallState.WaitingForPermission,
            UpdateInstallState.Refused,
            -> Unit
        }
    }
    if (state == UpdateInstallState.Refused) {
        UpdatePermissionExplanation(onRetry = viewModel::retry, onDismiss = onClose)
    }
}

@Composable
private fun UpdatePermissionExplanation(onRetry: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_permission_title)) },
        text = { Text(stringResource(R.string.update_permission_body)) },
        confirmButton = {
            TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.update_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
