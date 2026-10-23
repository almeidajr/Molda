package com.almeidajr.molda.skirt.ui

import android.animation.ValueAnimator
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.skirt.domain.SkirtCalculation
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.skirt.domain.calculateSkirtRadii
import com.almeidajr.molda.ui.navigation.rememberTitleFocusRequester
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

// Leaves most of a short window to the instructions while the drawing stays pinned.
private const val DRAWING_HEIGHT_FRACTION = 0.4f

// Below this height (landscape phones) the drawing scrolls with the text instead of being pinned.
private val PinnedDrawingMinHeight = 560.dp

// Above this text scale the step position moves above the buttons so nothing truncates.
private const val STACKED_STEP_BAR_FONT_SCALE = 1.3f

/** The example draft of a guide opened from Guias, as its label lists it. */
private val ExampleSkirtDraft = SkirtDraft(
    waist = Length(80.0),
    ease = Length(2.0),
    finishedLength = Length(60.0),
    waistSeamAllowance = Length(1.0),
    hemAllowance = Length(2.0),
)

/** An example measurement as the example label lists it: "80", or "1,50" when not whole. */
internal fun exampleCentimetres(length: Length): String =
    formatDecimal(length.centimetres).removeSuffix(",00")

/** The radii a guide opened from Guias shows. */
internal val ExampleSkirtRadii: SkirtRadii =
    when (val result = calculateSkirtRadii(ExampleSkirtDraft)) {
        is SkirtCalculation.Success -> result.radii
        else -> error("The example skirt draft must calculate: $result")
    }

/** The tonal entry to the guide player; it takes focus back when the player closes. */
@Composable
internal fun FabricFoldingGuideEntry(onOpen: () -> Unit, modifier: Modifier = Modifier) {
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
    ) { Text(stringResource(R.string.guide_open)) }
}

/**
 * The full-screen fabric-folding guide: a pinned drawing, scrolling instructions and a
 * fixed step bar. Concluir calls [onClose], and so does the close button when [isCloseShown];
 * system Back is the host's, so it can preview the screen beneath. The player never changes the
 * calculation. With [onCalculateOwn], the [radii] are
 * an example: a pinned label says so and offers to calculate the user's own skirt.
 */
@Composable
internal fun FabricFoldingPlayer(
    radii: SkirtRadii,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isCloseShown: Boolean = true,
    onCalculateOwn: (() -> Unit)? = null,
) {
    var progress by rememberSaveable(stateSaver = GuideProgressSaver) {
        mutableStateOf(GuideProgress())
    }
    val motionEnabled = rememberSystemMotionEnabled { progress = progress.motionChanged() }
    val file = rememberGuideFile()
    Surface(modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            val drawingSize =
                min(min(maxWidth, 640.dp) - 32.dp, maxHeight * DRAWING_HEIGHT_FRACTION)
            val isDrawingPinned = maxHeight >= PinnedDrawingMinHeight
            Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                PlayerTopBar(stringResource(progress.step.title), onClose.takeIf { isCloseShown })
                if (onCalculateOwn != null) ExampleLabel(onCalculateOwn)
                key(progress.step) {
                    val animation: @Composable () -> Unit = {
                        GuideAnimation(file, radii, progress, motionEnabled, drawingSize) {
                            progress = progress.togglePause()
                        }
                    }
                    if (isDrawingPinned) animation()
                    Column(
                        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (!isDrawingPinned) animation()
                        if (!motionEnabled) {
                            Text(stringResource(R.string.guide_motion_off))
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
private fun PlayerTopBar(title: String, onClose: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .padding(start = if (onClose == null) 16.dp else 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (onClose != null) {
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.guide_close),
                )
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
                .focusRequester(rememberTitleFocusRequester()).focusable().semantics { heading() },
        )
    }
}

/** Says the guide shows example dimensions, which ones, and how to calculate one's own. */
@Composable
private fun ExampleLabel(onCalculateOwn: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(painterResource(R.drawable.ic_info), contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.guide_example_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    stringResource(
                        R.string.guide_example_inputs,
                        exampleCentimetres(ExampleSkirtDraft.waist),
                        exampleCentimetres(ExampleSkirtDraft.ease),
                        exampleCentimetres(ExampleSkirtDraft.finishedLength),
                        exampleCentimetres(ExampleSkirtDraft.waistSeamAllowance),
                        exampleCentimetres(ExampleSkirtDraft.hemAllowance),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Text buttons pad their label by 12dp: pull it back to the text's start edge.
                TextButton(
                    onClick = onCalculateOwn,
                    modifier = Modifier.heightIn(min = 48.dp).offset(x = (-12).dp),
                ) { Text(stringResource(R.string.guide_calculate_own)) }
            }
        }
    }
}

@Composable
private fun StepBar(progress: GuideProgress, onPrevious: () -> Unit, onForward: () -> Unit) {
    val isStacked = LocalDensity.current.fontScale > STACKED_STEP_BAR_FONT_SCALE
    val position: @Composable (Modifier) -> Unit = {
        Text(
            stringResource(
                R.string.guide_position,
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
                ) { Text(stringResource(R.string.guide_previous)) }
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

/**
 * Follows the system animator scale, including changes made while the player is open, calling
 * [onChange] on the main thread each time motion is switched on or off. Every switch counts, even
 * an off and on quicker than a frame, which a recomposition would only see as unchanged.
 */
@Composable
private fun rememberSystemMotionEnabled(onChange: () -> Unit): Boolean {
    var motionEnabled by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    val currentOnChange by rememberUpdatedState(onChange)
    LaunchedEffect(Unit) {
        // The listener runs on a binder thread; collecting moves each change to the main thread.
        callbackFlow {
            val listener = ValueAnimator.DurationScaleChangeListener { trySend(it > 0f) }
            ValueAnimator.registerDurationScaleChangeListener(listener)
            // Catches a change made before the listener was registered.
            trySend(ValueAnimator.areAnimatorsEnabled())
            awaitClose { ValueAnimator.unregisterDurationScaleChangeListener(listener) }
        }.collect { enabled ->
            if (enabled != motionEnabled) {
                motionEnabled = enabled
                currentOnChange()
            }
        }
    }
    return motionEnabled
}
