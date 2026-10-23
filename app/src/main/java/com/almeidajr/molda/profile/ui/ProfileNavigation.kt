package com.almeidajr.molda.profile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.almeidajr.molda.R
import com.almeidajr.molda.about.navigation.AboutKey
import com.almeidajr.molda.profile.navigation.EditedProfile
import com.almeidajr.molda.profile.navigation.PickerProfileEditorKey
import com.almeidajr.molda.profile.navigation.ProfileEditorKey
import com.almeidajr.molda.profile.navigation.ProfileManagementKey
import com.almeidajr.molda.profile.navigation.ProfilePickerKey
import com.almeidajr.molda.ui.adaptive.LocalIsTwoPane
import com.almeidajr.molda.ui.adaptive.ReadablePane
import com.almeidajr.molda.ui.navigation.EditorReport
import com.almeidajr.molda.ui.navigation.MoldaTopAppBar
import com.almeidajr.molda.ui.navigation.Navigator
import com.almeidajr.molda.ui.navigation.ReportEditorStatus

internal fun ProfilesViewModel.openEditor(profile: EditedProfile) = when (profile) {
    EditedProfile.New -> startCreating()
    is EditedProfile.Existing -> startEditing(profile.id)
}

private val ProfilesState.editorReport: EditorReport
    get() = EditorReport(isDirty, isSaving, isUnavailable = isLoading || loadError != null)

/**
 * Perfis: the profiles beside the open editor, and the picker's editor, which returns the profile
 * it creates to the calculator that opened the picker. [onRequestEditor] opens an editor, asking
 * first when the open one has changes; [onCancelEditor] closes it the same way. [onShowSnackbar]
 * shows the message of a stored or deleted profile.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.profileSection(
    navigator: Navigator,
    profiles: ProfilesViewModel,
    onRequestEditor: (EditedProfile) -> Unit,
    onCancelEditor: () -> Unit,
    onShowSnackbar: (Int) -> Unit,
) {
    entry<ProfileManagementKey>(
        metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = {
            ProfilePane {
                Text(stringResource(R.string.profile_select_or_create), Modifier.padding(16.dp))
            }
        }),
    ) {
        Column(Modifier.fillMaxSize()) {
            MoldaTopAppBar(
                stringResource(R.string.profile_manage),
                onAbout = { navigator.push(AboutKey) },
            )
            ProfilePane { ProfileList(profiles, onRequestEditor, onShowSnackbar) }
        }
    }
    entry<ProfileEditorKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
        ReportEditorStatus(key) { profiles.state.value.editorReport }
        // Beside the list the editor is a pane, closed by Cancel, choosing from the list or Back.
        OpenEditor(profiles, onCancelEditor, onShowSnackbar, isUpShown = !LocalIsTwoPane.current)
    }
    entry<PickerProfileEditorKey> { key ->
        ReportEditorStatus(key) { profiles.state.value.editorReport }
        SendCreatedProfile(navigator, profiles, key)
        OpenEditor(profiles, onCancelEditor, onShowSnackbar, isUpShown = true)
    }
}

/**
 * Sends the profile the picker's editor [key] creates to its picker's result key, then closes the
 * editor and that picker.
 */
@Composable
private fun SendCreatedProfile(
    navigator: Navigator,
    profiles: ProfilesViewModel,
    key: PickerProfileEditorKey,
) {
    val created = profiles.state.collectAsStateWithLifecycle().value.createdProfile
    val results = LocalResultEventBus.current
    LaunchedEffect(created) {
        if (created != null) {
            results.sendResult(key.resultKey, created)
            navigator.removeAll { it == key || it == ProfilePickerKey(key.resultKey) }
        }
    }
}

/**
 * Keeps Perfis showing the editor [ProfilesViewModel] has open, unless the picker's editor holds
 * it: the ViewModel owns whether the editor is open, since a save or delete also closes it.
 */
@Composable
internal fun SyncProfileEditor(navigator: Navigator, profiles: ProfilesViewModel) {
    val state by profiles.state.collectAsStateWithLifecycle()
    // Closing the picker's editor on any stack can hand the open form back to Perfis.
    val stackSizes = navigator.state.roots.map { navigator.state.stackOf(it).size }
    LaunchedEffect(state.isEditorOpen, state.editingId, stackSizes) {
        syncProfileEditor(navigator, state.form)
    }
}

/**
 * Shows the editor [form] has open above profile management, unless the picker's editor holds
 * it, and drops any other editor from profile management.
 */
private fun syncProfileEditor(navigator: Navigator, form: ProfileForm) {
    val stacks = navigator.state
    val profile = form.editingId?.let(EditedProfile::Existing) ?: EditedProfile.New
    val editor = ProfileEditorKey(profile).takeIf { form.isEditorOpen }
    navigator.removeAll { it is ProfileEditorKey && it != editor }
    if (editor != null && stacks.stackOf(ProfileManagementKey).last() == ProfileManagementKey &&
        stacks.roots.none { root -> stacks.stackOf(root).any { it is PickerProfileEditorKey } }
    ) {
        navigator.push(editor, root = ProfileManagementKey)
    }
}

@Composable
private fun OpenEditor(
    profiles: ProfilesViewModel,
    onCancel: () -> Unit,
    onShowSnackbar: (Int) -> Unit,
    isUpShown: Boolean,
) {
    val state by profiles.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        MoldaTopAppBar(
            stringResource(
                if (state.editingId ==
                    null
                ) {
                    R.string.profile_editor_new
                } else {
                    R.string.profile_editor_edit
                },
            ),
            onUp = onCancel.takeIf { isUpShown },
        )
        ProfilePane {
            ProfileEditor(
                state,
                profiles::edit,
                { profiles.save(onShowSnackbar) },
                onCancel,
                Modifier,
                profiles.today,
                onRetry = profiles::load,
            )
        }
    }
}

@Composable
private fun ProfileList(
    profiles: ProfilesViewModel,
    onRequestEditor: (EditedProfile) -> Unit,
    onShowSnackbar: (Int) -> Unit,
) {
    val state by profiles.state.collectAsStateWithLifecycle()
    ProfilesContent(
        state,
        profiles::load,
        { onRequestEditor(EditedProfile.Existing(it)) },
        { profiles.delete(it, onShowSnackbar) },
        Modifier.padding(16.dp),
    )
}

@Composable
private fun ProfilePane(content: @Composable () -> Unit) {
    Box(Modifier.imePadding()) { ReadablePane(content) }
}
