package com.almeidajr.molda.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.contains
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/** Shows an [entry] that opens its own window above the entries below it, which stay composed. */
private data class WindowOverlayScene<T : Any>(
    override val key: Any,
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
) : OverlayScene<T> {
    override val overlaidEntries: List<NavEntry<T>> get() = previousEntries
    override val entries: List<NavEntry<T>> get() = listOf(entry)
    override val content: @Composable () -> Unit = { entry.Content() }
}

/**
 * Shows a destination marked with [windowOverlay] as an overlay scene. The destination draws its
 * own modal bottom sheet or dialog and dismisses it by leaving the back stack, so the window keeps
 * its Material look and enter and exit motion. Place it before any non-overlay strategy.
 */
class WindowOverlaySceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val last = entries.lastOrNull()?.takeIf { WindowOverlayKey in it.metadata } ?: return null
        return WindowOverlayScene(last.contentKey, last, entries.dropLast(1))
    }

    companion object {
        private object WindowOverlayKey : NavMetadataKey<Boolean>

        /** Marks a destination whose content is a modal bottom sheet or dialog. */
        fun windowOverlay(): Map<String, Any> = metadata { put(WindowOverlayKey, true) }
    }
}
