package com.almeidajr.molda.about.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** About Molda, pushed on the stack of the destination whose root opened it. */
@Serializable
internal data object AboutKey : NavKey

/** The open-source licences, opened from About. */
@Serializable
internal data object LicencesKey : NavKey
