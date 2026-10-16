package com.almeidajr.molda

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
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

internal fun minimumTwoPaneWidth(fontScale: Float): Dp =
    360.dp * fontScale.coerceAtLeast(1f) * 2 + 24.dp

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
    val info = currentWindowAdaptiveInfoV2()
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxSize()) {
        val base = calculatePaneScaffoldDirective(info, HingePolicy.AlwaysAvoid)
        val wide = maxWidth >= minimumTwoPaneWidth(fontScale)
        val directive = base.copy(
            maxHorizontalPartitions = if (wide) 2 else 1,
            maxVerticalPartitions = 1,
            horizontalPartitionSpacerSize = if (wide) 24.dp else 0.dp,
            defaultPanePreferredWidth = (maxWidth - 24.dp) / 2,
        )
        if (wide) {
            val strategy = rememberListDetailSceneStrategy<String>(
                directive = directive,
                backNavigationBehavior = BackNavigationBehavior.PopLatest,
            )
            NavDisplay(
                backStack = if (detailOpen) listOf("list", "detail") else listOf("list"),
                onBack = onBack,
                sceneStrategies = listOf(strategy),
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
private fun ReadablePane(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 640.dp).fillMaxWidth()) { content() }
    }
}

@Composable
// Material scenes exclude vertical hinges; compact and tabletop layouts need a safe region too.
internal fun HingeSafeContent(modifier: Modifier, content: @Composable () -> Unit) {
    val hinges = currentWindowAdaptiveInfoV2().windowPosture.hingeList
    val density = LocalDensity.current
    val minimumWidth = with(density) { (360.dp * fontScale.coerceAtLeast(1f)).toPx() }
    val spacerWidth = with(density) { 24.dp.toPx() }
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
                if (minOf(left, right) < minimumWidth + spacerWidth ||
                    area.width < minimumWidth * 2 + spacerWidth
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
