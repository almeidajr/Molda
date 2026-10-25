package com.almeidajr.molda.update.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.update.domain.Update
import com.almeidajr.molda.update.domain.UpdateInstall
import com.almeidajr.molda.update.domain.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** An install of [update] waiting on the "install unknown apps" permission. */
internal sealed interface InstallPermission {
    val update: Update

    /** Molda's setting should open, so the person can allow the install. */
    data class Needed(override val update: Update) : InstallPermission

    /** Molda's setting is open; the install continues when the person returns. */
    data class Asked(override val update: Update) : InstallPermission

    /** The person returned without allowing it: explained, with a retry. */
    data class Refused(override val update: Update) : InstallPermission
}

/**
 * The app's one way to install a downloaded update, owned by the activity so that every place
 * offering the install (About, the startup notice) goes through the same checks first, and so
 * that the wait on the permission survives the trip to Settings.
 */
@HiltViewModel
internal class UpdateInstallViewModel @Inject constructor(
    private val repository: UpdateRepository,
) : ViewModel() {
    private val mutablePermission = MutableStateFlow<InstallPermission?>(null)

    /** The install waiting on the permission; null when none is. */
    val permission: StateFlow<InstallPermission?> = mutablePermission.asStateFlow()

    /** Installs [update], which the system installer asks the person to confirm. */
    fun install(update: Update) = install(update, whenNotAllowed = InstallPermission::Needed)

    /** Molda's setting opened for [InstallPermission.Needed]. */
    fun onSettingOpened() {
        val needed = mutablePermission.value as? InstallPermission.Needed ?: return
        mutablePermission.value = InstallPermission.Asked(needed.update)
    }

    /** The person returned from Molda's setting: continues the install, or explains once. */
    fun onSettingClosed() {
        val asked = mutablePermission.value as? InstallPermission.Asked ?: return
        install(asked.update, whenNotAllowed = InstallPermission::Refused)
    }

    /** Dismisses the explanation without installing. */
    fun dismiss() {
        mutablePermission.value = null
    }

    private fun install(update: Update, whenNotAllowed: (Update) -> InstallPermission) {
        viewModelScope.launch {
            mutablePermission.value = when (repository.install(update)) {
                UpdateInstall.Started -> null
                UpdateInstall.NotAllowed -> whenNotAllowed(update)
            }
        }
    }
}
