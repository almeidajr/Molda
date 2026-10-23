package com.almeidajr.molda.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey

/** One editor's unsaved and in-flight work. */
data class EditorReport(val isDirty: Boolean = false, val isSaving: Boolean = false)

/**
 * The status of each editor entry on the back stacks, which the app reads when the sewist acts.
 * An editor reports while composed; its last source stays until its key leaves the back stacks,
 * since a hidden tab or a screen above it takes it out of composition. Only [composedReport] is
 * observable state.
 */
class EditorStatusHolder {
    // Keyed like Navigation 3's default content key, so a restored report finds its entry.
    private val sources = mutableMapOf<String, () -> EditorReport>()
    private val composed = mutableStateMapOf<String, EditorReport>()

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

    /**
     * The status of the editor on [key] while it is composed, or null; observable, so controls
     * shown beside the editor can follow it.
     */
    fun composedReport(key: NavKey): EditorReport? = composed[key.toString()]

    internal fun showComposed(key: NavKey, report: EditorReport) {
        composed[key.toString()] = report
    }

    internal fun hideComposed(key: NavKey) {
        composed -= key.toString()
    }

    internal fun dirtyIds(): List<String> = sources.filterValues { it().isDirty }.keys.toList()

    /** Marks the editors [ids] dirty until they report again; an editor already reporting wins. */
    internal fun restoreDirty(ids: List<String>) {
        ids.filterNot(sources::containsKey).forEach {
            sources[it] = { EditorReport(isDirty = true) }
        }
    }
}

/**
 * Saves [live]'s unsaved changes and restores them into it. After a configuration change [live] is
 * the retained holder, whose reports win; after process death it is new, and only unsaved changes
 * come back, since a save in flight or a load does not outlive the process.
 */
internal fun editorStatusHolderSaver(live: EditorStatusHolder): Saver<EditorStatusHolder, Any> =
    listSaver(save = { it.dirtyIds() }, restore = { ids -> live.apply { restoreDirty(ids) } })

/**
 * The app's [EditorStatusHolder]. Retained across configuration changes, so a hidden editor's
 * save or load still counts; editors out of composition cannot report again until they return.
 */
@Composable
fun rememberEditorStatusHolder(): EditorStatusHolder {
    val holder = retain { EditorStatusHolder() }
    return rememberSaveable(saver = editorStatusHolderSaver(holder)) { holder }
}

/** The [EditorStatusHolder] of the app around an entry. */
val LocalEditorStatusHolder = staticCompositionLocalOf<EditorStatusHolder> {
    error("No EditorStatusHolder: entries report only inside the app's route.")
}

/**
 * Reports the status of the editor entry on [key] while it is composed, with [report], its
 * collected status, for [EditorStatusHolder.composedReport]. [source] is read whenever the sewist
 * acts, also once the entry is out of composition, so it must read state that outlives the
 * composition, such as a ViewModel's.
 */
@Composable
fun ReportEditorStatus(key: NavKey, report: EditorReport, source: () -> EditorReport) {
    val holder = LocalEditorStatusHolder.current
    SideEffect { holder.report(key, source) }
    DisposableEffect(holder, key, report) {
        holder.showComposed(key, report)
        onDispose { holder.hideComposed(key) }
    }
}
