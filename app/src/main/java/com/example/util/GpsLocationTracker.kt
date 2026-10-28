package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GpsTelemetryPoint(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val speedMps: Float,
    val accuracyMeters: Float,
    val bearingDegrees: Float,
    val provider: String,
    val timestampMillis: Long,
    val isSimulated: Boolean = false
)

class GpsLocationTracker(
    context: Context,
    private val onLocationUpdated: (GpsTelemetryPoint) -> Unit,
    private val onProviderStateChanged: (Boolean, String) -> Unit
) {
    private val appContext = context.applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private var isListening = false

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            emitLocation(location)
        }

        override fun onLocationChanged(locations: MutableList<Location>) {
            locations.lastOrNull()?.let { emitLocation(it) }
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            checkProviderStatus()
        }

        override fun onProviderEnabled(provider: String) {
            checkProviderStatus()
        }

        override fun onProviderDisabled(provider: String) {
            checkProviderStatus()
        }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isGpsHardwareEnabled(): Boolean {
        val lm = locationManager ?: return false
        return try {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
    }

    fun checkProviderStatus() {
        if (!hasLocationPermission()) {
            onProviderStateChanged(false, "Permissão de Localização Pendente")
            return
        }
        val lm = locationManager
        if (lm == null) {
            onProviderStateChanged(false, "Serviço de GPS Indisponível")
            return
        }
        val gpsEnabled = runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
        val netEnabled = runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
        when {
            gpsEnabled -> onProviderStateChanged(true, "GPS Satélite Ativo")
            netEnabled -> onProviderStateChanged(true, "Localização via Rede Ativa")
            else -> onProviderStateChanged(false, "GPS Desativado no Aparelho")
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchLastKnownLocation(): GpsTelemetryPoint? {
        if (!hasLocationPermission()) return null
        val lm = locationManager ?: return null

        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(LocationManager.FUSED_PROVIDER)
            }
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }

        var bestLocation: Location? = null
        for (provider in providers) {
            val loc = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
            if (bestLocation == null) {
                bestLocation = loc
            } else {
                val timeDelta = loc.time - bestLocation.time
                val isNewer = timeDelta > 60_000L
                val isMoreAccurate = loc.accuracy > 0f && loc.accuracy < bestLocation.accuracy
                if (isNewer || (timeDelta > -60_000L && isMoreAccurate)) {
                    bestLocation = loc
                }
            }
        }

        return bestLocation?.let { loc ->
            val point = loc.toTelemetryPoint()
            onLocationUpdated(point)
            point
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        checkProviderStatus()
        if (!hasLocationPermission()) return
        val lm = locationManager ?: return

        fetchLastKnownLocation()

        if (isListening) return
        val mainLooper = Looper.getMainLooper()

        // Subscribe to GPS_PROVIDER for high-precision satellite fixes
        runCatching {
            if (lm.allProviders.contains(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    0.5f,
                    locationListener,
                    mainLooper
                )
                isListening = true
            }
        }

        // Subscribe to FUSED_PROVIDER on Android 12+ for fast sensor-fused fixes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                if (lm.allProviders.contains(LocationManager.FUSED_PROVIDER)) {
                    lm.requestLocationUpdates(
                        LocationManager.FUSED_PROVIDER,
                        1000L,
                        0.5f,
                        locationListener,
                        mainLooper
                    )
                    isListening = true
                }
            }
        }

        // Subscribe to NETWORK_PROVIDER as fallback for fast initial lock
        runCatching {
            if (lm.allProviders.contains(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    1.0f,
                    locationListener,
                    mainLooper
                )
                isListening = true
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopTracking() {
        val lm = locationManager ?: return
        if (isListening) {
            runCatching { lm.removeUpdates(locationListener) }
            isListening = false
        }
    }

    private fun emitLocation(location: Location) {
        if (location.latitude == 0.0 && location.longitude == 0.0) return
        onLocationUpdated(location.toTelemetryPoint())
    }

    private fun Location.toTelemetryPoint(): GpsTelemetryPoint {
        return GpsTelemetryPoint(
            latitude = latitude,
            longitude = longitude,
            altitudeMeters = if (hasAltitude()) altitude else 0.0,
            speedMps = if (hasSpeed()) speed.coerceAtLeast(0f) else 0f,
            accuracyMeters = if (hasAccuracy()) accuracy.coerceAtLeast(1f) else 10f,
            bearingDegrees = if (hasBearing()) bearing else 0f,
            provider = (provider ?: "GPS").uppercase(),
            timestampMillis = if (time > 0L) time else System.currentTimeMillis(),
            isSimulated = false
        )
    }

    companion object {
        /**
         * Exact geographic distance in meters using WGS84 Haversine / Android Location formula.
         */
        fun distanceBetweenMeters(
            lat1: Double,
            lng1: Double,
            lat2: Double,
            lng2: Double
        ): Double {
            if (lat1 == lat2 && lng1 == lng2) return 0.0
            val results = FloatArray(1)
            runCatching {
                Location.distanceBetween(lat1, lng1, lat2, lng2, results)
            }
            if (results[0] > 0f && !results[0].isNaN()) {
                return results[0].toDouble()
            }
            // Pure Haversine formula fallback
            val earthRadiusMeters = 6_371_000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2.0)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return earthRadiusMeters * c
        }

        /**
         * Initial compass bearing (0..360 degrees) from (lat1, lng1) to (lat2, lng2).
         */
        fun bearingBetweenDegrees(
            lat1: Double,
            lng1: Double,
            lat2: Double,
            lng2: Double
        ): Float {
            if (lat1 == lat2 && lng1 == lng2) return 0f
            val phi1 = Math.toRadians(lat1)
            val phi2 = Math.toRadians(lat2)
            val deltaLambda = Math.toRadians(lng2 - lng1)
            val y = sin(deltaLambda) * cos(phi2)
            val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
            val theta = Math.toDegrees(atan2(y, x))
            return ((theta + 360.0) % 360.0).toFloat()
        }

        fun cardinalDirectionLabel(bearingDegrees: Float): String {
            val normalized = ((bearingDegrees % 360f) + 360f) % 360f
            return when {
                normalized < 22.5f || normalized >= 337.5f -> "N"
                normalized < 67.5f -> "NE"
                normalized < 112.5f -> "L"
                normalized < 157.5f -> "SE"
                normalized < 202.5f -> "S"
                normalized < 247.5f -> "SO"
                normalized < 292.5f -> "O"
                else -> "NO"
            }
        }
    }
}
