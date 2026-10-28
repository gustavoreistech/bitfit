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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ElasticStrengthExercises
import com.example.data.model.StrengthExercise
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
import java.util.Locale

@Composable
fun ElasticStrengthScreen(
    viewModel: BitFitViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val exercises = viewModel.elasticExercises
    val completedSetsMap by viewModel.elasticCompletedSetsMap.collectAsState()
    val restSecondsLeft by viewModel.elasticRestSecondsLeft.collectAsState()
    val restTargetExerciseName by viewModel.elasticRestTargetExerciseName.collectAsState()
    val activeExerciseId by viewModel.elasticActiveExerciseId.collectAsState()
    val stretchSecondsLeft by viewModel.elasticStretchSecondsLeft.collectAsState()
    val stretchRunning by viewModel.elasticStretchRunning.collectAsState()
    val stretchCompleted by viewModel.elasticStretchCompleted.collectAsState()
    val elapsedSeconds by viewModel.elasticWorkoutElapsedSeconds.collectAsState()
    val savedBanner by viewModel.elasticWorkoutSavedBanner.collectAsState()

    val totalSetsDone = completedSetsMap.values.sumOf { it.size }
    val totalTargetSets = ElasticStrengthExercises.totalTargetSets // 22 séries
    val totalRepsDone = completedSetsMap.values.sumOf { it.sum() }
    val progressRatio = (totalSetsDone.toFloat() / totalTargetSets.coerceAtLeast(1)).coerceIn(0f, 1f)
    var selectedDetailExercise by remember { mutableStateOf<StrengthExercise?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        BitFitTopAppBar(
            title = "MUSCULAÇÃO C/ ELÁSTICO",
            subtitle = "TERÇA & QUINTA • $totalSetsDone/$totalTargetSets SÉRIES • $totalRepsDone REPS",
            onTuneClick = { onNavigate(AppScreen.GOALS) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            // 1. PAINEL SUPERIOR DE PROGRESSO GERAL (SÉRIES, REPS & ALONGAMENTO)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 18.dp,
                    borderColor = PrimaryNeon.copy(alpha = 0.35f)
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
                            Column {
                                Text(
                                    text = "Treino de Força & Postura (Terça / Quinta)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                                Text(
                                    text = "6 Exercícios c/ Elástico (60s desc.) + 10 min Descompressão Lombar",
                                    fontSize = 11.sp,
                                    color = SecondaryCyan
                                )
                            }
                            NeonBadge(
                                text = formatSeconds(elapsedSeconds),
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
                                    .background(SurfaceContainerLowest)
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "SÉRIES FEITAS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceVariant
                                )
                                Text(
                                    text = "$totalSetsDone / $totalTargetSets",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryNeon
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceContainerLowest)
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "TOTAL DE REPS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceVariant
                                )
                                Text(
                                    text = "$totalRepsDone reps",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SecondaryCyan
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceContainerLowest)
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "LOMBAR (10M)",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceVariant
                                )
                                Text(
                                    text = if (stretchCompleted) "FEITO ✅" else "PENDENTE",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (stretchCompleted) PrimaryNeon else TertiaryOrange
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PrimaryNeon,
                            trackColor = SurfaceContainerHigh
                        )

                        if (savedBanner) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryNeon.copy(alpha = 0.16f))
                                    .border(1.dp, PrimaryNeon, RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "✅ Musculação com Elástico salva no Histórico com todas as séries e repetições!",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNeon
                                )
                            }
                        }
                    }
                }
            }

            // 2. CRONÔMETRO DE DESCANSO DE 60s EM DESTAQUE SEMPRE QUE ATIVO
            if (restSecondsLeft > 0) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        borderColor = TertiaryOrange
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
                                    Box(
                                        modifier = Modifier.size(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { (restSecondsLeft / 60f).coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxSize(),
                                            color = TertiaryOrange,
                                            strokeWidth = 4.dp
                                        )
                                        Text(
                                            text = "${restSecondsLeft}s",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TertiaryOrange
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "⏱️ DESCANSO ENTRE SÉRIES (60s)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TertiaryOrange
                                        )
                                        if (restTargetExerciseName.isNotBlank()) {
                                            Text(
                                                text = restTargetExerciseName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OnSurface
                                            )
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.addElasticRestSeconds(15) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("+15s", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryNeon)
                                    }
                                    Button(
                                        onClick = { viewModel.skipElasticRest() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = TertiaryOrange,
                                            contentColor = Color.Black
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = "Pular Descanso",
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Pular", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. LISTA DOS EXERCÍCIOS DE MUSCULAÇÃO COM ELÁSTICO (1 a 6) + ALONGAMENTO LOMBAR (7)
            items(exercises, key = { it.id }) { exercise ->
                if (exercise.isTimedStretch) {
                    LumbarStretchCard(
                        exercise = exercise,
                        secondsLeft = stretchSecondsLeft,
                        isRunning = stretchRunning,
                        isCompleted = stretchCompleted,
                        onToggleTimer = { viewModel.toggleElasticStretchTimer() },
                        onMarkDoneNow = { viewModel.completeElasticStretchNow() },
                        onOpenDetail = { selectedDetailExercise = exercise }
                    )
                } else {
                    val completedRepsList = completedSetsMap[exercise.id].orEmpty()
                    val isHighlighted = activeExerciseId == exercise.id && completedRepsList.size < exercise.targetSets
                    ElasticStrengthExerciseCard(
                        exercise = exercise,
                        completedRepsList = completedRepsList,
                        isHighlighted = isHighlighted,
                        onLogSet = { reps -> viewModel.logElasticSet(exercise.id, reps) },
                        onUndoLastSet = { viewModel.removeLastElasticSet(exercise.id) },
                        onOpenDetail = { selectedDetailExercise = exercise }
                    )
                }
            }

            // 4. BOTÕES DE FINALIZAR E SALVAR NO HISTÓRICO OU REINICIAR SESSÃO
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.finishAndSaveElasticWorkout {
                                onNavigate(AppScreen.HISTORY)
                            }
                        },
                        enabled = totalSetsDone > 0 || stretchCompleted,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryNeon,
                            contentColor = OnPrimaryContainer
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .testTag("btn_save_elastic_workout")
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Salvar Musculação",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Finalizar Treino de Força ($totalSetsDone Séries)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (totalSetsDone > 0 || stretchCompleted) {
                        OutlinedButton(
                            onClick = { viewModel.resetElasticWorkout() },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Zerar Sessão Atual",
                                tint = TertiaryOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Limpar Séries da Sessão Atual",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TertiaryOrange
                            )
                        }
                    }
                }
            }
        }
    }

    selectedDetailExercise?.let { ex ->
        AlertDialog(
            onDismissRequest = { selectedDetailExercise = null },
            title = {
                Column {
                    Text(
                        text = "EXERCÍCIO #${ex.id} • ${ex.targetSets}x ${ex.targetRepsRange}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryNeon
                    )
                    Text(
                        text = ex.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(2.dp, PrimaryNeon, RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = ex.drawableResId),
                            contentDescription = ex.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Text(
                        text = "🎯 Setup: ${ex.anchorSetup}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryCyan
                    )
                    Text(
                        text = "💡 ${ex.biomechanicalTip}",
                        fontSize = 12.sp,
                        color = OnSurface,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedDetailExercise = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryNeon,
                        contentColor = OnPrimaryContainer
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Fechar", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun ElasticStrengthExerciseCard(
    exercise: StrengthExercise,
    completedRepsList: List<Int>,
    isHighlighted: Boolean,
    onLogSet: (Int) -> Unit,
    onUndoLastSet: () -> Unit,
    onOpenDetail: () -> Unit
) {
    var selectedReps by remember(exercise.id, completedRepsList.size) {
        mutableIntStateOf(completedRepsList.lastOrNull() ?: exercise.defaultReps)
    }
    val setsDone = completedRepsList.size
    val isAllSetsDone = setsDone >= exercise.targetSets
    val nextSetNumber = (setsDone + 1).coerceAtMost(exercise.targetSets)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        borderColor = when {
            isAllSetsDone -> PrimaryNeon.copy(alpha = 0.65f)
            isHighlighted -> SecondaryCyan.copy(alpha = 0.7f)
            else -> OutlineVariant.copy(alpha = 0.22f)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cabeçalho com Imagem .jpg, Nome, Grupo Muscular e Setup do Elástico
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 116.dp, height = 86.dp)
                        .clickable { onOpenDetail() }
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
                            text = if (isAllSetsDone) "#${exercise.id} ✅" else "#${exercise.id}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isAllSetsDone) PrimaryNeon else SecondaryCyan
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeonBadge(
                            text = "${exercise.targetSets}x ${exercise.targetRepsRange}",
                            color = if (isAllSetsDone) PrimaryNeon else SecondaryCyan
                        )
                        if (isAllSetsDone) {
                            NeonBadge(text = "CONCLUÍDO ✅", color = PrimaryNeon)
                        } else {
                            NeonBadge(text = "Descanso ${exercise.restSeconds}s", color = TertiaryOrange)
                        }
                    }

                    Text(
                        text = exercise.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = exercise.muscleGroup,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryCyan
                    )

                    Text(
                        text = "🎯 Setup: ${exercise.anchorSetup}",
                        fontSize = 10.sp,
                        color = OnSurfaceVariant,
                        lineHeight = 13.sp
                    )
                }
            }

            // Dica biomecânica
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceContainerLowest)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "💡 ${exercise.biomechanicalTip}",
                    fontSize = 10.sp,
                    color = OnSurfaceVariant,
                    lineHeight = 14.sp
                )
            }

            // Linha de Chips Visuais das Séries (S1, S2, S3, S4)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..exercise.targetSets).forEach { setNum ->
                    val repsInSet = completedRepsList.getOrNull(setNum - 1)
                    val isSetDone = repsInSet != null
                    val isNextSet = setNum == setsDone + 1 && !isAllSetsDone

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    isSetDone -> PrimaryNeon.copy(alpha = 0.2f)
                                    isNextSet -> SecondaryCyan.copy(alpha = 0.18f)
                                    else -> SurfaceContainerHigh
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    isSetDone -> PrimaryNeon
                                    isNextSet -> SecondaryCyan
                                    else -> OutlineVariant.copy(alpha = 0.3f)
                                },
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isSetDone) {
                                "S$setNum: ${repsInSet}✓"
                            } else if (isNextSet) {
                                "S$setNum: Agora"
                            } else {
                                "S$setNum: Pendente"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSetDone -> PrimaryNeon
                                isNextSet -> SecondaryCyan
                                else -> OnSurfaceVariant
                            }
                        )
                    }
                }

                if (setsDone > 0) {
                    IconButton(
                        onClick = onUndoLastSet,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Desfazer última série",
                            tint = TertiaryOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Seletor Rápido de Repetições (- / +) + Botão "Concluir Série X (15 reps) ✅"
            if (!isAllSetsDone) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stepper de Repetições
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerHigh)
                            .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = { selectedReps = (selectedReps - 1).coerceAtLeast(1) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Menos 1 rep",
                                tint = OnSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = "$selectedReps",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryNeon
                            )
                            Text(
                                text = "reps",
                                fontSize = 8.sp,
                                color = OnSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { selectedReps = (selectedReps + 1).coerceAtMost(40) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Mais 1 rep",
                                tint = OnSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { onLogSet(selectedReps) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryNeon,
                            contentColor = OnPrimaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .testTag("btn_log_set_${exercise.id}")
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Concluir Série",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Concluir Série $nextSetNumber ($selectedReps reps) ✅",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LumbarStretchCard(
    exercise: StrengthExercise,
    secondsLeft: Int,
    isRunning: Boolean,
    isCompleted: Boolean,
    onToggleTimer: () -> Unit,
    onMarkDoneNow: () -> Unit,
    onOpenDetail: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        borderColor = if (isCompleted) PrimaryNeon else TertiaryOrange.copy(alpha = 0.6f)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ExerciseIllustration(
                        exerciseId = exercise.id,
                        drawableResId = exercise.drawableResId,
                        modifier = Modifier
                            .size(width = 96.dp, height = 72.dp)
                            .clickable { onOpenDetail() },
                        cornerRadius = 12.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        NeonBadge(
                            text = if (isCompleted) "CONCLUÍDO ✅" else "BLOCO FINAL • 10 MIN",
                            color = if (isCompleted) PrimaryNeon else TertiaryOrange
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = exercise.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                        Text(
                            text = exercise.muscleGroup,
                            fontSize = 10.sp,
                            color = SecondaryCyan
                        )
                    }
                }

                Text(
                    text = formatSeconds(secondsLeft),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCompleted) PrimaryNeon else TertiaryOrange
                )
            }

            Text(
                text = "💡 ${exercise.biomechanicalTip}",
                fontSize = 11.sp,
                color = OnSurfaceVariant,
                lineHeight = 15.sp
            )

            if (!isCompleted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onToggleTimer,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) TertiaryOrange else SecondaryCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Cronômetro Alongamento",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRunning) "Pausar (${formatSeconds(secondsLeft)})" else "Iniciar Alongamento (10m)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onMarkDoneNow,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .testTag("btn_complete_lumbar_stretch")
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Marcar Concluído",
                            tint = PrimaryNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Marcar Concluído ✅",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNeon
                        )
                    }
                }
            }
        }
    }
}

private fun formatSeconds(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}
