package com.almeidajr.molda.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.ResultEventBus
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.scene.Scene

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
        val decorated = stacks.mapIndexed { index, stack ->
            val root = roots[index]
            rememberDecoratedNavEntries(
                stack,
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberResultEventBusNavEntryDecorator(resultBus),
                ),
            ) { key ->
                val entry = entryProvider(key)
                val marked = entry.metadata + metadata { put(TopLevelKey, root) }
                NavEntry(key, entry.contentKey, marked) {
                    entry.Content()
                }
            }
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

/** Marks each entry with the root of the destination whose stack holds it. */
private object TopLevelKey : NavMetadataKey<NavKey>

private val Scene<NavKey>.topLevel: NavKey? get() = entries.lastOrNull()?.metadata?.get(TopLevelKey)

/**
 * Material motion for [TopLevelBackStacks]: a fade-through between destinations and a shared-axis
 * slide within one destination's stack, forward when [isForward] and back otherwise.
 */
fun AnimatedContentTransitionScope<Scene<NavKey>>.materialTransition(
    density: Density,
    isForward: Boolean,
): ContentTransform {
    // Material's motion: the outgoing screen fades in 90ms, then the incoming one enters in 210ms.
    val enter = tween<Float>(durationMillis = 210, delayMillis = 90)
    val exit = tween<Float>(durationMillis = 90)
    return if (initialState.topLevel != targetState.topLevel) {
        fadeIn(enter) + scaleIn(enter, initialScale = 0.92f) togetherWith fadeOut(exit)
    } else {
        val offset = with(density) { 30.dp.roundToPx() } * if (isForward) 1 else -1
        val slide = tween<IntOffset>(durationMillis = 300)
        slideInHorizontally(slide) { offset } + fadeIn(enter) togetherWith
            slideOutHorizontally(slide) { -offset } + fadeOut(exit)
    }
}
