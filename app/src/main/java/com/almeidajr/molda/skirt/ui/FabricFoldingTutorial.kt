package com.almeidajr.molda.skirt.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import app.rive.Fit
import app.rive.RenderBackend
import app.rive.Result
import app.rive.Rive
import app.rive.RiveFile
import app.rive.RiveFileSource
import app.rive.rememberArtboardResult
import app.rive.rememberRiveFile
import app.rive.rememberRiveWorkerOrNull
import app.rive.rememberViewModelInstanceResult
import com.almeidajr.molda.R
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.skirt.domain.SkirtRadii

@Composable
internal fun TutorialAnimation(
    radii: SkirtRadii,
    step: FoldingStep,
    motionEnabled: Boolean,
    drawingSize: Dp,
) {
    // Vulkan avoids blank frames on software GLES drivers; Rive falls back when unavailable.
    val worker = rememberRiveWorkerOrNull(renderBackend = RenderBackend.Vulkan)
    val file = worker?.let {
        rememberRiveFile(RiveFileSource.RawRes.from(R.raw.fabric_folding), it)
    }
    var replay by remember(motionEnabled) { mutableIntStateOf(0) }
    var paused by remember(motionEnabled) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The artboards are transparent: this frame is the page behind the drawing.
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.width(drawingSize).heightIn(min = drawingSize),
        ) {
            Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                when (file) {
                    null -> Text(stringResource(R.string.tutorial_animation_unavailable))

                    is Result.Loading -> Text(stringResource(R.string.tutorial_animation_loading))

                    is Result.Error -> Text(stringResource(R.string.tutorial_animation_error))

                    is Result.Success -> TutorialDrawing(
                        file.value,
                        radii,
                        step,
                        motionEnabled && replay > 0,
                        replay,
                        paused,
                    )
                }
            }
        }
        if (file is Result.Success) {
            FlowRow(horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = {
                    replay++
                    paused = false
                }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(replayLabel(step, hasPlayed = replay > 0)))
                }
                if (motionEnabled && replay > 0) {
                    TextButton(
                        onClick = { paused = !paused },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(
                            stringResource(
                                if (paused) R.string.tutorial_resume else R.string.tutorial_pause,
                            ),
                        )
                    }
                }
            }
        }
    }
}

/** The replay button's label: Marking names its marking, and a played step offers a repeat. */
@StringRes
private fun replayLabel(step: FoldingStep, hasPlayed: Boolean): Int = when {
    step == FoldingStep.Marking && hasPlayed -> R.string.tutorial_replay_marking
    step == FoldingStep.Marking -> R.string.tutorial_play_marking
    hasPlayed -> R.string.tutorial_replay_step
    else -> R.string.tutorial_play_step
}

@Composable
private fun TutorialDrawing(
    file: RiveFile,
    radii: SkirtRadii,
    step: FoldingStep,
    play: Boolean,
    replay: Int,
    paused: Boolean,
) {
    val artboard = rememberArtboardResult(file, step.artboard)
    if (artboard !is Result.Success) {
        Text(stringResource(R.string.tutorial_drawing_preparing))
        return
    }
    when (val result = rememberViewModelInstanceResult(file)) {
        is Result.Loading -> Text(stringResource(R.string.tutorial_measurements_loading))

        is Result.Error -> Text(stringResource(R.string.tutorial_measurements_error))

        is Result.Success -> {
            val instance = result.value
            val palette = tutorialPalette(MaterialTheme.colorScheme)
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
            }
            LaunchedEffect(instance, play, replay) {
                instance.setBoolean("play", play)
                if (play) instance.fireTrigger("restart")
            }
            val description = when (step) {
                FoldingStep.Marking -> stringResource(R.string.tutorial_drawing_marking)
                FoldingStep.Cutting -> stringResource(R.string.tutorial_drawing_cutting)
                FoldingStep.Unfolding -> stringResource(R.string.tutorial_drawing_unfolding)
                else -> stringResource(R.string.tutorial_drawing_fold, step.ordinal + 1)
            }
            if (ready) {
                Rive(
                    file = file,
                    artboard = artboard.value,
                    viewModelInstance = instance,
                    playing = play && !paused,
                    fit = Fit.Contain(),
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).semantics {
                        contentDescription = description
                    },
                )
            }
        }
    }
}

/**
 * The drawing's colour properties by view-model name. The cutting card's roles: fabric is its
 * paper and lines its ink, so the drawing reads on the light, dark and dynamic schemes alike.
 */
internal fun tutorialPalette(colors: ColorScheme): Map<String, Color> = mapOf(
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
            Text(stringResource(R.string.tutorial_preparation_care))
            Text(stringResource(R.string.tutorial_preparation_layout))
        }

        FoldingStep.FirstFold -> Text(stringResource(R.string.tutorial_first_fold_context))

        FoldingStep.SecondFold -> Text(stringResource(R.string.tutorial_second_fold_context))

        FoldingStep.Marking -> MarkingContext(radii)

        FoldingStep.Cutting -> {
            Text(stringResource(R.string.tutorial_cutting_arcs))
            Text(stringResource(R.string.tutorial_cutting_keep))
            coincidenceNotes(cuttingCard(radii, text), text).forEach { Text(it) }
            Text(stringResource(R.string.tutorial_cutting_garment))
        }

        FoldingStep.Unfolding -> {
            Text(stringResource(R.string.tutorial_unfolding_result))
            Text(stringResource(R.string.tutorial_unfolding_center))
        }
    }
    Disclosure(stringResource(R.string.tutorial_details)) {
        // Supporting text, quieter than the lead and context through the smaller type.
        ProvideTextStyle(
            MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            stepDetails(step).forEach { Text(stringResource(it)) }
            Text(stringResource(R.string.tutorial_details_motion))
        }
    }
}

/** The one action to take now, short enough to read under the pinned drawing. */
@StringRes
private fun stepLead(step: FoldingStep): Int = when (step) {
    FoldingStep.Preparation -> R.string.tutorial_lead_preparation
    FoldingStep.FirstFold -> R.string.tutorial_lead_first_fold
    FoldingStep.SecondFold -> R.string.tutorial_lead_second_fold
    FoldingStep.Marking -> R.string.tutorial_lead_marking
    FoldingStep.Cutting -> R.string.tutorial_lead_cutting
    FoldingStep.Unfolding -> R.string.tutorial_lead_unfolding
}

/** The current radii in key order, then where each line sits and which are not cut. */
@Composable
private fun MarkingContext(radii: SkirtRadii) {
    val text: (Int) -> String = LocalResources.current::getString
    val card = cuttingCard(radii, text)
    // The four radii read as one key: closer together than the paragraphs around them.
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        card.flatMap { it.entries }.sortedBy { it.number }.forEach { entry ->
            Text("${entry.number}. ${entry.label}: ${entry.value}")
        }
    }
    Text(stringResource(R.string.tutorial_marking_references))
    Text(stringResource(R.string.tutorial_marking_allowances))
    coincidenceNotes(card, text).forEach { Text(it) }
}

/** Conventions, honest limits and practice notes that support the step but don't change it. */
private fun stepDetails(step: FoldingStep): List<Int> = when (step) {
    FoldingStep.Preparation -> listOf(
        R.string.tutorial_details_preparation,
        R.string.tutorial_details_practice_material,
        R.string.tutorial_illustrative_fabric,
    )

    FoldingStep.FirstFold -> listOf(
        R.string.tutorial_details_first_fold,
        R.string.tutorial_illustrative_fabric,
    )

    FoldingStep.SecondFold -> listOf(
        R.string.tutorial_details_second_fold,
        R.string.tutorial_details_same_square,
        R.string.tutorial_illustrative_fabric,
    )

    FoldingStep.Marking -> listOf(
        R.string.tutorial_proportional_drawing,
        R.string.tutorial_construction_boundary,
    )

    FoldingStep.Cutting -> listOf(
        R.string.tutorial_details_cutting_replay,
        R.string.tutorial_practice_first,
        R.string.tutorial_proportional_drawing,
        R.string.tutorial_construction_boundary,
    )

    FoldingStep.Unfolding -> listOf(
        R.string.tutorial_details_unfolding_scale,
        R.string.tutorial_details_unfolding_limits,
        R.string.tutorial_practice_first,
        R.string.tutorial_proportional_drawing,
        R.string.tutorial_construction_boundary,
    )
}
