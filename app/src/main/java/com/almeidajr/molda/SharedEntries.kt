package com.almeidajr.molda

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.navigation.ProfilePickerKey
import com.almeidajr.molda.profile.ui.ProfileListViewModel
import com.almeidajr.molda.profile.ui.ProfilePicker
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.project.navigation.ProjectEditorGuideKey
import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.project.navigation.ProjectGuideKey
import com.almeidajr.molda.project.navigation.SaveProjectKey
import com.almeidajr.molda.project.navigation.profileResultKey
import com.almeidajr.molda.project.ui.OpenedProject
import com.almeidajr.molda.project.ui.ProjectUpdateEffect
import com.almeidajr.molda.project.ui.ProjectUpdateViewModel
import com.almeidajr.molda.skirt.ui.CalculatorDestination
import com.almeidajr.molda.skirt.ui.CalculatorState
import com.almeidajr.molda.skirt.ui.FabricFoldingPlayer
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel
import com.almeidajr.molda.skirt.ui.useProfile
import com.almeidajr.molda.ui.navigation.EditorKind
import com.almeidajr.molda.ui.navigation.Navigator
import com.almeidajr.molda.ui.navigation.ReportEditorStatus
import com.almeidajr.molda.ui.navigation.WindowOverlaySceneStrategy.Companion.windowOverlay
import kotlinx.coroutines.flow.map

/*
 * Entries where two features meet. Each feature's own section shows only its own UI, so the app
 * wires these: a saved project's skirt draft, and the overlays shared by features.
 */

/** The calculated result as a project snapshot, or null before a successful calculation. */
internal fun CalculatorState.snapshot(): SkirtSnapshot? =
    result?.let { SkirtSnapshot(it.draft, it.radii, form.source) }

/** The result key the discard dialog sends its confirmed [AfterDiscard] to. */
internal const val DISCARD_RESULT_KEY = "discard"

/**
 * A saved project's skirt draft: its editor, with a calculator of its own started from the key's
 * snapshot so editing a project never touches the calculator in Moldes, and its folding guides.
 * [onShowSnackbar] shows the outcome of updating the project; [onCloseEditor] asks before the
 * editor's changes are dropped.
 */
internal fun EntryProviderScope<NavKey>.projectDraftSection(
    navigator: Navigator,
    onShowSnackbar: (Int) -> Unit,
    onCloseEditor: () -> Unit,
) {
    entry<ProjectEditorKey> { key ->
        val editor = hiltViewModel<SkirtCalculatorViewModel, SkirtCalculatorViewModel.Factory> {
            it.create(key.snapshot.draft, key.snapshot.source)
        }
        val update = hiltViewModel<ProjectUpdateViewModel, ProjectUpdateViewModel.Factory> {
            it.create(key, editor.state.map { state -> state.isDirty })
        }
        ReportEditorStatus(key, update.report)
        ProjectUpdateEffect(update, key.id, navigator, onShowSnackbar)
        ResultEffect<MeasurementProfile>(key.profileResultKey) { editor.useProfile(it) }
        OpenedProject(key.id, onClose = { navigator.remove(key) }) { project ->
            CalculatorDestination(
                editor,
                title = stringResource(R.string.project_edit_title, project.name.text),
                onUp = onCloseEditor,
                onUseProfile = { navigator.push(ProfilePickerKey(key.profileResultKey)) },
                onOpenGuide = {
                    editor.state.value.result?.radii?.let {
                        navigator.push(ProjectEditorGuideKey(it))
                    }
                },
                onSaveProject = { editor.state.value.snapshot()?.let(update::update) },
                onSaveAsNewProject = {
                    editor.state.value.snapshot()?.let {
                        navigator.push(SaveProjectKey(it, showsSaved = true))
                    }
                },
            )
        }
    }
    entry<ProjectEditorGuideKey> { key ->
        FabricFoldingPlayer(key.radii, onClose = { navigator.remove(key) })
    }
    entry<ProjectGuideKey> { key ->
        val close: () -> Unit = { navigator.remove(key) }
        OpenedProject(key.id, onClose = close) { project ->
            FabricFoldingPlayer(project.snapshot.radii, close)
        }
    }
}

/**
 * The profile picker, with a profile list of its own, which sends the chosen profile to its
 * result key or [onCreateProfile] leaves for a new profile's editor that sends the created one
 * there, and the dialog that sends what confirming the discard of a dirty editor does next to the
 * app's wiring, as a result on [DISCARD_RESULT_KEY].
 */
internal fun EntryProviderScope<NavKey>.overlaySection(
    navigator: Navigator,
    onCreateProfile: (resultKey: String) -> Unit,
) {
    entry<ProfilePickerKey>(metadata = windowOverlay()) { key ->
        val results = LocalResultEventBus.current
        AppProfilePicker(
            hiltViewModel(),
            onSelect = { results.sendResult(key.resultKey, it) },
            onDismiss = { navigator.remove(key) },
            onCreate = { onCreateProfile(key.resultKey) },
        )
    }
    entry<DiscardChangesKey>(metadata = windowOverlay()) { key ->
        val body = when (key.afterDiscard.discards) {
            EditorKind.Profile -> R.string.profile_discard_body
            EditorKind.Project -> R.string.project_discard_body
        }
        val results = LocalResultEventBus.current
        DiscardDialog(body, onDismiss = { navigator.remove(key) }, onDiscard = {
            results.sendResult(DISCARD_RESULT_KEY, key.afterDiscard)
            navigator.remove(key)
        })
    }
}

@Composable
private fun DiscardDialog(@StringRes body: Int, onDismiss: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.profile_discard_title)) },
        text = { Text(stringResource(body)) },
        confirmButton = {
            TextButton(onClick = {
                onDiscard()
            }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.profile_discard))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
            }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.profile_keep_editing))
            }
        },
    )
}

@Composable
private fun AppProfilePicker(
    profiles: ProfileListViewModel,
    onSelect: (MeasurementProfile) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
) {
    val state by profiles.state.collectAsStateWithLifecycle()
    ProfilePicker(state, onDismiss, profiles::load, onSelect = {
        onSelect(it)
        onDismiss()
    }, onCreate = onCreate)
}
