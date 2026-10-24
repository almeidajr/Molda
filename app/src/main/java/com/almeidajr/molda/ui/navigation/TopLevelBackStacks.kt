package com.almeidajr.molda.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.ResultEventBus
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator

/**
 * One saveable back stack per top-level destination, each starting at its root key. The first
 * root is the start destination: its stack always sits below the selected one, so Back at another
 * root returns to it and Back at its own root leaves the app. [Navigator] changes it. Every
 * stack's entries share [resultBus], so an entry's result reaches its requester on any stack.
 */
@Stable
class TopLevelBackStacks internal constructor(
    val roots: List<NavKey>,
    selectedIndex: MutableIntState,
    internal val stacks: List<NavBackStack<NavKey>>,
    private val resultBus: ResultEventBus,
) {
    internal var selectedIndex by selectedIndex

    val selected: NavKey get() = roots[selectedIndex]

    /** The selected destination's back stack, read-only. */
    val current: List<NavKey> get() = stacks[selectedIndex]

    /** The back stack of the destination whose root is [root], read-only. */
    fun stackOf(root: NavKey): List<NavKey> = stacks[roots.indexOf(root)]

    /** The entries to show: the start stack, then the selected stack when it is another one. */
    @Composable
    fun entries(entryProvider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> {
        val decorated = stacks.map { stack ->
            rememberDecoratedNavEntries(
                stack,
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator<NavKey>(),
                    rememberResultEventBusNavEntryDecorator(resultBus),
                ),
                entryProvider,
            )
        }
        return if (selectedIndex == 0) decorated[0] else decorated[0] + decorated[selectedIndex]
    }
}

/**
 * Remembers one back stack per root in [roots], the first being the start destination, whose
 * entries all send and receive results on [resultBus].
 */
@Composable
fun rememberTopLevelBackStacks(roots: List<NavKey>, resultBus: ResultEventBus): TopLevelBackStacks {
    val selectedIndex = rememberSaveable { mutableIntStateOf(0) }
    val stacks = roots.map { rememberNavBackStack(it) }
    return remember(roots, resultBus) {
        TopLevelBackStacks(roots, selectedIndex, stacks, resultBus)
    }
}
