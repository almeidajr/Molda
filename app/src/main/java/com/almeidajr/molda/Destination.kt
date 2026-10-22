package com.almeidajr.molda

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.guide.navigation.GuideLibraryKey
import com.almeidajr.molda.pattern.navigation.PatternCatalogKey
import com.almeidajr.molda.profile.navigation.ProfileManagementKey
import com.almeidajr.molda.project.navigation.ProjectsKey

/** The top-level destinations, in navigation order; the first is the start destination. */
internal enum class Destination(
    val root: NavKey,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
) {
    Patterns(
        PatternCatalogKey,
        R.string.navigation_patterns,
        R.drawable.ic_design_services,
        R.drawable.ic_design_services_filled,
    ),
    Projects(
        ProjectsKey,
        R.string.navigation_projects,
        R.drawable.ic_folder,
        R.drawable.ic_folder_filled,
    ),
    Guides(
        GuideLibraryKey,
        R.string.navigation_guides,
        R.drawable.ic_menu_book,
        R.drawable.ic_menu_book_filled,
    ),
    Profiles(
        ProfileManagementKey,
        R.string.navigation_profiles,
        R.drawable.ic_group,
        R.drawable.ic_group_filled,
    ),
}

internal val TopLevelRoots: List<NavKey> = Destination.entries.map { it.root }
