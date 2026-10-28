package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_records")
data class WeightRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateMillis: Long,
    val dateDisplay: String, // e.g. "Hoje, 04 Nov", "28 Out", "18 Set (Início)"
    val weightKg: Float,
    val deltaKg: Float, // e.g. -0.6f, +0.2f
    val calculatedBmi: Float,
    val isBaseline: Boolean = false,
    val note: String = ""
)
