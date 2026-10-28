package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.WorkoutRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_records ORDER BY timestamp DESC")
    fun getAllWorkouts(): Flow<List<WorkoutRecord>>

    @Query("SELECT * FROM workout_records WHERE type = :type ORDER BY timestamp DESC")
    fun getWorkoutsByType(type: String): Flow<List<WorkoutRecord>>

    @Query("SELECT * FROM workout_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentWorkouts(limit: Int): Flow<List<WorkoutRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(record: WorkoutRecord): Long

    @Query("DELETE FROM workout_records WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    @Query("DELETE FROM workout_records")
    suspend fun deleteAllWorkouts()

    @Query("SELECT COUNT(*) FROM workout_records")
    fun getWorkoutCount(): Flow<Int>

    @Query("SELECT SUM(distanceMeters) FROM workout_records WHERE type = 'RUN'")
    fun getTotalRunDistanceMeters(): Flow<Int?>

    @Query("SELECT SUM(calories) FROM workout_records")
    fun getTotalCalories(): Flow<Int?>
}
