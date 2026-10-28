package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "workout_records")
data class WorkoutRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "RUN" or "CALISTHENICS"
    val title: String,
    val timestamp: Long,
    val durationSeconds: Int,
    val distanceMeters: Int,
    val avgPaceSecondsPerKm: Int,
    val calories: Int,
    val pausesCount: Int = 0,
    val pauseDurationSeconds: Int = 0,
    val tag: String = "", // "Meta Batida", "Concluído 100%", "Corrida + Caminhada", etc.
    val exercisesCount: Int = 0,
    val splitsJson: String = "", // e.g. "KM 1: 6'12\", KM 2: 6'24\""
    // Real GPS Telemetry Fields
    val startLat: Double = 0.0,
    val startLng: Double = 0.0,
    val endLat: Double = 0.0,
    val endLng: Double = 0.0,
    val avgSpeedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val elevationGainMeters: Int = 0,
    val gpsAccuracyMeters: Float = 0f,
    val stepsEstimate: Int = 0,
    val routeCoordinatesJson: String = "", // "lat,lng;lat,lng;..."
    // Detailed Run/Walk & Calisthenics Routine Mapping Fields
    val runningSeconds: Int = 0,
    val walkingSeconds: Int = 0,
    val routineDetailsJson: String = ""
) {
    val durationFormatted: String
        get() {
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            return "${mins}m ${String.format(Locale.US, "%02d", secs)}s"
        }

    val runningFormatted: String
        get() {
            val effRun = if (runningSeconds > 0 || walkingSeconds > 0) runningSeconds else durationSeconds
            return String.format(Locale.US, "%02d:%02d", effRun / 60, effRun % 60)
        }

    val walkingFormatted: String
        get() = String.format(Locale.US, "%02d:%02d", walkingSeconds / 60, walkingSeconds % 60)

    val runningPercentage: Int
        get() {
            val total = (runningSeconds + walkingSeconds).takeIf { it > 0 } ?: durationSeconds.coerceAtLeast(1)
            val effRun = if (runningSeconds > 0 || walkingSeconds > 0) runningSeconds else durationSeconds
            return ((effRun.toFloat() / total) * 100f).toInt().coerceIn(0, 100)
        }

    val paceFormatted: String
        get() {
            if (avgPaceSecondsPerKm <= 0) return "--"
            val mins = avgPaceSecondsPerKm / 60
            val secs = avgPaceSecondsPerKm % 60
            return "${mins}'${String.format(Locale.US, "%02d", secs)}\"/km"
        }

    val distanceKmFormatted: String
        get() = String.format(Locale.US, "%.2f km", distanceMeters / 1000.0)

    val hasGpsCoordinates: Boolean
        get() = startLat != 0.0 || startLng != 0.0

    val coordinatesSummaryFormatted: String
        get() {
            if (!hasGpsCoordinates) return "Sem coordenadas registradas"
            return String.format(
                Locale.US,
                "%.5f, %.5f → %.5f, %.5f",
                startLat,
                startLng,
                endLat,
                endLng
            )
        }

    val speedSummaryFormatted: String
        get() {
            if (avgSpeedKmh <= 0f) return "-- km/h"
            return String.format(Locale.US, "%.1f km/h (Máx %.1f)", avgSpeedKmh, maxSpeedKmh)
        }

    fun parsedRoutePoints(): List<Pair<Double, Double>> {
        if (routeCoordinatesJson.isBlank()) return emptyList()
        return routeCoordinatesJson.split(";")
            .mapNotNull { token ->
                val parts = token.split(",")
                if (parts.size == 2) {
                    val lat = parts[0].toDoubleOrNull()
                    val lng = parts[1].toDoubleOrNull()
                    if (lat != null && lng != null) Pair(lat, lng) else null
                } else null
            }
    }
}
