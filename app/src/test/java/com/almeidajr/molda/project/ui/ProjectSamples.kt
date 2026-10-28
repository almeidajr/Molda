package com.almeidajr.molda.project.ui

import com.almeidajr.molda.project.ReferenceSnapshot
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.ProjectName
import java.time.Instant

/** "Saia antiga", stored on 01/09/2026 from a hand-typed waist. */
internal val saved = Project(
    ProjectId(1),
    ProjectName("Saia antiga"),
    ReferenceSnapshot.copy(source = null),
    createdAt = Instant.parse("2026-09-01T10:00:00Z"),
    updatedAt = Instant.parse("2026-09-01T10:00:00Z"),
)
