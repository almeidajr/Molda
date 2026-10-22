package com.almeidajr.molda.about.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.about.navigation.AboutKey
import com.almeidajr.molda.about.navigation.LicencesKey
import com.almeidajr.molda.ui.navigation.Navigator

/** About Molda, showing [versionName], and its licences. */
internal fun EntryProviderScope<NavKey>.aboutSection(navigator: Navigator, versionName: String) {
    entry<AboutKey> {
        AboutScreen(
            versionName = versionName,
            onUp = navigator::back,
            onOpenLicences = { navigator.push(LicencesKey) },
        )
    }
    entry<LicencesKey> { LicencesScreen(onUp = navigator::back) }
}
