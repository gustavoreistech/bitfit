package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BitFitTopAppBar
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonBadge
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.OnPrimaryContainer
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.TertiaryOrange
import com.example.viewmodel.AppScreen
import com.example.viewmodel.BitFitViewModel
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun BodyEvolutionScreen(
    viewModel: BitFitViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val allWeights by viewModel.allWeights.collectAsState()
    val latestWeight by viewModel.latestWeight.collectAsState()

    var showAddWeightModal by remember { mutableStateOf(false) }
    var showEditBiometricsModal by remember { mutableStateOf(false) }
    var chartFilter by remember { mutableStateOf("Tudo") }

    val height = settings.heightMeters.coerceAtLeast(1.0f)
    val currentWeightKg = latestWeight?.weightKg ?: settings.initialWeightKg
    val currentBmi = latestWeight?.calculatedBmi ?: (((currentWeightKg / (height * height)) * 10f).toInt() / 10f)

    // Total variation compared to baseline/first record
    val baselineRecord = allWeights.lastOrNull()
    val baselineWeight = baselineRecord?.weightKg ?: settings.initialWeightKg
    val totalVariationKg = currentWeightKg - baselineWeight

    // Distance to goal
    val targetWeight = settings.targetWeightKg
    val diffToGoal = abs(currentWeightKg - targetWeight)
    val initialDistanceToGoal = abs(baselineWeight - targetWeight).coerceAtLeast(0.1f)
    val goalProgressPct = if (abs(currentWeightKg - targetWeight) <= 0.1f) {
        100
    } else {
        (((initialDistanceToGoal - diffToGoal) / initialDistanceToGoal) * 100f).toInt().coerceIn(0, 100)
    }

    val avgDeltaGrams = if (allWeights.size > 1) {
        ((totalVariationKg / (allWeights.size - 1)) * 1000f).toInt()
    } else {
        0
    }

    // BMI Category determination
    val (bmiCategory, bmiColor) = when {
        currentBmi < 18.5f -> "Abaixo do Peso" to SecondaryCyan
        currentBmi <= 24.9f -> "Peso Adequado / Saudável" to PrimaryNeon
        currentBmi <= 29.9f -> "Sobrepeso" to TertiaryOrange
        else -> "Obesidade" to ErrorRed
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        BitFitTopAppBar(
            title = "BITFIT",
            subtitle = "BIOMETRIA & DESEMPENHO REAL"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Screen Headline
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BIOMETRIA DE ${settings.userName.ifBlank { "ATLETA" }.uppercase()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryCyan,
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sincronizado",
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Dados Reais SQLite",
                            fontSize = 10.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Evolução Corporal",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Meta",
                        tint = TertiaryOrange,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Meta configurada: ",
                        fontSize = 12.sp,
                        color = OnSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f kg", targetWeight).replace('.', ','),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNeon
                    )
                }
            }

            // 1. CARTÃO PRINCIPAL DE STATUS ATUAL
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                borderColor = PrimaryNeon.copy(alpha = 0.25f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "PESO ATUAL REGISTRADO",
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
                                    text = String.format(Locale.US, "%.1f", currentWeightKg).replace('.', ','),
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface,
                                    letterSpacing = (-1).sp
                                )
                                Text(
                                    text = "kg",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(PrimaryNeon.copy(alpha = 0.15f))
                                    .border(1.dp, PrimaryNeon.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = "Variação",
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val sign = if (totalVariationKg > 0f) "+" else ""
                                Text(
                                    text = "$sign${String.format(Locale.US, "%.1f kg", totalVariationKg).replace('.', ',')}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNeon
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "desde o início (${String.format(Locale.US, "%.1f kg", baselineWeight).replace('.', ',')})",
                                fontSize = 10.sp,
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    // User Height & Target Weight Edit Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerLow.copy(alpha = 0.8f))
                            .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Height,
                                contentDescription = "Altura",
                                tint = SecondaryCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Altura: ${String.format(Locale.US, "%.2f m", height).replace('.', ',')} • Meta: ${String.format(Locale.US, "%.1f kg", targetWeight).replace('.', ',')}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface
                            )
                        }

                        Text(
                            text = "Editar ✏️",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryCyan,
                            modifier = Modifier
                                .testTag("btn_edit_height")
                                .clickable { showEditBiometricsModal = true }
                        )
                    }

                    // Automatic IMC Status Block
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ÍNDICE DE MASSA CORPORAL (IMC)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceVariant,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                NeonBadge(
                                    text = String.format(Locale.US, "%.1f", currentBmi).replace('.', ','),
                                    color = bmiColor
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(bmiColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = bmiCategory,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = bmiColor
                                )
                            }
                        }

                        val bmiIndicatorOffset = ((currentBmi - 15f) / 20f).coerceIn(0.05f, 0.95f)

                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(bmiIndicatorOffset)
                                        .height(14.dp),
                                    contentAlignment = Alignment.BottomEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Posição IMC",
                                        tint = bmiColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(modifier = Modifier.weight(0.25f).height(8.dp).background(SecondaryCyan))
                                Box(modifier = Modifier.weight(0.35f).height(8.dp).background(PrimaryNeon))
                                Box(modifier = Modifier.weight(0.25f).height(8.dp).background(TertiaryOrange))
                                Box(modifier = Modifier.weight(0.15f).height(8.dp).background(ErrorRed))
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "< 18.5 Baixo", fontSize = 9.sp, color = OnSurfaceVariant)
                                Text(text = "18.5 - 24.9 Normal", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PrimaryNeon)
                                Text(text = "25.0 - 29.9 Sobrepeso", fontSize = 9.sp, color = OnSurfaceVariant)
                                Text(text = "30.0+ Obesidade", fontSize = 9.sp, color = OnSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // 2. BOTÃO DE AÇÃO RÁPIDA: REGISTRAR NOVA PESAGEM
            Button(
                onClick = { showAddWeightModal = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryNeon,
                    contentColor = OnPrimaryContainer
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .testTag("btn_register_new_weight")
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Scale,
                    contentDescription = "Balança",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Registrar Nova Pesagem Real",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "Adicionar",
                    modifier = Modifier.size(18.dp)
                )
            }

            // 3. GRÁFICO DE EVOLUÇÃO DE PESO COM ESCALA DINÂMICA REAL
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = "Curva",
                                tint = PrimaryNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Curva de Peso Real",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainerLowest)
                                .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            listOf("1M", "3M", "6M", "Tudo").forEach { filterKey ->
                                val isSel = chartFilter == filterKey
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) PrimaryContainer else Color.Transparent)
                                        .clickable { chartFilter = filterKey }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = filterKey,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) Color.Black else OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp, 3.dp).background(PrimaryNeon, RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Registro Real (kg)", fontSize = 11.sp, color = OnSurface)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp, 2.dp).background(SecondaryCyan, RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Meta Alvo (${String.format(Locale.US, "%.1f", targetWeight).replace('.', ',')} kg)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SecondaryCyan
                            )
                        }
                    }

                    val points = if (allWeights.isNotEmpty()) allWeights.reversed() else emptyList()
                    val weightsList = points.map { it.weightKg }

                    val rawMin = if (weightsList.isNotEmpty()) min(weightsList.minOrNull() ?: currentWeightKg, targetWeight) else min(currentWeightKg, targetWeight)
                    val rawMax = if (weightsList.isNotEmpty()) max(weightsList.maxOrNull() ?: currentWeightKg, targetWeight) else max(currentWeightKg, targetWeight)
                    val minW = (rawMin - 2f).coerceAtLeast(20f)
                    val maxW = (rawMax + 2f).coerceAtLeast(minW + 4f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerLowest.copy(alpha = 0.6f))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp)) {
                            val w = size.width
                            val h = size.height

                            val targetY = (h - (((targetWeight - minW) / (maxW - minW)) * h)).coerceIn(0f, h)
                            drawLine(
                                color = SecondaryCyan,
                                start = Offset(0f, targetY),
                                end = Offset(w, targetY),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )

                            if (weightsList.size >= 2) {
                                val stepX = w / (weightsList.size - 1)
                                val path = Path()
                                val fillPath = Path()

                                weightsList.forEachIndexed { i, weightVal ->
                                    val px = i * stepX
                                    val py = (h - (((weightVal - minW) / (maxW - minW)) * h)).coerceIn(0f, h)
                                    if (i == 0) {
                                        path.moveTo(px, py)
                                        fillPath.moveTo(px, py)
                                    } else {
                                        path.lineTo(px, py)
                                        fillPath.lineTo(px, py)
                                    }
                                }

                                fillPath.lineTo(w, h)
                                fillPath.lineTo(0f, h)
                                fillPath.close()

                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            PrimaryNeon.copy(alpha = 0.35f),
                                            PrimaryNeon.copy(alpha = 0.0f)
                                        )
                                    )
                                )

                                drawPath(
                                    path = path,
                                    brush = Brush.horizontalGradient(listOf(SecondaryCyan, PrimaryNeon)),
                                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                                )

                                weightsList.forEachIndexed { i, weightVal ->
                                    val px = i * stepX
                                    val py = (h - (((weightVal - minW) / (maxW - minW)) * h)).coerceIn(0f, h)
                                    val isLast = i == weightsList.size - 1

                                    if (isLast) {
                                        drawCircle(color = PrimaryNeon.copy(alpha = 0.3f), radius = 10f, center = Offset(px, py))
                                        drawCircle(color = PrimaryNeon, radius = 6f, center = Offset(px, py))
                                    } else {
                                        drawCircle(color = SurfaceContainerLowest, radius = 5f, center = Offset(px, py))
                                        drawCircle(color = SecondaryCyan, radius = 3.5f, center = Offset(px, py))
                                    }
                                }
                            } else if (weightsList.size == 1) {
                                val px = w * 0.5f
                                val py = (h - (((weightsList[0] - minW) / (maxW - minW)) * h)).coerceIn(0f, h)
                                drawCircle(color = PrimaryNeon.copy(alpha = 0.3f), radius = 12f, center = Offset(px, py))
                                drawCircle(color = PrimaryNeon, radius = 6f, center = Offset(px, py))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val labels = if (points.isNotEmpty()) {
                            points.takeLast(5).map { it.dateDisplay }
                        } else {
                            listOf("Sem pesagens registradas")
                        }

                        labels.forEachIndexed { idx, label ->
                            val isLast = idx == labels.size - 1
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                                color = if (isLast) PrimaryNeon else OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 4. RESUMO DE PROGRESSO DO OBJETIVO REAL (3 Bento Cards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "VARIAÇÃO TOTAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.US, "%.1f", totalVariationKg).replace('.', ','),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNeon
                            )
                            Text(text = " kg", fontSize = 11.sp, color = OnSurfaceVariant)
                        }
                        Text(text = "✓ $goalProgressPct% da meta", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = PrimaryNeon)
                    }
                }

                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "FALTAM P/ META", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.US, "%.1f", diffToGoal).replace('.', ','),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryCyan
                            )
                            Text(text = " kg", fontSize = 11.sp, color = OnSurfaceVariant)
                        }
                        Text(
                            text = "🎯 Alvo: ${String.format(Locale.US, "%.1f", targetWeight).replace('.', ',')} kg",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SecondaryCyan
                        )
                    }
                }

                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "MÉDIA / PESAGEM", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$avgDeltaGrams",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TertiaryOrange
                            )
                            Text(text = " g", fontSize = 11.sp, color = OnSurfaceVariant)
                        }
                        Text(text = "${allWeights.size} pesagens", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = TertiaryOrange)
                    }
                }
            }

            // 5. HISTÓRICO DE PESAGENS EM LISTA (Feed SQLite / Room)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Scale,
                                contentDescription = "Pesagens",
                                tint = SecondaryCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Histórico de Pesagens Reais",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        }
                        Text(
                            text = "${allWeights.size} registros locais",
                            fontSize = 11.sp,
                            color = OnSurfaceVariant
                        )
                    }

                    if (allWeights.isEmpty()) {
                        Text(
                            text = "Nenhuma pesagem registrada ainda. Toque em 'Registrar Nova Pesagem Real' acima.",
                            fontSize = 12.sp,
                            color = OnSurfaceVariant
                        )
                    }

                    allWeights.forEach { record ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerHigh.copy(alpha = 0.5f))
                                .border(1.dp, OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (record.isBaseline) SecondaryCyan.copy(alpha = 0.15f)
                                            else SurfaceContainerHighest
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (record.isBaseline) Icons.Default.Flag else Icons.Default.CalendarToday,
                                        contentDescription = "Data",
                                        tint = if (record.isBaseline) SecondaryCyan else OnSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = record.dateDisplay,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = buildString {
                                            append("IMC: ${String.format(Locale.US, "%.1f", record.calculatedBmi).replace('.', ',')}")
                                            if (record.note.isNotBlank()) {
                                                append(" • ${record.note}")
                                            }
                                        },
                                        fontSize = 11.sp,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", record.weightKg).replace('.', ',')} kg",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface
                                    )

                                    if (record.isBaseline) {
                                        Text(
                                            text = "Base Inicial",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SecondaryCyan
                                        )
                                    } else {
                                        val isDown = record.deltaKg <= 0
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isDown) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = "Delta",
                                                tint = if (isDown) PrimaryNeon else TertiaryOrange,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "${if (record.deltaKg > 0) "+" else ""}${String.format(Locale.US, "%.1f kg", record.deltaKg).replace('.', ',')}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDown) PrimaryNeon else TertiaryOrange
                                            )
                                        }
                                    }
                                }

                                if (allWeights.size > 1) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteWeightRecord(record.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remover",
                                            tint = OnSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Modal: Registrar Nova Pesagem
    if (showAddWeightModal) {
        var inputWeight by remember { mutableStateOf(String.format(Locale.US, "%.1f", currentWeightKg)) }
        var inputNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddWeightModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Scale, contentDescription = null, tint = PrimaryNeon)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nova Pesagem", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Informe seu peso medido hoje:",
                        fontSize = 12.sp,
                        color = OnSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputWeight,
                        onValueChange = { inputWeight = it },
                        label = { Text("Peso Atual (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryNeon,
                            focusedLabelColor = PrimaryNeon
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_weight")
                    )

                    OutlinedTextField(
                        value = inputNote,
                        onValueChange = { inputNote = it },
                        label = { Text("Anotação / Contexto (opcional)") },
                        placeholder = { Text("Ex: Pós-treino de corrida") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = inputWeight.replace(',', '.').toFloatOrNull()
                        if (parsed != null && parsed > 20f && parsed < 300f) {
                            viewModel.addWeightRecord(
                                weightKg = parsed,
                                dateMillis = System.currentTimeMillis(),
                                note = inputNote
                            ) {
                                showAddWeightModal = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryNeon,
                        contentColor = OnPrimaryContainer
                    ),
                    modifier = Modifier.testTag("btn_save_weight_modal")
                ) {
                    Text("Salvar no SQLite", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddWeightModal = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal: Configurar Altura e Meta de Peso
    if (showEditBiometricsModal) {
        var inputHeight by remember { mutableStateOf(String.format(Locale.US, "%.2f", height)) }
        var inputTargetWeight by remember { mutableStateOf(String.format(Locale.US, "%.1f", targetWeight)) }

        AlertDialog(
            onDismissRequest = { showEditBiometricsModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Height, contentDescription = null, tint = SecondaryCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Altura & Meta de Peso", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputHeight,
                        onValueChange = { inputHeight = it },
                        label = { Text("Altura (m ou cm, ex: 1.78)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SecondaryCyan,
                            focusedLabelColor = SecondaryCyan
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_height")
                    )

                    OutlinedTextField(
                        value = inputTargetWeight,
                        onValueChange = { inputTargetWeight = it },
                        label = { Text("Meta de Peso (kg, ex: 72.0)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryNeon,
                            focusedLabelColor = PrimaryNeon
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rawH = inputHeight.replace(',', '.').toFloatOrNull()
                        val parsedH = if (rawH != null && rawH > 3.0f) rawH / 100f else rawH
                        val parsedTarget = inputTargetWeight.replace(',', '.').toFloatOrNull()
                        if (parsedH != null && parsedH in 1.0f..2.5f) {
                            viewModel.updateHeight(parsedH) {
                                if (parsedTarget != null && parsedTarget in 25f..300f) {
                                    viewModel.updateTargetWeight(parsedTarget) {
                                        showEditBiometricsModal = false
                                    }
                                } else {
                                    showEditBiometricsModal = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryCyan,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("btn_save_height_modal")
                ) {
                    Text("Salvar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditBiometricsModal = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
