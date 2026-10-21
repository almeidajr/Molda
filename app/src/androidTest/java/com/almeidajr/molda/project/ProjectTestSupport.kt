package com.almeidajr.molda.project

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.almeidajr.molda.click
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.measurement.MeasurementSource
import com.almeidajr.molda.project.domain.PatternDraftKind
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import java.time.Instant
import java.time.LocalDate

/**
 * "Saia da formatura", saved at [savedAt]: an 80 cm waist from Ana's profile measured on
 * 12/10/2026, with radii that differ from a recalculation so a test can tell stored from
 * recalculated.
 */
internal fun formaturaProject(savedAt: Instant) = Project(
    ProjectId(1),
    ProjectName("Saia da formatura"),
    PatternDraftKind.FullCircleSkirt,
    SkirtSnapshot(
        SkirtDraft(Length(80.0), Length(2.0), Length(60.0), Length(1.0), Length(2.0)),
        SkirtRadii(Length(11.11), Length(71.11), Length(10.11), Length(73.11)),
        MeasurementSource("Ana", LocalDate.of(2026, 10, 12)),
    ),
    createdAt = savedAt,
    updatedAt = savedAt,
)

/** Opens the saved project [name] by tapping its name: a short row's centre falls on its Ações button. */
internal fun SemanticsNodeInteractionsProvider.clickProject(name: String) =
    click(name, useUnmergedTree = true)
