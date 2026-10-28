package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.WorkoutRecord
import com.example.util.GpsLocationTracker
import com.example.viewmodel.AppScreen
import com.example.viewmodel.BitFitViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BitFit", appName)
    }

    @Test
    fun `bitfit viewmodel initializes cleanly and switches screens`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = BitFitViewModel(application)
        assertNotNull(viewModel)
        assertEquals(AppScreen.DASHBOARD, viewModel.currentScreen.value)

        viewModel.navigateTo(AppScreen.RUN_TRACKER)
        assertEquals(AppScreen.RUN_TRACKER, viewModel.currentScreen.value)

        viewModel.toggleIndoorSimulationMode()
        assertTrue(viewModel.isIndoorSimulationMode.value)
        assertTrue(viewModel.hasGpsFix.value)

        viewModel.openProfileSetupModal()
        assertTrue(viewModel.showProfileSetupModal.value)
        viewModel.closeProfileSetupModal()
        assertFalse(viewModel.showProfileSetupModal.value)
    }

    @Test
    fun `gps distance and bearing calculation is accurate`() {
        val lat1 = -23.58742
        val lng1 = -46.65764
        val lat2 = -23.58291
        val lng2 = -46.65764

        val dist = GpsLocationTracker.distanceBetweenMeters(lat1, lng1, lat2, lng2)
        assertTrue("Expected ~500m north displacement, got $dist", dist in 495.0..508.0)

        val bearing = GpsLocationTracker.bearingBetweenDegrees(lat1, lng1, lat2, lng2)
        val cardinal = GpsLocationTracker.cardinalDirectionLabel(bearing)
        assertEquals("N", cardinal)
    }

    @Test
    fun `workout record parses route coordinates and formats telemetry`() {
        val record = WorkoutRecord(
            type = "RUN",
            title = "Corrida ao Ar Livre (GPS)",
            timestamp = 1730000000000L,
            durationSeconds = 600,
            distanceMeters = 1600,
            avgPaceSecondsPerKm = 375,
            calories = 140,
            startLat = -23.58742,
            startLng = -46.65764,
            endLat = -23.58291,
            endLng = -46.65128,
            avgSpeedKmh = 9.6f,
            maxSpeedKmh = 12.2f,
            elevationGainMeters = 18,
            routeCoordinatesJson = "-23.58742,-46.65764;-23.58500,-46.65400;-23.58291,-46.65128"
        )

        assertTrue(record.hasGpsCoordinates)
        assertEquals(3, record.parsedRoutePoints().size)
        assertEquals("6'15\"/km", record.paceFormatted)
    }
}
