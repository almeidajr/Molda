package com.almeidajr.molda.guide.ui

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.almeidajr.molda.R
import com.almeidajr.molda.ui.navigation.CatalogEntry
import com.almeidajr.molda.ui.navigation.CatalogScreen
import com.almeidajr.molda.ui.navigation.CatalogSelection

/**
 * The Guias root: the library of guides, of which fabric folding is the first. Beside the guides,
 * [fabricFoldingSelection] marks whether its entry is the one shown.
 */
@Composable
fun GuideLibrary(
    fabricFoldingSelection: CatalogSelection,
    onOpenFabricFolding: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CatalogScreen(
        stringResource(R.string.navigation_guides),
        stringResource(R.string.guide_library_intro),
        onAbout,
        modifier,
    ) {
        item {
            CatalogEntry(
                stringResource(R.string.guide_open),
                stringResource(R.string.guide_fabric_folding_description),
                onOpenFabricFolding,
                selection = fabricFoldingSelection,
            ) { Icon(painterResource(R.drawable.ic_fabric_fold), contentDescription = null) }
        }
    }
}
