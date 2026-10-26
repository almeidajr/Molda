package com.almeidajr.molda.update.navigation

import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.update.domain.Update
import kotlinx.serialization.Serializable

/**
 * The result key on which a request to install a downloaded update, the [Update] itself, reaches
 * the app's wiring, which asks first when an editor has unsaved changes.
 */
internal const val INSTALL_UPDATE_RESULT_KEY = "install-update"

/**
 * Installs [update], asking for the "install unknown apps" permission when Molda lacks it; on the
 * back stack while it waits, so the wait survives process death.
 */
@Serializable
internal data class InstallUpdateKey(val update: Update) : NavKey
