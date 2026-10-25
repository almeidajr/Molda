package com.almeidajr.molda.update.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.almeidajr.molda.R
import com.almeidajr.molda.update.domain.UpdateCheck

/** The update on offer, checked by the hosting entry's own [UpdateCheckViewModel]. */
@Composable
fun UpdateStatus(modifier: Modifier = Modifier) {
    val viewModel = hiltViewModel<UpdateCheckViewModel>()
    val check by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    UpdateStatus(check, onCheck = viewModel::check, onOpenPage = uriHandler::openUri, modifier)
}

/**
 * The outcome of the last [check] (null while checking), the release page of an available update,
 * and the action that checks again. A local build, which never checks, shows nothing.
 */
@Composable
fun UpdateStatus(
    check: UpdateCheck?,
    onCheck: () -> Unit,
    onOpenPage: (String) -> Unit,
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

@Composable
private fun UpdateCheck?.headline(): String = when (this) {
    null -> stringResource(R.string.update_checking)
    is UpdateCheck.Available -> stringResource(R.string.update_available, update.version)
    UpdateCheck.UpToDate -> stringResource(R.string.update_up_to_date)
    UpdateCheck.Failed, UpdateCheck.NotChecked -> stringResource(R.string.update_failed)
}
