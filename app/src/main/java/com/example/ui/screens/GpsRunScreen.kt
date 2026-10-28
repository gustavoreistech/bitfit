package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BitFitTopAppBar
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonBadge
import com.example.ui.theme.OnPrimaryContainer
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.TertiaryOrange
import com.example.viewmodel.AppScreen
import com.example.viewmodel.BitFitViewModel
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GpsRunScreen(
    viewModel: BitFitViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val isActive by viewModel.runIsActive.collectAsState()
    val isPaused by viewModel.runIsPaused.collectAsState()
    val isWalkingMode by viewModel.isWalkingMode.collectAsState()
    val durationSeconds by viewModel.runDurationSeconds.collectAsState()
    val runningSeconds by viewModel.runningSeconds.collectAsState()
    val walkingSeconds by viewModel.walkingSeconds.collectAsState()
    val runSummaryVisible by viewModel.runSummaryVisible.collectAsState()
    val pauseDurationSeconds by viewModel.runPauseDurationSeconds.collectAsState()
    val pausesCount by viewModel.runPausesCount.collectAsState()
    val distanceMeters by viewModel.runDistanceMeters.collectAsState()
    val avgPaceSeconds by viewModel.runCurrentPaceSeconds.collectAsState()
    val instantPaceSeconds by viewModel.runInstantPaceSeconds.collectAsState()
    val calories by viewModel.runCalories.collectAsState()
    val splits by viewModel.runSplits.collectAsState()
    val routePoints by viewModel.routePoints.collectAsState()
    val gpsTrackPoints by viewModel.gpsTrackPoints.collectAsState()

    // Real GPS Hardware Telemetry
    val gpsPermissionGranted by viewModel.gpsPermissionGranted.collectAsState()
    val hasGpsFix by viewModel.hasGpsFix.collectAsState()
    val gpsStatusLabel by viewModel.gpsStatusLabel.collectAsState()
    val gpsProviderName by viewModel.gpsProviderName.collectAsState()
    val isIndoorSim by viewModel.isIndoorSimulationMode.collectAsState()
    val currentLat by viewModel.currentLat.collectAsState()
    val currentLng by viewModel.currentLng.collectAsState()
    val startLat by viewModel.startLat.collectAsState()
    val startLng by viewModel.startLng.collectAsState()
    val gpsAccuracyMeters by viewModel.gpsAccuracyMeters.collectAsState()
    val currentAltitude by viewModel.currentAltitudeMeters.collectAsState()
    val elevationGain by viewModel.elevationGainMeters.collectAsState()
    val currentSpeedKmh by viewModel.currentSpeedKmh.collectAsState()
    val maxSpeedKmh by viewModel.maxSpeedKmh.collectAsState()
    val avgSpeedKmh by viewModel.avgSpeedKmh.collectAsState()
    val compassBearing by viewModel.compassBearingDegrees.collectAsState()
    val compassDirection by viewModel.compassDirectionLabel.collectAsState()
    val stepsEstimate by viewModel.runStepsEstimate.collectAsState()
    val cadenceSpm by viewModel.runCadenceSpm.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshGpsPermissionAndPassiveFix()
        if (!viewModel.gpsPermissionGranted.value) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    var showCancelDialog by remember { mutableStateOf(false) }

    val targetDistance = settings.targetRunDistanceMeters.coerceAtLeast(100)
    val progressRatio = (distanceMeters.toFloat() / targetDistance).coerceIn(0f, 1f)

    val mins = durationSeconds / 60
    val secs = durationSeconds % 60
    val durationText = "${mins}m ${String.format(Locale.US, "%02d", secs)}s"

    val avgPaceText = if (avgPaceSeconds > 0) {
        "${avgPaceSeconds / 60}'${String.format(Locale.US, "%02d", avgPaceSeconds % 60)}\"/km"
    } else {
        "--'--\"/km"
    }

    val instantPaceText = if (instantPaceSeconds > 0) {
        "${instantPaceSeconds / 60}'${String.format(Locale.US, "%02d", instantPaceSeconds % 60)}\"/km"
    } else {
        "--'--\"/km"
    }

    val latFormatted = if (currentLat != 0.0) {
        String.format(Locale.US, "%.6f°", currentLat)
    } else {
        "Buscando satélite..."
    }
    val lngFormatted = if (currentLng != 0.0) {
        String.format(Locale.US, "%.6f°", currentLng)
    } else {
        "Buscando satélite..."
    }

    val totalClockFormatted = String.format(Locale.US, "%02d:%02d", durationSeconds / 60, durationSeconds % 60)
    val runningClockFormatted = String.format(Locale.US, "%02d:%02d", runningSeconds / 60, runningSeconds % 60)
    val walkingClockFormatted = String.format(Locale.US, "%02d:%02d", walkingSeconds / 60, walkingSeconds % 60)
    val walkingYellow = Color(0xFFFFCA28)
    val stopRed = Color(0xFFFF5252)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        BitFitTopAppBar(
            title = "CORRIDA & CAMINHADA",
            subtitle = if (isActive) {
                if (isWalkingMode) "ESTADO ATUAL: 🚶 CAMINHANDO" else "ESTADO ATUAL: 🏃 CORRENDO"
            } else {
                "CRONÔMETRO DUPLO + TELEMETRIA GPS"
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // CARD PRINCIPAL: CRONÔMETRO CENTRAL (TEMPO TOTAL) + TOGGLE GIGANTE CORRENDO / CAMINHANDO + RESUMO
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp,
                borderColor = when {
                    runSummaryVisible -> SecondaryCyan.copy(alpha = 0.6f)
                    isActive && isWalkingMode -> walkingYellow.copy(alpha = 0.7f)
                    isActive -> PrimaryNeon.copy(alpha = 0.7f)
                    else -> OutlineVariant.copy(alpha = 0.25f)
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (runSummaryVisible) {
                        // TELA DE RESUMO PÓS-"FIM DE TREINO"
                        NeonBadge(text = "RESUMO DO TREINO FINALIZADO", color = SecondaryCyan)

                        Text(
                            text = "Treino Encerrado! 🏁",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnSurface
                        )

                        // Tempo Total em destaque
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TEMPO TOTAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = totalClockFormatted,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnSurface
                            )
                        }

                        // Comparativo Tempo Correndo vs Tempo Caminhando
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(PrimaryNeon.copy(alpha = 0.14f))
                                    .border(1.dp, PrimaryNeon.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🏃 TEMPO CORRENDO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryNeon
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = runningClockFormatted,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryNeon
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(walkingYellow.copy(alpha = 0.14f))
                                    .border(1.dp, walkingYellow.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🚶 TEMPO CAMINHANDO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = walkingYellow
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = walkingClockFormatted,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = walkingYellow
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.cancelRun() },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Descartar", color = OnSurfaceVariant, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.finishAndSaveRun {
                                        onNavigate(AppScreen.HISTORY)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryNeon,
                                    contentColor = OnPrimaryContainer
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .testTag("btn_save_summary_run")
                                    .weight(1.3f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Salvar",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Salvar no Histórico", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            }
                        }
                    } else {
                        // CRONÔMETRO PRINCIPAL (TEMPO TOTAL)
                        Text(
                            text = "CRONÔMETRO PRINCIPAL • TEMPO TOTAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = totalClockFormatted,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnSurface,
                            letterSpacing = (-1).sp
                        )

                        // Contadores em Segundo Plano: Correndo (Verde) vs Caminhando (Amarelo)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isActive && !isWalkingMode) PrimaryNeon.copy(alpha = 0.18f)
                                        else SurfaceContainerLowest
                                    )
                                    .border(
                                        1.dp,
                                        if (isActive && !isWalkingMode) PrimaryNeon else OutlineVariant.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🏃 Correndo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNeon
                                )
                                Text(
                                    text = runningClockFormatted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryNeon
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isActive && isWalkingMode) walkingYellow.copy(alpha = 0.18f)
                                        else SurfaceContainerLowest
                                    )
                                    .border(
                                        1.dp,
                                        if (isActive && isWalkingMode) walkingYellow else OutlineVariant.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🚶 Caminhando",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = walkingYellow
                                )
                                Text(
                                    text = walkingClockFormatted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = walkingYellow
                                )
                            }
                        }

                        // BOTÃO DE AÇÃO PRINCIPAL (TOGGLE GRANDE PENSADO PARA USUÁRIO EM MOVIMENTO)
                        if (isActive) {
                            Button(
                                onClick = { viewModel.toggleRunWalkMode() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isWalkingMode) walkingYellow else PrimaryNeon,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .testTag("btn_toggle_run_walk")
                                    .fillMaxWidth()
                                    .height(68.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isWalkingMode) "🚶 CAMINHANDO" else "🏃 CORRENDO",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = if (isWalkingMode) {
                                            "Toque aqui para voltar a 🏃 CORRER"
                                        } else {
                                            "Toque aqui para mudar para 🚶 CAMINHANDO"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black.copy(alpha = 0.78f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // 1. GPS Status & Mode Selector Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isActive && !isPaused -> PrimaryNeon
                                            hasGpsFix || isIndoorSim -> SecondaryCyan
                                            else -> TertiaryOrange
                                        }
                                    )
                            )
                            Column {
                                Text(
                                    text = gpsStatusLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                                Text(
                                    text = if (isIndoorSim) {
                                        "Coordenadas dinâmicas ativas para treino indoor/emulador"
                                    } else if (gpsPermissionGranted) {
                                        "Receptor ${gpsProviderName} conectado ao hardware Android"
                                    } else {
                                        "Conceda acesso ao GPS ou use o Modo Simulador Indoor"
                                    },
                                    fontSize = 10.sp,
                                    color = OnSurfaceVariant
                                )
                            }
                        }

                        NeonBadge(
                            text = "Meta: ${targetDistance}m",
                            color = SecondaryCyan
                        )
                    }

                    // Mode Toggle Row: GPS Real vs Modo Indoor/Teste + Permission Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pill 1: GPS Satélite Real
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (!isIndoorSim) PrimaryNeon.copy(alpha = 0.18f) else SurfaceContainerHighest
                                )
                                .border(
                                    1.dp,
                                    if (!isIndoorSim) PrimaryNeon else OutlineVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (isIndoorSim) viewModel.toggleIndoorSimulationMode()
                                    if (!gpsPermissionGranted) {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GpsFixed,
                                    contentDescription = "GPS Real",
                                    tint = if (!isIndoorSim) PrimaryNeon else OnSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "GPS Satélite Real",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isIndoorSim) PrimaryNeon else OnSurfaceVariant
                                )
                            }
                        }

                        // Pill 2: Modo Simulador Indoor / Teste
                        Box(
                            modifier = Modifier
                                .testTag("btn_toggle_indoor_gps")
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isIndoorSim) SecondaryCyan.copy(alpha = 0.2f) else SurfaceContainerHighest
                                )
                                .border(
                                    1.dp,
                                    if (isIndoorSim) SecondaryCyan else OutlineVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.toggleIndoorSimulationMode() }
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsWalk,
                                    contentDescription = "Modo Indoor",
                                    tint = if (isIndoorSim) SecondaryCyan else OnSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Modo Indoor / Teste",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIndoorSim) SecondaryCyan else OnSurfaceVariant
                                )
                            }
                        }
                    }

                    if (!gpsPermissionGranted && !isIndoorSim) {
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecondaryCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Permitir GPS",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AUTORIZAR LOCALIZAÇÃO EXATA DO APARELHO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Real-Time Exact GPS Coordinates & Satellite Telemetry Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Coordenadas",
                                tint = PrimaryNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LOCALIZAÇÃO EXATA (WGS84)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Text(
                            text = if (gpsAccuracyMeters > 0f) {
                                String.format(Locale.US, "Precisão ±%.1f m", gpsAccuracyMeters)
                            } else {
                                "Provedor: $gpsProviderName"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNeon
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Latitude Box
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceContainerLowest)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "LATITUDE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceVariant
                            )
                            Text(
                                text = latFormatted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryCyan
                            )
                        }

                        // Longitude Box
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceContainerLowest)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "LONGITUDE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceVariant
                            )
                            Text(
                                text = lngFormatted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryCyan
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terrain,
                                contentDescription = "Altitude",
                                tint = TertiaryOrange,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Altitude: ${currentAltitude.toInt()}m (Ganho +${elevationGain}m)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Bússola",
                                tint = PrimaryNeon,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Rumo: $compassDirection (${compassBearing.toInt()}°)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Big Metric Visualizer: Distance Display & Goal Progress
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "DISTÂNCIA REAL PERCORRIDA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = String.format(Locale.US, "%,d", distanceMeters),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface,
                            letterSpacing = (-1.5).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "m",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryCyan,
                            modifier = Modifier.padding(bottom = 7.dp)
                        )
                    }

                    Text(
                        text = String.format(
                            Locale.US,
                            "%.2f km • Vel. Média %.1f km/h",
                            distanceMeters / 1000f,
                            avgSpeedKmh
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryCyan
                    )

                    // Target Distance Progress Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = if (progressRatio >= 1f) PrimaryNeon else SecondaryCyan,
                            trackColor = SurfaceContainerLowest
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Faltam ${(targetDistance - distanceMeters).coerceAtLeast(0)}m p/ meta",
                                fontSize = 10.sp,
                                color = OnSurfaceVariant
                            )
                            Text(
                                text = "${(progressRatio * 100).toInt()}% (${targetDistance}m)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (progressRatio >= 1f) PrimaryNeon else SecondaryCyan
                            )
                        }
                    }
                }
            }

            // 4. Telemetry Bento Grid (Row 1: Tempo, Velocidade, Pace Médio)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Tempo Ativo
                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Tempo",
                                tint = SecondaryCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "TEMPO ATIVO",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceVariant
                            )
                        }
                        Text(
                            text = durationText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                        Text(
                            text = if (pausesCount > 0) "$pausesCount pausas (${pauseDurationSeconds}s)" else "Sem pausas",
                            fontSize = 9.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }

                // Card 2: Velocidade (Atual & Máxima)
                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Velocidade",
                                tint = PrimaryNeon,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "VELOCIDADE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceVariant
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "%.1f km/h", currentSpeedKmh),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNeon
                        )
                        Text(
                            text = String.format(Locale.US, "Pico: %.1f km/h", maxSpeedKmh),
                            fontSize = 9.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }

                // Card 3: Pace Médio & Atual
                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Pace",
                                tint = SecondaryCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "RITMO / PACE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceVariant
                            )
                        }
                        Text(
                            text = avgPaceText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryCyan
                        )
                        Text(
                            text = "Atual: $instantPaceText",
                            fontSize = 9.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            // 5. Telemetry Bento Grid (Row 2: Passos, Cadência, Calorias)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 4: Passos Estimados
                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = "Passos",
                                tint = PrimaryNeon,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PASSOS",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceVariant
                            )
                        }
                        Text(
                            text = "$stepsEstimate",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                        Text(
                            text = "Cadência: $cadenceSpm spm",
                            fontSize = 9.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }

                // Card 5: Ganho de Elevação
                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terrain,
                                contentDescription = "Altimetria",
                                tint = SecondaryCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ALTIMETRIA",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceVariant
                            )
                        }
                        Text(
                            text = "+${elevationGain} m",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryCyan
                        )
                        Text(
                            text = "Alt: ${currentAltitude.toInt()} m",
                            fontSize = 9.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }

                // Card 6: Calorias Queimadas
                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Calorias",
                                tint = TertiaryOrange,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "GASTO ATIVO",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceVariant
                            )
                        }
                        Text(
                            text = "$calories kcal",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TertiaryOrange
                        )
                        Text(
                            text = "MET Dinâmico",
                            fontSize = 9.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            // 6. Real Coordinate Trajectory Map (Canvas)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = "Traçado GPS",
                                tint = SecondaryCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mapa Vetorial da Rota GPS",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        }
                        Text(
                            text = "${gpsTrackPoints.size} pts gravados",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNeon
                        )
                    }

                    // Route Canvas with Compass & Real Projected Points
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLowest)
                            .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Tactical coordinate grid
                            val gridCols = 4
                            val gridRows = 3
                            for (c in 1 until gridCols) {
                                val gx = (w / gridCols) * c
                                drawLine(
                                    color = OutlineVariant.copy(alpha = 0.16f),
                                    start = Offset(gx, 0f),
                                    end = Offset(gx, h),
                                    strokeWidth = 1f
                                )
                            }
                            for (r in 1 until gridRows) {
                                val gy = (h / gridRows) * r
                                drawLine(
                                    color = OutlineVariant.copy(alpha = 0.16f),
                                    start = Offset(0f, gy),
                                    end = Offset(w, gy),
                                    strokeWidth = 1f
                                )
                            }

                            if (routePoints.size > 1) {
                                val path = Path()
                                routePoints.forEachIndexed { idx, pt ->
                                    val x = pt.first * w
                                    val y = pt.second * h
                                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                // Outer glow path
                                drawPath(
                                    path = path,
                                    color = PrimaryNeon.copy(alpha = 0.25f),
                                    style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                                // Core GPS polyline
                                drawPath(
                                    path = path,
                                    color = PrimaryNeon,
                                    style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )

                                // Start coordinate pin (Cyan)
                                val startX = routePoints.first().first * w
                                val startY = routePoints.first().second * h
                                drawCircle(color = SecondaryCyan.copy(alpha = 0.35f), radius = 11f, center = Offset(startX, startY))
                                drawCircle(color = SecondaryCyan, radius = 5.5f, center = Offset(startX, startY))

                                // Current runner position + heading indicator
                                val lastX = routePoints.last().first * w
                                val lastY = routePoints.last().second * h
                                val rad = Math.toRadians((compassBearing - 90f).toDouble())
                                val dirX = lastX + (cos(rad) * 16f).toFloat()
                                val dirY = lastY + (sin(rad) * 16f).toFloat()
                                drawLine(
                                    color = PrimaryNeon,
                                    start = Offset(lastX, lastY),
                                    end = Offset(dirX, dirY),
                                    strokeWidth = 3f,
                                    cap = StrokeCap.Round
                                )
                                drawCircle(color = PrimaryNeon.copy(alpha = 0.3f), radius = 14f, center = Offset(lastX, lastY))
                                drawCircle(color = PrimaryNeon, radius = 7.5f, center = Offset(lastX, lastY))
                                drawCircle(color = Color.White, radius = 3.5f, center = Offset(lastX, lastY))
                            } else {
                                val centerX = w * 0.5f
                                val centerY = h * 0.5f
                                drawCircle(color = SecondaryCyan.copy(alpha = 0.18f), radius = 32f, center = Offset(centerX, centerY))
                                drawCircle(color = PrimaryNeon.copy(alpha = 0.35f), radius = 16f, center = Offset(centerX, centerY))
                                drawCircle(color = PrimaryNeon, radius = 6f, center = Offset(centerX, centerY))
                            }
                        }

                        // North Indicator Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceContainerHigh.copy(alpha = 0.85f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "N ↑",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryCyan
                            )
                        }

                        // Legend Overlay
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceContainerHigh.copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SecondaryCyan))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Início", fontSize = 9.sp, color = OnSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PrimaryNeon))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Posição Atual", fontSize = 9.sp, color = OnSurface)
                            }
                        }
                    }

                    // Start vs Current Coordinate Footer
                    if (startLat != 0.0 || currentLat != 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (startLat != 0.0) {
                                    String.format(Locale.US, "Início: %.5f, %.5f", startLat, startLng)
                                } else {
                                    "Início: aguardando largada"
                                },
                                fontSize = 9.sp,
                                color = OnSurfaceVariant
                            )
                            Text(
                                text = if (currentLat != 0.0) {
                                    String.format(Locale.US, "Atual: %.5f, %.5f", currentLat, currentLng)
                                } else {
                                    ""
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SecondaryCyan
                            )
                        }
                    }
                }
            }

            // 7. Splits por Quilômetro (1 KM Splits + Next Split Progress)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PARCIAIS POR QUILÔMETRO (SPLITS)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceVariant
                        )
                        val metersInCurrentKm = distanceMeters % 1000
                        Text(
                            text = "KM ${splits.size + 1}: ${metersInCurrentKm}/1000m",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryCyan
                        )
                    }

                    if (splits.isEmpty()) {
                        Text(
                            text = "As parciais de ritmo e velocidade são registradas automaticamente a cada 1.000m percorridos pelo GPS.",
                            fontSize = 11.sp,
                            color = OnSurfaceVariant
                        )
                    } else {
                        splits.forEach { splitStr ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceContainerHigh.copy(alpha = 0.55f))
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = splitStr,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryCyan
                                )
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = "Split Concluído",
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // BARRA DE CONTROLES FIXA NA BASE (Iniciar Treino / Toggle Correndo-Caminhando / Botão Vermelho Fim de Treino)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLowest.copy(alpha = 0.96f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isActive && !runSummaryVisible) {
                Button(
                    onClick = {
                        if (!gpsPermissionGranted && !isIndoorSim) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                        viewModel.startRun()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryNeon,
                        contentColor = OnPrimaryContainer
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .testTag("btn_start_run")
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Iniciar Treino",
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "INICIAR TREINO",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            } else if (isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Botão rápido de alternância (Toggle Correndo / Caminhando) também acessível na barra fixa
                    Button(
                        onClick = { viewModel.toggleRunWalkMode() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isWalkingMode) walkingYellow else PrimaryNeon,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (isWalkingMode) "🚶 Caminhando" else "🏃 Correndo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Botão Inferior de "Fim de Treino" (Cor Vermelha)
                    Button(
                        onClick = { viewModel.stopRunAndShowSummary() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = stopRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .testTag("btn_finish_run")
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(
                            text = "FIM DE TREINO",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (isPaused) viewModel.resumeRun() else viewModel.pauseRun()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .testTag("btn_pause_run")
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Retomar" else "Pausar",
                            tint = OnSurface,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPaused) "Retomar" else "Pausar",
                            fontSize = 11.sp,
                            color = OnSurface
                        )
                    }

                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancelar",
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Descartar",
                            fontSize = 11.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Descartar Corrida?") },
            text = { Text("Esta sessão não será salva no histórico local do SQLite.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelRun()
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TertiaryOrange)
                ) {
                    Text("Descartar", color = Color.Black)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelDialog = false }) {
                    Text("Voltar")
                }
            }
        )
    }
}
