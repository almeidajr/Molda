package com.almeidajr.molda.project.ui

import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.measurement.MeasurementSource
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import java.time.Instant
import java.time.LocalDate

/** A calculated skirt whose waist came from Ana's profile measured on 12/10/2026. */
internal val fromProfile = SkirtSnapshot(
    SkirtDraft(Length(80.0), Length(2.0), Length(60.0), Length(1.0), Length(2.0)),
    SkirtRadii(Length(13.05), Length(73.05), Length(12.05), Length(75.05)),
    MeasurementSource("Ana", LocalDate.of(2026, 10, 12)),
)

/** "Saia antiga", stored on 01/09/2026 from a hand-typed waist. */
internal val saved = Project(
    ProjectId(1),
    ProjectName("Saia antiga"),
    fromProfile.copy(source = null),
    createdAt = Instant.parse("2026-09-01T10:00:00Z"),
    updatedAt = Instant.parse("2026-09-01T10:00:00Z"),
)
