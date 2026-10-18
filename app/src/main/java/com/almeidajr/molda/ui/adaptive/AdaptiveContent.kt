package com.almeidajr.molda.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay

/** The narrowest readable pane at a font scale of 1; it grows with larger text. */
private val ReadablePaneMinWidth = 360.dp

/** The gap between two panes. */
private val PaneGap = 24.dp

private fun readablePaneMinWidth(fontScale: Float): Dp =
    ReadablePaneMinWidth * fontScale.coerceAtLeast(1f)

/**
 * A list-detail strategy showing two readable panes beside each other, avoiding any hinge, or
 * null when they do not fit in [maxWidth] and the window shows one pane at a time.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun <T : Any> rememberReadableListDetailStrategy(
    maxWidth: Dp,
): ListDetailSceneStrategy<T>? {
    val paneWidth = readablePaneMinWidth(LocalDensity.current.fontScale)
    if (maxWidth < paneWidth * 2 + PaneGap) return null
    val base =
        calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2(), HingePolicy.AlwaysAvoid)
    return rememberListDetailSceneStrategy(
        directive = base.copy(
            maxHorizontalPartitions = 2,
            maxVerticalPartitions = 1,
            horizontalPartitionSpacerSize = PaneGap,
            defaultPanePreferredWidth = (maxWidth - PaneGap) / 2,
        ),
        backNavigationBehavior = BackNavigationBehavior.PopLatest,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun AdaptiveContent(
    modifier: Modifier = Modifier,
    detailOpen: Boolean = true,
    onBack: () -> Unit = {},
    primary: @Composable () -> Unit,
    detail: @Composable () -> Unit,
    compact: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val listDetail = rememberReadableListDetailStrategy<String>(maxWidth)
        if (listDetail != null) {
            NavDisplay(
                backStack = if (detailOpen) listOf("list", "detail") else listOf("list"),
                onBack = onBack,
                sceneStrategies = listOf(listDetail),
                entryProvider = { key ->
                    NavEntry(
                        key,
                        metadata = if (key == "list") {
                            ListDetailSceneStrategy.listPane(detailPlaceholder = {
                                ReadablePane(detail)
                            })
                        } else {
                            ListDetailSceneStrategy.detailPane()
                        },
                    ) { ReadablePane(if (key == "list") primary else detail) }
                },
            )
        } else {
            ReadablePane(compact)
        }
    }
}

@Composable
internal fun ReadablePane(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 640.dp).fillMaxWidth()) { content() }
    }
}

/**
 * Places [content] on one side of any hinge that would split it. Material scenes exclude
 * vertical hinges; compact and tabletop layouts need a safe region too. One column (narrow
 * content, or [isSingleColumn] content such as the tutorial player) moves off a half-open or
 * separating vertical fold but spans a flat one; two panes move only when they cannot sit
 * beside the fold.
 */
@Composable
internal fun HingeSafeContent(
    modifier: Modifier,
    isSingleColumn: Boolean = false,
    content: @Composable () -> Unit,
) {
    val hinges = currentWindowAdaptiveInfoV2().windowPosture.hingeList
    val density = LocalDensity.current
    val minimumWidth = with(density) { readablePaneMinWidth(fontScale).toPx() }
    val spacerWidth = with(density) { PaneGap.toPx() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    Layout(
        content = content,
        modifier = modifier.onGloballyPositioned { origin = it.positionInWindow() },
    ) { children, constraints ->
        var area = Rect(0f, 0f, constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        hinges.forEach { hinge ->
            val bounds = hinge.bounds.translate(-origin)
            if (hinge.isVertical && bounds.left > area.left && bounds.right < area.right) {
                val left = bounds.left - area.left
                val right = area.right - bounds.right
                val isTwoPane = !isSingleColumn && area.width >= minimumWidth * 2 + spacerWidth
                if (hinge.displacesContent(
                        isTwoPane,
                        minOf(left, right),
                        minimumWidth + spacerWidth,
                    )
                ) {
                    area = if (left >= right) {
                        area.copy(right = bounds.left)
                    } else {
                        area.copy(left = bounds.right)
                    }
                }
            } else if (!hinge.isVertical && bounds.top > area.top && bounds.bottom < area.bottom) {
                area = if (bounds.top - area.top >= area.bottom - bounds.bottom) {
                    area.copy(bottom = bounds.top)
                } else {
                    area.copy(top = bounds.bottom)
                }
            }
        }
        val placeable = children.single().measure(
            Constraints.fixed(
                area.width.toInt().coerceAtLeast(0),
                area.height.toInt().coerceAtLeast(0),
            ),
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(area.left.toInt(), area.top.toInt())
        }
    }
}

/** A half-open (book posture) or physically separating fold splits one column; a flat one does not. */
private val HingeInfo.splitsSingleColumn: Boolean get() = isSeparating || !isFlat

/**
 * Whether content must move off this vertical hinge: two panes when the [narrowerSide] cannot
 * hold a pane plus its gap ([paneWidth]), one column when the fold would split it.
 */
private fun HingeInfo.displacesContent(
    isTwoPane: Boolean,
    narrowerSide: Float,
    paneWidth: Float,
): Boolean = if (isTwoPane) narrowerSide < paneWidth else splitsSingleColumn
