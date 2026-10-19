package com.almeidajr.molda.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.R

/** A motif's dash pattern, given in its 48-unit space, scaled by [unit]; solid when null. */
fun motifDashEffect(dashes: FloatArray?, unit: Float): PathEffect? =
    dashes?.let { PathEffect.dashPathEffect(it.map { dash -> dash * unit }.toFloatArray()) }

/** Whether a catalog entry shows selection: only in a list beside its detail. */
enum class CatalogSelection {
    /** The list stands alone; opening an entry replaces it. */
    None,

    /** The list sits beside a detail showing another entry, or none yet. */
    Unselected,

    /** The list sits beside this entry's detail. */
    Selected,
}

/**
 * A catalog root: a top app bar with [title] and the About action, then an [intro] and the
 * [entries] in a grid of readable columns.
 */
@Composable
fun CatalogScreen(
    title: String,
    intro: String,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
    entries: LazyGridScope.() -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        MoldaTopAppBar(title, onAbout = onAbout)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(320.dp),
                modifier = Modifier.widthIn(max = 1040.dp).fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        intro,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                entries()
            }
        }
    }
}

/**
 * One whole-row entry of the Moldes catalog or the Guias library: a decorative thumbnail drawn by
 * [content], the [name], a one-sentence [description] and a decorative chevron, announced as one
 * button. Beside its detail ([selection] other than [CatalogSelection.None]) the selected entry
 * fills with the secondary container, drops the chevron and is exposed as selected.
 */
@Composable
fun CatalogEntry(
    name: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selection: CatalogSelection = CatalogSelection.None,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val isSelected = selection == CatalogSelection.Selected
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 88.dp).semantics {
            role = Role.Button
            if (selection != CatalogSelection.None) selected = isSelected
        },
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) colors.secondaryContainer else colors.surfaceContainerLow,
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumbnailShape = MaterialTheme.shapes.medium
            Box(
                Modifier.size(56.dp).background(colors.secondaryContainer, thumbnailShape),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides colors.onSecondaryContainer) {
                    content()
                }
            }
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            if (!isSelected) {
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                )
            }
        }
    }
}
