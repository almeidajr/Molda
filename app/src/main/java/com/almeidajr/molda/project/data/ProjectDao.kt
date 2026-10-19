package com.almeidajr.molda.project.data

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/** A full-circle skirt project: its snapshot in typed columns, lengths in centimetres. */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    // The pattern draft kind's stored name, such as "full_circle_skirt".
    @ColumnInfo(name = "pattern_kind") val patternKind: String,
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
    // Epoch milliseconds.
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updated_at DESC, id DESC")
    fun projects(): Flow<List<ProjectEntity>>

    @Insert
    suspend fun insert(project: ProjectEntity): Long

    @Query(
        "UPDATE projects SET waist = :waist, ease = :ease, finished_length = :finishedLength, " +
            "waist_seam_allowance = :waistSeamAllowance, hem_allowance = :hemAllowance, " +
            "waist_stitching_radius = :waistStitchingRadius, " +
            "finished_hem_radius = :finishedHemRadius, " +
            "waist_cutting_radius = :waistCuttingRadius, hem_cutting_radius = :hemCuttingRadius, " +
            "profile_name = :profileName, measurement_date = :measurementDate, " +
            "updated_at = :updatedAt WHERE id = :id",
    )
    @Suppress("LongParameterList") // One parameter per snapshot column.
    suspend fun update(
        id: Long,
        waist: Double,
        ease: Double,
        finishedLength: Double,
        waistSeamAllowance: Double,
        hemAllowance: Double,
        waistStitchingRadius: Double,
        finishedHemRadius: Double,
        waistCuttingRadius: Double,
        hemCuttingRadius: Double,
        profileName: String?,
        measurementDate: String?,
        updatedAt: Long,
    ): Int

    @Query("UPDATE projects SET name = :name, updated_at = :updatedAt WHERE id = :id")
    suspend fun rename(id: Long, name: String, updatedAt: Long): Int

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: Long): Int
}
