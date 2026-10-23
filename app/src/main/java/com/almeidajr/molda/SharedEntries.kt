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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.profile.navigation.ProfilePickerKey
import com.almeidajr.molda.profile.ui.ProfilePicker
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.project.navigation.ProjectEditorGuideKey
import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.project.navigation.ProjectGuideKey
import com.almeidajr.molda.project.ui.OpenedProject
import com.almeidajr.molda.project.ui.ProjectsViewModel
import com.almeidajr.molda.skirt.ui.CalculatedFoldingGuide
import com.almeidajr.molda.skirt.ui.CalculatorDestination
import com.almeidajr.molda.skirt.ui.FabricFoldingPlayer
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel
import com.almeidajr.molda.skirt.ui.snapshot
import com.almeidajr.molda.ui.navigation.Navigator
import com.almeidajr.molda.ui.navigation.WindowOverlaySceneStrategy.Companion.windowOverlay

/*
 * Entries where two features meet. Each feature's own section shows only its own UI, so the app
 * wires these: a saved project's skirt draft, and the overlays shared by features.
 */

/**
 * A saved project's skirt draft: its editor on [editor], a draft of its own so editing a project
 * never touches the calculator in Moldes, and its folding guides. [onCloseEditor] asks before the
 * editor's changes are dropped.
 */
internal fun EntryProviderScope<NavKey>.projectDraftSection(
    navigator: Navigator,
    projects: ProjectsViewModel,
    editor: SkirtCalculatorViewModel,
    onCloseEditor: () -> Unit,
) {
    entry<ProjectEditorKey> { key ->
        OpenedProject(key.id, projects, onClose = { navigator.remove(key) }) { project ->
            CalculatorDestination(
                editor,
                title = stringResource(R.string.project_edit_title, project.name.text),
                onUp = onCloseEditor,
                onUseProfile = { navigator.push(ProfilePickerKey) },
                onOpenGuide = { navigator.push(ProjectEditorGuideKey) },
                onSaveProject = { _ ->
                    editor.state.value.snapshot()?.let { projects.update(key.id, it) }
                },
                onSaveAsNewProject = { patternName ->
                    editor.state.value.snapshot()?.let {
                        projects.startSaving(it, patternName, showsSaved = true)
                    }
                },
            )
        }
    }
    entry<ProjectEditorGuideKey> {
        val state by editor.state.collectAsStateWithLifecycle()
        CalculatedFoldingGuide(state.result?.radii) { navigator.remove(ProjectEditorGuideKey) }
    }
    entry<ProjectGuideKey> { key ->
        val close: () -> Unit = { navigator.remove(key) }
        OpenedProject(key.id, projects, onClose = close) { project ->
            FabricFoldingPlayer(project.snapshot.radii, close)
        }
    }
}

/**
 * The profile picker over the calculator [pickerCalculator] names, which [onCreateProfile] leaves
 * for a new profile's editor, and the dialog that confirms [onDiscard] of a dirty editor.
 */
internal fun EntryProviderScope<NavKey>.overlaySection(
    navigator: Navigator,
    profiles: ProfilesViewModel,
    pickerCalculator: () -> SkirtCalculatorViewModel,
    onCreateProfile: () -> Unit,
    onDiscard: (AfterDiscard) -> Unit,
) {
    entry<ProfilePickerKey>(metadata = windowOverlay()) {
        AppProfilePicker(
            profiles,
            pickerCalculator(),
            onDismiss = { navigator.remove(ProfilePickerKey) },
            onCreate = onCreateProfile,
        )
    }
    entry<DiscardChangesKey>(metadata = windowOverlay()) { key ->
        val body = when (key.afterDiscard) {
            AfterDiscard.CloseProjectEditor, AfterDiscard.ShowProjects ->
                R.string.project_discard_body

            else -> R.string.profile_discard_body
        }
        DiscardDialog(body, onDismiss = { navigator.remove(key) }, onDiscard = {
            navigator.remove(key)
            onDiscard(key.afterDiscard)
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
    profiles: ProfilesViewModel,
    calculator: SkirtCalculatorViewModel,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
) {
    val state by profiles.state.collectAsStateWithLifecycle()
    ProfilePicker(state, onDismiss, profiles::load, onSelect = {
        val measurements = it.measurements
        calculator.selectProfile(measurements.waist, measurements.source)
        onDismiss()
    }, onCreate = onCreate)
}
