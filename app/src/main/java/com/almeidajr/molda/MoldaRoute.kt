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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.almeidajr.molda.profile.data.LocalProfileRepository
import com.almeidajr.molda.profile.ui.ProfilePicker
import com.almeidajr.molda.profile.ui.ProfilesContent
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.profile.ui.profileWaistText
import com.almeidajr.molda.skirt.ui.SkirtCalculatorScreen
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel

// Let the IME consume Back before leaving profile management.
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MoldaRoute(
    modifier: Modifier = Modifier,
    calculator: SkirtCalculatorViewModel = viewModel(),
    profiles: ProfilesViewModel = profileViewModel(),
) {
    val calculatorState by calculator.state.collectAsStateWithLifecycle()
    val profileState by profiles.state.collectAsStateWithLifecycle()
    var showManagement by rememberSaveable { mutableStateOf(false) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showManagement && !showPicker && !WindowInsets.isImeVisible) {
        showManagement =
            false
    }
    if (showManagement) {
        Column(
            modifier.fillMaxSize().imePadding().padding(16.dp),
        ) {
            TextButton(onClick = { showManagement = false }) { Text("Voltar ao cálculo") }
            ProfilesContent(
                profileState,
                profiles::startCreating,
                profiles::edit,
                profiles::save,
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
            onUseProfile = { showPicker = true },
        ) {
            TextButton(onClick = { showManagement = true }) { Text("Perfis de medidas") }
        }
    }
    if (showPicker) {
        ProfilePicker(
            profileState,
            onDismiss = { showPicker = false },
            onRetry = profiles::load,
            onSelect = {
                calculator.selectProfile(
                    profileWaistText(it.measurements.waist),
                    it.measurements.name,
                )
                showPicker = false
            },
            onCreate = {
                showPicker = false
                showManagement = true
                profiles.startCreating()
            },
        )
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
