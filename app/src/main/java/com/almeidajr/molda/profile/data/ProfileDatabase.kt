package com.almeidajr.molda.profile.data

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "profiles")
internal data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val waist: Double,
    // ISO-8601 text; the repository parses it so malformed dates fail loading.
    @ColumnInfo(name = "measurement_date") val measurementDate: String,
    val notes: String,
)

@Dao
internal interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY id")
    fun profiles(): Flow<List<ProfileEntity>>

    @Insert
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity): Int

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun delete(id: Long): Int
}

// Every schema change after version 1 ships a Migration (docs/adr/0002).
@Database(entities = [ProfileEntity::class], version = 1)
internal abstract class ProfileDatabase : RoomDatabase() {
    abstract fun profiles(): ProfileDao
}
