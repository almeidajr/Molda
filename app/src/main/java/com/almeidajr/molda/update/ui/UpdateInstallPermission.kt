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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.almeidajr.molda.R

/**
 * Asks for the "install unknown apps" permission an install of the activity's
 * [UpdateInstallViewModel] waits on: opens Molda's own setting, and explains once, with a retry,
 * when the person returns without allowing it. Placed once, outside the entries.
 */
@Composable
fun UpdateInstallPermission() {
    val viewModel = hiltViewModel<UpdateInstallViewModel>()
    val permission by viewModel.permission.collectAsStateWithLifecycle()
    val packageName = LocalContext.current.packageName
    // The setting answers no result; returning from it is the cue to try again.
    val setting = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.onSettingClosed() }
    LaunchedEffect(permission) {
        if (permission is InstallPermission.Needed) {
            setting.launch(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:$packageName".toUri()),
            )
            viewModel.onSettingOpened()
        }
    }
    val refused = permission as? InstallPermission.Refused ?: return
    UpdatePermissionExplanation(
        onRetry = { viewModel.install(refused.update) },
        onDismiss = viewModel::dismiss,
    )
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
