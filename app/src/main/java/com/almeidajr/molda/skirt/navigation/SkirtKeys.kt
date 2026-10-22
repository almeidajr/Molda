package com.almeidajr.molda.skirt.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal data object SkirtCalculatorKey : NavKey

/** The fabric-folding guide on the calculated radii, opened from a result. */
@Serializable
internal data object FabricFoldingGuideKey : NavKey
