package com.almeidajr.molda.skirt.navigation

import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.skirt.domain.SkirtRadii
import kotlinx.serialization.Serializable

@Serializable
internal data object SkirtCalculatorKey : NavKey

/** The result key the Moldes calculator listens on for a profile from its picker. */
internal const val MOLDES_PROFILE_RESULT_KEY = "profile/moldes"

/** The fabric-folding guide on the calculated [radii], opened from a result. */
@Serializable
internal data class FabricFoldingGuideKey(val radii: SkirtRadii) : NavKey
