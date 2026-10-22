package com.almeidajr.molda.skirt.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import app.rive.Fit
import app.rive.RenderBackend
import app.rive.Result
import app.rive.Rive
import app.rive.RiveFile
import app.rive.RiveFileSource
import app.rive.RivePointerInputMode
import app.rive.rememberArtboardResult
import app.rive.rememberRiveFile
import app.rive.rememberRiveWorkerOrNull
import app.rive.rememberViewModelInstanceResult
import com.almeidajr.molda.R
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.skirt.domain.SkirtRadii

/**
 * The guide's Rive file, loaded once for the whole player so a step change only swaps the step's
 * artboard; `null` when no Rive worker can start.
 */
@Composable
internal fun rememberGuideFile(): Result<RiveFile>? {
    // Vulkan avoids blank frames on software GLES drivers; Rive falls back when unavailable.
    val worker = rememberRiveWorkerOrNull(renderBackend = RenderBackend.Vulkan)
    return worker?.let {
        rememberRiveFile(RiveFileSource.RawRes.from(R.raw.fabric_folding), it)
    }
}

@Composable
internal fun GuideAnimation(
    file: Result<RiveFile>?,
    radii: SkirtRadii,
    step: FoldingStep,
    motionEnabled: Boolean,
    drawingSize: Dp,
) {
    GuideFrame(file, step, motionEnabled, drawingSize) { loaded, play, paused, onLoaded ->
        GuideDrawing(loaded, radii, step, play, paused, onLoaded)
    }
}

/**
 * The drawing's frame, which owns pausing. Once [file] has loaded, [drawing] draws it, told
 * whether to play and whether it is paused, and calls back once it has drawn or failed. Until then
 * the frame stays empty at its final size, described for TalkBack; a long load shows a progress
 * indicator. With motion enabled, the drawn step plays at once and the whole frame toggles its
 * pause, with a badge showing what a tap does.
 */
@Composable
internal fun <T> GuideFrame(
    file: Result<T>?,
    step: FoldingStep,
    motionEnabled: Boolean,
    drawingSize: Dp,
    drawing: @Composable (
        file: T,
        play: Boolean,
        paused: Boolean,
        onLoaded: () -> Unit,
    ) -> Unit,
) {
    var paused by remember(motionEnabled) { mutableStateOf(false) }
    var isDrawingLoaded by remember { mutableStateOf(false) }
    val isToggle = motionEnabled && isDrawingLoaded && file is Result.Success
    val shape = MaterialTheme.shapes.large
    val playingState = stringResource(R.string.guide_playing)
    val pausedState = stringResource(R.string.guide_paused)
    val toggleLabel = stringResource(if (paused) R.string.guide_resume else R.string.guide_pause)
    // One modifier chain whether or not it toggles, so the drawing keeps its loaded state.
    val toggle = if (isToggle) {
        Modifier.clip(shape)
            .semantics {
                stateDescription = if (paused) pausedState else playingState
                onClick(label = toggleLabel) {
                    paused = !paused
                    true
                }
            }
            .toggleable(value = !paused, onValueChange = { paused = !it })
    } else {
        Modifier
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The artboards are transparent: this frame is the page behind the drawing.
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.width(drawingSize).heightIn(min = drawingSize).then(toggle),
        ) {
            Box {
                Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                    when (file) {
                        null -> Text(stringResource(R.string.guide_animation_unavailable))

                        is Result.Error -> Text(stringResource(R.string.guide_animation_error))

                        else -> {
                            val description = drawingDescription(step)
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(1f).semantics {
                                    contentDescription = description
                                },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (file is Result.Success) {
                                    drawing(file.value, motionEnabled, paused) {
                                        isDrawingLoaded = true
                                    }
                                }
                                GuideLoadingIndicator(!isDrawingLoaded, Modifier.matchParentSize())
                            }
                        }
                    }
                }
                if (isToggle) PlaybackBadge(paused, Modifier.align(Alignment.BottomEnd))
            }
        }
    }
}

/** Shows what a tap on the frame does: pause, or play when paused. Decorative for TalkBack. */
@Composable
private fun PlaybackBadge(paused: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.padding(8.dp).size(40.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(if (paused) R.drawable.ic_play_arrow else R.drawable.ic_pause),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            // Glyph-named so tests can tell the badge's two states apart.
            modifier = Modifier.testTag(if (paused) "guide_badge_play" else "guide_badge_pause"),
        )
    }
}

@Composable
private fun GuideDrawing(
    file: RiveFile,
    radii: SkirtRadii,
    step: FoldingStep,
    play: Boolean,
    paused: Boolean,
    onLoaded: () -> Unit,
) {
    // While the artboard or view-model instance loads, the frame stays empty.
    val artboard = rememberArtboardResult(file, step.artboard)
    if (artboard is Result.Error) {
        Text(stringResource(R.string.guide_animation_error))
        LaunchedEffect(Unit) { onLoaded() }
    }
    if (artboard !is Result.Success) return
    when (val result = rememberViewModelInstanceResult(file)) {
        is Result.Loading -> Unit

        is Result.Error -> {
            Text(stringResource(R.string.guide_measurements_error))
            LaunchedEffect(Unit) { onLoaded() }
        }

        is Result.Success -> {
            val instance = result.value
            val palette = guidePalette(MaterialTheme.colorScheme)
            var ready by remember(instance, radii) { mutableStateOf(false) }
            LaunchedEffect(instance, radii, palette) {
                palette.forEach { (name, color) -> instance.setColor(name, color.toArgb()) }
                val markingDiameter = 800f

                // Normalize before converting to Float; large valid centimetre values stay finite.
                fun diameter(radius: Length) =
                    (markingDiameter * (radius.centimetres / radii.hemCutting.centimetres))
                        .toFloat()
                instance.setNumber("waistStitching", diameter(radii.waistStitching))
                instance.setNumber("waistCutting", diameter(radii.waistCutting))
                instance.setNumber("finishedHem", diameter(radii.finishedHem))
                instance.setNumber("hemCutting", markingDiameter)
                ready = true
                onLoaded()
            }
            LaunchedEffect(instance, play) { instance.setBoolean("play", play) }
            if (ready) {
                Rive(
                    file = file,
                    artboard = artboard.value,
                    viewModelInstance = instance,
                    playing = play && !paused,
                    fit = Fit.Contain(),
                    // The frame around the drawing takes the tap that pauses it.
                    pointerInputMode = RivePointerInputMode.Observe,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** What the step's drawing shows, for TalkBack. */
@Composable
private fun drawingDescription(step: FoldingStep): String = when (step) {
    FoldingStep.Marking -> stringResource(R.string.guide_drawing_marking)
    FoldingStep.Cutting -> stringResource(R.string.guide_drawing_cutting)
    FoldingStep.Unfolding -> stringResource(R.string.guide_drawing_unfolding)
    else -> stringResource(R.string.guide_drawing_fold, step.ordinal + 1)
}

/**
 * The drawing's colour properties by view-model name. The cutting card's roles: fabric is its
 * paper and lines its ink, so the drawing reads on the light, dark and dynamic schemes alike.
 */
internal fun guidePalette(colors: ColorScheme): Map<String, Color> = mapOf(
    "ink" to colors.onSecondaryContainer,
    "fabric" to colors.secondaryContainer,
    "fabricFold" to colors.tertiaryContainer,
    "accent" to colors.tertiary,
)

/**
 * One step's written lesson: the action to take now, the context that changes it, then the
 * supporting detail behind a collapsed "Detalhes" disclosure.
 */
@Composable
internal fun StepInstructions(radii: SkirtRadii, step: FoldingStep) {
    val text: (Int) -> String = LocalResources.current::getString
    Text(stringResource(stepLead(step)), style = MaterialTheme.typography.titleMedium)
    when (step) {
        FoldingStep.Preparation -> {
            Text(stringResource(R.string.guide_preparation_care))
            Text(stringResource(R.string.guide_preparation_layout))
        }

        FoldingStep.FirstFold -> Text(stringResource(R.string.guide_first_fold_context))

        FoldingStep.SecondFold -> Text(stringResource(R.string.guide_second_fold_context))

        FoldingStep.Marking -> MarkingContext(radii)

        FoldingStep.Cutting -> {
            Text(stringResource(R.string.guide_cutting_arcs))
            Text(stringResource(R.string.guide_cutting_keep))
            coincidenceNotes(cuttingCard(radii, text), text).forEach { Text(it) }
            Text(stringResource(R.string.guide_cutting_garment))
        }

        FoldingStep.Unfolding -> {
            Text(stringResource(R.string.guide_unfolding_result))
            Text(stringResource(R.string.guide_unfolding_center))
        }
    }
    Disclosure(stringResource(R.string.guide_details)) {
        // Supporting text, quieter than the lead and context through the smaller type.
        ProvideTextStyle(
            MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            stepDetails(step).forEach { Text(stringResource(it)) }
            Text(stringResource(R.string.guide_details_motion))
        }
    }
}

/** The one action to take now, short enough to read under the pinned drawing. */
@StringRes
private fun stepLead(step: FoldingStep): Int = when (step) {
    FoldingStep.Preparation -> R.string.guide_lead_preparation
    FoldingStep.FirstFold -> R.string.guide_lead_first_fold
    FoldingStep.SecondFold -> R.string.guide_lead_second_fold
    FoldingStep.Marking -> R.string.guide_lead_marking
    FoldingStep.Cutting -> R.string.guide_lead_cutting
    FoldingStep.Unfolding -> R.string.guide_lead_unfolding
}

/** The current radii in key order, then where each line sits and which are not cut. */
@Composable
private fun MarkingContext(radii: SkirtRadii) {
    val text: (Int) -> String = LocalResources.current::getString
    val card = cuttingCard(radii, text)
    // The four radii read as one key: closer together than the paragraphs around them.
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        card.flatMap { it.entries }.sortedBy { it.line.number }.forEach { entry ->
            Text("${entry.line.number}. ${entry.label}: ${entry.value}")
        }
    }
    Text(stringResource(R.string.guide_marking_references))
    Text(stringResource(R.string.guide_marking_allowances))
    coincidenceNotes(card, text).forEach { Text(it) }
}

/** Conventions, honest limits and practice notes that support the step but don't change it. */
private fun stepDetails(step: FoldingStep): List<Int> = when (step) {
    FoldingStep.Preparation -> listOf(
        R.string.guide_details_preparation,
        R.string.guide_details_practice_material,
        R.string.guide_illustrative_fabric,
    )

    FoldingStep.FirstFold -> listOf(
        R.string.guide_details_first_fold,
        R.string.guide_illustrative_fabric,
    )

    FoldingStep.SecondFold -> listOf(
        R.string.guide_details_second_fold,
        R.string.guide_details_same_square,
        R.string.guide_illustrative_fabric,
    )

    FoldingStep.Marking -> listOf(
        R.string.guide_proportional_drawing,
        R.string.guide_construction_boundary,
    )

    FoldingStep.Cutting -> listOf(
        R.string.guide_details_cutting_replay,
        R.string.guide_practice_first,
        R.string.guide_proportional_drawing,
        R.string.guide_construction_boundary,
    )

    FoldingStep.Unfolding -> listOf(
        R.string.guide_details_unfolding_scale,
        R.string.guide_details_unfolding_limits,
        R.string.guide_practice_first,
        R.string.guide_proportional_drawing,
        R.string.guide_construction_boundary,
    )
}
