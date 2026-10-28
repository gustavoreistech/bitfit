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
import com.example.data.model.CalisthenicsExercises
import com.example.data.model.Exercise
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
    CALISTHENICS,
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
    // CALISTHENICS FOLLOW-ALONG (20 EXERCÍCIOS • ~20 MIN)
    // -------------------------------------------------------------
    private var calisJob: Job? = null
    val calisExercises: List<Exercise> = CalisthenicsExercises.routine20

    private val _calisIsActive = MutableStateFlow(false)
    val calisIsActive: StateFlow<Boolean> = _calisIsActive.asStateFlow()

    private val _calisIsPaused = MutableStateFlow(false)
    val calisIsPaused: StateFlow<Boolean> = _calisIsPaused.asStateFlow()

    private val _calisExerciseIndex = MutableStateFlow(0)
    val calisExerciseIndex: StateFlow<Int> = _calisExerciseIndex.asStateFlow()

    private val _calisIsRest = MutableStateFlow(false)
    val calisIsRest: StateFlow<Boolean> = _calisIsRest.asStateFlow()

    private val _calisSecondsLeft = MutableStateFlow(45)
    val calisSecondsLeft: StateFlow<Int> = _calisSecondsLeft.asStateFlow()

    private val _calisTotalElapsedSeconds = MutableStateFlow(0)
    val calisTotalElapsedSeconds: StateFlow<Int> = _calisTotalElapsedSeconds.asStateFlow()

    private val _calisCompleted = MutableStateFlow(false)
    val calisCompleted: StateFlow<Boolean> = _calisCompleted.asStateFlow()

    private val _calisCompletedExerciseIds = MutableStateFlow<Set<Int>>(emptySet())
    val calisCompletedExerciseIds: StateFlow<Set<Int>> = _calisCompletedExerciseIds.asStateFlow()

    private val _calisSkippedExerciseIds = MutableStateFlow<Set<Int>>(emptySet())
    val calisSkippedExerciseIds: StateFlow<Set<Int>> = _calisSkippedExerciseIds.asStateFlow()

    private val _calisSaveEnabled = MutableStateFlow(true)
    val calisSaveEnabled: StateFlow<Boolean> = _calisSaveEnabled.asStateFlow()

    private val _calisLastSavedToHistory = MutableStateFlow(false)
    val calisLastSavedToHistory: StateFlow<Boolean> = _calisLastSavedToHistory.asStateFlow()

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

    fun toggleCalisSaveEnabled() {
        _calisSaveEnabled.value = !_calisSaveEnabled.value
    }

    fun startCalisthenics(startIndex: Int = 0) {
        val safeIdx = startIndex.coerceIn(0, (calisExercises.size - 1).coerceAtLeast(0))
        val targetExercise = calisExercises.getOrNull(safeIdx) ?: return
        if (!_calisIsActive.value && safeIdx == 0) {
            _calisCompletedExerciseIds.value = emptySet()
            _calisSkippedExerciseIds.value = emptySet()
            _calisTotalElapsedSeconds.value = 0
        }
        _calisIsActive.value = true
        _calisIsPaused.value = false
        _calisExerciseIndex.value = safeIdx
        _calisIsRest.value = false
        _calisSecondsLeft.value = targetExercise.durationSeconds
        _calisCompleted.value = false
        _calisLastSavedToHistory.value = false

        calisJob?.cancel()
        calisJob = viewModelScope.launch {
            while (_calisIsActive.value && !_calisCompleted.value) {
                delay(1000L)
                if (!_calisIsPaused.value) {
                    _calisTotalElapsedSeconds.value += 1
                    val currentLeft = _calisSecondsLeft.value - 1
                    _calisSecondsLeft.value = currentLeft

                    if (currentLeft in 1..3) {
                        soundFeedback.playBeepShort()
                    }

                    if (currentLeft <= 0) {
                        val currentEx = calisExercises.getOrElse(_calisExerciseIndex.value) { calisExercises[0] }
                        if (!_calisIsRest.value) {
                            // Automatic completion of the current exercise when its timer finishes!
                            _calisCompletedExerciseIds.value = _calisCompletedExerciseIds.value + currentEx.id
                            _calisSkippedExerciseIds.value = _calisSkippedExerciseIds.value - currentEx.id
                            soundFeedback.playTransitionSuccess()

                            val nextIdx = _calisExerciseIndex.value + 1
                            if (nextIdx >= calisExercises.size) {
                                // Last exercise completed automatically
                                completeCalisthenicsSession(saveToHistory = _calisSaveEnabled.value)
                            } else if (currentEx.restSeconds > 0) {
                                // Enter Rest phase before automatically jumping to the next exercise
                                _calisIsRest.value = true
                                _calisSecondsLeft.value = currentEx.restSeconds
                            } else {
                                val nextEx = calisExercises[nextIdx]
                                _calisExerciseIndex.value = nextIdx
                                _calisIsRest.value = false
                                _calisSecondsLeft.value = nextEx.durationSeconds
                            }
                        } else {
                            // Rest finished -> automatically jump to the next exercise!
                            val nextIdx = _calisExerciseIndex.value + 1
                            if (nextIdx < calisExercises.size) {
                                val nextEx = calisExercises[nextIdx]
                                soundFeedback.playTransitionSuccess()
                                _calisExerciseIndex.value = nextIdx
                                _calisIsRest.value = false
                                _calisSecondsLeft.value = nextEx.durationSeconds
                            } else {
                                completeCalisthenicsSession(saveToHistory = _calisSaveEnabled.value)
                            }
                        }
                    }
                }
            }
        }
    }

    fun addCalisthenicsSeconds(extraSeconds: Int = 15) {
        if (_calisIsActive.value) {
            _calisSecondsLeft.value = (_calisSecondsLeft.value + extraSeconds).coerceAtMost(180)
            soundFeedback.playBeepShort()
        }
    }

    fun pauseCalisthenics() {
        if (_calisIsActive.value && !_calisIsPaused.value) {
            _calisIsPaused.value = true
        }
    }

    fun resumeCalisthenics() {
        if (_calisIsActive.value && _calisIsPaused.value) {
            _calisIsPaused.value = false
        }
    }

    /**
     * Marks current exercise as completed with success and advances to the next exercise.
     */
    fun nextExercise() {
        val currentEx = calisExercises.getOrElse(_calisExerciseIndex.value) { calisExercises[0] }
        _calisCompletedExerciseIds.value = _calisCompletedExerciseIds.value + currentEx.id
        _calisSkippedExerciseIds.value = _calisSkippedExerciseIds.value - currentEx.id

        val nextIdx = _calisExerciseIndex.value + 1
        if (nextIdx < calisExercises.size) {
            val nextEx = calisExercises[nextIdx]
            _calisExerciseIndex.value = nextIdx
            _calisIsRest.value = false
            _calisSecondsLeft.value = nextEx.durationSeconds
            soundFeedback.playTransitionSuccess()
        } else {
            completeCalisthenicsSession(saveToHistory = _calisSaveEnabled.value)
        }
    }

    /**
     * Skips the current exercise without marking it as completed (for when an exercise cannot be done).
     */
    fun skipExerciseWithoutRecording() {
        val currentEx = calisExercises.getOrElse(_calisExerciseIndex.value) { calisExercises[0] }
        _calisSkippedExerciseIds.value = _calisSkippedExerciseIds.value + currentEx.id
        _calisCompletedExerciseIds.value = _calisCompletedExerciseIds.value - currentEx.id

        val nextIdx = _calisExerciseIndex.value + 1
        if (nextIdx < calisExercises.size) {
            val nextEx = calisExercises[nextIdx]
            _calisExerciseIndex.value = nextIdx
            _calisIsRest.value = false
            _calisSecondsLeft.value = nextEx.durationSeconds
            soundFeedback.playBeepShort()
        } else {
            completeCalisthenicsSession(saveToHistory = _calisSaveEnabled.value)
        }
    }

    fun previousExercise() {
        val prevIdx = (_calisExerciseIndex.value - 1).coerceAtLeast(0)
        val prevEx = calisExercises[prevIdx]
        _calisExerciseIndex.value = prevIdx
        _calisIsRest.value = false
        _calisSecondsLeft.value = prevEx.durationSeconds
        soundFeedback.playBeepShort()
    }

    fun completeCalisthenicsSession(saveToHistory: Boolean = _calisSaveEnabled.value) {
        // If user clicks Finish during an exercise that ran for at least 10s, count it as well
        val currentEx = calisExercises.getOrNull(_calisExerciseIndex.value)
        if (currentEx != null && !_calisIsRest.value) {
            val spentOnCurrent = currentEx.durationSeconds - _calisSecondsLeft.value
            if (spentOnCurrent >= 10 && currentEx.id !in _calisSkippedExerciseIds.value) {
                _calisCompletedExerciseIds.value = _calisCompletedExerciseIds.value + currentEx.id
            }
        }

        _calisCompleted.value = true
        _calisIsActive.value = false
        calisJob?.cancel()
        soundFeedback.playTransitionSuccess()

        val completedCount = _calisCompletedExerciseIds.value.size
        val shouldPersist = saveToHistory && completedCount > 0
        _calisLastSavedToHistory.value = shouldPersist

        if (shouldPersist) {
            val duration = _calisTotalElapsedSeconds.value.coerceAtLeast(completedCount * 30)
            val calories = (completedCount * 10.5f).toInt().coerceAtLeast(12)
            val totalCount = calisExercises.size
            val tag = if (completedCount >= totalCount) {
                "Concluído 100% ⭐"
            } else {
                "Concluído ($completedCount/$totalCount) ✅"
            }

            val completedIds = _calisCompletedExerciseIds.value
            val skippedIds = _calisSkippedExerciseIds.value
            val phaseLabels = mapOf(
                1 to "F1 Mobilidade",
                2 to "F2 Força",
                3 to "F3 Core",
                4 to "F4 Finisher"
            )
            val phaseSummary = (1..4).joinToString(" • ") { phaseNum ->
                val phaseExs = calisExercises.filter { it.phaseNumber == phaseNum }
                val doneInPhase = phaseExs.count { it.id in completedIds }
                "${phaseLabels[phaseNum]}: $doneInPhase/${phaseExs.size}"
            }
            val completedNames = calisExercises.filter { it.id in completedIds }.joinToString(", ") { it.name }
            val skippedNames = calisExercises.filter { it.id in skippedIds }.joinToString(", ") { it.name }
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
                        type = "CALISTHENICS",
                        title = "Calistenia ($completedCount/$totalCount Exercícios)",
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

    /**
     * Cancels and discards the current Calisthenics session without saving to SQLite (matches cancelRun).
     */
    fun cancelCalisthenics() {
        resetCalisthenics()
    }

    fun resetCalisthenics() {
        calisJob?.cancel()
        calisJob = null
        _calisIsActive.value = false
        _calisIsPaused.value = false
        _calisCompleted.value = false
        _calisExerciseIndex.value = 0
        _calisIsRest.value = false
        _calisTotalElapsedSeconds.value = 0
        _calisCompletedExerciseIds.value = emptySet()
        _calisSkippedExerciseIds.value = emptySet()
        _calisLastSavedToHistory.value = false
        _calisSecondsLeft.value = calisExercises.firstOrNull()?.durationSeconds ?: 45
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
            resetCalisthenics()
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
        calisJob?.cancel()
    }
}
