package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.db.BitFitDatabase
import com.example.data.model.ElasticStrengthExercises
import com.example.data.model.Exercise
import com.example.data.model.HiitExercises
import com.example.data.model.StrengthExercise
import com.example.data.model.UserSettings
import com.example.data.model.WeightRecord
import com.example.data.model.WorkoutRecord
import com.example.data.repository.BitFitRepository
import com.example.ui.components.SoundFeedback
import com.example.util.GpsLocationTracker
import com.example.util.GpsTelemetryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class AppScreen {
    DASHBOARD,
    RUN_TRACKER,
    HIIT,
    ELASTIC_STRENGTH,
    GOALS,
    BODY_EVOLUTION,
    HISTORY
}

class BitFitViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BitFitRepository = BitFitRepository(BitFitDatabase.getDatabase(application))
    private val soundFeedback = SoundFeedback(application)

    // Navigation
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen == AppScreen.RUN_TRACKER) {
            refreshGpsPermissionAndPassiveFix()
        }
    }

    // Repository Flows
    val allWorkouts: StateFlow<List<WorkoutRecord>> = repository.allWorkouts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWeights: StateFlow<List<WeightRecord>> = repository.allWeights
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestWeight: StateFlow<WeightRecord?> = repository.latestWeight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isSettingsLoaded = MutableStateFlow(false)
    val isSettingsLoaded: StateFlow<Boolean> = _isSettingsLoaded.asStateFlow()

    private val _showProfileSetupModal = MutableStateFlow(false)
    val showProfileSetupModal: StateFlow<Boolean> = _showProfileSetupModal.asStateFlow()

    fun openProfileSetupModal() {
        _showProfileSetupModal.value = true
    }

    fun closeProfileSetupModal() {
        _showProfileSetupModal.value = false
    }

    val settings: StateFlow<UserSettings> = repository.settings
        .map { loaded ->
            if (loaded != null) {
                _isSettingsLoaded.value = true
                loaded
            } else {
                UserSettings()
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            UserSettings()
        )

    // History filter
    private val _historyFilter = MutableStateFlow("ALL")
    val historyFilter: StateFlow<String> = _historyFilter.asStateFlow()

    fun setHistoryFilter(filter: String) {
        _historyFilter.value = filter
    }

    // Health tips rotation
    private val healthTips = listOf(
        Pair("Dica de Hidratação", "Beba 500ml de água pós-treino. Ajuda a restaurar o volume plasmático, reduz a fadiga muscular e acelera a síntese proteica após o esforço físico."),
        Pair("Recuperação Ativa", "No dia seguinte a treinos intensos de corrida, faça 15 min de caminhada leve ou mobilidade articular para irrigar tecidos e drenar ácido lático."),
        Pair("Consistência de Força", "Na calistenia, a cadência controlada (2s descida, 1s pausa, 1s subida) recruta até 30% mais unidades motoras sem sobrecarregar tendões."),
        Pair("Sono Reparador", "Dormir de 7 a 9 horas por noite libera até 75% do hormônio do crescimento (GH), essencial na reparação miofibrilar e queima de gordura."),
        Pair("Calibragem de Ritmo", "Mantenha o ritmo de corrida conversacional (Zona 2) para expandir sua capacidade mitocondrial aeróbica antes de buscar velocidade máxima.")
    )
    private val _currentTipIndex = MutableStateFlow(0)
    private val _currentTip = MutableStateFlow(healthTips[0])
    val currentTip: StateFlow<Pair<String, String>> = _currentTip.asStateFlow()

    fun nextTip() {
        val nextIdx = (_currentTipIndex.value + 1) % healthTips.size
        _currentTipIndex.value = nextIdx
        _currentTip.value = healthTips[nextIdx]
    }

    // -------------------------------------------------------------
    // RUN / REAL GPS TRACKER STATE & TELEMETRY
    // -------------------------------------------------------------
    private var runJob: Job? = null

    private val _gpsPermissionGranted = MutableStateFlow(false)
    val gpsPermissionGranted: StateFlow<Boolean> = _gpsPermissionGranted.asStateFlow()

    private val _gpsHardwareEnabled = MutableStateFlow(false)
    val gpsHardwareEnabled: StateFlow<Boolean> = _gpsHardwareEnabled.asStateFlow()

    private val _hasGpsFix = MutableStateFlow(false)
    val hasGpsFix: StateFlow<Boolean> = _hasGpsFix.asStateFlow()

    private val _gpsStatusLabel = MutableStateFlow("Verificando GPS...")
    val gpsStatusLabel: StateFlow<String> = _gpsStatusLabel.asStateFlow()

    private val _gpsProviderName = MutableStateFlow("GPS")
    val gpsProviderName: StateFlow<String> = _gpsProviderName.asStateFlow()

    // Indoor/Emulator movement simulation toggle so stationary users can also test route displacement
    private val _isIndoorSimulationMode = MutableStateFlow(false)
    val isIndoorSimulationMode: StateFlow<Boolean> = _isIndoorSimulationMode.asStateFlow()

    private val _runIsActive = MutableStateFlow(false)
    val runIsActive: StateFlow<Boolean> = _runIsActive.asStateFlow()

    private val _runIsPaused = MutableStateFlow(false)
    val runIsPaused: StateFlow<Boolean> = _runIsPaused.asStateFlow()

    private val _isWalkingMode = MutableStateFlow(false) // false = 🏃 Correndo, true = 🚶 Caminhando
    val isWalkingMode: StateFlow<Boolean> = _isWalkingMode.asStateFlow()

    private val _runDurationSeconds = MutableStateFlow(0)
    val runDurationSeconds: StateFlow<Int> = _runDurationSeconds.asStateFlow()

    private val _runningSeconds = MutableStateFlow(0)
    val runningSeconds: StateFlow<Int> = _runningSeconds.asStateFlow()

    private val _walkingSeconds = MutableStateFlow(0)
    val walkingSeconds: StateFlow<Int> = _walkingSeconds.asStateFlow()

    private val _runToggleCount = MutableStateFlow(0)
    val runToggleCount: StateFlow<Int> = _runToggleCount.asStateFlow()

    private val _runSummaryVisible = MutableStateFlow(false)
    val runSummaryVisible: StateFlow<Boolean> = _runSummaryVisible.asStateFlow()

    private val _runPauseDurationSeconds = MutableStateFlow(0)
    val runPauseDurationSeconds: StateFlow<Int> = _runPauseDurationSeconds.asStateFlow()

    private val _runDistanceMeters = MutableStateFlow(0)
    val runDistanceMeters: StateFlow<Int> = _runDistanceMeters.asStateFlow()

    private val _runCurrentPaceSeconds = MutableStateFlow(0) // Average pace (sec/km)
    val runCurrentPaceSeconds: StateFlow<Int> = _runCurrentPaceSeconds.asStateFlow()

    private val _runInstantPaceSeconds = MutableStateFlow(0) // Instant pace (sec/km)
    val runInstantPaceSeconds: StateFlow<Int> = _runInstantPaceSeconds.asStateFlow()

    private val _runCalories = MutableStateFlow(0)
    val runCalories: StateFlow<Int> = _runCalories.asStateFlow()

    private val _runPausesCount = MutableStateFlow(0)
    val runPausesCount: StateFlow<Int> = _runPausesCount.asStateFlow()

    private val _runSplits = MutableStateFlow<List<String>>(emptyList())
    val runSplits: StateFlow<List<String>> = _runSplits.asStateFlow()

    // Exact GPS Coordinates & Telemetry
    private val _currentLat = MutableStateFlow(0.0)
    val currentLat: StateFlow<Double> = _currentLat.asStateFlow()

    private val _currentLng = MutableStateFlow(0.0)
    val currentLng: StateFlow<Double> = _currentLng.asStateFlow()

    private val _startLat = MutableStateFlow(0.0)
    val startLat: StateFlow<Double> = _startLat.asStateFlow()

    private val _startLng = MutableStateFlow(0.0)
    val startLng: StateFlow<Double> = _startLng.asStateFlow()

    private val _gpsAccuracyMeters = MutableStateFlow(0f)
    val gpsAccuracyMeters: StateFlow<Float> = _gpsAccuracyMeters.asStateFlow()

    private val _currentAltitudeMeters = MutableStateFlow(0.0)
    val currentAltitudeMeters: StateFlow<Double> = _currentAltitudeMeters.asStateFlow()

    private val _elevationGainMeters = MutableStateFlow(0)
    val elevationGainMeters: StateFlow<Int> = _elevationGainMeters.asStateFlow()

    private val _currentSpeedKmh = MutableStateFlow(0f)
    val currentSpeedKmh: StateFlow<Float> = _currentSpeedKmh.asStateFlow()

    private val _maxSpeedKmh = MutableStateFlow(0f)
    val maxSpeedKmh: StateFlow<Float> = _maxSpeedKmh.asStateFlow()

    private val _avgSpeedKmh = MutableStateFlow(0f)
    val avgSpeedKmh: StateFlow<Float> = _avgSpeedKmh.asStateFlow()

    private val _compassBearingDegrees = MutableStateFlow(0f)
    val compassBearingDegrees: StateFlow<Float> = _compassBearingDegrees.asStateFlow()

    private val _compassDirectionLabel = MutableStateFlow("N")
    val compassDirectionLabel: StateFlow<String> = _compassDirectionLabel.asStateFlow()

    private val _runStepsEstimate = MutableStateFlow(0)
    val runStepsEstimate: StateFlow<Int> = _runStepsEstimate.asStateFlow()

    private val _runCadenceSpm = MutableStateFlow(0)
    val runCadenceSpm: StateFlow<Int> = _runCadenceSpm.asStateFlow()

    private val _gpsTrackPoints = MutableStateFlow<List<GpsTelemetryPoint>>(emptyList())
    val gpsTrackPoints: StateFlow<List<GpsTelemetryPoint>> = _gpsTrackPoints.asStateFlow()

    // Normalized (0..1) coordinates projected from real (lat, lng) points for route map canvas
    private val _routePoints = MutableStateFlow<List<Pair<Float, Float>>>(emptyList())
    val routePoints: StateFlow<List<Pair<Float, Float>>> = _routePoints.asStateFlow()

    private var accumulatedDistanceExactMeters = 0.0
    private var lastAcceptedGpsPoint: GpsTelemetryPoint? = null
    private var lastAltitudeReference: Double? = null
    private var lastKmSplitTimestampSec = 0

    // -------------------------------------------------------------
    // MÓDULO 2: HIIT CARDIO EM CASA (27 EXERCÍCIOS • 25 MIN)
    // -------------------------------------------------------------
    private var hiitJob: Job? = null
    val hiitExercises: List<Exercise> = HiitExercises.routine27

    private val _hiitIsActive = MutableStateFlow(false)
    val hiitIsActive: StateFlow<Boolean> = _hiitIsActive.asStateFlow()

    private val _hiitIsPaused = MutableStateFlow(false)
    val hiitIsPaused: StateFlow<Boolean> = _hiitIsPaused.asStateFlow()

    private val _hiitExerciseIndex = MutableStateFlow(0)
    val hiitExerciseIndex: StateFlow<Int> = _hiitExerciseIndex.asStateFlow()

    private val _hiitIsRest = MutableStateFlow(false)
    val hiitIsRest: StateFlow<Boolean> = _hiitIsRest.asStateFlow()

    private val _hiitSecondsLeft = MutableStateFlow(20)
    val hiitSecondsLeft: StateFlow<Int> = _hiitSecondsLeft.asStateFlow()

    private val _hiitTotalElapsedSeconds = MutableStateFlow(0)
    val hiitTotalElapsedSeconds: StateFlow<Int> = _hiitTotalElapsedSeconds.asStateFlow()

    private val _hiitCompleted = MutableStateFlow(false)
    val hiitCompleted: StateFlow<Boolean> = _hiitCompleted.asStateFlow()

    private val _hiitCompletedExerciseIds = MutableStateFlow<Set<Int>>(emptySet())
    val hiitCompletedExerciseIds: StateFlow<Set<Int>> = _hiitCompletedExerciseIds.asStateFlow()

    private val _hiitSkippedExerciseIds = MutableStateFlow<Set<Int>>(emptySet())
    val hiitSkippedExerciseIds: StateFlow<Set<Int>> = _hiitSkippedExerciseIds.asStateFlow()

    private val _hiitSaveEnabled = MutableStateFlow(true)
    val hiitSaveEnabled: StateFlow<Boolean> = _hiitSaveEnabled.asStateFlow()

    private val _hiitLastSavedToHistory = MutableStateFlow(false)
    val hiitLastSavedToHistory: StateFlow<Boolean> = _hiitLastSavedToHistory.asStateFlow()

    // -------------------------------------------------------------
    // MÓDULO 3: MUSCULAÇÃO COM ELÁSTICO (SÉRIES, REPS & DESCANSO 60s)
    // -------------------------------------------------------------
    private var elasticRestJob: Job? = null
    private var elasticStretchJob: Job? = null
    private var elasticElapsedJob: Job? = null

    val elasticExercises: List<StrengthExercise> = ElasticStrengthExercises.routine7

    // Chave = exercise.id, Valor = lista de repetições concluídas em cada série (ex: [15, 15, 14, 12])
    private val _elasticCompletedSetsMap = MutableStateFlow<Map<Int, List<Int>>>(emptyMap())
    val elasticCompletedSetsMap: StateFlow<Map<Int, List<Int>>> = _elasticCompletedSetsMap.asStateFlow()

    private val _elasticRestSecondsLeft = MutableStateFlow(0)
    val elasticRestSecondsLeft: StateFlow<Int> = _elasticRestSecondsLeft.asStateFlow()

    private val _elasticRestTargetExerciseName = MutableStateFlow("")
    val elasticRestTargetExerciseName: StateFlow<String> = _elasticRestTargetExerciseName.asStateFlow()

    private val _elasticActiveExerciseId = MutableStateFlow(1)
    val elasticActiveExerciseId: StateFlow<Int> = _elasticActiveExerciseId.asStateFlow()

    private val _elasticStretchSecondsLeft = MutableStateFlow(600) // 10 min = 600s
    val elasticStretchSecondsLeft: StateFlow<Int> = _elasticStretchSecondsLeft.asStateFlow()

    private val _elasticStretchRunning = MutableStateFlow(false)
    val elasticStretchRunning: StateFlow<Boolean> = _elasticStretchRunning.asStateFlow()

    private val _elasticStretchCompleted = MutableStateFlow(false)
    val elasticStretchCompleted: StateFlow<Boolean> = _elasticStretchCompleted.asStateFlow()

    private val _elasticWorkoutElapsedSeconds = MutableStateFlow(0)
    val elasticWorkoutElapsedSeconds: StateFlow<Int> = _elasticWorkoutElapsedSeconds.asStateFlow()

    private val _elasticWorkoutSavedBanner = MutableStateFlow(false)
    val elasticWorkoutSavedBanner: StateFlow<Boolean> = _elasticWorkoutSavedBanner.asStateFlow()

    private val gpsTracker: GpsLocationTracker = GpsLocationTracker(
        context = application,
        onLocationUpdated = { point -> onHardwareGpsLocation(point) },
        onProviderStateChanged = { enabled, status ->
            _gpsHardwareEnabled.value = enabled
            if (!_hasGpsFix.value) {
                _gpsStatusLabel.value = status
            }
        }
    )

    init {
        runCatching {
            refreshGpsPermissionAndPassiveFix()
        }
    }

    fun refreshGpsPermissionAndPassiveFix() {
        val granted = gpsTracker.hasLocationPermission()
        _gpsPermissionGranted.value = granted
        _gpsHardwareEnabled.value = gpsTracker.isGpsHardwareEnabled()
        if (granted) {
            gpsTracker.startTracking()
        } else {
            _gpsStatusLabel.value = "Permissão de GPS Necessária"
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _gpsPermissionGranted.value = granted
        if (granted) {
            gpsTracker.startTracking()
        } else {
            _gpsStatusLabel.value = "Permissão Negada • Ative o GPS ou Modo Indoor"
        }
    }

    fun toggleIndoorSimulationMode() {
        val nextMode = !_isIndoorSimulationMode.value
        _isIndoorSimulationMode.value = nextMode
        if (nextMode) {
            // Seed anchor coordinates if no real fix has arrived yet
            if (_currentLat.value == 0.0 && _currentLng.value == 0.0) {
                _currentLat.value = -23.587420
                _currentLng.value = -46.657640
                _currentAltitudeMeters.value = 762.0
                _gpsAccuracyMeters.value = 3.2f
                _hasGpsFix.value = true
            }
            _gpsProviderName.value = "SIM-GPS"
            _gpsStatusLabel.value = "Simulador GPS Ativo (±3.2m)"
        } else {
            refreshGpsPermissionAndPassiveFix()
        }
    }

    private fun onHardwareGpsLocation(point: GpsTelemetryPoint) {
        // Always update passive coordinates on screen when real GPS reports a fix
        _currentLat.value = point.latitude
        _currentLng.value = point.longitude
        if (point.altitudeMeters != 0.0) {
            _currentAltitudeMeters.value = point.altitudeMeters
        }
        _gpsAccuracyMeters.value = point.accuracyMeters
        _gpsProviderName.value = point.provider
        _hasGpsFix.value = true
        _gpsStatusLabel.value = String.format(
            Locale.US,
            "Sinal %s Fixo (±%.1fm)",
            point.provider,
            point.accuracyMeters
        )

        if (_runIsActive.value && !_runIsPaused.value && !_isIndoorSimulationMode.value) {
            processGpsFixForActiveRun(point)
        }
    }

    private fun processGpsFixForActiveRun(point: GpsTelemetryPoint) {
        // Ignore very inaccurate readings (> 50m radius) unless it's our very first point
        val prev = lastAcceptedGpsPoint
        if (prev == null) {
            _startLat.value = point.latitude
            _startLng.value = point.longitude
            lastAcceptedGpsPoint = point
            if (point.altitudeMeters != 0.0) {
                lastAltitudeReference = point.altitudeMeters
            }
            _gpsTrackPoints.value = listOf(point)
            recalculateNormalizedRouteProjection()
            return
        }

        if (point.accuracyMeters > 50f && !point.isSimulated) return

        val deltaMeters = GpsLocationTracker.distanceBetweenMeters(
            prev.latitude,
            prev.longitude,
            point.latitude,
            point.longitude
        )
        val deltaTimeSec = ((point.timestampMillis - prev.timestampMillis) / 1000.0).coerceAtLeast(0.5)
        val impliedSpeedMps = deltaMeters / deltaTimeSec

        // Filter out stationary GPS jitter (< 0.7m) and impossible teleportation spikes (> 17 m/s = 61 km/h)
        val minMovementThreshold = if (point.isSimulated) 0.3 else 0.75
        if (deltaMeters < minMovementThreshold || impliedSpeedMps > 17.5) {
            // If stationary, decay instant speed smoothly toward 0
            if (deltaTimeSec >= 2.0 && point.speedMps < 0.3f) {
                _currentSpeedKmh.value = 0f
                _runInstantPaceSeconds.value = 0
                _runCadenceSpm.value = 0
            }
            return
        }

        // Valid movement segment!
        accumulatedDistanceExactMeters += deltaMeters
        val newDistMeters = accumulatedDistanceExactMeters.toInt()
        _runDistanceMeters.value = newDistMeters

        // Bearing & cardinal direction
        val bearing = if (point.bearingDegrees > 0f) {
            point.bearingDegrees
        } else {
            GpsLocationTracker.bearingBetweenDegrees(
                prev.latitude,
                prev.longitude,
                point.latitude,
                point.longitude
            )
        }
        _compassBearingDegrees.value = bearing
        _compassDirectionLabel.value = GpsLocationTracker.cardinalDirectionLabel(bearing)

        // Speed (km/h): combine Doppler hardware speed if available with Haversine segment speed
        val segmentSpeedKmh = (impliedSpeedMps * 3.6).toFloat()
        val liveSpeedKmh = if (point.speedMps > 0.3f) {
            ((point.speedMps * 3.6f) * 0.6f) + (segmentSpeedKmh * 0.4f)
        } else {
            segmentSpeedKmh
        }
        _currentSpeedKmh.value = liveSpeedKmh.coerceIn(0f, 45f)
        if (_currentSpeedKmh.value > _maxSpeedKmh.value) {
            _maxSpeedKmh.value = _currentSpeedKmh.value
        }

        // Instant Pace (sec/km)
        if (_currentSpeedKmh.value >= 1.5f) {
            _runInstantPaceSeconds.value = (3600f / _currentSpeedKmh.value).toInt().coerceIn(150, 1200)
        }

        // Average Speed & Average Pace
        val elapsedActiveSec = _runDurationSeconds.value.coerceAtLeast(1)
        if (newDistMeters >= 5) {
            val avgSpd = ((newDistMeters.toDouble() / elapsedActiveSec) * 3.6).toFloat()
            _avgSpeedKmh.value = avgSpd
            val avgPace = ((elapsedActiveSec.toDouble() / newDistMeters) * 1000.0).toInt()
            _runCurrentPaceSeconds.value = avgPace.coerceIn(150, 1200)
        }

        // Elevation Gain (+m) with 1.2m hysteresis
        if (point.altitudeMeters != 0.0) {
            val refAlt = lastAltitudeReference
            if (refAlt == null) {
                lastAltitudeReference = point.altitudeMeters
            } else {
                val altDiff = point.altitudeMeters - refAlt
                if (altDiff >= 1.2) {
                    _elevationGainMeters.value += altDiff.toInt()
                    lastAltitudeReference = point.altitudeMeters
                } else if (altDiff <= -1.2) {
                    lastAltitudeReference = point.altitudeMeters
                }
            }
        }

        // Calories (~1.036 kcal per kg per km)
        val userWeightKg = (latestWeight.value?.weightKg ?: settings.value.targetWeightKg).coerceIn(45f, 150f)
        val kcal = ((newDistMeters / 1000.0) * userWeightKg * 1.036).toInt()
        _runCalories.value = kcal

        // Steps & Cadence estimation from distance and speed
        val strideMeters = (0.72 + (_currentSpeedKmh.value * 0.018)).coerceIn(0.70, 1.15)
        _runStepsEstimate.value = (newDistMeters / strideMeters).toInt()
        val stepsPerSec = (_currentSpeedKmh.value / 3.6) / strideMeters
        _runCadenceSpm.value = (stepsPerSec * 60.0).toInt().coerceIn(0, 210)

        // Check 1 KM splits
        val completedKms = newDistMeters / 1000
        if (completedKms > _runSplits.value.size) {
            val splitDurationSec = (elapsedActiveSec - lastKmSplitTimestampSec).coerceAtLeast(180)
            lastKmSplitTimestampSec = elapsedActiveSec
            val splitMin = splitDurationSec / 60
            val splitSec = splitDurationSec % 60
            val splitKmh = 3600f / splitDurationSec
            val splitStr = String.format(
                Locale.US,
                "KM %d: %d'%02d\"/km (%.1f km/h)",
                completedKms,
                splitMin,
                splitSec,
                splitKmh
            )
            _runSplits.value = _runSplits.value + splitStr
            soundFeedback.playTransitionSuccess()
        }

        lastAcceptedGpsPoint = point
        _gpsTrackPoints.value = _gpsTrackPoints.value + point
        recalculateNormalizedRouteProjection()
    }

    /**
     * Projects the real WGS84 (latitude, longitude) coordinates into a [0.08..0.92] normalized
     * canvas coordinate space while preserving real geographic aspect ratio.
     */
    private fun recalculateNormalizedRouteProjection() {
        val pts = _gpsTrackPoints.value
        if (pts.isEmpty()) {
            _routePoints.value = emptyList()
            return
        }
        if (pts.size == 1) {
            _routePoints.value = listOf(Pair(0.5f, 0.5f))
            return
        }

        val minLat = pts.minOf { it.latitude }
        val maxLat = pts.maxOf { it.latitude }
        val minLng = pts.minOf { it.longitude }
        val maxLng = pts.maxOf { it.longitude }

        val midLatRad = Math.toRadians((minLat + maxLat) / 2.0)
        val lngScale = cos(midLatRad).coerceAtLeast(0.2)

        val latSpan = max(maxLat - minLat, 0.00036)
        val lngSpan = max((maxLng - minLng) * lngScale, 0.00036)
        val maxSpan = max(latSpan, lngSpan)

        val projected = pts.map { p ->
            val normX = 0.5 + (((p.longitude - (minLng + maxLng) / 2.0) * lngScale) / maxSpan) * 0.72
            val normY = 0.5 - ((p.latitude - (minLat + maxLat) / 2.0) / maxSpan) * 0.72
            Pair(
                normX.toFloat().coerceIn(0.08f, 0.92f),
                normY.toFloat().coerceIn(0.08f, 0.92f)
            )
        }
        _routePoints.value = projected
    }

    fun startRun() {
        if (_runIsActive.value) return

        refreshGpsPermissionAndPassiveFix()

        if (!_gpsPermissionGranted.value) {
            _isIndoorSimulationMode.value = true
        }

        _runIsActive.value = true
        _runIsPaused.value = false
        _isWalkingMode.value = false
        _runSummaryVisible.value = false
        _runDurationSeconds.value = 0
        _runningSeconds.value = 0
        _walkingSeconds.value = 0
        _runPauseDurationSeconds.value = 0
        _runDistanceMeters.value = 0
        _runCurrentPaceSeconds.value = 0
        _runInstantPaceSeconds.value = 0
        _runCalories.value = 0
        _runPausesCount.value = 0
        _runSplits.value = emptyList()
        _elevationGainMeters.value = 0
        _currentSpeedKmh.value = 0f
        _maxSpeedKmh.value = 0f
        _avgSpeedKmh.value = 0f
        _runStepsEstimate.value = 0
        _runCadenceSpm.value = 0
        accumulatedDistanceExactMeters = 0.0
        lastAcceptedGpsPoint = null
        lastAltitudeReference = null
        lastKmSplitTimestampSec = 0

        val anchorLat = if (_currentLat.value != 0.0) _currentLat.value else -23.587420
        val anchorLng = if (_currentLng.value != 0.0) _currentLng.value else -46.657640
        val anchorAlt = if (_currentAltitudeMeters.value != 0.0) _currentAltitudeMeters.value else 762.0

        if (_currentLat.value != 0.0 || _isIndoorSimulationMode.value) {
            _currentLat.value = anchorLat
            _currentLng.value = anchorLng
            _startLat.value = anchorLat
            _startLng.value = anchorLng
            _currentAltitudeMeters.value = anchorAlt
            val initialPoint = GpsTelemetryPoint(
                latitude = anchorLat,
                longitude = anchorLng,
                altitudeMeters = anchorAlt,
                speedMps = 0f,
                accuracyMeters = if (_gpsAccuracyMeters.value > 0f) _gpsAccuracyMeters.value else 3.8f,
                bearingDegrees = 45f,
                provider = if (_isIndoorSimulationMode.value) "SIM-GPS" else _gpsProviderName.value,
                timestampMillis = System.currentTimeMillis(),
                isSimulated = _isIndoorSimulationMode.value
            )
            lastAcceptedGpsPoint = initialPoint
            lastAltitudeReference = anchorAlt
            _gpsTrackPoints.value = listOf(initialPoint)
            recalculateNormalizedRouteProjection()
        } else {
            _gpsTrackPoints.value = emptyList()
            _routePoints.value = emptyList()
        }

        runJob?.cancel()
        runJob = viewModelScope.launch {
            var simStep = 0
            while (_runIsActive.value) {
                delay(1000L)
                if (_runIsPaused.value) {
                    _runPauseDurationSeconds.value += 1
                    _currentSpeedKmh.value = 0f
                    _runInstantPaceSeconds.value = 0
                    _runCadenceSpm.value = 0
                } else {
                    _runDurationSeconds.value += 1
                    if (_isWalkingMode.value) {
                        _walkingSeconds.value += 1
                    } else {
                        _runningSeconds.value += 1
                    }
                    simStep++

                    if (_isIndoorSimulationMode.value) {
                        simulateNextGpsStep(simStep)
                    } else {
                        val dist = _runDistanceMeters.value
                        val dur = _runDurationSeconds.value
                        if (dist >= 5 && dur > 0) {
                            _avgSpeedKmh.value = ((dist.toDouble() / dur) * 3.6).toFloat()
                            _runCurrentPaceSeconds.value = ((dur.toDouble() / dist) * 1000.0).toInt().coerceIn(150, 1200)
                        }
                    }
                }
            }
        }
    }

    /**
     * Alterna o estado principal entre "🏃 Correndo" (verde/ativo) e "🚶 Caminhando" (amarelo/relaxado).
     */
    fun toggleRunWalkMode() {
        if (!_runIsActive.value) return
        if (_runIsPaused.value) {
            _runIsPaused.value = false
        }
        _isWalkingMode.value = !_isWalkingMode.value
        _runToggleCount.value += 1
        soundFeedback.playBeepShort()
    }

    /**
     * Botão "Fim de Treino": para o cronômetro e exibe o resumo (Tempo Total, Tempo Correndo e Tempo Caminhando).
     */
    fun stopRunAndShowSummary() {
        runJob?.cancel()
        runJob = null
        _runIsActive.value = false
        _runIsPaused.value = false
        _currentSpeedKmh.value = 0f
        _runInstantPaceSeconds.value = 0
        _runCadenceSpm.value = 0
        _runSummaryVisible.value = true
        soundFeedback.playTransitionSuccess()
    }

    private fun simulateNextGpsStep(step: Int) {
        val prev = lastAcceptedGpsPoint ?: GpsTelemetryPoint(
            latitude = if (_currentLat.value != 0.0) _currentLat.value else -23.587420,
            longitude = if (_currentLng.value != 0.0) _currentLng.value else -46.657640,
            altitudeMeters = 762.0,
            speedMps = 2.75f,
            accuracyMeters = 3.2f,
            bearingDegrees = 45f,
            provider = "SIM-GPS",
            timestampMillis = System.currentTimeMillis() - 1000L,
            isSimulated = true
        )

        val speedMps = (2.72 + 0.28 * sin(step * 0.12)).toFloat()
        val headingRad = (0.65 + step * 0.022)
        val deltaNorthMeters = speedMps * cos(headingRad)
        val deltaEastMeters = speedMps * sin(headingRad)

        val dLat = deltaNorthMeters / 111_320.0
        val dLng = deltaEastMeters / (111_320.0 * cos(Math.toRadians(prev.latitude)).coerceAtLeast(0.2))

        val newLat = prev.latitude + dLat
        val newLng = prev.longitude + dLng
        val newAlt = prev.altitudeMeters + (0.18 * sin(step * 0.08))

        val simPoint = GpsTelemetryPoint(
            latitude = newLat,
            longitude = newLng,
            altitudeMeters = newAlt,
            speedMps = speedMps,
            accuracyMeters = 3.2f,
            bearingDegrees = ((Math.toDegrees(headingRad) % 360.0 + 360.0) % 360.0).toFloat(),
            provider = "SIM-GPS",
            timestampMillis = System.currentTimeMillis(),
            isSimulated = true
        )

        _currentLat.value = simPoint.latitude
        _currentLng.value = simPoint.longitude
        _currentAltitudeMeters.value = simPoint.altitudeMeters
        _gpsAccuracyMeters.value = simPoint.accuracyMeters
        _gpsProviderName.value = "SIM-GPS"
        _hasGpsFix.value = true
        _gpsStatusLabel.value = "Simulador GPS Ativo (±3.2m)"

        processGpsFixForActiveRun(simPoint)
    }

    fun pauseRun() {
        if (_runIsActive.value && !_runIsPaused.value) {
            _runIsPaused.value = true
            _runPausesCount.value += 1
            _currentSpeedKmh.value = 0f
            _runInstantPaceSeconds.value = 0
            _runCadenceSpm.value = 0
            soundFeedback.playBeepShort()
        }
    }

    fun resumeRun() {
        if (_runIsActive.value && _runIsPaused.value) {
            _runIsPaused.value = false
            soundFeedback.playBeepShort()
        }
    }

    fun finishAndSaveRun(onSaved: () -> Unit) {
        val dist = _runDistanceMeters.value
        val duration = _runDurationSeconds.value.coerceAtLeast(1)
        val pauseDur = _runPauseDurationSeconds.value
        val pace = if (_runCurrentPaceSeconds.value > 0) {
            _runCurrentPaceSeconds.value
        } else if (dist > 0) {
            ((duration.toDouble() / dist) * 1000.0).toInt().coerceIn(150, 1200)
        } else {
            0
        }
        val cals = _runCalories.value.coerceAtLeast(if (dist > 0) 5 else 1)
        val pauses = _runPausesCount.value
        val runSec = _runningSeconds.value
        val walkSec = _walkingSeconds.value
        val toggleCount = _runToggleCount.value
        val totalActive = (runSec + walkSec).coerceAtLeast(1)
        val runPct = ((runSec * 100f) / totalActive).toInt().coerceIn(0, 100)
        val walkPct = (100 - runPct).coerceIn(0, 100)
        val modeSummary = String.format(
            Locale.US,
            "Correndo: %02d:%02d (%d%%) | Caminhando: %02d:%02d (%d%%)",
            runSec / 60, runSec % 60, runPct,
            walkSec / 60, walkSec % 60, walkPct
        )
        val routineMappingText = String.format(
            Locale.US,
            "🏃 Correndo: %02d:%02d (%d%%) • 🚶 Caminhando: %02d:%02d (%d%%) • Alternâncias: %d",
            runSec / 60, runSec % 60, runPct,
            walkSec / 60, walkSec % 60, walkPct,
            toggleCount
        )
        val combinedSplits = (listOf(modeSummary) + _runSplits.value).joinToString(", ")

        val sLat = if (_startLat.value != 0.0) _startLat.value else _currentLat.value
        val sLng = if (_startLng.value != 0.0) _startLng.value else _currentLng.value
        val eLat = _currentLat.value
        val eLng = _currentLng.value
        val avgSpd = _avgSpeedKmh.value
        val maxSpd = _maxSpeedKmh.value
        val elevGain = _elevationGainMeters.value
        val acc = _gpsAccuracyMeters.value
        val steps = _runStepsEstimate.value

        val pts = _gpsTrackPoints.value
        val stepSize = (pts.size / 80).coerceAtLeast(1)
        val sampledCoords = pts.filterIndexed { index, _ -> index % stepSize == 0 || index == pts.lastIndex }
            .joinToString(";") { point ->
                String.format(Locale.US, "%.6f,%.6f", point.latitude, point.longitude)
            }

        val targetDist = settings.value.targetRunDistanceMeters
        val tag = when {
            dist >= targetDist -> "Meta Batida 🎯"
            walkSec > 0 -> "Corrida + Caminhada"
            _isIndoorSimulationMode.value -> "Corrida GPS (Indoor)"
            else -> "Corrida GPS Registrada"
        }

        viewModelScope.launch {
            repository.saveWorkout(
                WorkoutRecord(
                    type = "RUN",
                    title = if (walkSec > 0) "Corrida & Caminhada (GPS)" else "Corrida ao Ar Livre (GPS)",
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = duration,
                    distanceMeters = dist,
                    avgPaceSecondsPerKm = pace,
                    calories = cals,
                    pausesCount = pauses,
                    pauseDurationSeconds = pauseDur,
                    tag = tag,
                    splitsJson = combinedSplits,
                    startLat = sLat,
                    startLng = sLng,
                    endLat = eLat,
                    endLng = eLng,
                    avgSpeedKmh = avgSpd,
                    maxSpeedKmh = maxSpd,
                    elevationGainMeters = elevGain,
                    gpsAccuracyMeters = acc,
                    stepsEstimate = steps,
                    routeCoordinatesJson = sampledCoords,
                    runningSeconds = runSec,
                    walkingSeconds = walkSec,
                    routineDetailsJson = routineMappingText
                )
            )
            resetRunState()
            soundFeedback.playTransitionSuccess()
            onSaved()
        }
    }

    fun cancelRun() {
        resetRunState()
    }

    private fun resetRunState() {
        runJob?.cancel()
        runJob = null
        _runIsActive.value = false
        _runIsPaused.value = false
        _isWalkingMode.value = false
        _runSummaryVisible.value = false
        _runDurationSeconds.value = 0
        _runningSeconds.value = 0
        _walkingSeconds.value = 0
        _runToggleCount.value = 0
        _runPauseDurationSeconds.value = 0
        _runDistanceMeters.value = 0
        _runCurrentPaceSeconds.value = 0
        _runInstantPaceSeconds.value = 0
        _runCalories.value = 0
        _runPausesCount.value = 0
        _runSplits.value = emptyList()
        _elevationGainMeters.value = 0
        _currentSpeedKmh.value = 0f
        _maxSpeedKmh.value = 0f
        _avgSpeedKmh.value = 0f
        _runStepsEstimate.value = 0
        _runCadenceSpm.value = 0
        _gpsTrackPoints.value = emptyList()
        _routePoints.value = emptyList()
        accumulatedDistanceExactMeters = 0.0
        lastAcceptedGpsPoint = null
        lastAltitudeReference = null
        lastKmSplitTimestampSec = 0
    }

    // -------------------------------------------------------------
    // MÓDULO 2: HIIT CARDIO EM CASA (25 MIN) ACTIONS
    // -------------------------------------------------------------
    fun toggleHiitSaveEnabled() {
        _hiitSaveEnabled.value = !_hiitSaveEnabled.value
    }

    fun startHiit(startIndex: Int = 0) {
        val safeIdx = startIndex.coerceIn(0, (hiitExercises.size - 1).coerceAtLeast(0))
        val targetExercise = hiitExercises.getOrNull(safeIdx) ?: return
        if (!_hiitIsActive.value && safeIdx == 0) {
            _hiitCompletedExerciseIds.value = emptySet()
            _hiitSkippedExerciseIds.value = emptySet()
            _hiitTotalElapsedSeconds.value = 0
        }
        _hiitIsActive.value = true
        _hiitIsPaused.value = false
        _hiitExerciseIndex.value = safeIdx
        _hiitIsRest.value = false
        _hiitSecondsLeft.value = targetExercise.durationSeconds
        _hiitCompleted.value = false
        _hiitLastSavedToHistory.value = false

        hiitJob?.cancel()
        hiitJob = viewModelScope.launch {
            while (_hiitIsActive.value && !_hiitCompleted.value) {
                delay(1000L)
                if (!_hiitIsPaused.value) {
                    _hiitTotalElapsedSeconds.value += 1
                    val currentLeft = _hiitSecondsLeft.value - 1
                    _hiitSecondsLeft.value = currentLeft

                    if (currentLeft in 1..3) {
                        soundFeedback.playBeepShort()
                    }

                    if (currentLeft <= 0) {
                        val currentEx = hiitExercises.getOrElse(_hiitExerciseIndex.value) { hiitExercises[0] }
                        if (!_hiitIsRest.value) {
                            // Automatic completion of the current exercise when its timer finishes
                            _hiitCompletedExerciseIds.value = _hiitCompletedExerciseIds.value + currentEx.id
                            _hiitSkippedExerciseIds.value = _hiitSkippedExerciseIds.value - currentEx.id
                            soundFeedback.playTransitionSuccess()

                            val nextIdx = _hiitExerciseIndex.value + 1
                            if (nextIdx >= hiitExercises.size) {
                                completeHiitSession(saveToHistory = _hiitSaveEnabled.value)
                            } else if (currentEx.restSeconds > 0) {
                                // Enter 15s Rest phase where UI highlights the NEXT exercise!
                                _hiitIsRest.value = true
                                _hiitSecondsLeft.value = currentEx.restSeconds
                            } else {
                                // Warmup (restSeconds == 0) jumps straight to next exercise
                                val nextEx = hiitExercises[nextIdx]
                                _hiitExerciseIndex.value = nextIdx
                                _hiitIsRest.value = false
                                _hiitSecondsLeft.value = nextEx.durationSeconds
                            }
                        } else {
                            // Rest finished -> automatically start the next exercise!
                            val nextIdx = _hiitExerciseIndex.value + 1
                            if (nextIdx < hiitExercises.size) {
                                val nextEx = hiitExercises[nextIdx]
                                soundFeedback.playTransitionSuccess()
                                _hiitExerciseIndex.value = nextIdx
                                _hiitIsRest.value = false
                                _hiitSecondsLeft.value = nextEx.durationSeconds
                            } else {
                                completeHiitSession(saveToHistory = _hiitSaveEnabled.value)
                            }
                        }
                    }
                }
            }
        }
    }

    fun addHiitSeconds(extraSeconds: Int = 15) {
        if (_hiitIsActive.value) {
            _hiitSecondsLeft.value = (_hiitSecondsLeft.value + extraSeconds).coerceAtMost(180)
            soundFeedback.playBeepShort()
        }
    }

    fun pauseHiit() {
        if (_hiitIsActive.value && !_hiitIsPaused.value) {
            _hiitIsPaused.value = true
        }
    }

    fun resumeHiit() {
        if (_hiitIsActive.value && _hiitIsPaused.value) {
            _hiitIsPaused.value = false
        }
    }

    fun nextHiitExercise() {
        val currentEx = hiitExercises.getOrElse(_hiitExerciseIndex.value) { hiitExercises[0] }
        if (!_hiitIsRest.value) {
            _hiitCompletedExerciseIds.value = _hiitCompletedExerciseIds.value + currentEx.id
            _hiitSkippedExerciseIds.value = _hiitSkippedExerciseIds.value - currentEx.id
        }

        val nextIdx = _hiitExerciseIndex.value + 1
        if (nextIdx < hiitExercises.size) {
            val nextEx = hiitExercises[nextIdx]
            _hiitExerciseIndex.value = nextIdx
            _hiitIsRest.value = false
            _hiitSecondsLeft.value = nextEx.durationSeconds
            soundFeedback.playTransitionSuccess()
        } else {
            completeHiitSession(saveToHistory = _hiitSaveEnabled.value)
        }
    }

    fun skipHiitExerciseWithoutRecording() {
        val currentEx = hiitExercises.getOrElse(_hiitExerciseIndex.value) { hiitExercises[0] }
        if (!_hiitIsRest.value) {
            _hiitSkippedExerciseIds.value = _hiitSkippedExerciseIds.value + currentEx.id
            _hiitCompletedExerciseIds.value = _hiitCompletedExerciseIds.value - currentEx.id
        }

        val nextIdx = _hiitExerciseIndex.value + 1
        if (nextIdx < hiitExercises.size) {
            val nextEx = hiitExercises[nextIdx]
            _hiitExerciseIndex.value = nextIdx
            _hiitIsRest.value = false
            _hiitSecondsLeft.value = nextEx.durationSeconds
            soundFeedback.playBeepShort()
        } else {
            completeHiitSession(saveToHistory = _hiitSaveEnabled.value)
        }
    }

    fun previousHiitExercise() {
        val prevIdx = (_hiitExerciseIndex.value - 1).coerceAtLeast(0)
        val prevEx = hiitExercises[prevIdx]
        _hiitExerciseIndex.value = prevIdx
        _hiitIsRest.value = false
        _hiitSecondsLeft.value = prevEx.durationSeconds
        soundFeedback.playBeepShort()
    }

    fun completeHiitSession(saveToHistory: Boolean = _hiitSaveEnabled.value) {
        val currentEx = hiitExercises.getOrNull(_hiitExerciseIndex.value)
        if (currentEx != null && !_hiitIsRest.value) {
            val spentOnCurrent = currentEx.durationSeconds - _hiitSecondsLeft.value
            if (spentOnCurrent >= 8 && currentEx.id !in _hiitSkippedExerciseIds.value) {
                _hiitCompletedExerciseIds.value = _hiitCompletedExerciseIds.value + currentEx.id
            }
        }

        _hiitCompleted.value = true
        _hiitIsActive.value = false
        hiitJob?.cancel()
        soundFeedback.playTransitionSuccess()

        val completedCount = _hiitCompletedExerciseIds.value.size
        val shouldPersist = saveToHistory && completedCount > 0
        _hiitLastSavedToHistory.value = shouldPersist

        if (shouldPersist) {
            val duration = _hiitTotalElapsedSeconds.value.coerceAtLeast(completedCount * 25)
            val calories = (completedCount * 10.4f).toInt().coerceAtLeast(15)
            val totalCount = hiitExercises.size
            val tag = if (completedCount >= totalCount) {
                "HIIT 100% 🔥"
            } else {
                "HIIT ($completedCount/$totalCount) ✅"
            }

            val completedIds = _hiitCompletedExerciseIds.value
            val skippedIds = _hiitSkippedExerciseIds.value
            val warmupExs = hiitExercises.filter { it.phaseNumber == 1 }
            val hiitMainExs = hiitExercises.filter { it.phaseNumber == 2 }
            val doneWarmup = warmupExs.count { it.id in completedIds }
            val doneMain = hiitMainExs.count { it.id in completedIds }

            val phaseSummary = "F1 Aquecimento: $doneWarmup/${warmupExs.size} • F2 Cardio HIIT: $doneMain/${hiitMainExs.size}"
            val completedNames = hiitExercises.filter { it.id in completedIds }.joinToString(", ") { it.name }
            val skippedNames = hiitExercises.filter { it.id in skippedIds }.joinToString(", ") { it.name }
            val routineSummary = buildString {
                append(phaseSummary)
                if (completedNames.isNotBlank()) {
                    append("\nConcluídos ✅: $completedNames")
                }
                if (skippedNames.isNotBlank()) {
                    append("\nPulados: $skippedNames")
                }
            }

            viewModelScope.launch {
                repository.saveWorkout(
                    WorkoutRecord(
                        type = "HIIT",
                        title = "HIIT Cardio em Casa ($completedCount/$totalCount Ex.)",
                        timestamp = System.currentTimeMillis(),
                        durationSeconds = duration,
                        distanceMeters = 0,
                        avgPaceSecondsPerKm = 0,
                        calories = calories,
                        tag = tag,
                        exercisesCount = completedCount,
                        routineDetailsJson = routineSummary
                    )
                )
            }
        }
    }

    fun cancelHiit() {
        resetHiit()
    }

    fun resetHiit() {
        hiitJob?.cancel()
        hiitJob = null
        _hiitIsActive.value = false
        _hiitIsPaused.value = false
        _hiitCompleted.value = false
        _hiitExerciseIndex.value = 0
        _hiitIsRest.value = false
        _hiitTotalElapsedSeconds.value = 0
        _hiitCompletedExerciseIds.value = emptySet()
        _hiitSkippedExerciseIds.value = emptySet()
        _hiitLastSavedToHistory.value = false
        _hiitSecondsLeft.value = hiitExercises.firstOrNull()?.durationSeconds ?: 20
    }

    // -------------------------------------------------------------
    // MÓDULO 3: MUSCULAÇÃO COM ELÁSTICO (SÉRIES, REPS & DESCANSO 60s)
    // -------------------------------------------------------------
    private fun ensureElasticElapsedTimerStarted() {
        if (elasticElapsedJob?.isActive == true) return
        elasticElapsedJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _elasticWorkoutElapsedSeconds.value += 1
            }
        }
    }

    fun logElasticSet(exerciseId: Int, reps: Int) {
        ensureElasticElapsedTimerStarted()
        _elasticWorkoutSavedBanner.value = false
        val exercise = elasticExercises.firstOrNull { it.id == exerciseId } ?: return
        val currentList = _elasticCompletedSetsMap.value[exerciseId].orEmpty()
        if (currentList.size >= exercise.targetSets) return

        val updatedList = currentList + reps.coerceIn(1, 50)
        val updatedMap = _elasticCompletedSetsMap.value.toMutableMap().apply {
            put(exerciseId, updatedList)
        }
        _elasticCompletedSetsMap.value = updatedMap

        val finishedThisExercise = updatedList.size >= exercise.targetSets
        if (finishedThisExercise) {
            soundFeedback.playTransitionSuccess()
            // Avança o destaque automaticamente para o próximo exercício pendente
            val nextPending = elasticExercises.firstOrNull { ex ->
                if (ex.isTimedStretch) {
                    !_elasticStretchCompleted.value
                } else {
                    (updatedMap[ex.id]?.size ?: 0) < ex.targetSets
                }
            }
            if (nextPending != null) {
                _elasticActiveExerciseId.value = nextPending.id
                _elasticRestTargetExerciseName.value = "Próximo Exercício: ${nextPending.name} (Série 1/${nextPending.targetSets})"
            } else {
                _elasticRestTargetExerciseName.value = "Todas as Séries Concluídas! Faça o Alongamento Lombar."
            }
        } else {
            soundFeedback.playBeepShort()
            _elasticActiveExerciseId.value = exerciseId
            val nextSetNum = updatedList.size + 1
            _elasticRestTargetExerciseName.value = "${exercise.name} • Próxima: Série $nextSetNum/${exercise.targetSets} (${exercise.targetRepsRange})"
        }

        if (exercise.restSeconds > 0) {
            startElasticRestTimer(exercise.restSeconds)
        }
    }

    fun removeLastElasticSet(exerciseId: Int) {
        val currentList = _elasticCompletedSetsMap.value[exerciseId].orEmpty()
        if (currentList.isEmpty()) return
        val updatedList = currentList.dropLast(1)
        val updatedMap = _elasticCompletedSetsMap.value.toMutableMap()
        if (updatedList.isEmpty()) {
            updatedMap.remove(exerciseId)
        } else {
            updatedMap[exerciseId] = updatedList
        }
        _elasticCompletedSetsMap.value = updatedMap
        _elasticActiveExerciseId.value = exerciseId
        soundFeedback.playBeepShort()
    }

    fun startElasticRestTimer(seconds: Int = 60) {
        elasticRestJob?.cancel()
        _elasticRestSecondsLeft.value = seconds.coerceAtLeast(5)
        elasticRestJob = viewModelScope.launch {
            while (_elasticRestSecondsLeft.value > 0) {
                delay(1000L)
                val left = _elasticRestSecondsLeft.value - 1
                _elasticRestSecondsLeft.value = left
                if (left in 1..3) {
                    soundFeedback.playBeepShort()
                } else if (left == 0) {
                    soundFeedback.playTransitionSuccess()
                }
            }
        }
    }

    fun addElasticRestSeconds(extraSeconds: Int = 15) {
        if (_elasticRestSecondsLeft.value > 0) {
            _elasticRestSecondsLeft.value = (_elasticRestSecondsLeft.value + extraSeconds).coerceAtMost(240)
            soundFeedback.playBeepShort()
        } else {
            startElasticRestTimer(extraSeconds)
        }
    }

    fun skipElasticRest() {
        elasticRestJob?.cancel()
        elasticRestJob = null
        _elasticRestSecondsLeft.value = 0
        soundFeedback.playBeepShort()
    }

    fun toggleElasticStretchTimer() {
        ensureElasticElapsedTimerStarted()
        if (_elasticStretchCompleted.value) return
        if (_elasticStretchRunning.value) {
            _elasticStretchRunning.value = false
            elasticStretchJob?.cancel()
        } else {
            _elasticStretchRunning.value = true
            elasticStretchJob?.cancel()
            elasticStretchJob = viewModelScope.launch {
                while (_elasticStretchRunning.value && _elasticStretchSecondsLeft.value > 0) {
                    delay(1000L)
                    val left = _elasticStretchSecondsLeft.value - 1
                    _elasticStretchSecondsLeft.value = left
                    if (left in 1..3) {
                        soundFeedback.playBeepShort()
                    } else if (left <= 0) {
                        _elasticStretchRunning.value = false
                        _elasticStretchCompleted.value = true
                        soundFeedback.playTransitionSuccess()
                    }
                }
            }
        }
    }

    fun completeElasticStretchNow() {
        ensureElasticElapsedTimerStarted()
        elasticStretchJob?.cancel()
        _elasticStretchRunning.value = false
        _elasticStretchCompleted.value = true
        _elasticStretchSecondsLeft.value = 0
        soundFeedback.playTransitionSuccess()
    }

    fun finishAndSaveElasticWorkout(onSaved: () -> Unit = {}) {
        val setsMap = _elasticCompletedSetsMap.value
        val totalSetsDone = setsMap.values.sumOf { it.size }
        val totalRepsDone = setsMap.values.sumOf { it.sum() }
        val stretchDone = _elasticStretchCompleted.value

        if (totalSetsDone == 0 && !stretchDone) return

        val strengthDoneCount = ElasticStrengthExercises.strengthOnly6.count { ex ->
            (setsMap[ex.id]?.size ?: 0) > 0
        }
        val completedExercisesCount = strengthDoneCount + (if (stretchDone) 1 else 0)
        val duration = _elasticWorkoutElapsedSeconds.value.coerceAtLeast(
            (totalSetsDone * 90) + (if (stretchDone) 600 else 0)
        )
        val calories = ((totalSetsDone * 9.5f) + (if (stretchDone) 35f else 0f)).toInt().coerceAtLeast(25)

        val exerciseBreakdown = ElasticStrengthExercises.strengthOnly6.mapNotNull { ex ->
            val repsList = setsMap[ex.id].orEmpty()
            if (repsList.isNotEmpty()) {
                "${ex.name}: ${repsList.size}x (${repsList.joinToString(",")})"
            } else {
                null
            }
        }

        val routineSummary = buildString {
            append("Séries: $totalSetsDone/${ElasticStrengthExercises.totalTargetSets} • Volume: $totalRepsDone reps")
            if (stretchDone) {
                append(" • Alongamento Lombar: 10 min ✅")
            }
            if (exerciseBreakdown.isNotEmpty()) {
                append("\n")
                append(exerciseBreakdown.joinToString(" • "))
            }
        }

        val tag = if (totalSetsDone >= ElasticStrengthExercises.totalTargetSets && stretchDone) {
            "Treino Completo 100% 💪"
        } else {
            "$totalSetsDone Séries • $totalRepsDone Reps"
        }

        viewModelScope.launch {
            repository.saveWorkout(
                WorkoutRecord(
                    type = "ELASTIC",
                    title = "Musculação c/ Elástico ($totalSetsDone Séries • $totalRepsDone Reps)",
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = duration,
                    distanceMeters = 0,
                    avgPaceSecondsPerKm = 0,
                    calories = calories,
                    tag = tag,
                    exercisesCount = completedExercisesCount,
                    routineDetailsJson = routineSummary
                )
            )
            resetElasticWorkout()
            _elasticWorkoutSavedBanner.value = true
            soundFeedback.playTransitionSuccess()
            onSaved()
        }
    }

    fun resetElasticWorkout() {
        elasticRestJob?.cancel()
        elasticRestJob = null
        elasticStretchJob?.cancel()
        elasticStretchJob = null
        elasticElapsedJob?.cancel()
        elasticElapsedJob = null
        _elasticCompletedSetsMap.value = emptyMap()
        _elasticRestSecondsLeft.value = 0
        _elasticRestTargetExerciseName.value = ""
        _elasticActiveExerciseId.value = 1
        _elasticStretchSecondsLeft.value = 600
        _elasticStretchRunning.value = false
        _elasticStretchCompleted.value = false
        _elasticWorkoutElapsedSeconds.value = 0
    }

    // -------------------------------------------------------------
    // GOALS ACTIONS
    // -------------------------------------------------------------
    fun saveGoals(
        distanceMeters: Int,
        runFrequency: Int,
        calisDays: Int,
        activeCalisDaysCsv: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.updateGoals(
                distanceMeters = distanceMeters,
                runFrequency = runFrequency,
                calisDays = calisDays,
                activeCalisDaysCsv = activeCalisDaysCsv
            )
            soundFeedback.playTransitionSuccess()
            onSuccess()
        }
    }

    fun resetGoalsToDefaults(onReset: () -> Unit) {
        viewModelScope.launch {
            repository.resetGoalsToDefault()
            soundFeedback.playBeepShort()
            onReset()
        }
    }

    // -------------------------------------------------------------
    // BODY EVOLUTION ACTIONS
    // -------------------------------------------------------------
    fun addWeightRecord(weightKg: Float, dateMillis: Long, note: String = "", onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.addWeight(weightKg, dateMillis, note)
            soundFeedback.playTransitionSuccess()
            onSaved()
        }
    }

    fun updateHeight(heightMeters: Float, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.updateHeight(heightMeters)
            soundFeedback.playTransitionSuccess()
            onSaved()
        }
    }

    fun updateTargetWeight(targetWeightKg: Float, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.updateTargetWeight(targetWeightKg)
            soundFeedback.playTransitionSuccess()
            onSaved()
        }
    }

    fun saveUserProfile(
        userName: String,
        heightMeters: Float,
        currentWeightKg: Float,
        targetWeightKg: Float,
        targetRunDistanceMeters: Int,
        targetRunFrequency: Int = 3,
        targetCalisDays: Int = 4,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveInitialProfile(
                userName = userName,
                heightMeters = heightMeters,
                currentWeightKg = currentWeightKg,
                targetWeightKg = targetWeightKg,
                targetRunDistanceMeters = targetRunDistanceMeters,
                targetRunFrequency = targetRunFrequency,
                targetCalisDays = targetCalisDays
            )
            _showProfileSetupModal.value = false
            soundFeedback.playTransitionSuccess()
            onSaved()
        }
    }

    fun resetAllDataAndRestartProfile(onResetComplete: () -> Unit = {}) {
        viewModelScope.launch {
            resetRunState()
            resetHiit()
            resetElasticWorkout()
            repository.resetAllData()
            _showProfileSetupModal.value = true
            soundFeedback.playBeepShort()
            onResetComplete()
        }
    }

    fun deleteWeightRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteWeight(id)
        }
    }

    fun deleteWorkoutRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteWorkout(id)
        }
    }

    // -------------------------------------------------------------
    // EXPORT / BACKUP / RESTORE DATA
    // -------------------------------------------------------------
    fun exportBackup(context: Context) {
        viewModelScope.launch {
            val json = repository.exportDataJson()
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, json)
                putExtra(Intent.EXTRA_TITLE, "BitFit_Backup.json")
                type = "application/json"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Exportar Backup BitFit (SQLite)")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        }
    }

    fun saveBackupToUri(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val json = repository.exportDataJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                    true
                } catch (e: Exception) {
                    false
                }
            }
            if (success) soundFeedback.playTransitionSuccess()
            onResult(success)
        }
    }

    fun importBackupFromUri(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    } ?: return@withContext false
                    repository.importDataJson(json)
                } catch (e: Exception) {
                    false
                }
            }
            if (success) soundFeedback.playTransitionSuccess()
            onResult(success)
        }
    }

    override fun onCleared() {
        super.onCleared()
        gpsTracker.stopTracking()
        soundFeedback.release()
        runJob?.cancel()
        hiitJob?.cancel()
        elasticRestJob?.cancel()
        elasticStretchJob?.cancel()
    }
}

