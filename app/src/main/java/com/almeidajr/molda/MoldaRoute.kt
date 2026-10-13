package com.almeidajr.molda

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.profile.ui.profileWaistText
import com.almeidajr.molda.skirt.ui.SkirtCalculatorScreen
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel

// Let the IME consume Back before leaving profile management.
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
// Keep saveable destinations and their transitions together.
@Suppress("LongMethod")
@Composable
fun MoldaRoute(
    modifier: Modifier = Modifier,
    calculator: SkirtCalculatorViewModel = viewModel(),
    profiles: ProfilesViewModel = profileViewModel(),
) {
    val profileState by profiles.state.collectAsStateWithLifecycle()
    var showManagement by rememberSaveable { mutableStateOf(false) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var editorFromPicker by rememberSaveable { mutableStateOf(false) }
    var showDiscard by rememberSaveable { mutableStateOf(false) }
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
    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(snackbar, Modifier.onSizeChanged { snackbarHeight = it.height })
        },
        floatingActionButton = {
            if (showManagement && !profileState.isCreating) {
                ProfileCreateAction(profileState, profiles::startCreating)
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        // Keep bottom actions reachable while storage feedback is visible.
        val contentModifier = Modifier.padding(padding).padding(bottom = snackbarInset)
        Destination(
            calculator,
            profiles,
            showManagement,
            { showManagement = it },
            { showPicker = true },
            cancelEditor,
            contentModifier,
        )
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
    if (showDiscard) {
        DiscardDialog(onDismiss = { showDiscard = false }, onDiscard = {
            showDiscard = false
            finishEditor()
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
    cancelEditor: () -> Unit,
    modifier: Modifier,
) {
    val calculatorState by calculator.state.collectAsStateWithLifecycle()
    val profileState by profiles.state.collectAsStateWithLifecycle()
    if (showManagement && profileState.isCreating) {
        ProfileEditor(
            profileState,
            profiles::edit,
            profiles::save,
            cancelEditor,
            modifier.imePadding(),
            profiles.today,
            onRetry = profiles::load,
        )
    } else if (showManagement) {
        Column(
            modifier.fillMaxSize().imePadding().padding(16.dp),
        ) {
            TextButton(onClick = { onManage(false) }) { Text("Voltar ao cálculo") }
            ProfilesContent(
                profileState,
                profiles::load,
                profiles::startEditing,
                profiles::delete,
            )
        }
    } else {
        SkirtCalculatorScreen(
            calculatorState,
            calculator::edit,
            calculator::calculate,
            modifier,
            onBlur = calculator::blur,
            onUseProfile = onUseProfile,
        ) {
            TextButton(onClick = { onManage(true) }) { Text("Perfis de medidas") }
        }
    }
}

@Composable
private fun DiscardDialog(onDismiss: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Descartar alterações?") },
        text = { Text("As alterações deste perfil ainda não foram salvas.") },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onDiscard()
            }) { Text("Descartar") }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) { Text("Continuar editando") }
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ProfileBackHandler(
    showManagement: Boolean,
    showPicker: Boolean,
    state: com.almeidajr.molda.profile.ui.ProfilesState,
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
