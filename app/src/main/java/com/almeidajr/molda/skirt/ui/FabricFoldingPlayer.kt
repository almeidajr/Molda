package com.almeidajr.molda.skirt.ui

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.almeidajr.molda.R
import com.almeidajr.molda.skirt.domain.SkirtRadii

// Not `const`: ktlint then demands SCREAMING_SNAKE names, which Detekt's naming rule rejects.
// Leaves most of a short window to the instructions while the drawing stays pinned.
@Suppress("MayBeConstant")
private val DrawingHeightFraction = 0.4f

// Below this height (landscape phones) the drawing scrolls with the text instead of being pinned.
private val PinnedDrawingMinHeight = 560.dp

// Above this text scale the step position moves above the buttons so nothing truncates.
@Suppress("MayBeConstant")
private val StackedStepBarFontScale = 1.3f

/** The tonal entry to the tutorial player; it takes focus back when the player closes. */
@Composable
internal fun FabricFoldingTutorialEntry(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    // Restored with the calculator when the player closes, so focus returns here.
    var hasOpened by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (hasOpened) {
            hasOpened = false
            focusRequester.requestFocus()
        }
    }
    FilledTonalButton(
        onClick = {
            hasOpened = true
            onOpen()
        },
        modifier = modifier.heightIn(min = 48.dp).focusRequester(focusRequester),
    ) { Text(stringResource(R.string.tutorial_open)) }
}

/**
 * The full-screen fabric-folding tutorial: a pinned drawing, scrolling instructions and a
 * fixed step bar. Back and Concluir call [onClose]; the player never changes the calculation.
 */
@Composable
internal fun FabricFoldingPlayer(
    radii: SkirtRadii,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var progress by rememberSaveable(stateSaver = TutorialProgressSaver) {
        mutableStateOf(TutorialProgress())
    }
    BackHandler(onBack = onClose)
    val motionEnabled = rememberSystemMotionEnabled()
    Surface(modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            val drawingSize = min(min(maxWidth, 640.dp) - 32.dp, maxHeight * DrawingHeightFraction)
            val isDrawingPinned = maxHeight >= PinnedDrawingMinHeight
            Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                PlayerTopBar(stringResource(progress.step.title), onClose)
                key(progress.step) {
                    val animation: @Composable () -> Unit = {
                        TutorialAnimation(radii, progress.step, motionEnabled, drawingSize)
                    }
                    if (isDrawingPinned) animation()
                    Column(
                        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (!isDrawingPinned) animation()
                        if (!motionEnabled) {
                            Text(stringResource(R.string.tutorial_motion_off))
                        }
                        StepInstructions(radii, progress.step)
                    }
                }
                StepBar(
                    progress,
                    onPrevious = { progress = progress.previous() },
                    onForward = { progress.next()?.let { progress = it } ?: onClose() },
                )
            }
        }
    }
}

@Composable
private fun PlayerTopBar(title: String, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
            Icon(
                painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.tutorial_close),
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).padding(end = 12.dp).semantics { heading() },
        )
    }
}

@Composable
private fun StepBar(progress: TutorialProgress, onPrevious: () -> Unit, onForward: () -> Unit) {
    val isStacked = LocalDensity.current.fontScale > StackedStepBarFontScale
    val position: @Composable (Modifier) -> Unit = {
        Text(
            stringResource(
                R.string.tutorial_position,
                progress.stepNumber,
                FoldingStep.entries.size,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = it,
        )
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (isStacked) position(Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onPrevious,
                    enabled = progress.canGoBack,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.tutorial_previous)) }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (!isStacked) position(Modifier)
                }
                Button(onClick = onForward, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(progress.forwardLabel))
                }
            }
        }
    }
}

/** Follows the system animator scale, including changes made while the player is open. */
@Composable
private fun rememberSystemMotionEnabled(): Boolean {
    var motionEnabled by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(Unit) {
        val listener = ValueAnimator.DurationScaleChangeListener { scale ->
            motionEnabled = scale > 0f
        }
        ValueAnimator.registerDurationScaleChangeListener(listener)
        motionEnabled = ValueAnimator.areAnimatorsEnabled()
        onDispose { ValueAnimator.unregisterDurationScaleChangeListener(listener) }
    }
    return motionEnabled
}
