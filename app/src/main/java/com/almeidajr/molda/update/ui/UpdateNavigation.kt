package com.almeidajr.molda.update.ui

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.ui.navigation.Navigator
import com.almeidajr.molda.ui.navigation.WindowOverlaySceneStrategy.Companion.windowOverlay
import com.almeidajr.molda.update.navigation.InstallUpdateKey

/** The install of a downloaded update, which leaves the back stack once started or dismissed. */
internal fun EntryProviderScope<NavKey>.updateSection(navigator: Navigator) {
    entry<InstallUpdateKey>(metadata = windowOverlay()) { key ->
        val viewModel = hiltViewModel<UpdateInstallViewModel, UpdateInstallViewModel.Factory> {
            it.create(key.update)
        }
        UpdateInstall(viewModel, onClose = { navigator.remove(key) })
    }
}
