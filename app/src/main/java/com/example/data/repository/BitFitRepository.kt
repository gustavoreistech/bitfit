package com.example.data.repository

import com.example.data.db.BitFitDatabase
import com.example.data.model.UserSettings
import com.example.data.model.WeightRecord
import com.example.data.model.WorkoutRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class BitFitRepository(private val database: BitFitDatabase) {
    val allWorkouts: Flow<List<WorkoutRecord>> = database.workoutDao().getAllWorkouts()
    val allWeights: Flow<List<WeightRecord>> = database.weightDao().getAllWeightRecords()
    val latestWeight: Flow<WeightRecord?> = database.weightDao().getLatestWeightRecord()
    val settings: Flow<UserSettings?> = database.settingsDao().getSettings()

    fun getWorkoutsByType(type: String): Flow<List<WorkoutRecord>> {
        return if (type == "ALL") allWorkouts else database.workoutDao().getWorkoutsByType(type)
    }

    suspend fun saveWorkout(record: WorkoutRecord): Long {
        return database.workoutDao().insertWorkout(record)
    }

    suspend fun deleteWorkout(id: Long) {
        database.workoutDao().deleteWorkout(id)
    }

    suspend fun saveInitialProfile(
        userName: String,
        heightMeters: Float,
        currentWeightKg: Float,
        targetWeightKg: Float,
        targetRunDistanceMeters: Int,
        targetRunFrequency: Int = 3,
        targetCalisDays: Int = 4
    ) {
        val currentSettings = database.settingsDao().getSettings().firstOrNull() ?: UserSettings()
        val safeHeight = heightMeters.coerceIn(1.0f, 2.5f)
        val safeWeight = currentWeightKg.coerceIn(25.0f, 300.0f)
        val safeTargetWeight = targetWeightKg.coerceIn(25.0f, 300.0f)
        val safeRunDist = targetRunDistanceMeters.coerceIn(200, 42000)

        database.settingsDao().saveSettings(
            currentSettings.copy(
                id = 1,
                userName = userName.trim().ifBlank { "Atleta" },
                initialWeightKg = safeWeight,
                heightMeters = safeHeight,
                targetWeightKg = safeTargetWeight,
                targetRunDistanceMeters = safeRunDist,
                targetRunFrequency = targetRunFrequency.coerceIn(1, 7),
                targetCalisDays = targetCalisDays.coerceIn(1, 7),
                isProfileConfigured = true
            )
        )

        val existingWeights = database.weightDao().getAllWeightRecords().firstOrNull().orEmpty()
        val bmi = ((safeWeight / (safeHeight * safeHeight)) * 10f).roundToInt() / 10f
        val now = System.currentTimeMillis()
        val formatter = SimpleDateFormat("dd MMM", Locale("pt", "BR"))
        val dateDisplay = "${formatter.format(Date(now))} (Início)"

        if (existingWeights.isEmpty()) {
            database.weightDao().insertWeightRecord(
                WeightRecord(
                    dateMillis = now,
                    dateDisplay = dateDisplay,
                    weightKg = safeWeight,
                    deltaKg = 0.0f,
                    calculatedBmi = bmi,
                    isBaseline = true,
                    note = "Peso Inicial Cadastrado"
                )
            )
        } else {
            // Update latest weight or add new record if weight changed
            val latest = existingWeights.first()
            if (kotlin.math.abs(latest.weightKg - safeWeight) >= 0.1f) {
                val delta = safeWeight - latest.weightKg
                database.weightDao().insertWeightRecord(
                    WeightRecord(
                        dateMillis = now,
                        dateDisplay = formatter.format(Date(now)),
                        weightKg = safeWeight,
                        deltaKg = (delta * 10f).roundToInt() / 10f,
                        calculatedBmi = bmi,
                        isBaseline = false,
                        note = "Atualização de Perfil"
                    )
                )
            } else {
                database.weightDao().insertWeightRecord(
                    latest.copy(calculatedBmi = bmi)
                )
            }
        }
    }

    suspend fun resetAllData() {
        database.workoutDao().deleteAllWorkouts()
        database.weightDao().deleteAllWeightRecords()
        database.settingsDao().deleteAllSettings()
        database.settingsDao().saveSettings(
            UserSettings(
                id = 1,
                userName = "",
                initialWeightKg = 78.0f,
                heightMeters = 1.75f,
                targetWeightKg = 72.0f,
                targetRunDistanceMeters = 1600,
                targetRunFrequency = 3,
                targetCalisDays = 4,
                activeCalisDaysCsv = "SEG,TER,QUI,SEX",
                streakDays = 0,
                isProfileConfigured = false
            )
        )
    }

    suspend fun addWeight(weightKg: Float, dateMillis: Long, note: String = "") {
        val currentSettings = database.settingsDao().getSettings().firstOrNull() ?: UserSettings()
        val allExisting = database.weightDao().getAllWeightRecords().firstOrNull().orEmpty()
        val latest = allExisting.firstOrNull()

        val height = currentSettings.heightMeters.coerceAtLeast(1.0f)
        val imc = ((weightKg / (height * height)) * 10f).roundToInt() / 10f
        val delta = if (latest != null) ((weightKg - latest.weightKg) * 10f).roundToInt() / 10f else 0.0f
        val isFirstRecord = allExisting.isEmpty()

        val formatter = SimpleDateFormat("dd MMM", Locale("pt", "BR"))
        val baseDateStr = formatter.format(Date(dateMillis))
        val dateDisplay = if (isFirstRecord) "$baseDateStr (Início)" else baseDateStr

        val record = WeightRecord(
            dateMillis = dateMillis,
            dateDisplay = dateDisplay,
            weightKg = weightKg,
            deltaKg = delta,
            calculatedBmi = imc,
            isBaseline = isFirstRecord,
            note = note.ifBlank { if (isFirstRecord) "Base Inicial" else "" }
        )
        database.weightDao().insertWeightRecord(record)
    }

    suspend fun deleteWeight(id: Long) {
        database.weightDao().deleteWeightRecord(id)
    }

    suspend fun updateHeight(heightMeters: Float) {
        val safeHeight = heightMeters.coerceIn(1.0f, 2.5f)
        val current = database.settingsDao().getSettings().firstOrNull() ?: UserSettings()
        database.settingsDao().saveSettings(current.copy(heightMeters = safeHeight))

        // Recalculate BMI across existing weight records
        val allRecords = database.weightDao().getAllWeightRecords().firstOrNull().orEmpty()
        for (rec in allRecords) {
            val newBmi = ((rec.weightKg / (safeHeight * safeHeight)) * 10f).roundToInt() / 10f
            database.weightDao().insertWeightRecord(rec.copy(calculatedBmi = newBmi))
        }
    }

    suspend fun updateTargetWeight(targetWeightKg: Float) {
        val safeTarget = targetWeightKg.coerceIn(25.0f, 300.0f)
        val current = database.settingsDao().getSettings().firstOrNull() ?: UserSettings()
        database.settingsDao().saveSettings(current.copy(targetWeightKg = safeTarget))
    }

    suspend fun updateGoals(
        distanceMeters: Int,
        runFrequency: Int,
        calisDays: Int,
        activeCalisDaysCsv: String
    ) {
        val current = database.settingsDao().getSettings().firstOrNull() ?: UserSettings()
        database.settingsDao().saveSettings(
            current.copy(
                targetRunDistanceMeters = distanceMeters,
                targetRunFrequency = runFrequency,
                targetCalisDays = calisDays,
                activeCalisDaysCsv = activeCalisDaysCsv
            )
        )
    }

    suspend fun resetGoalsToDefault() {
        val current = database.settingsDao().getSettings().firstOrNull() ?: UserSettings()
        database.settingsDao().saveSettings(
            current.copy(
                targetRunDistanceMeters = 1600,
                targetRunFrequency = 3,
                targetCalisDays = 4,
                activeCalisDaysCsv = "SEG,TER,QUI,SEX"
            )
        )
    }

    suspend fun exportDataJson(): String {
        val workouts = database.workoutDao().getAllWorkouts().firstOrNull() ?: emptyList()
        val weights = database.weightDao().getAllWeightRecords().firstOrNull() ?: emptyList()
        val currentSettings = database.settingsDao().getSettings().firstOrNull() ?: UserSettings()

        val root = JSONObject()
        root.put("app", "BitFit")
        root.put("version", "2.0")
        root.put("exportedAt", System.currentTimeMillis())

        val settingsObj = JSONObject()
        settingsObj.put("userName", currentSettings.userName)
        settingsObj.put("initialWeightKg", currentSettings.initialWeightKg.toDouble())
        settingsObj.put("heightMeters", currentSettings.heightMeters.toDouble())
        settingsObj.put("targetWeightKg", currentSettings.targetWeightKg.toDouble())
        settingsObj.put("targetRunDistanceMeters", currentSettings.targetRunDistanceMeters)
        settingsObj.put("targetRunFrequency", currentSettings.targetRunFrequency)
        settingsObj.put("targetCalisDays", currentSettings.targetCalisDays)
        settingsObj.put("activeCalisDays", currentSettings.activeCalisDaysCsv)
        settingsObj.put("isProfileConfigured", currentSettings.isProfileConfigured)
        root.put("settings", settingsObj)

        val workoutsArray = JSONArray()
        for (w in workouts) {
            val obj = JSONObject()
            obj.put("id", w.id)
            obj.put("type", w.type)
            obj.put("title", w.title)
            obj.put("timestamp", w.timestamp)
            obj.put("durationSeconds", w.durationSeconds)
            obj.put("distanceMeters", w.distanceMeters)
            obj.put("paceSecondsPerKm", w.avgPaceSecondsPerKm)
            obj.put("calories", w.calories)
            obj.put("pausesCount", w.pausesCount)
            obj.put("pauseDurationSeconds", w.pauseDurationSeconds)
            obj.put("tag", w.tag)
            obj.put("exercisesCount", w.exercisesCount)
            obj.put("splitsJson", w.splitsJson)
            obj.put("startLat", w.startLat)
            obj.put("startLng", w.startLng)
            obj.put("endLat", w.endLat)
            obj.put("endLng", w.endLng)
            obj.put("avgSpeedKmh", w.avgSpeedKmh.toDouble())
            obj.put("maxSpeedKmh", w.maxSpeedKmh.toDouble())
            obj.put("elevationGainMeters", w.elevationGainMeters)
            obj.put("gpsAccuracyMeters", w.gpsAccuracyMeters.toDouble())
            obj.put("stepsEstimate", w.stepsEstimate)
            obj.put("routeCoordinatesJson", w.routeCoordinatesJson)
            obj.put("runningSeconds", w.runningSeconds)
            obj.put("walkingSeconds", w.walkingSeconds)
            obj.put("routineDetailsJson", w.routineDetailsJson)
            workoutsArray.put(obj)
        }
        root.put("workouts", workoutsArray)

        val weightsArray = JSONArray()
        for (wt in weights) {
            val obj = JSONObject()
            obj.put("id", wt.id)
            obj.put("dateMillis", wt.dateMillis)
            obj.put("dateDisplay", wt.dateDisplay)
            obj.put("weightKg", wt.weightKg.toDouble())
            obj.put("deltaKg", wt.deltaKg.toDouble())
            obj.put("calculatedBmi", wt.calculatedBmi.toDouble())
            obj.put("isBaseline", wt.isBaseline)
            obj.put("note", wt.note)
            weightsArray.put(obj)
        }
        root.put("weights", weightsArray)

        return root.toString(2)
    }

    suspend fun importDataJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val settingsObj = root.optJSONObject("settings")
            val workoutsArray = root.optJSONArray("workouts")
            val weightsArray = root.optJSONArray("weights")

            if (settingsObj == null && workoutsArray == null && weightsArray == null) {
                return false
            }

            database.workoutDao().deleteAllWorkouts()
            database.weightDao().deleteAllWeightRecords()

            if (settingsObj != null) {
                database.settingsDao().saveSettings(
                    UserSettings(
                        id = 1,
                        userName = settingsObj.optString("userName", "Atleta"),
                        initialWeightKg = settingsObj.optDouble("initialWeightKg", 78.0).toFloat(),
                        heightMeters = settingsObj.optDouble("heightMeters", 1.75).toFloat(),
                        targetWeightKg = settingsObj.optDouble("targetWeightKg", 72.0).toFloat(),
                        targetRunDistanceMeters = settingsObj.optInt("targetRunDistanceMeters", 1600),
                        targetRunFrequency = settingsObj.optInt("targetRunFrequency", 3),
                        targetCalisDays = settingsObj.optInt("targetCalisDays", 4),
                        activeCalisDaysCsv = settingsObj.optString("activeCalisDays", "SEG,TER,QUI,SEX"),
                        streakDays = 0,
                        isProfileConfigured = true
                    )
                )
            }

            if (workoutsArray != null) {
                for (i in 0 until workoutsArray.length()) {
                    val w = workoutsArray.getJSONObject(i)
                    database.workoutDao().insertWorkout(
                        WorkoutRecord(
                            type = w.optString("type", "RUN"),
                            title = w.optString("title", "Treino"),
                            timestamp = w.optLong("timestamp", System.currentTimeMillis()),
                            durationSeconds = w.optInt("durationSeconds", 0),
                            distanceMeters = w.optInt("distanceMeters", 0),
                            avgPaceSecondsPerKm = w.optInt("paceSecondsPerKm", 0),
                            calories = w.optInt("calories", 0),
                            pausesCount = w.optInt("pausesCount", 0),
                            pauseDurationSeconds = w.optInt("pauseDurationSeconds", 0),
                            tag = w.optString("tag", ""),
                            exercisesCount = w.optInt("exercisesCount", 0),
                            splitsJson = w.optString("splitsJson", ""),
                            startLat = w.optDouble("startLat", 0.0),
                            startLng = w.optDouble("startLng", 0.0),
                            endLat = w.optDouble("endLat", 0.0),
                            endLng = w.optDouble("endLng", 0.0),
                            avgSpeedKmh = w.optDouble("avgSpeedKmh", 0.0).toFloat(),
                            maxSpeedKmh = w.optDouble("maxSpeedKmh", 0.0).toFloat(),
                            elevationGainMeters = w.optInt("elevationGainMeters", 0),
                            gpsAccuracyMeters = w.optDouble("gpsAccuracyMeters", 0.0).toFloat(),
                            stepsEstimate = w.optInt("stepsEstimate", 0),
                            routeCoordinatesJson = w.optString("routeCoordinatesJson", ""),
                            runningSeconds = w.optInt("runningSeconds", 0),
                            walkingSeconds = w.optInt("walkingSeconds", 0),
                            routineDetailsJson = w.optString("routineDetailsJson", "")
                        )
                    )
                }
            }

            if (weightsArray != null) {
                val formatter = SimpleDateFormat("dd MMM", Locale("pt", "BR"))
                for (i in 0 until weightsArray.length()) {
                    val wt = weightsArray.getJSONObject(i)
                    val dateMillis = wt.optLong("dateMillis", System.currentTimeMillis())
                    database.weightDao().insertWeightRecord(
                        WeightRecord(
                            dateMillis = dateMillis,
                            dateDisplay = wt.optString("dateDisplay", formatter.format(Date(dateMillis))),
                            weightKg = wt.optDouble("weightKg", 75.0).toFloat(),
                            deltaKg = wt.optDouble("deltaKg", 0.0).toFloat(),
                            calculatedBmi = wt.optDouble("calculatedBmi", 24.0).toFloat(),
                            isBaseline = wt.optBoolean("isBaseline", i == weightsArray.length() - 1),
                            note = wt.optString("note", "")
                        )
                    )
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
