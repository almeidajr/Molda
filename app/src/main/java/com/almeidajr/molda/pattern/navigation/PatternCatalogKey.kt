package com.almeidajr.molda.pattern.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The Moldes root: the catalog of pattern drafts. */
@Serializable
internal data object PatternCatalogKey : NavKey
