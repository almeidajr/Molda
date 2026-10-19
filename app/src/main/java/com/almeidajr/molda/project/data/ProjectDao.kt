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
    val waist: Double,
    val ease: Double,
    @ColumnInfo(name = "finished_length") val finishedLength: Double,
    @ColumnInfo(name = "waist_seam_allowance") val waistSeamAllowance: Double,
    @ColumnInfo(name = "hem_allowance") val hemAllowance: Double,
    @ColumnInfo(name = "waist_stitching_radius") val waistStitchingRadius: Double,
    @ColumnInfo(name = "finished_hem_radius") val finishedHemRadius: Double,
    @ColumnInfo(name = "waist_cutting_radius") val waistCuttingRadius: Double,
    @ColumnInfo(name = "hem_cutting_radius") val hemCuttingRadius: Double,
    // Both null when the waist was typed by hand; copied values, never a profile reference.
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

    @Query("UPDATE projects SET name = :name, updated_at = :updatedAt WHERE id = :id")
    suspend fun rename(id: Long, name: String, updatedAt: Long): Int

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: Long): Int
}
