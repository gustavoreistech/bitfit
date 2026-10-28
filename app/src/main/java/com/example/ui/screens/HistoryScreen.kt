package com.example.ui.screens

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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkoutRecord
import com.example.ui.components.BitFitTopAppBar
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonBadge
import com.example.ui.theme.ErrorRed
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max

@Composable
fun HistoryScreen(
    viewModel: BitFitViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val context = LocalContext.current
    val allWorkouts by viewModel.allWorkouts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val filter by viewModel.historyFilter.collectAsState()

    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var feedbackBannerMessage by remember { mutableStateOf("") }

    // File Picker to Save Backup JSON directly to device
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.saveBackupToUri(context, uri) { success ->
                feedbackBannerMessage = if (success) {
                    "✓ Backup salvo com sucesso no seu aparelho!"
                } else {
                    "Não foi possível salvar o arquivo de backup."
                }
            }
        }
    }

    // File Picker to Restore Backup JSON from device
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackupFromUri(context, uri) { success ->
                feedbackBannerMessage = if (success) {
                    "✓ Backup restaurado com sucesso!"
                } else {
                    "Arquivo inválido. Selecione um backup JSON do BitFit."
                }
            }
        }
    }

    val filteredList = when (filter) {
        "RUN" -> allWorkouts.filter { it.type == "RUN" }
        "CALISTHENICS" -> allWorkouts.filter { it.type == "CALISTHENICS" }
        else -> allWorkouts
    }

    val totalCount = allWorkouts.size
    val runCount = allWorkouts.count { it.type == "RUN" }
    val calisCount = allWorkouts.count { it.type == "CALISTHENICS" }

    val totalDistanceMeters = allWorkouts.filter { it.type == "RUN" }.sumOf { it.distanceMeters }
    val totalCalories = allWorkouts.sumOf { it.calories }
    val totalRunningSec = allWorkouts.filter { it.type == "RUN" }.sumOf {
        if (it.runningSeconds > 0 || it.walkingSeconds > 0) it.runningSeconds else it.durationSeconds
    }
    val totalWalkingSec = allWorkouts.filter { it.type == "RUN" }.sumOf { it.walkingSeconds }
    val totalCalisExercisesDone = allWorkouts.filter { it.type == "CALISTHENICS" }.sumOf { it.exercisesCount }

    // Real Weekly Frequency Bar Data (last 7 days)
    val now = System.currentTimeMillis()
    val realWeekBars = remember(allWorkouts) {
        buildRealWeeklyMinutesBars(allWorkouts, now)
    }
    val totalWeeklyMinutes = realWeekBars.sumOf { it.second }
    val activeDaysCount = realWeekBars.count { it.second > 0 }
    val currentMonthYear = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))
        .format(Date(now))
        .uppercase(Locale("pt", "BR"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        BitFitTopAppBar(
            title = "BITFIT",
            subtitle = "RELATÓRIOS REAIS & BACKUP"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "$currentMonthYear • ATLETA: ${settings.userName.ifBlank { "ATLETA" }.uppercase()}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryCyan,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Histórico & Relatórios Reais",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
            }

            if (feedbackBannerMessage.isNotBlank()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp,
                    borderColor = PrimaryNeon
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = feedbackBannerMessage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNeon,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { feedbackBannerMessage = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Top Aggregate Metrics Bar (Treinos, Distância, Gasto)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "TREINOS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
                        Text(text = "$totalCount", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryNeon)
                        Text(text = "registrados", fontSize = 10.sp, color = OnSurfaceVariant)
                    }

                    Box(modifier = Modifier.width(1.dp).height(36.dp).background(OutlineVariant.copy(alpha = 0.3f)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "DISTÂNCIA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
                        Text(
                            text = String.format(Locale.US, "%.2f", totalDistanceMeters / 1000f).replace('.', ','),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryCyan
                        )
                        Text(text = "km rodados", fontSize = 10.sp, color = OnSurfaceVariant)
                    }

                    Box(modifier = Modifier.width(1.dp).height(36.dp).background(OutlineVariant.copy(alpha = 0.3f)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "GASTO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
                        Text(
                            text = String.format(Locale.US, "%,d", totalCalories),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TertiaryOrange
                        )
                        Text(text = "kcal ativas", fontSize = 10.sp, color = OnSurfaceVariant)
                    }
                }
            }

            // Resumo Acumulado de Modalidades: 🏃 Correndo vs 🚶 Caminhando & 💪 Calistenia
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                borderColor = PrimaryNeon.copy(alpha = 0.28f)
            ) {
                val totalRunWalkSec = (totalRunningSec + totalWalkingSec).coerceAtLeast(1)
                val runRatio = if (totalRunningSec + totalWalkingSec > 0) {
                    (totalRunningSec.toFloat() / totalRunWalkSec).coerceIn(0.08f, 0.92f)
                } else {
                    0.5f
                }
                val runPctTotal = if (totalRunningSec + totalWalkingSec > 0) {
                    ((totalRunningSec * 100f) / totalRunWalkSec).toInt()
                } else {
                    0
                }
                val walkPctTotal = if (totalRunningSec + totalWalkingSec > 0) (100 - runPctTotal) else 0

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MAPEAMENTO ACUMULADO DE MODALIDADES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryCyan,
                            letterSpacing = 1.sp
                        )
                        NeonBadge(
                            text = "Rotina Real",
                            color = PrimaryNeon
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🏃 CORRENDO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", totalRunningSec / 60, totalRunningSec % 60),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnSurface
                            )
                            Text(
                                text = "$runPctTotal% do tempo GPS",
                                fontSize = 9.sp,
                                color = OnSurfaceVariant
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🚶 CAMINHANDO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", totalWalkingSec / 60, totalWalkingSec % 60),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnSurface
                            )
                            Text(
                                text = "$walkPctTotal% do tempo GPS",
                                fontSize = 9.sp,
                                color = OnSurfaceVariant
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(TertiaryOrange.copy(alpha = 0.12f))
                                .border(1.dp, TertiaryOrange.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "💪 CALISTENIA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TertiaryOrange
                            )
                            Text(
                                text = "$totalCalisExercisesDone",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnSurface
                            )
                            Text(
                                text = "exercícios feitos",
                                fontSize = 9.sp,
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    if (totalRunningSec + totalWalkingSec > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceContainerHighest)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(runRatio)
                                    .height(8.dp)
                                    .background(Color(0xFF10B981))
                            )
                            Box(
                                modifier = Modifier
                                    .weight((1f - runRatio).coerceAtLeast(0.08f))
                                    .height(8.dp)
                                    .background(Color(0xFFF59E0B))
                            )
                        }
                    }
                }
            }

            // Filter Chips Bar (Todos, Corridas, Calistenia)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isAll = filter == "ALL"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isAll) PrimaryNeon else SurfaceContainerHigh)
                        .clickable { viewModel.setHistoryFilter("ALL") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Todos ($totalCount)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAll) Color.Black else OnSurfaceVariant
                    )
                }

                val isRun = filter == "RUN"
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isRun) SecondaryCyan else SurfaceContainerHigh)
                        .clickable { viewModel.setHistoryFilter("RUN") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = if (isRun) Color.Black else SecondaryCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Corridas ($runCount)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRun) Color.Black else OnSurfaceVariant
                        )
                    }
                }

                val isCalis = filter == "CALISTHENICS"
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isCalis) TertiaryOrange else SurfaceContainerHigh)
                        .clickable { viewModel.setHistoryFilter("CALISTHENICS") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = if (isCalis) Color.Black else TertiaryOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Calistenia ($calisCount)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCalis) Color.Black else OnSurfaceVariant
                        )
                    }
                }
            }

            // Frequência Semanal Chart Real (Barras por minutos dos últimos 7 dias)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = PrimaryNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Volume Semanal Real (7 Dias)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PrimaryNeon))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$activeDaysCount/7 Dias Ativos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNeon
                            )
                        }
                    }

                    val maxMinutesInDay = (realWeekBars.maxOfOrNull { it.second } ?: 30).coerceAtLeast(30)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        realWeekBars.forEach { (dayLabel, minutes) ->
                            val heightRatio = if (minutes == 0) 0.10f else (minutes.toFloat() / maxMinutesInDay).coerceIn(0.18f, 1f)
                            val barColor = when {
                                minutes >= 20 -> PrimaryNeon
                                minutes > 0 -> SecondaryCyan
                                else -> SurfaceContainerHighest
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (minutes == 0) "0m" else "${minutes}m",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (minutes > 0) PrimaryNeon else OnSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height((75 * heightRatio).dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(barColor)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = dayLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (minutes > 0) PrimaryNeon else OnSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PrimaryNeon))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Calculado dos seus treinos reais", fontSize = 10.sp, color = OnSurfaceVariant)
                        }
                        Text(
                            text = "Total: $totalWeeklyMinutes min",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryCyan
                        )
                    }
                }
            }

            // Diário de Treinos & Mapeamento de Rotina (Agrupado por Dia - 100% Real)
            val groupedByDay = remember(filteredList, now) {
                groupWorkoutsByDay(filteredList, now)
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Diário de Treinos & Rotinas",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Text(
                        text = "AGRUPADO POR DIA",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryCyan,
                        letterSpacing = 1.sp
                    )
                }

                if (filteredList.isEmpty()) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Nenhum treino registrado ainda",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Text(
                                text = "Seus relatórios estão 100% limpos e prontos. Assim que você finalizar uma Corrida/Caminhada GPS ou sessão de Calistenia, ela aparecerá aqui mapeada por dia e por fase!",
                                fontSize = 12.sp,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }

                groupedByDay.forEach { (dayHeader, dayWorkouts) ->
                    val dayTotalSec = dayWorkouts.sumOf { it.durationSeconds }
                    val dayTotalMin = (dayTotalSec / 60).coerceAtLeast(1)
                    val dayTotalKcal = dayWorkouts.sumOf { it.calories }
                    val hasRunInDay = dayWorkouts.any { it.type == "RUN" }
                    val hasCalisInDay = dayWorkouts.any { it.type == "CALISTHENICS" }
                    val dayModalitiesLabel = buildList {
                        if (hasRunInDay) add("🏃 Corrida/Caminhada")
                        if (hasCalisInDay) add("💪 Calistenia")
                    }.joinToString(" + ")

                    // Cabeçalho do Dia (Resumo Diário)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerHigh)
                            .border(1.dp, OutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = dayHeader,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryNeon
                            )
                            Text(
                                text = "${dayWorkouts.size} treino(s) • $dayModalitiesLabel",
                                fontSize = 10.sp,
                                color = OnSurfaceVariant
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NeonBadge(
                                text = "${dayTotalMin} min",
                                color = SecondaryCyan
                            )
                            NeonBadge(
                                text = "$dayTotalKcal kcal",
                                color = TertiaryOrange
                            )
                        }
                    }

                    dayWorkouts.forEach { workout ->
                        val isRun = workout.type == "RUN"
                        val iconColor = if (isRun) SecondaryCyan else TertiaryOrange

                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(iconColor.copy(alpha = 0.15f))
                                                .border(1.dp, iconColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isRun) Icons.Default.DirectionsRun else Icons.Default.FitnessCenter,
                                                contentDescription = null,
                                                tint = iconColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            val dateStr = SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale("pt", "BR"))
                                                .format(Date(workout.timestamp))
                                            Text(
                                                text = dateStr,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OnSurfaceVariant
                                            )
                                            Text(
                                                text = workout.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OnSurface
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (workout.tag.isNotBlank()) {
                                            NeonBadge(
                                                text = workout.tag,
                                                color = if (workout.tag.contains("Meta") || workout.tag.contains("100%")) PrimaryNeon else SecondaryCyan
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteWorkoutRecord(workout.id) },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Excluir Treino",
                                                tint = OnSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                if (isRun) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = "DISTÂNCIA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
                                            Text(text = "${workout.distanceMeters} m", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                                            Text(text = workout.distanceKmFormatted, fontSize = 10.sp, color = SecondaryCyan)
                                        }

                                        Column {
                                            Text(text = "TEMPO TOTAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
                                            Text(text = workout.durationFormatted, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                                            Text(text = "${workout.pausesCount} pausas", fontSize = 10.sp, color = OnSurfaceVariant)
                                        }

                                        Column {
                                            Text(text = "PACE MÉDIO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
                                            Text(text = workout.paceFormatted, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SecondaryCyan)
                                            Text(text = "${workout.calories} kcal", fontSize = 10.sp, color = TertiaryOrange)
                                        }

                                        Column {
                                            Text(text = "VEL. / ELEV.", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
                                            Text(
                                                text = if (workout.avgSpeedKmh > 0f) String.format(Locale.US, "%.1f km/h", workout.avgSpeedKmh) else "--",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryNeon
                                            )
                                            Text(
                                                text = "+${workout.elevationGainMeters}m • ${workout.stepsEstimate} passos",
                                                fontSize = 10.sp,
                                                color = OnSurfaceVariant
                                            )
                                        }
                                    }

                                    // Mapeamento da Rotina de Corrida vs Caminhada (🏃 Correndo vs 🚶 Caminhando)
                                    val runPct = workout.runningPercentage
                                    val walkPct = (100 - runPct).coerceIn(0, 100)
                                    val runBarWeight = (runPct / 100f).coerceIn(0.08f, 0.92f)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SurfaceContainerLowest)
                                            .border(1.dp, OutlineVariant.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🏃 CORRENDO: ${workout.runningFormatted} ($runPct%)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981)
                                            )
                                            Text(
                                                text = "🚶 CAMINHANDO: ${workout.walkingFormatted} ($walkPct%)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF59E0B)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(7.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(SurfaceContainerHigh)
                                        ) {
                                            if (runPct >= 100) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(7.dp)
                                                        .background(Color(0xFF10B981))
                                                )
                                            } else if (runPct <= 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(7.dp)
                                                        .background(Color(0xFFF59E0B))
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(runBarWeight)
                                                        .height(7.dp)
                                                        .background(Color(0xFF10B981))
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .weight((1f - runBarWeight).coerceAtLeast(0.08f))
                                                        .height(7.dp)
                                                        .background(Color(0xFFF59E0B))
                                                )
                                            }
                                        }

                                        if (workout.routineDetailsJson.isNotBlank()) {
                                            Text(
                                                text = workout.routineDetailsJson,
                                                fontSize = 10.sp,
                                                color = OnSurfaceVariant
                                            )
                                        }
                                    }

                                    if (workout.hasGpsCoordinates) {
                                        val savedPts = workout.parsedRoutePoints()
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(SurfaceContainerLowest)
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.LocationOn,
                                                        contentDescription = "Coordenadas GPS",
                                                        tint = PrimaryNeon,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "COORDENADAS GPS GRAVADAS",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = PrimaryNeon
                                                    )
                                                }
                                                Text(
                                                    text = workout.coordinatesSummaryFormatted,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = OnSurface
                                                )
                                                if (workout.splitsJson.isNotBlank()) {
                                                    Text(
                                                        text = "Splits: ${workout.splitsJson}",
                                                        fontSize = 9.sp,
                                                        color = SecondaryCyan
                                                    )
                                                }
                                            }

                                            if (savedPts.size > 1) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 72.dp, height = 46.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(SurfaceContainerHigh)
                                                        .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                ) {
                                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                                        val minLat = savedPts.minOf { it.first }
                                                        val maxLat = savedPts.maxOf { it.first }
                                                        val minLng = savedPts.minOf { it.second }
                                                        val maxLng = savedPts.maxOf { it.second }
                                                        val lngScale = cos(Math.toRadians((minLat + maxLat) / 2.0)).coerceAtLeast(0.2)
                                                        val span = max(max(maxLat - minLat, (maxLng - minLng) * lngScale), 0.0002)

                                                        val path = Path()
                                                        savedPts.forEachIndexed { idx, (lat, lng) ->
                                                            val nx = 0.5 + (((lng - (minLng + maxLng) / 2.0) * lngScale) / span) * 0.72
                                                            val ny = 0.5 - ((lat - (minLat + maxLat) / 2.0) / span) * 0.72
                                                            val px = (nx.toFloat().coerceIn(0.1f, 0.9f)) * size.width
                                                            val py = (ny.toFloat().coerceIn(0.1f, 0.9f)) * size.height
                                                            if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                                        }
                                                        drawPath(
                                                            path = path,
                                                            color = PrimaryNeon,
                                                            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = "EXERCÍCIOS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
                                            Text(text = "${workout.exercisesCount}/21", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                                        }

                                        Column {
                                            Text(text = "TEMPO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
                                            Text(text = workout.durationFormatted, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                                        }

                                        Column {
                                            Text(text = "ESTRUTURA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
                                            Text(text = "4 Fases (21x)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TertiaryOrange)
                                        }

                                        Column {
                                            Text(text = "CALORIAS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
                                            Text(text = "~${workout.calories} kcal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryNeon)
                                        }
                                    }

                                    if (workout.routineDetailsJson.isNotBlank()) {
                                        val detailLines = workout.routineDetailsJson.lines().filter { it.isNotBlank() }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(SurfaceContainerLowest)
                                                .border(1.dp, TertiaryOrange.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "MAPEAMENTO DA ROTINA POR FASE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TertiaryOrange,
                                                letterSpacing = 0.8.sp
                                            )
                                            detailLines.forEachIndexed { idx, line ->
                                                Text(
                                                    text = line,
                                                    fontSize = if (idx == 0) 11.sp else 10.sp,
                                                    fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (idx == 0) PrimaryNeon else OnSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Central de Backup, Restauração & Zerar Dados (SQLite / Room)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                borderColor = SecondaryCyan.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SecondaryCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = "Banco",
                                tint = SecondaryCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Central de Backup & Gerenciamento de Dados",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Text(
                                text = "Salve seus dados em arquivo JSON, compartilhe ou zere tudo para recomeçar.",
                                fontSize = 11.sp,
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    // Button 1: Salvar Arquivo de Backup (.json) no Aparelho
                    Button(
                        onClick = {
                            val dateStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            createBackupLauncher.launch("BitFit_Backup_$dateStamp.json")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryNeon,
                            contentColor = OnPrimaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .testTag("btn_save_file_backup")
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Salvar Backup",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fazer Backup em Arquivo (.JSON)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Row: Compartilhar Backup + Restaurar Backup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.exportBackup(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceContainerHighest,
                                contentColor = OnSurface
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .testTag("btn_export_backup")
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartilhar",
                                tint = SecondaryCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Compartilhar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceContainerHighest,
                                contentColor = OnSurface
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .testTag("btn_restore_backup")
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = "Restaurar",
                                tint = PrimaryNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Restaurar JSON",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Button 4: Zerar Todos os Dados e Recomeçar do Zero
                    OutlinedButton(
                        onClick = { showResetConfirmDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .testTag("btn_reset_all_data")
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Zerar Dados",
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Zerar Dados e Recomeçar do Zero",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(
                    text = "Zerar Todos os Dados?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Isso apagará todos os treinos e pesagens atuais e abrirá a tela inicial para você cadastrar seu Nome, Altura, Peso Atual e Metas para registrar apenas seus dados reais a partir de agora.",
                    fontSize = 13.sp,
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        viewModel.resetAllDataAndRestartProfile()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("btn_confirm_reset_all")
                ) {
                    Text("Sim, Zerar e Recomeçar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private fun buildRealWeeklyMinutesBars(
    workouts: List<WorkoutRecord>,
    nowMillis: Long
): List<Pair<String, Int>> {
    val dayShortNames = mapOf(
        Calendar.MONDAY to "Seg",
        Calendar.TUESDAY to "Ter",
        Calendar.WEDNESDAY to "Qua",
        Calendar.THURSDAY to "Qui",
        Calendar.FRIDAY to "Sex",
        Calendar.SATURDAY to "Sáb",
        Calendar.SUNDAY to "Dom"
    )
    return (6 downTo 0).map { daysAgo ->
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }
        val label = dayShortNames[cal.get(Calendar.DAY_OF_WEEK)] ?: "Dia"
        val dayWorkouts = workouts.filter { w ->
            val wc = Calendar.getInstance().apply { timeInMillis = w.timestamp }
            wc.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                wc.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
        }
        val totalSeconds = dayWorkouts.sumOf { it.durationSeconds }
        val totalMins = if (totalSeconds > 0) (totalSeconds / 60).coerceAtLeast(1) else 0
        Pair(label, totalMins)
    }
}

private fun groupWorkoutsByDay(
    workouts: List<WorkoutRecord>,
    nowMillis: Long
): List<Pair<String, List<WorkoutRecord>>> {
    val todayCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val yesterdayCal = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    val weekdayFormat = SimpleDateFormat("EEEE", Locale("pt", "BR"))

    val groupedMap = linkedMapOf<String, MutableList<WorkoutRecord>>()
    workouts.forEach { w ->
        val wc = Calendar.getInstance().apply { timeInMillis = w.timestamp }
        val dateStr = dateFormat.format(Date(w.timestamp))
        val dayTitle = when {
            wc.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                wc.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) ->
                "HOJE • $dateStr"
            wc.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                wc.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR) ->
                "ONTEM • $dateStr"
            else -> {
                val weekDay = weekdayFormat.format(Date(w.timestamp)).uppercase(Locale("pt", "BR"))
                "$weekDay • $dateStr"
            }
        }
        groupedMap.getOrPut(dayTitle) { mutableListOf() }.add(w)
    }
    return groupedMap.map { (k, v) -> Pair(k, v) }
}

