package com.almeidajr.molda.ui.navigation

import androidx.compose.runtime.mutableIntStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.ResultEventBus

/**
 * A [Navigator] over real back stacks holding [stacks], each starting at its destination's root,
 * the first being the start destination, with [selected]'s destination selected.
 */
internal fun navigatorOf(
    stacks: List<List<NavKey>>,
    selected: NavKey = stacks.first().first(),
    onSwitch: () -> Unit = {},
): Navigator {
    val roots = stacks.map { it.first() }
    val state = TopLevelBackStacks(
        roots,
        mutableIntStateOf(roots.indexOf(selected)),
        stacks.map { NavBackStack(*it.toTypedArray()) },
        ResultEventBus(),
    )
    return Navigator(state, onSwitch)
}

/** A snapshot of every destination's back stack, in the roots' order, to compare by value. */
internal val Navigator.stacks: List<List<NavKey>>
    get() = state.roots.map { state.stackOf(it).toList() }
