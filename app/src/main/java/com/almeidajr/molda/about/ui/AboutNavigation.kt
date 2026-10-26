package com.almeidajr.molda.about.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.about.navigation.AboutKey
import com.almeidajr.molda.about.navigation.LicensesKey
import com.almeidajr.molda.ui.navigation.Navigator

/**
 * About Molda, showing [versionName] and the app's [updateStatus], which composes within the About
 * entry; and its licenses.
 */
internal fun EntryProviderScope<NavKey>.aboutSection(
    navigator: Navigator,
    versionName: String,
    updateStatus: @Composable () -> Unit,
) {
    entry<AboutKey> {
        AboutScreen(
            versionName = versionName,
            onUp = navigator::back,
            onOpenLicenses = { navigator.push(LicensesKey) },
            content = updateStatus,
        )
    }
    entry<LicensesKey> { LicensesScreen(onUp = navigator::back) }
}
