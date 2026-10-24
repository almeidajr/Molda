package com.almeidajr.molda.ui.navigation

/** Whether a catalog entry shows selection: only in a list beside its detail. */
enum class CatalogSelection {
    /** The list stands alone; opening an entry replaces it. */
    None,

    /** The list sits beside a detail showing another entry, or none yet. */
    Unselected,

    /** The list sits beside this entry's detail. */
    Selected,
}
