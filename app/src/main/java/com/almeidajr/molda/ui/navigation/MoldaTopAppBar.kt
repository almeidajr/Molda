package com.almeidajr.molda.ui.navigation

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R

/**
 * A screen's top app bar: its [title] as a heading, with an up arrow when [onUp] is given and the
 * "Sobre o Molda" action when [onAbout] is given (root screens only).
 */
@Composable
fun MoldaTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onUp: (() -> Unit)? = null,
    onAbout: (() -> Unit)? = null,
) {
    TopAppBar(
        title = {
            // TopAppBar measures the title as if its 16dp start inset were free, so a title that
            // wraps at large text would run under an action without this end padding.
            Text(
                title,
                Modifier.padding(end = 16.dp).focusRequester(rememberTitleFocusRequester())
                    .focusable().semantics { heading() },
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (onUp != null) {
                IconButton(onClick = onUp) {
                    Icon(
                        painterResource(R.drawable.ic_arrow_back),
                        stringResource(R.string.navigation_up),
                    )
                }
            }
        },
        actions = {
            if (onAbout != null) {
                IconButton(onClick = onAbout, modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(R.drawable.ic_info), stringResource(R.string.about_title))
                }
            }
        },
    )
}
