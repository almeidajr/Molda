package com.almeidajr.molda.pattern.ui

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.almeidajr.molda.R
import com.almeidajr.molda.ui.navigation.CatalogEntry
import com.almeidajr.molda.ui.navigation.CatalogScreen

/** The Moldes root: the catalog of pattern drafts, of which the full-circle skirt is the first. */
@Composable
fun PatternCatalog(onOpenSkirt: () -> Unit, onAbout: () -> Unit, modifier: Modifier = Modifier) {
    CatalogScreen(
        stringResource(R.string.navigation_patterns),
        stringResource(R.string.pattern_catalog_intro),
        onAbout,
        modifier,
    ) {
        item {
            CatalogEntry(
                stringResource(R.string.skirt_title),
                stringResource(R.string.pattern_catalog_skirt),
                onOpenSkirt,
            ) { Icon(painterResource(R.drawable.ic_skirt), contentDescription = null) }
        }
    }
}
