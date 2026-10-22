package com.almeidajr.molda.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation3.runtime.NavKey

/**
 * The commands that change [state]: selecting a destination, pushing and removing keys. [onSwitch]
 * runs whenever another destination is about to be selected.
 */
@Stable
class Navigator(val state: TopLevelBackStacks, private val onSwitch: () -> Unit) {
    /** Switches to [root]'s destination, or returns it to its root when already selected. */
    fun select(root: NavKey) {
        if (root == state.selected) {
            popAbove(0)
        } else {
            switchTo(state.roots.indexOf(root))
        }
    }

    /** Pops the selected stack; at another destination's root, returns to the start one. */
    fun back() {
        val stack = state.stacks[state.selectedIndex]
        if (stack.size > 1) stack.removeAt(stack.lastIndex) else switchTo(0)
    }

    /** Pushes [key] onto the stack of the destination whose root is [root]. */
    fun push(key: NavKey, root: NavKey = state.selected) {
        state.stacks[state.roots.indexOf(root)].add(key)
    }

    /** Removes every key matching [predicate] from every stack; roots always stay. */
    fun removeAll(predicate: (NavKey) -> Boolean) {
        state.stacks.forEach { stack -> stack.removeAll { it !in state.roots && predicate(it) } }
    }

    /** Removes [key] from every stack. */
    fun remove(key: NavKey) = removeAll { it == key }

    /** Pops the selected stack down to [key], pushing it when the stack does not hold it. */
    fun popTo(key: NavKey) {
        val index = state.current.indexOf(key)
        if (index < 0) push(key) else popAbove(index)
    }

    private fun switchTo(index: Int) {
        if (index != state.selectedIndex) onSwitch()
        state.selectedIndex = index
    }

    private fun popAbove(index: Int) {
        val stack = state.stacks[state.selectedIndex]
        stack.subList(index + 1, stack.size).clear()
    }
}

/** Remembers a [Navigator] over [state]; [onSwitch] runs before another destination is selected. */
@Composable
fun rememberNavigator(state: TopLevelBackStacks, onSwitch: () -> Unit): Navigator {
    val currentOnSwitch by rememberUpdatedState(onSwitch)
    return remember(state) { Navigator(state, onSwitch = { currentOnSwitch() }) }
}
