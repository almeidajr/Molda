package com.almeidajr.molda.update.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.update.domain.Update
import com.almeidajr.molda.update.domain.UpdateInstall
import com.almeidajr.molda.update.domain.UpdateRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Where one install of a downloaded update stands. */
internal enum class UpdateInstallState {
    Installing,

    /** The system installer took the APK and asks the person to confirm: the entry closes. */
    Started,

    /** Molda may not install apps: Molda's setting should open. */
    NeedsPermission,

    /** Molda's setting is open; the install continues when the person returns. */
    WaitingForPermission,

    /** The person returned without allowing it: explained, with a retry. */
    Refused,
}

/**
 * Installs [update] as soon as its entry opens, waiting on the "install unknown apps" permission
 * when Molda lacks it. That the setting was opened is saved, so returning from it continues the
 * install even after process death, and explains instead of opening it again.
 */
@HiltViewModel(assistedFactory = UpdateInstallViewModel.Factory::class)
internal class UpdateInstallViewModel @AssistedInject constructor(
    private val repository: UpdateRepository,
    savedState: SavedStateHandle,
    @Assisted private val update: Update,
) : ViewModel() {
    private var hasOpenedSetting by savedState.saved { false }
    private val mutableState = MutableStateFlow(UpdateInstallState.Installing)
    val state: StateFlow<UpdateInstallState> = mutableState.asStateFlow()

    init {
        install(
            whenNotAllowed = if (hasOpenedSetting) {
                UpdateInstallState.Refused
            } else {
                UpdateInstallState.NeedsPermission
            },
        )
    }

    /** Molda's setting opened for [UpdateInstallState.NeedsPermission]. */
    fun onSettingOpened() {
        if (mutableState.value != UpdateInstallState.NeedsPermission) return
        hasOpenedSetting = true
        mutableState.value = UpdateInstallState.WaitingForPermission
    }

    /** The person returned from Molda's setting: continues the install, or explains once. */
    fun onSettingClosed() {
        if (mutableState.value != UpdateInstallState.WaitingForPermission) return
        install(whenNotAllowed = UpdateInstallState.Refused)
    }

    /** Tries again after the explanation, opening the setting again while still not allowed. */
    fun retry() {
        if (mutableState.value != UpdateInstallState.Refused) return
        install(whenNotAllowed = UpdateInstallState.NeedsPermission)
    }

    private fun install(whenNotAllowed: UpdateInstallState) {
        mutableState.value = UpdateInstallState.Installing
        viewModelScope.launch {
            mutableState.value = when (repository.install(update)) {
                UpdateInstall.Started -> UpdateInstallState.Started
                UpdateInstall.NotAllowed -> whenNotAllowed
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(update: Update): UpdateInstallViewModel
    }
}
