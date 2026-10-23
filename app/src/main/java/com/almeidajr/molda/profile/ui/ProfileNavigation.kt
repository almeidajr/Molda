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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.almeidajr.molda.profile.navigation.closeEditorOf
import com.almeidajr.molda.profile.navigation.closePickerEditor
import com.almeidajr.molda.profile.navigation.showsProfileCreateAction
import com.almeidajr.molda.ui.adaptive.LocalIsTwoPane
import com.almeidajr.molda.ui.adaptive.ReadablePane
import com.almeidajr.molda.ui.navigation.LocalEditorStatusHolder
import com.almeidajr.molda.ui.navigation.MoldaTopAppBar
import com.almeidajr.molda.ui.navigation.Navigator
import com.almeidajr.molda.ui.navigation.ReportEditorStatus

/**
 * Perfis: the profiles beside the open editor, and the picker's editor, which returns the profile
 * it creates to the calculator that opened the picker. Each list and each editor entry has its own
 * ViewModel. [onRequestEditor] opens an editor, asking first when the open one has changes;
 * [onCancelEditor] closes it the same way. [onShowSnackbar] shows the message of a stored or
 * deleted profile.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.profileSection(
    navigator: Navigator,
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
        val profiles = hiltViewModel<ProfileListViewModel>()
        ProfileDeletedEffect(profiles, navigator, onShowSnackbar)
        Column(Modifier.fillMaxSize()) {
            MoldaTopAppBar(
                stringResource(R.string.profile_manage),
                onAbout = { navigator.push(AboutKey) },
            )
            ProfilePane { ProfileList(profiles, navigator, onRequestEditor) }
        }
    }
    entry<ProfileEditorKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
        val editor = editorViewModel(key.profile)
        ReportEditorStatus(key, editor.report)
        ProfileSavedEffect(editor) {
            onShowSnackbar(R.string.profile_saved)
            navigator.remove(key)
        }
        // Beside the list the editor is a pane, closed by Cancel, choosing from the list or Back.
        OpenEditor(editor, onCancelEditor, isUpShown = !LocalIsTwoPane.current)
    }
    entry<PickerProfileEditorKey> { key ->
        val editor = editorViewModel(EditedProfile.New)
        ReportEditorStatus(key, editor.report)
        val results = LocalResultEventBus.current
        ProfileSavedEffect(editor) { save ->
            onShowSnackbar(R.string.profile_saved)
            if (save is ProfileSave.Created) results.sendResult(key.resultKey, save.profile)
            navigator.closePickerEditor(key)
        }
        OpenEditor(editor, onCancelEditor, isUpShown = true)
    }
}

@Composable
private fun editorViewModel(profile: EditedProfile) =
    hiltViewModel<ProfileEditorViewModel, ProfileEditorViewModel.Factory> { it.create(profile) }

/** Runs [onSaved] once [editor]'s save has stored the profile; it closes the editor. */
@Composable
private fun ProfileSavedEffect(editor: ProfileEditorViewModel, onSaved: (ProfileSave) -> Unit) {
    val saved = editor.state.collectAsStateWithLifecycle().value.saved
    val currentOnSaved by rememberUpdatedState(onSaved)
    LaunchedEffect(saved) { saved?.let(currentOnSaved) }
}

/** Shows each deletion from [profiles] and closes any editor of the deleted profile. */
@Composable
private fun ProfileDeletedEffect(
    profiles: ProfileListViewModel,
    navigator: Navigator,
    onShowSnackbar: (Int) -> Unit,
) {
    val deleted = profiles.state.collectAsStateWithLifecycle().value.deleted
    val currentOnShowSnackbar by rememberUpdatedState(onShowSnackbar)
    LaunchedEffect(deleted) {
        if (deleted == null) return@LaunchedEffect
        currentOnShowSnackbar(R.string.profile_deleted)
        navigator.closeEditorOf(deleted)
        profiles.deletionShown()
    }
}

@Composable
private fun OpenEditor(editor: ProfileEditorViewModel, onCancel: () -> Unit, isUpShown: Boolean) {
    val state by editor.state.collectAsStateWithLifecycle()
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
                editor::edit,
                editor::save,
                onCancel,
                Modifier,
                editor.today,
                onRetry = editor::load,
            )
        }
    }
}

@Composable
private fun ProfileList(
    profiles: ProfileListViewModel,
    navigator: Navigator,
    onRequestEditor: (EditedProfile) -> Unit,
) {
    val state by profiles.state.collectAsStateWithLifecycle()
    ProfilesContent(
        state,
        profiles::load,
        { onRequestEditor(EditedProfile.Existing(it)) },
        profiles::delete,
        Modifier.padding(16.dp),
        canChangeProfiles = state.canChangeProfiles && !isPerfisEditorSaving(navigator),
    )
}

/**
 * The shell's floating action over Perfis: new profile, which [onCreate] opens an editor for. It
 * shows only where [showsProfileCreateAction] says, for whether panes sit side by side
 * ([isTwoPane]).
 */
@Composable
internal fun ProfileFloatingAction(navigator: Navigator, isTwoPane: Boolean, onCreate: () -> Unit) {
    if (navigator.state.showsProfileCreateAction(isTwoPane)) {
        ProfileCreateAction(onCreate, enabled = !isPerfisEditorSaving(navigator))
    }
}

/**
 * Whether the Perfis editor beside the list saves, so no other editor may open; observable, for
 * the controls that would open one.
 */
@Composable
private fun isPerfisEditorSaving(navigator: Navigator): Boolean = LocalEditorStatusHolder.current
    .collectStatusOn(navigator.state.stackOf(ProfileManagementKey))?.isSaving == true

@Composable
private fun ProfilePane(content: @Composable () -> Unit) {
    Box(Modifier.imePadding()) { ReadablePane(content) }
}
