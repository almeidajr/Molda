package com.almeidajr.molda.guide.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.R
import com.almeidajr.molda.about.navigation.AboutKey
import com.almeidajr.molda.guide.navigation.ExampleFabricFoldingGuideKey
import com.almeidajr.molda.guide.navigation.GuideLibraryKey
import com.almeidajr.molda.ui.adaptive.LocalIsTwoPane
import com.almeidajr.molda.ui.adaptive.ReadablePane
import com.almeidajr.molda.ui.navigation.CatalogSelection
import com.almeidajr.molda.ui.navigation.Navigator

/** The library of guides; the guide it opens sits beside it on a wide window. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.guideSection(navigator: Navigator) {
    entry<GuideLibraryKey>(
        metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = {
            ReadablePane {
                Text(stringResource(R.string.guide_choose), Modifier.padding(16.dp))
            }
        }),
    ) {
        val guias = navigator.state.stackOf(GuideLibraryKey)
        GuideLibrary(
            fabricFoldingSelection = when {
                !LocalIsTwoPane.current -> CatalogSelection.None
                guias.last() == ExampleFabricFoldingGuideKey -> CatalogSelection.Selected
                else -> CatalogSelection.Unselected
            },
            onOpenFabricFolding = {
                if (guias.last() != ExampleFabricFoldingGuideKey) {
                    navigator.push(ExampleFabricFoldingGuideKey)
                }
            },
            onAbout = { navigator.push(AboutKey) },
        )
    }
}
