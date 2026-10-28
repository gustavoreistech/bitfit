package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val id: Int = 1,
    val userName: String = "",
    val initialWeightKg: Float = 78.0f,
    val heightMeters: Float = 1.75f,
    val targetWeightKg: Float = 72.0f,
    val targetRunDistanceMeters: Int = 1600,
    val targetRunFrequency: Int = 3,
    val targetCalisDays: Int = 4,
    val activeCalisDaysCsv: String = "SEG,TER,QUI,SEX",
    val streakDays: Int = 0,
    val isProfileConfigured: Boolean = false
)
