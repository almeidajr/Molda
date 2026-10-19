package com.almeidajr.molda.project.domain

import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow

/** The positive identity of a stored [Project]. */
@JvmInline
value class ProjectId(val value: Long) {
    init {
        require(value > 0) { "Project identity must be positive: $value" }
    }
}

/** A non-blank project name without surrounding whitespace. */
@JvmInline
value class ProjectName(val text: String) {
    init {
        require(text.isNotBlank() && text == text.trim()) {
            "Project name must be non-blank and trimmed"
        }
    }
}

/** The measurement profile a snapshot's waist came from, copied as plain values. */
data class MeasurementSource(val profileName: String, val measurementDate: LocalDate)

/**
 * A full-circle skirt as it was saved: the draft (waist and drafting assumptions), the calculated
 * radii, and the profile the waist came from, or null when it was typed by hand.
 */
data class SkirtSnapshot(
    val draft: SkirtDraft,
    val radii: SkirtRadii,
    val source: MeasurementSource?,
)

/** A named, saved pattern draft; [updatedAt] is its last change. */
data class Project(
    val id: ProjectId,
    val name: ProjectName,
    val snapshot: SkirtSnapshot,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Stored projects; commands report storage failures as [Result.failure]. */
interface ProjectRepository {
    /** Every stored project, most recently changed first, emitted again after each change. */
    fun projects(): Flow<List<Project>>
    suspend fun create(
        name: ProjectName,
        snapshot: SkirtSnapshot,
        savedAt: Instant,
    ): Result<Project>

    /** Renames the project [id], which becomes the latest change at [renamedAt]. */
    suspend fun rename(id: ProjectId, name: ProjectName, renamedAt: Instant): Result<Unit>

    suspend fun delete(id: ProjectId): Result<Unit>
}

/** How a project's dates are shown. */
val ProjectDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")

/** The name a new project is offered, such as "Saia godê · Ana · 18/10/2026". */
fun defaultProjectName(patternName: String, profileName: String?, savedOn: LocalDate): String =
    listOfNotNull(patternName, profileName, savedOn.format(ProjectDateFormat)).joinToString(" · ")

/** The typed [text] as a project name, or null when it is blank. */
fun readProjectName(text: String): ProjectName? =
    text.trim().takeIf { it.isNotEmpty() }?.let(::ProjectName)
