package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.TertiaryContainer
import com.example.ui.theme.TertiaryOrange
import com.example.viewmodel.AppScreen
import com.example.viewmodel.BitFitViewModel
import kotlinx.coroutines.delay

@Composable
fun GoalsScreen(
    viewModel: BitFitViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    var distanceMeters by remember(settings) { mutableIntStateOf(settings.targetRunDistanceMeters) }
    var runFrequency by remember(settings) { mutableIntStateOf(settings.targetRunFrequency) }
    var calisDays by remember(settings) { mutableIntStateOf(settings.targetCalisDays) }
    var selectedDaysCsv by remember(settings) { mutableStateOf(settings.activeCalisDaysCsv) }

    var showSaveToast by remember { mutableStateOf(false) }

    // Selected days helper
    val daysList = listOf("SEG", "TER", "QUA", "QUI", "SEX", "SAB", "DOM")
    val activeDaysSet = selectedDaysCsv.split(",").filter { it.isNotBlank() }.toSet()

    val totalWorkouts = runFrequency + calisDays
    val runPct = if (totalWorkouts > 0) ((runFrequency.toFloat() / totalWorkouts) * 100).toInt() else 50
    val calisPct = 100 - runPct

    val kmPerSession = String.format("%.2f", distanceMeters / 1000f).replace('.', ',')
    val kmPerWeek = String.format("%.1f", (distanceMeters * runFrequency) / 1000f).replace('.', ',')

    val cadenceLabel = when {
        runFrequency <= 2 -> "Leve / Manutenção"
        runFrequency <= 4 -> "Intercalado (D+1)"
        else -> "Alta Performance Diária"
    }

    LaunchedEffect(showSaveToast) {
        if (showSaveToast) {
            delay(3500)
            showSaveToast = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        BitFitTopAppBar(
            title = "BITFIT",
            subtitle = "METAS & DIRETRIZES"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Intro
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Minhas Metas",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Ajuste seus objetivos diários e semanais. O Dashboard recalculará suas métricas e barras de progresso automaticamente.",
                    fontSize = 12.sp,
                    color = OnSurfaceVariant,
                    lineHeight = 17.sp
                )
            }

            // Live recalculation banner
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                borderColor = PrimaryNeon.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryNeon.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SyncAlt,
                            contentDescription = "Sync",
                            tint = PrimaryNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Impacto Dinâmico Ativo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNeon
                            )
                            NeonBadge(text = "EM TEMPO REAL", color = PrimaryNeon)
                        }
                        Text(
                            text = "Altera barras do Dashboard, GPS Tracker e calibragem.",
                            fontSize = 11.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            // SEÇÃO 1: Distância da Corrida por Sessão
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
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SecondaryCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Distância da Corrida por Sessão",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        }
                        NeonBadge(text = "GPS DIÁRIO", color = SecondaryCyan)
                    }

                    // Metric Visualizer Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLowest.copy(alpha = 0.8f))
                            .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "DISTÂNCIA ALVO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceVariant,
                                letterSpacing = 1.sp
                            )

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format("%,d", distanceMeters),
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface,
                                    letterSpacing = (-1).sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "m",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryCyan,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Km",
                                    tint = SecondaryCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Equivale a $kmPerSession km por treino",
                                    fontSize = 12.sp,
                                    color = OnSurfaceVariant
                                )
                            }

                            // Stepper and Slider
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceContainerHigh)
                                        .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            distanceMeters = (distanceMeters - 100).coerceAtLeast(500)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Diminuir 100m",
                                        tint = OnSurface
                                    )
                                }

                                Slider(
                                    value = distanceMeters.toFloat(),
                                    onValueChange = {
                                        distanceMeters = ((it / 100).toInt() * 100).coerceIn(500, 10000)
                                    },
                                    valueRange = 500f..10000f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = PrimaryNeon,
                                        activeTrackColor = PrimaryNeon,
                                        inactiveTrackColor = SurfaceContainerHighest
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceContainerHigh)
                                        .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            distanceMeters = (distanceMeters + 100).coerceAtMost(10000)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Aumentar 100m",
                                        tint = OnSurface
                                    )
                                }
                            }
                        }
                    }

                    // Presets Rápidos
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "PRESETS RÁPIDOS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        val presets = listOf(
                            Triple(1000, "1.000 m", "Iniciante"),
                            Triple(1600, "1.600 m", "Milha Padrão"),
                            Triple(3000, "3.000 m", "Avançado"),
                            Triple(5000, "5.000 m", "Desafio 5k")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.take(2).forEach { (presetVal, label, desc) ->
                                val isSelected = distanceMeters == presetVal
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) SecondaryCyan.copy(alpha = 0.15f)
                                            else SurfaceContainerHigh.copy(alpha = 0.6f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) SecondaryCyan else OutlineVariant.copy(alpha = 0.3f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { distanceMeters = presetVal }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) SecondaryCyan else OnSurface
                                        )
                                        Text(
                                            text = desc,
                                            fontSize = 10.sp,
                                            color = if (isSelected) SecondaryCyan else OnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.drop(2).forEach { (presetVal, label, desc) ->
                                val isSelected = distanceMeters == presetVal
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) SecondaryCyan.copy(alpha = 0.15f)
                                            else SurfaceContainerHigh.copy(alpha = 0.6f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) SecondaryCyan else OutlineVariant.copy(alpha = 0.3f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { distanceMeters = presetVal }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) SecondaryCyan else OnSurface
                                        )
                                        Text(
                                            text = desc,
                                            fontSize = 10.sp,
                                            color = if (isSelected) SecondaryCyan else OnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = SecondaryCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Define o percentual da barra de progresso no GPS Tracker e na Tela Inicial.",
                            fontSize = 10.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            // SEÇÃO 2: Meta Semanal de Corridas
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
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SecondaryCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Meta Semanal de Corridas",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        }
                        NeonBadge(text = "FREQUÊNCIA", color = SecondaryCyan)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLowest.copy(alpha = 0.8f))
                            .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "META SEMANAL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "$runFrequency",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = "sessões / semana",
                                        fontSize = 12.sp,
                                        color = OnSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "Equivalente a ~$kmPerWeek km / semana",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SecondaryCyan
                                )
                            }

                            // Stepper
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceContainerHigh)
                                    .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(4.dp)
                            ) {
                                IconButton(
                                    onClick = { runFrequency = (runFrequency - 1).coerceAtLeast(1) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Menos", tint = OnSurface)
                                }

                                Text(
                                    text = "$runFrequency",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNeon,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                IconButton(
                                    onClick = { runFrequency = (runFrequency + 1).coerceAtMost(7) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Mais", tint = OnSurface)
                                }
                            }
                        }
                    }

                    // Cadência e pontos
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Cadência Regular Recomendada",
                                fontSize = 11.sp,
                                color = OnSurfaceVariant
                            )
                            Text(
                                text = cadenceLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryCyan
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (i in 1..7) {
                                val isFill = i <= runFrequency
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isFill) SecondaryCyan else SurfaceContainerHighest)
                                )
                            }
                        }
                    }
                }
            }

            // SEÇÃO 3: Dias de Calistenia por Semana
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
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryNeon)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Dias de Calistenia por Semana",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        }
                        NeonBadge(text = "FORÇA CORPORAL", color = PrimaryNeon)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLowest.copy(alpha = 0.8f))
                            .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "FREQUÊNCIA PLANEJADA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "$calisDays",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryNeon
                                    )
                                    Text(
                                        text = "dias / semana",
                                        fontSize = 12.sp,
                                        color = OnSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${calisDays * 20} minutos de calistenia guiada",
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariant
                                )
                            }

                            // Stepper
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceContainerHigh)
                                    .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        calisDays = (calisDays - 1).coerceAtLeast(1)
                                        // sync days list
                                        selectedDaysCsv = daysList.take(calisDays).joinToString(",")
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Menos", tint = OnSurface)
                                }

                                Text(
                                    text = "$calisDays",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNeon,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                IconButton(
                                    onClick = {
                                        calisDays = (calisDays + 1).coerceAtMost(7)
                                        selectedDaysCsv = daysList.take(calisDays).joinToString(",")
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Mais", tint = OnSurface)
                                }
                            }
                        }
                    }

                    // Dias Alvo Selecionados Matrix (SEG a DOM)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "DIAS ALVO SELECIONADOS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Toque para alternar",
                                fontSize = 10.sp,
                                color = PrimaryNeon
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            daysList.forEach { dayKey ->
                                val isSelected = activeDaysSet.contains(dayKey)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) PrimaryNeon
                                            else SurfaceContainerHigh
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) PrimaryNeon else OutlineVariant.copy(alpha = 0.3f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            val newSet = if (isSelected) activeDaysSet - dayKey else activeDaysSet + dayKey
                                            val sorted = daysList.filter { newSet.contains(it) }
                                            selectedDaysCsv = sorted.joinToString(",")
                                            calisDays = sorted.size.coerceAtLeast(1)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayKey,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black else OnSurfaceVariant
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SEÇÃO 4: Resumo Consolidado & Projeção Semanal
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
                        Text(
                            text = "Resumo Consolidado",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                        NeonBadge(text = "PROJEÇÃO SEMANAL", color = TertiaryOrange)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLowest.copy(alpha = 0.9f))
                            .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        text = "CARGA DE TREINOS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurfaceVariant,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Meta Semanal: $totalWorkouts treinos totais",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = "($runFrequency Corridas + $calisDays Calistenia)",
                                        fontSize = 11.sp,
                                        color = OnSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "ADERÊNCIA EST.",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryNeon,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "85%",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryNeon
                                    )
                                }
                            }

                            // Proporção de barras corrida vs calistenia
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(runFrequency.toFloat().coerceAtLeast(0.1f))
                                            .height(8.dp)
                                            .background(SecondaryCyan)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(calisDays.toFloat().coerceAtLeast(0.1f))
                                            .height(8.dp)
                                            .background(PrimaryNeon)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "• Corrida ($runPct%)",
                                        fontSize = 10.sp,
                                        color = SecondaryCyan
                                    )
                                    Text(
                                        text = "• Calistenia ($calisPct%)",
                                        fontSize = 10.sp,
                                        color = PrimaryNeon
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainer.copy(alpha = 0.6f))
                            .border(1.dp, OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verificado",
                            tint = PrimaryNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Equilíbrio biomecânico ideal: descanso ativo garantido entre sessões de impacto.",
                            fontSize = 11.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            // SEÇÃO 5: Ações de Persistência (Salvar e Reset)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Primary Save Trigger Button
                Button(
                    onClick = {
                        viewModel.saveGoals(
                            distanceMeters = distanceMeters,
                            runFrequency = runFrequency,
                            calisDays = calisDays,
                            activeCalisDaysCsv = selectedDaysCsv
                        ) {
                            showSaveToast = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryNeon,
                        contentColor = OnPrimaryContainer
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .testTag("btn_save_goals")
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Salvar",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Salvar Metas no Dispositivo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Secondary Reset Button
                OutlinedButton(
                    onClick = {
                        viewModel.resetGoalsToDefaults {
                            distanceMeters = 1600
                            runFrequency = 3
                            calisDays = 4
                            selectedDaysCsv = "SEG,TER,QUI,SEX"
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .testTag("btn_reset_goals")
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Restaurar",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Restaurar Valores Padrão",
                        fontSize = 14.sp,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.openProfileSetupModal() },
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Editar Meu Perfil (Nome, Altura e Peso)",
                        fontSize = 13.sp,
                        color = SecondaryCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Feedback Toast
                AnimatedVisibility(
                    visible = showSaveToast,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        borderColor = PrimaryNeon.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryNeon.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Sucesso",
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "Salvo no SQLite local",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNeon
                                )
                                Text(
                                    text = "Metas e parâmetros sincronizados com a engine do BITFIT.",
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
