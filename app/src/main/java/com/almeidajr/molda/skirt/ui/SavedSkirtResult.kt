package com.almeidajr.molda.skirt.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii

/** A saved skirt result, read-only: the stored [draft] and [radii], never recalculated. */
@Composable
fun SavedSkirtResult(
    draft: SkirtDraft,
    radii: SkirtRadii,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SkirtResultContent(
        skirtResult(draft, radii, LocalResources.current::getString),
        onOpenGuide,
        modifier,
    )
}
