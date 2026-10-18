package com.almeidajr.molda

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.almeidajr.molda.profile.data.LocalProfileRepository
import com.almeidajr.molda.profile.ui.ProfileCreateAction
import com.almeidajr.molda.profile.ui.ProfileEditor
import com.almeidajr.molda.profile.ui.ProfilePicker
import com.almeidajr.molda.profile.ui.ProfilesContent
import com.almeidajr.molda.profile.ui.ProfilesState
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.profile.ui.profileWaistText
import com.almeidajr.molda.skirt.ui.FabricFoldingPlayer
import com.almeidajr.molda.skirt.ui.SkirtCalculatorScreen
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel
import com.almeidajr.molda.ui.adaptive.AdaptiveContent
import com.almeidajr.molda.ui.adaptive.HingeSafeContent
import com.almeidajr.molda.ui.adaptive.minimumTwoPaneWidth

// Keep saveable destinations and their transitions together.
@Suppress("LongMethod")
@Composable
fun MoldaRoute(
    modifier: Modifier = Modifier,
    calculator: SkirtCalculatorViewModel = viewModel(),
    profiles: ProfilesViewModel = profileViewModel(),
) {
    val profileState by profiles.state.collectAsStateWithLifecycle()
    val calculatorState by calculator.state.collectAsStateWithLifecycle()
    var showTutorial by rememberSaveable { mutableStateOf(false) }
    // Keeps the calculator's scroll and disclosures while the tutorial player replaces it.
    val destinations = rememberSaveableStateHolder()
    var showManagement by rememberSaveable { mutableStateOf(false) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var editorFromPicker by rememberSaveable { mutableStateOf(false) }
    var showDiscard by rememberSaveable { mutableStateOf(false) }
    var pendingEditor by rememberSaveable { mutableStateOf<Long?>(null) }
    val requestEditor: (Long) -> Unit = { id ->
        requestProfileEditor(profileState, profiles, id) {
            pendingEditor = id
            showDiscard = true
        }
    }
    val snackbar = remember { SnackbarHostState() }
    var snackbarHeight by remember { mutableIntStateOf(0) }
    val snackbarInset = with(LocalDensity.current) { snackbarHeight.toDp() }
    val finishEditor = {
        profiles.cancel()
        if (editorFromPicker) {
            showManagement = false
            showPicker = true
            editorFromPicker = false
        }
    }
    val cancelEditor = {
        if (profileState.isDirty) showDiscard = true else finishEditor()
    }
    ProfileSaveFeedback(profiles, snackbar, editorFromPicker) {
        showManagement = false
        showPicker = true
        editorFromPicker = false
    }
    ProfileBackHandler(showManagement, showPicker, profileState, cancelEditor) {
        showManagement =
            false
    }
    val tutorialRadii = calculatorState.radii?.takeIf { showTutorial }
    // The player is one column even on a wide window: keep it off a book-posture fold only.
    HingeSafeContent(modifier.fillMaxSize(), isSingleColumn = tutorialRadii != null) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= minimumTwoPaneWidth(LocalDensity.current.fontScale)
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = {
                    SnackbarHost(snackbar, Modifier.onSizeChanged { snackbarHeight = it.height })
                },
                floatingActionButtonPosition = if (wide) FabPosition.Start else FabPosition.End,
                floatingActionButton = {
                    if (showManagement && (!profileState.isCreating || wide)) {
                        ProfileCreateAction(profileState, { requestEditor(0L) })
                    }
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { padding ->
                // Keep bottom actions reachable while storage feedback is visible.
                val contentModifier = Modifier.padding(padding).padding(bottom = snackbarInset)
                if (tutorialRadii != null) {
                    FabricFoldingPlayer(tutorialRadii, { showTutorial = false }, contentModifier)
                } else {
                    destinations.SaveableStateProvider("destination") {
                        Destination(
                            calculator,
                            profiles,
                            showManagement,
                            { showManagement = it },
                            { showPicker = true },
                            { showTutorial = true },
                            cancelEditor,
                            requestEditor,
                            contentModifier,
                        )
                    }
                }
                if (showPicker) {
                    AppProfilePicker(
                        profiles,
                        calculator,
                        onDismiss = { showPicker = false },
                        onCreate = {
                            showPicker = false
                            showManagement = true
                            editorFromPicker = true
                            profiles.startCreating()
                        },
                    )
                }
            }
        }
    }
    if (showDiscard) {
        DiscardDialog(onDismiss = {
            showDiscard = false
            pendingEditor = null
        }, onDiscard = {
            showDiscard = false
            if (pendingEditor == null) finishEditor() else profiles.cancel()
            pendingEditor?.let { id ->
                pendingEditor = null
                if (id == 0L) profiles.startCreating() else profiles.startEditing(id)
            }
        })
    }
}

@Composable
private fun profileViewModel(): ProfilesViewModel {
    val context = LocalContext.current.applicationContext
    return viewModel(
        factory = viewModelFactory {
            initializer {
                ProfilesViewModel(LocalProfileRepository(context), createSavedStateHandle())
            }
        },
    )
}

@Composable
private fun Destination(
    calculator: SkirtCalculatorViewModel,
    profiles: ProfilesViewModel,
    showManagement: Boolean,
    onManage: (Boolean) -> Unit,
    onUseProfile: () -> Unit,
    onOpenTutorial: () -> Unit,
    cancelEditor: () -> Unit,
    requestEditor: (Long) -> Unit,
    modifier: Modifier,
) {
    val calculatorState by calculator.state.collectAsStateWithLifecycle()
    val profileState by profiles.state.collectAsStateWithLifecycle()
    if (showManagement) {
        val editor: @Composable () -> Unit = {
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
        val list: @Composable () -> Unit = {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                if (!profileState.isCreating) {
                    TextButton(onClick = {
                        onManage(false)
                    }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Voltar ao cálculo")
                    }
                }
                ProfilesContent(profileState, profiles::load, requestEditor, profiles::delete)
            }
        }
        AdaptiveContent(
            modifier = modifier.imePadding(),
            detailOpen = profileState.isCreating,
            onBack = { if (!profileState.isSaving) cancelEditor() },
            primary = list,
            detail = {
                if (profileState.isCreating) {
                    editor()
                } else {
                    Text("Selecione um perfil ou toque em Novo perfil.", Modifier.padding(16.dp))
                }
            },
            compact = { if (profileState.isCreating) editor() else list() },
        )
    } else {
        Column(modifier) {
            CalculatorTopBar(onManageProfiles = { onManage(true) })
            SkirtCalculatorScreen(
                calculatorState,
                calculator::edit,
                calculator::calculate,
                Modifier.weight(1f),
                onBlur = calculator::blur,
                onUseProfile = onUseProfile,
                onOpenTutorial = onOpenTutorial,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculatorTopBar(onManageProfiles: () -> Unit) {
    TopAppBar(
        title = {
            // TopAppBar measures the title as if its 16dp start inset were free, so a title that
            // wraps at large text would run under the action without this end padding.
            Text("Saia godê inteira", Modifier.padding(end = 16.dp).semantics { heading() })
        },
        actions = {
            TextButton(onClick = onManageProfiles, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Perfis de medidas")
            }
        },
    )
}

@Composable
private fun DiscardDialog(onDismiss: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Descartar alterações?") },
        text = { Text("As alterações deste perfil ainda não foram salvas.") },
        confirmButton = {
            TextButton(onClick = {
                onDiscard()
            }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Descartar") }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
            }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Continuar editando") }
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
        calculator.selectProfile(profileWaistText(it.measurements.waist), it.measurements.name)
        onDismiss()
    }, onCreate = onCreate)
}

@Composable
private fun ProfileSaveFeedback(
    profiles: ProfilesViewModel,
    snackbar: SnackbarHostState,
    editorFromPicker: Boolean,
    onReturnToPicker: () -> Unit,
) {
    val state by profiles.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedMessage) {
        state.savedMessage?.let {
            if (!state.isCreating && editorFromPicker) onReturnToPicker()
            snackbar.showSnackbar(it)
            profiles.dismissMessage()
        }
    }
}

// Let the IME consume Back before leaving profile management.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileBackHandler(
    showManagement: Boolean,
    showPicker: Boolean,
    state: ProfilesState,
    onCancel: () -> Unit,
    onLeave: () -> Unit,
) {
    BackHandler(enabled = showManagement && !showPicker && !WindowInsets.isImeVisible) {
        if (state.isCreating) {
            if (!state.isSaving) onCancel()
        } else {
            onLeave()
        }
    }
}

private fun requestProfileEditor(
    state: ProfilesState,
    profiles: ProfilesViewModel,
    id: Long,
    onDiscard: () -> Unit,
) {
    if (state.isSaving || state.isLoading || state.loadError != null) return
    if (state.isCreating && state.isDirty) {
        onDiscard()
    } else if (id == 0L) {
        profiles.startCreating()
    } else {
        profiles.startEditing(id)
    }
}
