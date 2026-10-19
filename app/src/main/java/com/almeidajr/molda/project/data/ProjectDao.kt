package com.almeidajr.molda.project.data

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/** A full-circle skirt project: its snapshot in typed columns, lengths in centimetres. */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    // The pattern draft kind's stored name, such as "full_circle_skirt".
    @ColumnInfo(name = "pattern_kind") val patternKind: String,
    @Embedded val skirt: SkirtColumns,
    // Epoch milliseconds.
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** A skirt snapshot's columns: the draft, the radii and the profile it came from. */
data class SkirtColumns(
    val waist: Double,
    val ease: Double,
    @ColumnInfo(name = "finished_length") val finishedLength: Double,
    @ColumnInfo(name = "waist_seam_allowance") val waistSeamAllowance: Double,
    @ColumnInfo(name = "hem_allowance") val hemAllowance: Double,
    @ColumnInfo(name = "waist_stitching_radius") val waistStitchingRadius: Double,
    @ColumnInfo(name = "finished_hem_radius") val finishedHemRadius: Double,
    @ColumnInfo(name = "waist_cutting_radius") val waistCuttingRadius: Double,
    @ColumnInfo(name = "hem_cutting_radius") val hemCuttingRadius: Double,
    // Null when the waist was typed by hand; copied values, never a profile reference.
    @ColumnInfo(name = "profile_name") val profileName: String?,
    // ISO-8601 text, like profiles.measurement_date.
    @ColumnInfo(name = "measurement_date") val measurementDate: String?,
)

/** A new snapshot for the project [id]; its name, kind and creation time are kept. */
data class SkirtUpdate(
    val id: Long,
    @Embedded val skirt: SkirtColumns,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updated_at DESC, id DESC")
    fun projects(): Flow<List<ProjectEntity>>

    @Insert
    suspend fun insert(project: ProjectEntity): Long

    @Update(entity = ProjectEntity::class)
    suspend fun update(update: SkirtUpdate): Int

    @Query("UPDATE projects SET name = :name, updated_at = :updatedAt WHERE id = :id")
    suspend fun rename(id: Long, name: String, updatedAt: Long): Int

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: Long): Int
}
