package com.almeidajr.molda.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.focus.FocusRequester

/**
 * Hands focus to the title of the screen a destination switch shows. Not observable state: the
 * request is read once, by the first title composed after it.
 */
class TitleFocus {
    private var isRequested = false

    /** Asks the next screen title composed to take focus. */
    fun request() {
        isRequested = true
    }

    internal fun take(): Boolean = isRequested.also { isRequested = false }
}

/** The [TitleFocus] of the navigation shell around a screen. */
val LocalTitleFocus = staticCompositionLocalOf { TitleFocus() }

/**
 * A requester for a screen's focusable title heading: the title takes focus when it is the first
 * composed after a destination switch.
 */
@Composable
fun rememberTitleFocusRequester(): FocusRequester {
    val titleFocus = LocalTitleFocus.current
    val requester = remember { FocusRequester() }
    LaunchedEffect(titleFocus) {
        if (titleFocus.take()) requester.requestFocus()
    }
    return requester
}
