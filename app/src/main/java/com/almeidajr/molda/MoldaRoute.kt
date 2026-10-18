package com.almeidajr.molda

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategy
import androidx.navigation3.ui.NavDisplay
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.ui.ProfileCreateAction
import com.almeidajr.molda.profile.ui.ProfileEditor
import com.almeidajr.molda.profile.ui.ProfileForm
import com.almeidajr.molda.profile.ui.ProfilePicker
import com.almeidajr.molda.profile.ui.ProfilesContent
import com.almeidajr.molda.profile.ui.ProfilesState
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.profile.ui.profileWaistText
import com.almeidajr.molda.skirt.ui.FabricFoldingPlayer
import com.almeidajr.molda.skirt.ui.SkirtCalculatorScreen
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel
import com.almeidajr.molda.ui.adaptive.HingeSafeContent
import com.almeidajr.molda.ui.adaptive.ReadablePane
import com.almeidajr.molda.ui.adaptive.minimumTwoPaneWidth
import com.almeidajr.molda.ui.adaptive.rememberReadableListDetailStrategy
import com.almeidajr.molda.ui.navigation.WindowOverlaySceneStrategy
import com.almeidajr.molda.ui.navigation.WindowOverlaySceneStrategy.Companion.windowOverlay
import kotlinx.serialization.Serializable

@Serializable
private data object SkirtCalculatorKey : NavKey

@Serializable
private data object FabricFoldingTutorialKey : NavKey

@Serializable
private data object ProfileManagementKey : NavKey

/** The open profile editor; a null [id] is a new profile. */
@Serializable
private data class ProfileEditorKey(val id: ProfileId?) : NavKey

/** The profile picker sheet over the calculator. */
@Serializable
private data object ProfilePickerKey : NavKey

/** Asks to discard the dirty editor, then opens the [requested] editor or, if null, closes it. */
@Serializable
private data class DiscardChangesKey(val requested: ProfileEditorKey? = null) : NavKey

/**
 * Shows every destination under one scene key, so a destination replaces the previous one within
 * a frame, without animation, as before the back stack. A scene change would keep the previous
 * destination for an extra frame, and removing its focused button then would move initial focus
 * into the new destination.
 */
private data class SameFrameScene<T : Any>(val scene: Scene<T>) : Scene<T> by scene {
    override val key: Any get() = SameFrameScene::class
}

// Keep saveable destinations and their transitions together.
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Suppress("LongMethod")
@Composable
fun MoldaRoute(
    modifier: Modifier = Modifier,
    calculator: SkirtCalculatorViewModel = hiltViewModel(),
    profiles: ProfilesViewModel = hiltViewModel(),
) {
    val profileState by profiles.state.collectAsStateWithLifecycle()
    val calculatorState by calculator.state.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(SkirtCalculatorKey)
    // ProfilesViewModel owns whether the editor is open (a save or delete also closes it).
    LaunchedEffect(profileState.isEditorOpen, profileState.editingId, backStack.size) {
        syncProfileEditor(backStack, profileState.form)
    }
    // A null id requests a new profile.
    val requestEditor: (ProfileId?) -> Unit = { id ->
        requestProfileEditor(profileState, profiles, id) {
            backStack.add(DiscardChangesKey(ProfileEditorKey(id)))
        }
    }
    val manageProfiles: () -> Unit = { backStack.add(ProfileManagementKey) }
    val snackbar = remember { SnackbarHostState() }
    var snackbarHeight by remember { mutableIntStateOf(0) }
    val snackbarInset = with(LocalDensity.current) { snackbarHeight.toDp() }
    // An editor opened from the picker returns to it.
    val returnToPicker: () -> Unit = {
        val picker = backStack.indexOf(ProfilePickerKey)
        if (picker >=
            0
        ) {
            while (backStack.lastIndex > picker) backStack.removeAt(backStack.lastIndex)
        }
    }
    val finishEditor = {
        profiles.cancel()
        returnToPicker()
    }
    val cancelEditor: () -> Unit = {
        if (profileState.isDirty) backStack.add(DiscardChangesKey()) else finishEditor()
    }
    ProfileSaveFeedback(profiles, snackbar, returnToPicker)
    val isManaging = ProfileManagementKey in backStack
    val isTutorialOpen = backStack.last() == FabricFoldingTutorialKey
    // The player is one column even on a wide window: keep it off a book-posture fold only.
    HingeSafeContent(modifier.fillMaxSize(), isSingleColumn = isTutorialOpen) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= minimumTwoPaneWidth(LocalDensity.current.fontScale)
            val listDetail = rememberReadableListDetailStrategy<NavKey>(maxWidth)
            val windowOverlay = remember { WindowOverlaySceneStrategy<NavKey>() }
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = {
                    SnackbarHost(snackbar, Modifier.onSizeChanged { snackbarHeight = it.height })
                },
                floatingActionButtonPosition = if (wide) FabPosition.Start else FabPosition.End,
                floatingActionButton = {
                    if (isManaging && (!profileState.isEditorOpen || wide)) {
                        ProfileCreateAction(profileState, { requestEditor(null) })
                    }
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { padding ->
                NavDisplay(
                    backStack = backStack,
                    // Keep bottom actions reachable while storage feedback is visible.
                    modifier = Modifier.padding(padding).padding(bottom = snackbarInset),
                    onBack = {
                        if (backStack.last() !is ProfileEditorKey) {
                            backStack.removeLastOrNull()
                        } else if (!profileState.isSaving) {
                            cancelEditor()
                        }
                    },
                    // A narrow window shows one destination at a time, as a plain single pane.
                    sceneStrategies = listOfNotNull(windowOverlay, listDetail.takeIf { wide }),
                    sceneDecoratorStrategies = listOf(
                        SceneDecoratorStrategy { scene -> SameFrameScene(scene) },
                    ),
                    entryProvider = entryProvider {
                        entry<SkirtCalculatorKey> {
                            CalculatorDestination(
                                calculator,
                                manageProfiles,
                                { backStack.add(ProfilePickerKey) },
                                { backStack.add(FabricFoldingTutorialKey) },
                            )
                        }
                        entry<FabricFoldingTutorialKey> {
                            val close: () -> Unit = { backStack.remove(FabricFoldingTutorialKey) }
                            val radii = calculatorState.radii
                            if (radii != null) {
                                FabricFoldingPlayer(radii, close)
                            } else {
                                // Results do not survive process death; show the calculator.
                                LaunchedEffect(Unit) { close() }
                            }
                        }
                        entry<ProfileManagementKey>(
                            metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = {
                                ProfilePane {
                                    Text(
                                        stringResource(R.string.profile_select_or_create),
                                        Modifier.padding(16.dp),
                                    )
                                }
                            }),
                        ) {
                            ProfilePane {
                                ProfileList(profiles, requestEditor) {
                                    backStack.remove(ProfileManagementKey)
                                }
                            }
                        }
                        entry<ProfilePickerKey>(metadata = windowOverlay()) {
                            AppProfilePicker(
                                profiles,
                                calculator,
                                onDismiss = { backStack.remove(ProfilePickerKey) },
                                onCreate = {
                                    profiles.startCreating()
                                    manageProfiles()
                                },
                            )
                        }
                        entry<DiscardChangesKey>(metadata = windowOverlay()) { key ->
                            DiscardDialog(onDismiss = { backStack.remove(key) }, onDiscard = {
                                backStack.remove(key)
                                discardEditor(profiles, key.requested, finishEditor)
                            })
                        }
                        entry<ProfileEditorKey>(metadata = ListDetailSceneStrategy.detailPane()) {
                            ProfilePane {
                                ProfileEditor(
                                    profileState,
                                    profiles::edit,
                                    profiles::save,
                                    cancelEditor,
                                    Modifier,
                                    profiles.today,
                                    onRetry = profiles::load,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

/** Drops the unsaved editor, then finishes editing or, if [next] is set, opens that editor. */
private fun discardEditor(
    profiles: ProfilesViewModel,
    next: ProfileEditorKey?,
    finish: () -> Unit,
) {
    if (next == null) return finish()
    profiles.cancel()
    val id = next.id
    if (id == null) profiles.startCreating() else profiles.startEditing(id)
}

/** Shows the editor [form] has open above profile management, and drops any other editor. */
private fun syncProfileEditor(backStack: NavBackStack<NavKey>, form: ProfileForm) {
    val editor = ProfileEditorKey(form.editingId).takeIf { form.isEditorOpen }
    backStack.removeAll { it is ProfileEditorKey && it != editor }
    if (editor != null && backStack.lastOrNull() == ProfileManagementKey) backStack.add(editor)
}

@Composable
private fun CalculatorDestination(
    calculator: SkirtCalculatorViewModel,
    onManageProfiles: () -> Unit,
    onUseProfile: () -> Unit,
    onOpenTutorial: () -> Unit,
) {
    val state by calculator.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        CalculatorTopBar(onManageProfiles)
        SkirtCalculatorScreen(
            state,
            calculator::edit,
            calculator::calculate,
            Modifier.weight(1f),
            onBlur = calculator::blur,
            onUseProfile = onUseProfile,
            onOpenTutorial = onOpenTutorial,
        )
    }
}

@Composable
private fun ProfileList(
    profiles: ProfilesViewModel,
    requestEditor: (ProfileId?) -> Unit,
    onBackToCalculator: () -> Unit,
) {
    val state by profiles.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (!state.isEditorOpen) {
            TextButton(onClick = onBackToCalculator, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.profile_back_to_calculator))
            }
        }
        ProfilesContent(state, profiles::load, requestEditor, profiles::delete)
    }
}

@Composable
private fun ProfilePane(content: @Composable () -> Unit) {
    Box(Modifier.imePadding()) { ReadablePane(content) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculatorTopBar(onManageProfiles: () -> Unit) {
    TopAppBar(
        title = {
            // TopAppBar measures the title as if its 16dp start inset were free, so a title that
            // wraps at large text would run under the action without this end padding.
            Text(
                stringResource(R.string.skirt_title),
                Modifier.padding(end = 16.dp).semantics {
                    heading()
                },
            )
        },
        actions = {
            TextButton(onClick = onManageProfiles, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.profile_manage))
            }
        },
    )
}

@Composable
private fun DiscardDialog(onDismiss: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.profile_discard_title)) },
        text = { Text(stringResource(R.string.profile_discard_body)) },
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
        calculator.selectProfile(profileWaistText(measurements.waist), measurements.name.text)
        onDismiss()
    }, onCreate = onCreate)
}

@Composable
private fun ProfileSaveFeedback(
    profiles: ProfilesViewModel,
    snackbar: SnackbarHostState,
    onReturnToPicker: () -> Unit,
) {
    val state by profiles.state.collectAsStateWithLifecycle()
    val message = state.savedMessage?.let { stringResource(it) }
    LaunchedEffect(message) {
        message?.let {
            if (!state.isEditorOpen) onReturnToPicker()
            snackbar.showSnackbar(it)
            profiles.dismissMessage()
        }
    }
}

private fun requestProfileEditor(
    state: ProfilesState,
    profiles: ProfilesViewModel,
    id: ProfileId?,
    onDiscard: () -> Unit,
) {
    if (state.isSaving || state.isLoading || state.loadError != null) return
    if (state.isEditorOpen && state.isDirty) {
        onDiscard()
    } else if (id == null) {
        profiles.startCreating()
    } else {
        profiles.startEditing(id)
    }
}
