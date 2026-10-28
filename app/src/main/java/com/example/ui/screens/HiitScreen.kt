package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Exercise
import com.example.ui.components.BitFitTopAppBar
import com.example.ui.components.ExerciseIllustration
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
import com.example.ui.theme.TertiaryOrange
import com.example.viewmodel.AppScreen
import com.example.viewmodel.BitFitViewModel

@Composable
fun HiitScreen(
    viewModel: BitFitViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val isActive by viewModel.hiitIsActive.collectAsState()
    val isPaused by viewModel.hiitIsPaused.collectAsState()
    val currentIndex by viewModel.hiitExerciseIndex.collectAsState()
    val isRest by viewModel.hiitIsRest.collectAsState()
    val secondsLeft by viewModel.hiitSecondsLeft.collectAsState()
    val totalElapsed by viewModel.hiitTotalElapsedSeconds.collectAsState()
    val isCompleted by viewModel.hiitCompleted.collectAsState()
    val completedExerciseIds by viewModel.hiitCompletedExerciseIds.collectAsState()
    val skippedExerciseIds by viewModel.hiitSkippedExerciseIds.collectAsState()
    val saveEnabled by viewModel.hiitSaveEnabled.collectAsState()
    val lastSavedToHistory by viewModel.hiitLastSavedToHistory.collectAsState()
    val exercises = viewModel.hiitExercises

    var showDetailExercise by remember { mutableStateOf<Exercise?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val currentExercise: Exercise = exercises.getOrElse(currentIndex) { exercises[0] }
    val nextExercise: Exercise? = exercises.getOrNull(currentIndex + 1)

    Column(modifier = Modifier.fillMaxSize()) {
        BitFitTopAppBar(
            title = "HIIT CARDIO (25 MIN)",
            subtitle = if (isActive) {
                "FASE ${currentExercise.phaseNumber}/2 • EX ${currentIndex + 1}/${exercises.size} • ${completedExerciseIds.size} CONCLUÍDOS ✅"
            } else {
                "SEG / QUA / SEX • 7 AQUECIMENTO + 20 CARDIO HIIT"
            },
            onTuneClick = { onNavigate(AppScreen.GOALS) }
        )

        when {
            isCompleted -> {
                HiitCompletedView(
                    totalElapsed = totalElapsed,
                    completedCount = completedExerciseIds.size,
                    skippedCount = skippedExerciseIds.size,
                    totalExerciseCount = exercises.size,
                    wasSavedToHistory = lastSavedToHistory,
                    onViewHistory = {
                        viewModel.resetHiit()
                        onNavigate(AppScreen.HISTORY)
                    },
                    onRestart = {
                        viewModel.resetHiit()
                        viewModel.startHiit(0)
                    },
                    onBackToOverview = {
                        viewModel.resetHiit()
                    }
                )
            }

            isActive -> {
                val displayedExercise = if (isRest && nextExercise != null) nextExercise else currentExercise
                HiitActiveView(
                    exercises = exercises,
                    currentExercise = currentExercise,
                    nextExercise = nextExercise,
                    currentIndex = currentIndex,
                    totalExercises = exercises.size,
                    completedExerciseIds = completedExerciseIds,
                    skippedExerciseIds = skippedExerciseIds,
                    saveEnabled = saveEnabled,
                    isRest = isRest,
                    isPaused = isPaused,
                    secondsLeft = secondsLeft,
                    totalElapsed = totalElapsed,
                    onToggleSaveMode = { viewModel.toggleHiitSaveEnabled() },
                    onPauseToggle = {
                        if (isPaused) viewModel.resumeHiit() else viewModel.pauseHiit()
                    },
                    onPrevious = { viewModel.previousHiitExercise() },
                    onNextSuccess = { viewModel.nextHiitExercise() },
                    onSkipWithoutRecording = { viewModel.skipHiitExerciseWithoutRecording() },
                    onAdd15s = { viewModel.addHiitSeconds(15) },
                    onFinishAndSave = {
                        viewModel.completeHiitSession(saveToHistory = saveEnabled)
                    },
                    onRequestDiscard = { showDiscardDialog = true },
                    onOpenDetail = { showDetailExercise = displayedExercise }
                )
            }

            else -> {
                HiitOverviewView(
                    exercises = exercises,
                    completedExerciseIds = completedExerciseIds,
                    skippedExerciseIds = skippedExerciseIds,
                    saveEnabled = saveEnabled,
                    onToggleSaveMode = { viewModel.toggleHiitSaveEnabled() },
                    onStartAll = { viewModel.startHiit(0) },
                    onStartAt = { idx -> viewModel.startHiit(idx) },
                    onSelectExercise = { ex -> showDetailExercise = ex }
                )
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Descartar Treino HIIT Cardio?") },
            text = {
                Text("Esta sessão será encerrada agora e não será gravada no histórico de treinos.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelHiit()
                        showDiscardDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TertiaryOrange)
                ) {
                    Text("Descartar (Não Gravar)", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDiscardDialog = false }) {
                    Text("Voltar ao Treino")
                }
            }
        )
    }

    showDetailExercise?.let { ex ->
        val exIdx = (ex.id - 1).coerceAtLeast(0)
        HiitExerciseDetailDialog(
            exercise = ex,
            totalExercises = exercises.size,
            isWorkoutActive = isActive,
            onStartFromHere = {
                showDetailExercise = null
                viewModel.startHiit(exIdx)
            },
            onDismiss = { showDetailExercise = null }
        )
    }
}

@Composable
private fun HiitOverviewView(
    exercises: List<Exercise>,
    completedExerciseIds: Set<Int>,
    skippedExerciseIds: Set<Int>,
    saveEnabled: Boolean,
    onToggleSaveMode: () -> Unit,
    onStartAll: () -> Unit,
    onStartAt: (Int) -> Unit,
    onSelectExercise: (Exercise) -> Unit
) {
    var selectedPhase by remember { mutableIntStateOf(0) } // 0 = Todas, 1 = Aquecimento, 2 = Cardio HIIT
    val filteredExercises = remember(exercises, selectedPhase) {
        if (selectedPhase == 0) exercises else exercises.filter { it.phaseNumber == selectedPhase }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HIIT Cardio em Casa (${exercises.size} Exercícios)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                        Text(
                            text = "F1: 7 Aquecimento (20s) • F2: 20 Cardio HIIT (45s/15s)",
                            fontSize = 11.sp,
                            color = SecondaryCyan
                        )
                    }
                    NeonBadge(text = "25 MIN • ~280 KCAL", color = TertiaryOrange)
                }

                // Seletor Modo Gravar vs Modo Sem Gravar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLowest)
                        .border(
                            1.dp,
                            if (saveEnabled) PrimaryNeon.copy(alpha = 0.35f) else TertiaryOrange.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onToggleSaveMode() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (saveEnabled) Icons.Default.Save else Icons.Default.Close,
                            contentDescription = "Modo de Gravação",
                            tint = if (saveEnabled) PrimaryNeon else TertiaryOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (saveEnabled) "GRAVAR HIIT NO HISTÓRICO: ATIVO" else "MODO SEM GRAVAR (APENAS PRATICAR)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (saveEnabled) PrimaryNeon else TertiaryOrange
                            )
                            Text(
                                text = if (saveEnabled) {
                                    "No descanso de 15s mostra a foto grande do PRÓXIMO exercício!"
                                } else {
                                    "O treino não será salvo no histórico ao finalizar"
                                },
                                fontSize = 10.sp,
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (saveEnabled) PrimaryNeon.copy(alpha = 0.18f)
                                else TertiaryOrange.copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (saveEnabled) "GRAVAR" else "NÃO GRAVAR",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (saveEnabled) PrimaryNeon else TertiaryOrange
                        )
                    }
                }

                Button(
                    onClick = onStartAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryNeon,
                        contentColor = OnPrimaryContainer
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .testTag("btn_start_hiit")
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Iniciar HIIT",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (saveEnabled) "INICIAR HIIT CARDIO (25 MIN)" else "INICIAR HIIT SEM GRAVAR",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filters = listOf(
                0 to "Todos (${exercises.size})",
                1 to "Fase 1: Aquecimento (7 • 20s)",
                2 to "Fase 2: Cardio HIIT (20 • 45s/15s)"
            )
            filters.forEach { (phaseId, label) ->
                val isSelected = selectedPhase == phaseId
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) PrimaryNeon else SurfaceContainerHigh)
                        .border(
                            1.dp,
                            if (isSelected) PrimaryNeon else OutlineVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedPhase = phaseId }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) OnPrimaryContainer else OnSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(filteredExercises, key = { _, ex -> ex.id }) { _, exercise ->
                HiitExerciseListItemCard(
                    exercise = exercise,
                    isCompleted = exercise.id in completedExerciseIds,
                    isSkipped = exercise.id in skippedExerciseIds,
                    onClick = { onSelectExercise(exercise) },
                    onStartHere = { onStartAt(exercise.id - 1) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun HiitExerciseListItemCard(
    exercise: Exercise,
    isCompleted: Boolean,
    isSkipped: Boolean,
    onClick: () -> Unit,
    onStartHere: () -> Unit
) {
    val phaseBadgeColor = if (exercise.phaseNumber == 1) SecondaryCyan else TertiaryOrange

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        cornerRadius = 16.dp,
        borderColor = when {
            isCompleted -> PrimaryNeon.copy(alpha = 0.6f)
            isSkipped -> TertiaryOrange.copy(alpha = 0.4f)
            else -> OutlineVariant.copy(alpha = 0.18f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(width = 132.dp, height = 88.dp)
            ) {
                ExerciseIllustration(
                    exerciseId = exercise.id,
                    drawableResId = exercise.drawableResId,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 12.dp
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0B1326).copy(alpha = 0.88f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isCompleted) "#${exercise.id} • F${exercise.phaseNumber} ✅" else "#${exercise.id} • F${exercise.phaseNumber}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isCompleted) PrimaryNeon else phaseBadgeColor
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0B1326).copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Ampliar imagem",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = exercise.phaseName.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = phaseBadgeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = exercise.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis
                )

                if (exercise.beginnerTip.isNotBlank()) {
                    Text(
                        text = "💡 ${exercise.beginnerTip}",
                        fontSize = 10.sp,
                        color = OnSurfaceVariant,
                        maxLines = 2,
                        lineHeight = 13.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isCompleted) {
                            NeonBadge(text = "CONCLUÍDO ✅", color = PrimaryNeon)
                        } else if (isSkipped) {
                            NeonBadge(text = "PULADO", color = TertiaryOrange)
                        } else {
                            NeonBadge(text = "${exercise.durationSeconds}s", color = PrimaryNeon)
                            if (exercise.restSeconds > 0) {
                                NeonBadge(text = "${exercise.restSeconds}s desc", color = TertiaryOrange)
                            } else {
                                NeonBadge(text = "Contínuo", color = SecondaryCyan)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(PrimaryNeon.copy(alpha = 0.2f))
                            .border(1.dp, PrimaryNeon, CircleShape)
                            .clickable { onStartHere() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Iniciar daqui",
                            tint = PrimaryNeon,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// SUB-TELA 2: TREINO HIIT ATIVO
// REGRA 4: QUANDO isRest == true, EXIBE EM DESTAQUE A FOTO GRANDE E NOME DO PRÓXIMO EXERCÍCIO!
// =============================================================================
@Composable
private fun HiitActiveView(
    exercises: List<Exercise>,
    currentExercise: Exercise,
    nextExercise: Exercise?,
    currentIndex: Int,
    totalExercises: Int,
    completedExerciseIds: Set<Int>,
    skippedExerciseIds: Set<Int>,
    saveEnabled: Boolean,
    isRest: Boolean,
    isPaused: Boolean,
    secondsLeft: Int,
    totalElapsed: Int,
    onToggleSaveMode: () -> Unit,
    onPauseToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNextSuccess: () -> Unit,
    onSkipWithoutRecording: () -> Unit,
    onAdd15s: () -> Unit,
    onFinishAndSave: () -> Unit,
    onRequestDiscard: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val maxDuration = if (isRest) currentExercise.restSeconds.coerceAtLeast(1) else currentExercise.durationSeconds.coerceAtLeast(1)
    val timerProgress = (secondsLeft.toFloat() / maxDuration).coerceIn(0f, 1f)

    // REGRA 4: Durante o descanso (isRest == true), o destaque principal passa imediatamente para o PRÓXIMO exercício!
    val featuredExercise = if (isRest && nextExercise != null) nextExercise else currentExercise

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. BARRA DE PROGRESSO SUPERIOR + TRILHA DE EXERCÍCIOS
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
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
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (isRest) TertiaryOrange else PrimaryNeon)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRest) {
                            "✅ #${currentExercise.id} CONCLUÍDO! • DESCANSO DE ${currentExercise.restSeconds}s"
                        } else {
                            "${currentExercise.phaseName.uppercase()} • EX ${currentIndex + 1}/$totalExercises"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isRest) TertiaryOrange else SecondaryCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (saveEnabled) PrimaryNeon.copy(alpha = 0.16f)
                            else TertiaryOrange.copy(alpha = 0.2f)
                        )
                        .border(
                            1.dp,
                            if (saveEnabled) PrimaryNeon.copy(alpha = 0.4f) else TertiaryOrange,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onToggleSaveMode() }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (saveEnabled) "GRAVANDO • ${formatHiitTime(totalElapsed)}" else "SEM GRAVAR • ${formatHiitTime(totalElapsed)}",
                        fontSize = 10.sp,
                        color = if (saveEnabled) PrimaryNeon else TertiaryOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (completedExerciseIds.size.toFloat() / totalExercises.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PrimaryNeon,
                trackColor = SurfaceContainerHigh
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                exercises.forEachIndexed { idx, ex ->
                    val isDone = ex.id in completedExerciseIds
                    val isSkip = ex.id in skippedExerciseIds
                    val isCurr = idx == currentIndex
                    val bgColor = when {
                        isDone -> PrimaryNeon.copy(alpha = 0.24f)
                        isCurr -> SecondaryCyan.copy(alpha = 0.25f)
                        isSkip -> TertiaryOrange.copy(alpha = 0.18f)
                        else -> SurfaceContainerHigh.copy(alpha = 0.6f)
                    }
                    val borderCol = when {
                        isDone -> PrimaryNeon
                        isCurr -> SecondaryCyan
                        isSkip -> TertiaryOrange.copy(alpha = 0.6f)
                        else -> Color.Transparent
                    }
                    val txtCol = when {
                        isDone -> PrimaryNeon
                        isCurr -> SecondaryCyan
                        isSkip -> TertiaryOrange
                        else -> OnSurfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(bgColor)
                            .border(1.dp, borderCol, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                isDone -> "${ex.id}✓"
                                isSkip -> "${ex.id}–"
                                else -> "${ex.id}"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = txtCol
                        )
                    }
                }
            }
        }

        // 2. CARD CENTRAL COM DESTAQUE DO PRÓXIMO EXERCÍCIO DURANTE O DESCANSO (REGRA 4!)
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            cornerRadius = 18.dp,
            borderColor = if (isRest) TertiaryOrange.copy(alpha = 0.7f) else PrimaryNeon.copy(alpha = 0.35f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isRest) {
                    // BANNER VIBRANTE OBRIGATÓRIO DURANTE OS 15s DE DESCANSO
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(TertiaryOrange.copy(alpha = 0.22f))
                            .border(1.5.dp, TertiaryOrange, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⏸️ DESCANSO (${secondsLeft}s) • PRÓXIMO EXERCÍCIO:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TertiaryOrange,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // FOTO GRANDE (190.dp) DO EXERCÍCIO EM DESTAQUE (PRÓXIMO EXERCÍCIO QUANDO EM DESCANSO!)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clickable { onOpenDetail() },
                    contentAlignment = Alignment.Center
                ) {
                    ExerciseIllustration(
                        exerciseId = featuredExercise.id,
                        drawableResId = featuredExercise.drawableResId,
                        modifier = Modifier.fillMaxSize(),
                        isRest = isRest,
                        cornerRadius = 14.dp
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0B1326).copy(alpha = 0.90f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isRest) {
                                "PRÓXIMO: #${featuredExercise.id}/$totalExercises (${featuredExercise.durationSeconds}s)"
                            } else {
                                "AGORA: #${featuredExercise.id}/$totalExercises • FASE ${featuredExercise.phaseNumber}"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isRest) TertiaryOrange else PrimaryNeon
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0B1326).copy(alpha = 0.84f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Ampliar",
                                tint = PrimaryNeon,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ampliar",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // NOME EM FONTE GRANDE (18.sp Bold), GRUPO MUSCULAR, INSTRUÇÃO E DICA TÉCNICA DO EXERCÍCIO EM DESTAQUE
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = featuredExercise.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRest) PrimaryNeon else OnSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = featuredExercise.muscleGroup,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryCyan,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (featuredExercise.beginnerTip.isNotBlank()) {
                            "${featuredExercise.instruction} • 💡 ${featuredExercise.beginnerTip}"
                        } else {
                            featuredExercise.instruction
                        },
                        fontSize = 11.sp,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        lineHeight = 14.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // CRONÔMETRO DIGITAL GRANDE + BOTÃO +15s
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerLowest.copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.fillMaxSize(),
                                color = SurfaceContainerHigh,
                                strokeWidth = 4.dp
                            )
                            CircularProgressIndicator(
                                progress = { timerProgress },
                                modifier = Modifier.fillMaxSize(),
                                color = if (isRest) TertiaryOrange else PrimaryNeon,
                                strokeWidth = 4.dp
                            )
                            Text(
                                text = "${secondsLeft}s",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = if (isRest) "PAUSA DE RECUPERAÇÃO (15s)" else "EM EXECUÇÃO (${currentExercise.durationSeconds}s)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRest) TertiaryOrange else PrimaryNeon
                            )
                            Text(
                                text = when {
                                    isPaused -> "TREINO PAUSADO"
                                    isRest -> "Começa ${featuredExercise.name} em ${secondsLeft}s!"
                                    currentExercise.restSeconds > 0 -> "Descanso de ${currentExercise.restSeconds}s começa em ${secondsLeft}s"
                                    else -> "Avança direto p/ próximo em ${secondsLeft}s"
                                },
                                fontSize = 11.sp,
                                color = if (isPaused) TertiaryOrange else OnSurfaceVariant,
                                fontWeight = if (isPaused) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerHigh)
                            .border(1.dp, PrimaryNeon.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable { onAdd15s() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Mais 15 segundos",
                                tint = PrimaryNeon,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "15s",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNeon
                            )
                        }
                    }
                }

                // Quando está em execução (isRest == false), mostra a miniatura do próximo exercício na barra inferior do card
                if (!isRest && nextExercise != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerHigh.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            ExerciseIllustration(
                                exerciseId = nextExercise.id,
                                drawableResId = nextExercise.drawableResId,
                                modifier = Modifier.size(width = 50.dp, height = 34.dp),
                                cornerRadius = 8.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "PRÓXIMO (#${nextExercise.id} • F${nextExercise.phaseNumber})",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNeon
                                )
                                Text(
                                    text = nextExercise.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        NeonBadge(
                            text = "${nextExercise.durationSeconds}s",
                            color = PrimaryNeon
                        )
                    }
                }
            }
        }

        // 3. CONTROLES DOCKADOS NA BASE
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPrevious,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurface
                    ),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .testTag("btn_hiit_prev")
                        .weight(0.75f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        modifier = Modifier.size(20.dp)
                    )
                }

                Button(
                    onClick = onPauseToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaused) PrimaryNeon else TertiaryOrange,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .testTag("btn_hiit_pause_toggle")
                        .weight(1.35f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Retomar" else "Pausar",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPaused) "RETOMAR" else "PAUSAR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onNextSuccess,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryNeon.copy(alpha = 0.22f),
                        contentColor = PrimaryNeon
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .testTag("btn_hiit_skip")
                        .weight(1.1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Feito e Avançar",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isRest) "PULAR PAUSA" else "FEITO ✅",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onSkipWithoutRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .testTag("btn_hiit_skip_unrecorded")
                        .weight(1.1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Pular sem gravar exercício",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "NÃO FIZ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRequestDiscard,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .testTag("btn_hiit_discard")
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Descartar sem gravar",
                        tint = TertiaryOrange,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Não Gravar / Sair",
                        fontSize = 11.sp,
                        color = TertiaryOrange,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onFinishAndSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryCyan,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .testTag("btn_hiit_finish")
                        .weight(1.2f)
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Finalizar e Salvar",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (saveEnabled) "Finalizar e Salvar" else "Finalizar (Sem Gravar)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HiitCompletedView(
    totalElapsed: Int,
    completedCount: Int,
    skippedCount: Int,
    totalExerciseCount: Int,
    wasSavedToHistory: Boolean,
    onViewHistory: () -> Unit,
    onRestart: () -> Unit,
    onBackToOverview: () -> Unit
) {
    val estimatedCalories = if (completedCount > 0) (completedCount * 10.4f).toInt().coerceAtLeast(15) else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(PrimaryNeon.copy(alpha = 0.2f))
                        .border(2.dp, PrimaryNeon, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "HIIT Concluído",
                        tint = PrimaryNeon,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Text(
                    text = "HIIT Cardio Finalizado! 🔥",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (wasSavedToHistory) {
                        "Você concluiu $completedCount de $totalExerciseCount exercícios com sucesso ✅. Seu treino HIIT foi registrado no Histórico!"
                    } else {
                        "Sessão HIIT concluída no Modo Sem Gravar ($completedCount exercícios feitos)."
                    },
                    fontSize = 13.sp,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$completedCount/$totalExerciseCount",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNeon
                        )
                        Text(text = "Exercícios ✅", fontSize = 11.sp, color = OnSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatHiitTime(totalElapsed),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryCyan
                        )
                        Text(text = "Tempo Total", fontSize = 11.sp, color = OnSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "~$estimatedCalories kcal",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TertiaryOrange
                        )
                        Text(text = "Queima HIIT", fontSize = 11.sp, color = OnSurfaceVariant)
                    }
                }

                if (skippedCount > 0) {
                    Text(
                        text = "$skippedCount exercício(s) pulado(s)",
                        fontSize = 11.sp,
                        color = TertiaryOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = if (wasSavedToHistory) onViewHistory else onBackToOverview,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryNeon,
                        contentColor = OnPrimaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (wasSavedToHistory) "Ver Histórico de Treinos" else "Voltar ao HIIT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                OutlinedButton(
                    onClick = onRestart,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Refazer HIIT",
                        tint = OnSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refazer HIIT Cardio", color = OnSurface, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun HiitExerciseDetailDialog(
    exercise: Exercise,
    totalExercises: Int,
    isWorkoutActive: Boolean,
    onStartFromHere: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${exercise.phaseName.uppercase()} • EX ${exercise.id}/$totalExercises",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNeon,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = exercise.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                }
                NeonBadge(text = "${exercise.durationSeconds}s", color = PrimaryNeon)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(2.dp, PrimaryNeon, RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = exercise.drawableResId),
                        contentDescription = exercise.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "MÚSCULOS RECRUTADOS:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryCyan
                    )
                    Text(
                        text = exercise.muscleGroup,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurface
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "EXECUÇÃO:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceVariant
                    )
                    Text(
                        text = exercise.instruction,
                        fontSize = 12.sp,
                        color = OnSurface,
                        lineHeight = 17.sp
                    )
                }

                if (exercise.beginnerTip.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryNeon.copy(alpha = 0.12f))
                            .border(1.dp, PrimaryNeon.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "💡 PROTEÇÃO ARTICULAR & BIOMECÂNICA:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryNeon
                            )
                            Text(
                                text = exercise.beginnerTip,
                                fontSize = 12.sp,
                                color = OnSurface,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isWorkoutActive) {
                    OutlinedButton(
                        onClick = onStartFromHere,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Treinar daqui",
                            tint = PrimaryNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Treinar Daqui", color = PrimaryNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryNeon,
                        contentColor = OnPrimaryContainer
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Fechar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    )
}

private fun formatHiitTime(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format("%02d:%02d", m, s)
}
