package com.almeidajr.molda.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R

/** A screen's top app bar: its [title] as a heading, with an up arrow when [onUp] is given. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoldaTopAppBar(title: String, modifier: Modifier = Modifier, onUp: (() -> Unit)? = null) {
    TopAppBar(
        title = {
            // TopAppBar measures the title as if its 16dp start inset were free, so a title that
            // wraps at large text would run under an action without this end padding.
            Text(
                title,
                Modifier.padding(end = 16.dp).semantics { heading() },
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
    )
}
