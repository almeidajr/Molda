package com.almeidajr.molda.guide.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The Guias root: the library of guides. */
@Serializable
internal data object GuideLibraryKey : NavKey

/** The fabric-folding guide on example dimensions, opened from Guias. */
@Serializable
internal data object ExampleFabricFoldingGuideKey : NavKey
