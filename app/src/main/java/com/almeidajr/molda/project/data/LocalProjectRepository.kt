package com.almeidajr.molda.project.data

import com.almeidajr.molda.data.resultOf
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.measurement.MeasurementSource
import com.almeidajr.molda.project.domain.PatternDraftKind
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed projects, read and written through [dao]. */
class LocalProjectRepository(private val dao: ProjectDao) : ProjectRepository {
    override fun projects(): Flow<List<Project>> =
        dao.projects().map { rows -> rows.map { it.toProject() } }

    override suspend fun create(
        name: ProjectName,
        snapshot: SkirtSnapshot,
        savedAt: Instant,
    ): Result<Project> = resultOf {
        val entity = snapshot.toEntity(name, savedAt)
        entity.copy(id = dao.insert(entity)).toProject()
    }

    override suspend fun update(
        id: ProjectId,
        snapshot: SkirtSnapshot,
        updatedAt: Instant,
    ): Result<Unit> = resultOf {
        val updated = dao.update(
            id = id.value,
            waist = snapshot.draft.waist.centimetres,
            ease = snapshot.draft.ease.centimetres,
            finishedLength = snapshot.draft.finishedLength.centimetres,
            waistSeamAllowance = snapshot.draft.waistSeamAllowance.centimetres,
            hemAllowance = snapshot.draft.hemAllowance.centimetres,
            waistStitchingRadius = snapshot.radii.waistStitching.centimetres,
            finishedHemRadius = snapshot.radii.finishedHem.centimetres,
            waistCuttingRadius = snapshot.radii.waistCutting.centimetres,
            hemCuttingRadius = snapshot.radii.hemCutting.centimetres,
            profileName = snapshot.source?.profileName,
            measurementDate = snapshot.source?.measurementDate?.toString(),
            updatedAt = updatedAt.toEpochMilli(),
        )
        check(updated == 1) { "Project no longer exists" }
    }

    override suspend fun rename(
        id: ProjectId,
        name: ProjectName,
        renamedAt: Instant,
    ): Result<Unit> = resultOf {
        check(dao.rename(id.value, name.text, renamedAt.toEpochMilli()) == 1) {
            "Project no longer exists"
        }
    }

    override suspend fun delete(id: ProjectId): Result<Unit> = resultOf {
        check(dao.delete(id.value) == 1) { "Project no longer exists" }
    }
}

// Domain constructors validate every loaded row, so a malformed row fails the load.
private fun ProjectEntity.toProject() = Project(
    id = ProjectId(id),
    name = ProjectName(name),
    kind = PatternDraftKind.entries.single { it.storedName == patternKind },
    snapshot = SkirtSnapshot(
        SkirtDraft(
            waist = Length(waist),
            ease = Length(ease),
            finishedLength = Length(finishedLength),
            waistSeamAllowance = Length(waistSeamAllowance),
            hemAllowance = Length(hemAllowance),
        ),
        SkirtRadii(
            waistStitching = Length(waistStitchingRadius),
            finishedHem = Length(finishedHemRadius),
            waistCutting = Length(waistCuttingRadius),
            hemCutting = Length(hemCuttingRadius),
        ),
        profileName?.let { MeasurementSource(it, measurementDate?.let(LocalDate::parse)) },
    ),
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

// A stable stored name, independent of the enum constant's.
private val PatternDraftKind.storedName: String
    get() = when (this) {
        PatternDraftKind.FullCircleSkirt -> "full_circle_skirt"
    }

private fun SkirtSnapshot.toEntity(name: ProjectName, savedAt: Instant) = ProjectEntity(
    name = name.text,
    patternKind = kind.storedName,
    waist = draft.waist.centimetres,
    ease = draft.ease.centimetres,
    finishedLength = draft.finishedLength.centimetres,
    waistSeamAllowance = draft.waistSeamAllowance.centimetres,
    hemAllowance = draft.hemAllowance.centimetres,
    waistStitchingRadius = radii.waistStitching.centimetres,
    finishedHemRadius = radii.finishedHem.centimetres,
    waistCuttingRadius = radii.waistCutting.centimetres,
    hemCuttingRadius = radii.hemCutting.centimetres,
    profileName = source?.profileName,
    measurementDate = source?.measurementDate?.toString(),
    createdAt = savedAt.toEpochMilli(),
    updatedAt = savedAt.toEpochMilli(),
)
