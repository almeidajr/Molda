package com.almeidajr.molda.pattern.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.about.navigation.AboutKey
import com.almeidajr.molda.pattern.navigation.PatternCatalogKey
import com.almeidajr.molda.skirt.navigation.SkirtCalculatorKey
import com.almeidajr.molda.ui.navigation.Navigator

/** The catalog of pattern drafts, each opening its calculator. */
internal fun EntryProviderScope<NavKey>.patternSection(navigator: Navigator) {
    entry<PatternCatalogKey> {
        PatternCatalog(
            onOpenSkirt = { navigator.push(SkirtCalculatorKey) },
            onAbout = { navigator.push(AboutKey) },
        )
    }
}
