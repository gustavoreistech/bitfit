package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BitFitBottomNav
import com.example.ui.screens.BodyEvolutionScreen
import com.example.ui.screens.CalisthenicsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.GpsRunScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.theme.BgDark
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.BitFitViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                BitFitApp()
            }
        }
    }
}

@Composable
fun BitFitApp(viewModel: BitFitViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isSettingsLoaded by viewModel.isSettingsLoaded.collectAsState()
    val showProfileSetupModal by viewModel.showProfileSetupModal.collectAsState()

    val needsInitialSetup = isSettingsLoaded && !settings.isProfileConfigured
    val showOnboardingOrEdit = needsInitialSetup || showProfileSetupModal

    BackHandler(enabled = showProfileSetupModal && settings.isProfileConfigured) {
        viewModel.closeProfileSetupModal()
    }

    BackHandler(enabled = !showOnboardingOrEdit && currentScreen != AppScreen.DASHBOARD) {
        viewModel.navigateTo(AppScreen.DASHBOARD)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BgDark,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!showOnboardingOrEdit) {
                BitFitBottomNav(
                    currentScreen = currentScreen,
                    onNavigate = { screen ->
                        viewModel.navigateTo(screen)
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .statusBarsPadding()
                .padding(innerPadding)
        ) {
            if (showOnboardingOrEdit) {
                ProfileSetupScreen(
                    viewModel = viewModel,
                    isEditingExisting = settings.isProfileConfigured,
                    onDismiss = { viewModel.closeProfileSetupModal() }
                )
            } else {
                when (currentScreen) {
                    AppScreen.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                    AppScreen.RUN_TRACKER -> GpsRunScreen(
                        viewModel = viewModel,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                    AppScreen.CALISTHENICS -> CalisthenicsScreen(
                        viewModel = viewModel,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                    AppScreen.GOALS -> GoalsScreen(
                        viewModel = viewModel,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                    AppScreen.BODY_EVOLUTION -> BodyEvolutionScreen(
                        viewModel = viewModel,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                    AppScreen.HISTORY -> HistoryScreen(
                        viewModel = viewModel,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                }
            }
        }
    }
}
