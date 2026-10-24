package com.almeidajr.molda.ui.command

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * A command's state in its entry's ViewModel: whether it runs, and an [outcome] the entry has yet
 * to react to. The ViewModel clears the outcome when the entry acknowledges it.
 */
interface CommandState<out T : Any> {
    val isBusy: Boolean
    val outcome: T?
}

/**
 * Reacts to [outcome] once: runs the latest [onOutcome] with it, then [onAcknowledge], which clears
 * it in the ViewModel. A handled outcome is never delivered again after recreation, and one that
 * arrives meanwhile is handled by the recreated entry.
 */
@Composable
fun <T : Any> OutcomeEffect(outcome: T?, onAcknowledge: () -> Unit, onOutcome: (T) -> Unit) {
    val currentOnOutcome by rememberUpdatedState(onOutcome)
    val currentOnAcknowledge by rememberUpdatedState(onAcknowledge)
    LaunchedEffect(outcome) {
        if (outcome == null) return@LaunchedEffect
        // Nothing suspends between the two, so a reaction that leaves the entry still acknowledges.
        currentOnOutcome(outcome)
        currentOnAcknowledge()
    }
}
