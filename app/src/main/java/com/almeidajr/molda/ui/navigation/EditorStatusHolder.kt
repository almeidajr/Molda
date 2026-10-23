package com.almeidajr.molda.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey

/** One editor's unsaved and in-flight work. */
data class EditorReport(
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    /** What the editor works on is loading or failed to load. */
    val isUnavailable: Boolean = false,
)

/**
 * The status of each editor entry on the back stacks, which the app reads when the sewist acts.
 * Not observable state. An editor reports while composed; its last source stays until its key
 * leaves the back stacks, since a hidden tab or a screen above it takes it out of composition.
 */
class EditorStatusHolder internal constructor(sources: Map<String, () -> EditorReport>) {
    // Keyed like Navigation 3's default content key, so a restored report finds its entry.
    private val sources = sources.toMutableMap()

    constructor() : this(emptyMap())

    /** Reads the status of the editor on [key] from [source] until [key] leaves the stacks. */
    fun report(key: NavKey, source: () -> EditorReport) {
        sources[key.toString()] = source
    }

    /** The status of each editor among [keys], all keys on the back stacks; others are dropped. */
    fun reports(keys: List<NavKey>): Map<NavKey, EditorReport> {
        val byId = keys.associateBy(NavKey::toString)
        sources.keys.retainAll(byId.keys)
        return sources.mapNotNull { (id, source) -> byId[id]?.let { it to source() } }.toMap()
    }

    internal fun dirtyIds(): List<String> = sources.filterValues { it().isDirty }.keys.toList()
}

/**
 * Saves only unsaved changes: an editor out of composition cannot report again until it returns,
 * and a save in flight or a load does not outlive the process.
 */
val EditorStatusHolderSaver: Saver<EditorStatusHolder, Any> = listSaver(
    save = { it.dirtyIds() },
    restore = { ids -> EditorStatusHolder(ids.associateWith { { EditorReport(isDirty = true) } }) },
)

/** The [EditorStatusHolder] of the app around an entry. */
val LocalEditorStatusHolder = staticCompositionLocalOf<EditorStatusHolder> {
    error("No EditorStatusHolder: entries report only inside the app's route.")
}

/**
 * Reports the status of the editor entry on [key] while it is composed. [source] is read whenever
 * the sewist acts, also once the entry is out of composition, so it must read state that outlives
 * the composition, such as a ViewModel's.
 */
@Composable
fun ReportEditorStatus(key: NavKey, source: () -> EditorReport) {
    val holder = LocalEditorStatusHolder.current
    SideEffect { holder.report(key, source) }
}
