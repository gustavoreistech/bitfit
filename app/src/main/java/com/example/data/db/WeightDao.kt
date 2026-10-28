package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.WeightRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_records ORDER BY dateMillis DESC")
    fun getAllWeightRecords(): Flow<List<WeightRecord>>

    @Query("SELECT * FROM weight_records ORDER BY dateMillis DESC LIMIT 1")
    fun getLatestWeightRecord(): Flow<WeightRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightRecord(record: WeightRecord): Long

    @Query("DELETE FROM weight_records WHERE id = :id")
    suspend fun deleteWeightRecord(id: Long)

    @Query("DELETE FROM weight_records")
    suspend fun deleteAllWeightRecords()
}
