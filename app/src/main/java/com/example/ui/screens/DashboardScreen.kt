package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkoutRecord
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
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.TertiaryContainer
import com.example.ui.theme.TertiaryOrange
import com.example.viewmodel.AppScreen
import com.example.viewmodel.BitFitViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: BitFitViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val allWorkouts by viewModel.allWorkouts.collectAsState()
    val latestWeight by viewModel.latestWeight.collectAsState()
    val currentTip by viewModel.currentTip.collectAsState()

    val now = System.currentTimeMillis()
    val todayRunWorkout = allWorkouts.firstOrNull { it.type == "RUN" && isSameCalendarDay(it.timestamp, now) }
    val todayHiitWorkout = allWorkouts.firstOrNull { (it.type == "HIIT" || it.type == "CALISTHENICS") && isSameCalendarDay(it.timestamp, now) }
    val todayElasticWorkout = allWorkouts.firstOrNull { it.type == "ELASTIC" && isSameCalendarDay(it.timestamp, now) }

    val todayRunDone = todayRunWorkout != null
    val todayHiitDone = todayHiitWorkout != null
    val todayElasticDone = todayElasticWorkout != null
    val completedTodayCount = (if (todayRunDone) 1 else 0) + (if (todayHiitDone) 1 else 0) + (if (todayElasticDone) 1 else 0)

    val realStreakDays = calculateStreakDays(allWorkouts)
    val displayName = settings.userName.ifBlank { "Atleta" }
    val todayDateFormatted = SimpleDateFormat("EEEE, dd 'de' MMM", Locale("pt", "BR"))
        .format(Date(now))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }

    val currentWeightKg = latestWeight?.weightKg ?: settings.initialWeightKg
    val currentBmi = latestWeight?.calculatedBmi ?: run {
        val h = settings.heightMeters.coerceAtLeast(1.0f)
        ((currentWeightKg / (h * h)) * 10f).toInt() / 10f
    }

    // Real 7-day rolling calendar & weekly adherence
    val last7Days = buildLast7DaysStatus(allWorkouts, now)
    val weeklyGoalWorkouts = (settings.targetRunFrequency + settings.targetCalisDays).coerceAtLeast(1)
    val sevenDaysAgoMillis = now - 7 * 86_400_000L
    val workoutsDoneLast7Days = allWorkouts.count { it.timestamp >= sevenDaysAgoMillis }
    val weeklyAdherenceRatio = (workoutsDoneLast7Days.toFloat() / weeklyGoalWorkouts).coerceIn(0f, 1f)
    val weeklyAdherencePct = (weeklyAdherenceRatio * 100).toInt()

    // Weekly Schedule Plan Detection based on Day of Week
    val dayOfWeek = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_WEEK)
    val (scheduleBadge, scheduleTitle, scheduleSubtitle, schedulePrimaryScreen) = when (dayOfWeek) {
        Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY -> Quadruple(
            "SEG / QUA / SEX • CARDIO & HIIT",
            "Dia de Cardio & HIIT",
            "Caminhada 3km + Corrida 1,5km no GPS + HIIT em Casa 25 min",
            AppScreen.HIIT
        )
        Calendar.TUESDAY, Calendar.THURSDAY -> Quadruple(
            "TER / QUI • FORÇA & COLUNA",
            "Dia de Musculação com Elástico",
            "6 exercícios de força (22 séries) + 10 min Alongamento Lombar",
            AppScreen.ELASTIC_STRENGTH
        )
        Calendar.SATURDAY -> Quadruple(
            "SÁBADO • DESCANSO ATIVO",
            "Descanso Ativo ao Ar Livre",
            "Caminhada Leve ao Ar Livre 3km no GPS para circulação e recuperação",
            AppScreen.RUN_TRACKER
        )
        else -> Quadruple(
            "DOMINGO • RECUPERAÇÃO TOTAL",
            "Descanso Total & Recuperação Muscular",
            "Hidratação (3,5L água), mobilidade leve e sono reparador",
            null
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        BitFitTopAppBar(
            title = "BITFIT",
            subtitle = "PAINEL DE PERFORMANCE REAL",
            onTuneClick = { onNavigate(AppScreen.GOALS) }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Greeting, Edit Profile, Metas Quick Button & Real Streak Header
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(PrimaryNeon.copy(alpha = 0.3f), SecondaryCyan.copy(alpha = 0.2f))
                                    )
                                )
                                .border(1.5.dp, PrimaryNeon, CircleShape)
                                .clickable { viewModel.openProfileSetupModal() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsRun,
                                contentDescription = "Avatar",
                                tint = PrimaryNeon,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Olá, $displayName! 👋",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .testTag("btn_edit_profile_dashboard")
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceContainerHigh)
                                        .clickable { viewModel.openProfileSetupModal() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar Perfil",
                                            tint = SecondaryCyan,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Perfil",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SecondaryCyan
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .testTag("btn_goals_quick_dashboard")
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PrimaryNeon.copy(alpha = 0.16f))
                                        .border(1.dp, PrimaryNeon.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .clickable { onNavigate(AppScreen.GOALS) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Metas",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryNeon
                                    )
                                }
                            }
                            Text(
                                text = todayDateFormatted,
                                fontSize = 11.sp,
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    // Real Streak Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(TertiaryContainer.copy(alpha = 0.2f))
                            .border(1.dp, TertiaryContainer.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$realStreakDays dias seguidos",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TertiaryOrange
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = TertiaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Card Cronograma do Dia (Planilha Semanal)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                borderColor = PrimaryNeon.copy(alpha = 0.35f)
            ) {
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
                        NeonBadge(text = "CRONOGRAMA DO DIA (PLANILHA SEMANAL)", color = PrimaryNeon)
                        NeonBadge(text = scheduleBadge, color = SecondaryCyan)
                    }

                    Text(
                        text = scheduleTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface
                    )
                    Text(
                        text = scheduleSubtitle,
                        fontSize = 12.sp,
                        color = OnSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    if (schedulePrimaryScreen != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (dayOfWeek == Calendar.MONDAY || dayOfWeek == Calendar.WEDNESDAY || dayOfWeek == Calendar.FRIDAY) {
                                Button(
                                    onClick = { onNavigate(AppScreen.RUN_TRACKER) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SecondaryCyan,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                ) {
                                    Text("1º Corrida GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onNavigate(AppScreen.HIIT) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TertiaryOrange,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                ) {
                                    Text("2º HIIT 25 min", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { onNavigate(schedulePrimaryScreen) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryNeon,
                                        contentColor = OnPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                ) {
                                    Text(
                                        text = if (schedulePrimaryScreen == AppScreen.ELASTIC_STRENGTH) {
                                            "Abrir Musculação com Elástico"
                                        } else {
                                            "Abrir Caminhada Leve no GPS"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Body Evolution Banner (100% Real Weight, Height & Goal)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(AppScreen.BODY_EVOLUTION) },
                cornerRadius = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
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
                                imageVector = Icons.Default.Timeline,
                                contentDescription = "Evolução",
                                tint = SecondaryCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Evolução Corporal & IMC",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Peso: %.1f kg (IMC %.1f) • Meta: %.1f kg",
                                    currentWeightKg,
                                    currentBmi,
                                    settings.targetWeightKg
                                ).replace('.', ','),
                                fontSize = 11.sp,
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Ver Evolução",
                        tint = SecondaryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 1. Treinos de Hoje (Resumo com status real dos 3 módulos)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Treinos de Hoje",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Text(
                        text = "$completedTodayCount / 3 CONCLUÍDOS HOJE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (completedTodayCount > 0) PrimaryNeon else OnSurfaceVariant
                    )
                }

                // Card 1: Corrida & Caminhada (GPS)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryNeon.copy(alpha = 0.15f))
                                    .border(1.dp, PrimaryNeon.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (todayRunDone) Icons.Default.Check else Icons.Default.DirectionsRun,
                                    contentDescription = "Status Corrida",
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Corrida & Caminhada (GPS • Meta ${settings.targetRunDistanceMeters}m)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                                if (todayRunWorkout != null) {
                                    Text(
                                        text = "Feito: ${todayRunWorkout.distanceMeters}m • 🏃 ${todayRunWorkout.runningFormatted} | 🚶 ${todayRunWorkout.walkingFormatted}",
                                        fontSize = 11.sp,
                                        color = PrimaryNeon
                                    )
                                } else {
                                    Text(
                                        text = "Status: Pendente hoje • Seg/Qua/Sex + Sáb",
                                        fontSize = 11.sp,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (todayRunDone) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(PrimaryNeon.copy(alpha = 0.15f))
                                    .border(1.dp, PrimaryNeon.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                    .clickable { onNavigate(AppScreen.RUN_TRACKER) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Concluído",
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Concluído",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNeon
                                )
                            }
                        } else {
                            Button(
                                onClick = { onNavigate(AppScreen.RUN_TRACKER) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SecondaryCyan,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Iniciar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Card 2: HIIT Cardio em Casa (25 min)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(TertiaryContainer.copy(alpha = 0.15f))
                                    .border(1.dp, TertiaryContainer.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Status HIIT",
                                    tint = TertiaryOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "HIIT Cardio em Casa (25 min • 27 Exercícios)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                                Text(
                                    text = if (todayHiitWorkout != null) {
                                        val firstLine = todayHiitWorkout.routineDetailsJson.lineSequence().firstOrNull().orEmpty()
                                        if (firstLine.isNotBlank()) {
                                            "Hoje (${todayHiitWorkout.durationFormatted}): $firstLine"
                                        } else {
                                            "Concluído hoje (${todayHiitWorkout.durationFormatted} • ${todayHiitWorkout.exercisesCount} ex.)"
                                        }
                                    } else {
                                        "Status: Pendente hoje • 7 Aquecimento + 20 HIIT (45s/15s)"
                                    },
                                    fontSize = 11.sp,
                                    color = if (todayHiitDone) PrimaryNeon else TertiaryOrange
                                )
                            }
                        }

                        Button(
                            onClick = { onNavigate(AppScreen.HIIT) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TertiaryOrange,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .testTag("btn_start_hiit_dashboard")
                                .height(34.dp)
                        ) {
                            Text(
                                text = if (todayHiitDone) "Refazer" else "Iniciar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Card 3: Musculação com Elástico (Séries & Reps)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SecondaryCyan.copy(alpha = 0.15f))
                                    .border(1.dp, SecondaryCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = "Status Elástico",
                                    tint = SecondaryCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Musculação com Elástico (Séries & Reps)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                                Text(
                                    text = if (todayElasticWorkout != null) {
                                        val firstLine = todayElasticWorkout.routineDetailsJson.lineSequence().firstOrNull().orEmpty()
                                        if (firstLine.isNotBlank()) {
                                            "Hoje: $firstLine"
                                        } else {
                                            "Concluído hoje (${todayElasticWorkout.exercisesCount} ex. • ${todayElasticWorkout.calories} kcal)"
                                        }
                                    } else {
                                        "Status: Pendente hoje • 6 Força (22 séries) + 10m Lombar"
                                    },
                                    fontSize = 11.sp,
                                    color = if (todayElasticDone) PrimaryNeon else SecondaryCyan
                                )
                            }
                        }

                        Button(
                            onClick = { onNavigate(AppScreen.ELASTIC_STRENGTH) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecondaryCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .testTag("btn_start_elastic_dashboard")
                                .height(34.dp)
                        ) {
                            Text(
                                text = if (todayElasticDone) "Refazer" else "Iniciar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Ações Rápidas (Pronto para treinar os 3 módulos)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ações Rápidas",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Text(
                        text = "3 MÓDULOS DE TREINO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }

                // Action 1: Iniciar Corrida / Caminhada (GPS)
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(AppScreen.RUN_TRACKER) },
                    cornerRadius = 16.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                NeonBadge(text = "GPS ATIVO", color = SecondaryCyan)
                                NeonBadge(text = "Meta: ${settings.targetRunDistanceMeters}m", color = PrimaryNeon)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Corrida & Caminhada (GPS)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Text(
                                text = "Alternância 🏃 Correndo / 🚶 Caminhando, rota e splits",
                                fontSize = 11.sp,
                                color = OnSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Começar agora",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryNeon
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Começar",
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PrimaryNeon)
                                .clickable { onNavigate(AppScreen.RUN_TRACKER) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsRun,
                                contentDescription = "Corrida",
                                tint = OnPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Action 2: HIIT Cardio em Casa (25 min)
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(AppScreen.HIIT) },
                    cornerRadius = 16.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                NeonBadge(text = "25 Min • 45s/15s", color = TertiaryOrange)
                                NeonBadge(text = "Prévia no Descanso", color = PrimaryNeon)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "HIIT Cardio em Casa (25 min)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Text(
                                text = "7 Aquecimento (20s) + 20 HIIT (45s ação / 15s mostrando próximo)",
                                fontSize = 11.sp,
                                color = OnSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "~305 kcal estimadas",
                                    fontSize = 11.sp,
                                    color = TertiaryOrange
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Iniciar HIIT →",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TertiaryOrange
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(TertiaryOrange)
                                .clickable { onNavigate(AppScreen.HIIT) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "HIIT",
                                tint = Color.Black,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Action 3: Musculação com Elástico (Séries & Reps)
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(AppScreen.ELASTIC_STRENGTH) },
                    cornerRadius = 16.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                NeonBadge(text = "22 Séries • 60s Descanso", color = SecondaryCyan)
                                NeonBadge(text = "+ 10m Lombar", color = PrimaryNeon)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Musculação com Elástico (Séries & Reps)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Text(
                                text = "Remada, Agachamento, Supino, Bíceps, Tríceps, Face Pull + Lombar",
                                fontSize = 11.sp,
                                color = OnSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Terças e Quintas",
                                    fontSize = 11.sp,
                                    color = SecondaryCyan
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Registrar séries →",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SecondaryCyan
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SecondaryCyan)
                                .clickable { onNavigate(AppScreen.ELASTIC_STRENGTH) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = "Musculação com Elástico",
                                tint = Color.Black,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            // 3. Calendário de Consistência Semanal Real
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp
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
                        Column {
                            Text(
                                text = "Consistência e Frequência (7 Dias)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Text(
                                text = "Calculado a partir dos seus treinos reais salvos",
                                fontSize = 11.sp,
                                color = OnSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ADERÊNCIA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNeon,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${weeklyAdherencePct}%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNeon
                            )
                        }
                    }

                    // Real 7-day rolling matrix
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        last7Days.forEach { (letter, dayOfMonth, isActive) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = letter,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceVariant
                                )

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isActive) PrimaryNeon.copy(alpha = 0.2f)
                                            else SurfaceContainerHigh
                                        )
                                        .border(
                                            1.dp,
                                            if (isActive) PrimaryNeon else OutlineVariant.copy(alpha = 0.3f),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$dayOfMonth",
                                            fontSize = 12.sp,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isActive) PrimaryNeon else OnSurface
                                        )
                                        if (isActive) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(PrimaryNeon)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Progress bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { weeklyAdherenceRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = PrimaryNeon,
                            trackColor = SurfaceContainerLowest
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Meta semanal: $weeklyGoalWorkouts treinos",
                                fontSize = 10.sp,
                                color = OnSurfaceVariant
                            )
                            Text(
                                text = "$workoutsDoneLast7Days / $weeklyGoalWorkouts feitos nos últimos 7 dias",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNeon
                            )
                        }
                    }
                }
            }

            // 4. Card de Dicas Diárias de Saúde / Recuperação
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.nextTip() },
                cornerRadius = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SecondaryCyan.copy(alpha = 0.15f))
                            .border(1.dp, SecondaryCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Opacity,
                            contentDescription = "Dica de Saúde",
                            tint = SecondaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentTip.first.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryCyan,
                                letterSpacing = 1.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Trocar dica",
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = currentTip.second,
                            fontSize = 12.sp,
                            color = OnSurface,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

private fun isSameCalendarDay(ts1: Long, ts2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = ts1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = ts2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
        c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

private fun calculateStreakDays(workouts: List<WorkoutRecord>): Int {
    if (workouts.isEmpty()) return 0
    val cal = Calendar.getInstance()
    val todayHasWorkout = workouts.any { isSameCalendarDay(it.timestamp, cal.timeInMillis) }
    if (!todayHasWorkout) {
        // Check if streak is still alive from yesterday
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayHasWorkout = workouts.any { isSameCalendarDay(it.timestamp, cal.timeInMillis) }
        if (!yesterdayHasWorkout) return 0
    }

    var streak = 0
    val checkCal = Calendar.getInstance()
    if (!todayHasWorkout) {
        checkCal.add(Calendar.DAY_OF_YEAR, -1)
    }
    while (true) {
        val dayTs = checkCal.timeInMillis
        val hasWorkoutOnDay = workouts.any { isSameCalendarDay(it.timestamp, dayTs) }
        if (hasWorkoutOnDay) {
            streak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            break
        }
    }
    return streak
}

private fun buildLast7DaysStatus(
    workouts: List<WorkoutRecord>,
    nowMillis: Long
): List<Triple<String, Int, Boolean>> {
    val dayLetters = mapOf(
        Calendar.SUNDAY to "D",
        Calendar.MONDAY to "S",
        Calendar.TUESDAY to "T",
        Calendar.WEDNESDAY to "Q",
        Calendar.THURSDAY to "Q",
        Calendar.FRIDAY to "S",
        Calendar.SATURDAY to "S"
    )
    return (6 downTo 0).map { daysAgo ->
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }
        val letter = dayLetters[cal.get(Calendar.DAY_OF_WEEK)] ?: "D"
        val dayNum = cal.get(Calendar.DAY_OF_MONTH)
        val hasWorkout = workouts.any { isSameCalendarDay(it.timestamp, cal.timeInMillis) }
        Triple(letter, dayNum, hasWorkout)
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

