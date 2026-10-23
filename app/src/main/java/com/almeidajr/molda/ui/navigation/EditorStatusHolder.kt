package com.almeidajr.molda.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Which editor an editor entry is, which decides what discarding its changes means. */
enum class EditorKind { Profile, Project }

/** One editor's kind and its unsaved and in-flight (saving or updating) work. */
data class EditorReport(
    val kind: EditorKind,
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
)

/** The editor entry on [key] and its [report] when an action asked. */
data class ReportedEditor(val key: NavKey, val report: EditorReport)

/**
 * The status of each editor entry on the back stacks, which the app reads when the sewist acts.
 * An editor reports from its first composition; its report stays until its key leaves the back
 * stacks, since a hidden tab or a screen above it takes it out of composition.
 */
class EditorStatusHolder {
    // Keyed like Navigation 3's default content key, so a restored report finds its entry.
    private val reports = mutableStateMapOf<String, StateFlow<EditorReport>>()

    /** Reads the status of the editor on [key] from [report] until [retainOnly] drops [key]. */
    fun report(key: NavKey, report: StateFlow<EditorReport>) {
        reports[key.toString()] = report
    }

    /** Drops the reports of editors whose keys are not among [keys], all keys on the stacks. */
    fun retainOnly(keys: List<NavKey>) {
        reports.keys.retainAll(keys.map(NavKey::toString).toSet())
    }

    /** The editor an action on [stack] concerns, the topmost one there that reports, or null. */
    fun editorOn(stack: List<NavKey>): ReportedEditor? =
        topmostOn(stack)?.let { ReportedEditor(it, reports.getValue(it.toString()).value) }

    /**
     * The status of the editor an action on [stack] concerns, as observable state, for controls
     * that follow it; actions read [editorOn] instead.
     */
    @Composable
    fun collectStatusOn(stack: List<NavKey>): EditorReport? =
        statusOn(stack).collectAsStateWithLifecycle().value

    /** The live status of the editor an action on [stack] concerns, or of no editor (null). */
    internal fun statusOn(stack: List<NavKey>): StateFlow<EditorReport?> =
        topmostOn(stack)?.let { reports[it.toString()] } ?: NoEditor

    private fun topmostOn(stack: List<NavKey>): NavKey? =
        stack.lastOrNull { it.toString() in reports }

    internal fun dirtyEditors(): Map<String, EditorKind> =
        reports.filterValues { it.value.isDirty }.mapValues { (_, report) -> report.value.kind }

    /** Marks the [editors] dirty until they report again; an editor already reporting wins. */
    internal fun restoreDirty(editors: Map<String, EditorKind>) {
        (editors - reports.keys).forEach { (id, kind) ->
            reports[id] = MutableStateFlow(EditorReport(kind, isDirty = true))
        }
    }
}

private val NoEditor: StateFlow<EditorReport?> = MutableStateFlow(null)

/**
 * Saves [live]'s unsaved changes and restores them into it. After a configuration change [live] is
 * the retained holder, whose reports win; after process death it is new, and only unsaved changes
 * come back, since a save in flight or a load does not outlive the process.
 *
 * It saves each id followed by its kind's name, the list `mapSaver` would save, but restores a
 * list `mapSaver` rejects: a pair cut short or of an unknown kind, as in the former list of plain
 * ids, is skipped.
 */
internal fun editorStatusHolderSaver(live: EditorStatusHolder): Saver<EditorStatusHolder, Any> =
    listSaver(
        save = { holder -> holder.dirtyEditors().flatMap { (id, kind) -> listOf(id, kind.name) } },
        restore = { saved ->
            live.apply { restoreDirty(saved.chunked(2).mapNotNull(::dirtyEditor).toMap()) }
        },
    )

private fun dirtyEditor(saved: List<String>): Pair<String, EditorKind>? =
    EditorKind.entries.find { it.name == saved.getOrNull(1) }?.let { saved.first() to it }

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
 * Reports the status of the editor entry on [key] as [report], which must outlive the composition
 * (a ViewModel's), since it is read whenever the sewist acts, also once the entry is hidden.
 */
@Composable
fun ReportEditorStatus(key: NavKey, report: StateFlow<EditorReport>) {
    val holder = LocalEditorStatusHolder.current
    SideEffect { holder.report(key, report) }
}
