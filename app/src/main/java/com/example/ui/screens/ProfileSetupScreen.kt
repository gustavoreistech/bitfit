package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonBadge
import com.example.ui.theme.BgDark
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.OnPrimaryContainer
import com.example.ui.theme.OnSurface
import com.example.ui.theme.OnSurfaceVariant
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.TertiaryOrange
import com.example.viewmodel.BitFitViewModel
import java.util.Locale
import kotlin.math.abs

@Composable
fun ProfileSetupScreen(
    viewModel: BitFitViewModel,
    isEditingExisting: Boolean = false,
    onDismiss: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsState()
    val latestWeight by viewModel.latestWeight.collectAsState()
    val focusManager = LocalFocusManager.current

    var nameInput by remember(settings.userName) {
        mutableStateOf(settings.userName)
    }
    var heightInput by remember(settings.heightMeters) {
        mutableStateOf(
            if (settings.isProfileConfigured) String.format(Locale.US, "%.2f", settings.heightMeters) else "1.75"
        )
    }
    var currentWeightInput by remember(latestWeight, settings.initialWeightKg) {
        val w = latestWeight?.weightKg ?: settings.initialWeightKg
        mutableStateOf(String.format(Locale.US, "%.1f", w))
    }
    var targetWeightInput by remember(settings.targetWeightKg) {
        mutableStateOf(String.format(Locale.US, "%.1f", settings.targetWeightKg))
    }
    var targetRunDistanceInput by remember(settings.targetRunDistanceMeters) {
        mutableStateOf(settings.targetRunDistanceMeters.toString())
    }
    var runFreqInput by remember(settings.targetRunFrequency) {
        mutableIntStateOf(settings.targetRunFrequency.coerceIn(1, 7))
    }
    var calisDaysInput by remember(settings.targetCalisDays) {
        mutableIntStateOf(settings.targetCalisDays.coerceIn(1, 7))
    }
    var errorMessage by remember { mutableStateOf("") }

    // Parse live numbers for preview (accepts both 1.78m and 178cm!)
    val rawHeight = heightInput.replace(',', '.').toFloatOrNull() ?: 1.75f
    val parsedHeightMeters = if (rawHeight > 3.0f) (rawHeight / 100f) else rawHeight
    val parsedWeightKg = currentWeightInput.replace(',', '.').toFloatOrNull() ?: 78.0f
    val parsedTargetWeightKg = targetWeightInput.replace(',', '.').toFloatOrNull() ?: 72.0f
    val parsedRunDist = targetRunDistanceInput.filter { it.isDigit() }.toIntOrNull() ?: 1600

    val previewBmi = if (parsedHeightMeters in 1.0f..2.5f && parsedWeightKg > 20f) {
        parsedWeightKg / (parsedHeightMeters * parsedHeightMeters)
    } else {
        24.0f
    }

    val (bmiLabel, bmiColor) = when {
        previewBmi < 18.5f -> "Abaixo do Peso" to SecondaryCyan
        previewBmi <= 24.9f -> "Peso Saudável" to PrimaryNeon
        previewBmi <= 29.9f -> "Sobrepeso" to TertiaryOrange
        else -> "Obesidade" to ErrorRed
    }

    val handleSaveProfile: () -> Unit = saveBlock@{
        focusManager.clearFocus()
        val cleanName = nameInput.trim()
        if (cleanName.isEmpty()) {
            errorMessage = "Por favor, digite seu nome para personalizar o app."
            return@saveBlock
        }
        if (parsedHeightMeters !in 1.0f..2.5f) {
            errorMessage = "Informe uma altura válida (ex: 1.78 ou 178)."
            return@saveBlock
        }
        if (parsedWeightKg !in 25.0f..300.0f || parsedTargetWeightKg !in 25.0f..300.0f) {
            errorMessage = "Informe valores de peso válidos em kg (ex: 78.5)."
            return@saveBlock
        }
        viewModel.saveUserProfile(
            userName = cleanName,
            heightMeters = parsedHeightMeters,
            currentWeightKg = parsedWeightKg,
            targetWeightKg = parsedTargetWeightKg,
            targetRunDistanceMeters = parsedRunDist.coerceIn(200, 42000),
            targetRunFrequency = runFreqInput,
            targetCalisDays = calisDaysInput,
            onSaved = onDismiss
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .navigationBarsPadding()
            .imePadding()
    ) {
        val isCompactHeight = maxHeight < 720.dp
        val sectionSpacing = if (isCompactHeight) 10.dp else 12.dp
        val cardPadding = if (isCompactHeight) 12.dp else 14.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            // Scrollable Form Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing)
            ) {
                // Top Header with Quick Save Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        NeonBadge(
                            text = if (isEditingExisting) "ATUALIZAR PERFIL" else "BEM-VINDO AO BITFIT • DADOS REAIS",
                            color = PrimaryNeon
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isEditingExisting) "Editar Perfil & Metas" else "Configure Seu Perfil Inicial",
                            fontSize = if (isCompactHeight) 18.sp else 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = handleSaveProfile,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryNeon,
                            contentColor = OnPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .testTag("btn_save_profile_top")
                            .heightIn(min = 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Salvar",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SALVAR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Text(
                    text = "Informe seu nome, altura, peso atual e metas. A partir de agora, todos os treinos e relatórios serão 100% reais.",
                    fontSize = 11.sp,
                    color = OnSurfaceVariant,
                    lineHeight = 15.sp
                )

                if (errorMessage.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ErrorRed.copy(alpha = 0.14f))
                            .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                    }
                }

                // 1. Identificação & Biometria Inicial
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    borderColor = PrimaryNeon.copy(alpha = 0.25f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(cardPadding),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = PrimaryNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "1. SEUS DADOS PESSOAIS & BIOMETRIA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNeon,
                                letterSpacing = 0.8.sp
                            )
                        }

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = {
                                nameInput = it
                                errorMessage = ""
                            },
                            label = { Text("Seu Nome ou Apelido") },
                            placeholder = { Text("Ex: Gustavo") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryNeon,
                                focusedLabelColor = PrimaryNeon
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_profile_name")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = heightInput,
                                onValueChange = {
                                    heightInput = it
                                    errorMessage = ""
                                },
                                label = { Text("Altura (m/cm)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                placeholder = { Text("1.78") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Height,
                                        contentDescription = null,
                                        tint = SecondaryCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SecondaryCyan,
                                    focusedLabelColor = SecondaryCyan
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_profile_height")
                            )

                            OutlinedTextField(
                                value = currentWeightInput,
                                onValueChange = {
                                    currentWeightInput = it
                                    errorMessage = ""
                                },
                                label = { Text("Peso Atual (kg)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                placeholder = { Text("78.5") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Scale,
                                        contentDescription = null,
                                        tint = PrimaryNeon,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryNeon,
                                    focusedLabelColor = PrimaryNeon
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_profile_weight")
                            )
                        }

                        // Live BMI Preview Pill
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceContainerLowest)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format(Locale.US, "Altura: %.2fm • IMC Inicial: %.1f", parsedHeightMeters, previewBmi),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface
                            )
                            Text(
                                text = bmiLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = bmiColor
                            )
                        }
                    }
                }

                // 2. Suas Metas (Peso Alvo + Corrida GPS + Frequência Semanal)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    borderColor = SecondaryCyan.copy(alpha = 0.25f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(cardPadding),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = SecondaryCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "2. SUAS METAS DE PESO E TREINO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryCyan,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = targetWeightInput,
                                onValueChange = {
                                    targetWeightInput = it
                                    errorMessage = ""
                                },
                                label = { Text("Meta Peso (kg)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                placeholder = { Text("72.0") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SecondaryCyan,
                                    focusedLabelColor = SecondaryCyan
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_profile_target_weight")
                            )

                            OutlinedTextField(
                                value = targetRunDistanceInput,
                                onValueChange = {
                                    targetRunDistanceInput = it
                                    errorMessage = ""
                                },
                                label = { Text("Meta Corrida (m)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                placeholder = { Text("1600") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsRun,
                                        contentDescription = null,
                                        tint = PrimaryNeon,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryNeon,
                                    focusedLabelColor = PrimaryNeon
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_profile_run_distance")
                            )
                        }

                        // Quick Run Distance Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1000 to "1 km", 1600 to "1.6 km", 3000 to "3 km", 5000 to "5 km").forEach { (meters, label) ->
                                val isSelected = parsedRunDist == meters
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) SecondaryCyan.copy(alpha = 0.2f) else SurfaceContainerHigh)
                                        .border(
                                            1.dp,
                                            if (isSelected) SecondaryCyan else OutlineVariant.copy(alpha = 0.3f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { targetRunDistanceInput = meters.toString() }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) SecondaryCyan else OnSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Weekly Frequency Selectors
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceContainerLowest)
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "CORRIDA / SEMANA",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceContainerHigh)
                                            .clickable { runFreqInput = (runFreqInput - 1).coerceAtLeast(1) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("-", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = OnSurface)
                                    }
                                    Text(
                                        text = "${runFreqInput}x / sem",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryCyan
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceContainerHigh)
                                            .clickable { runFreqInput = (runFreqInput + 1).coerceAtMost(7) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = OnSurface)
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceContainerLowest)
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "CALISTENIA / SEMANA",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceContainerHigh)
                                            .clickable { calisDaysInput = (calisDaysInput - 1).coerceAtLeast(1) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("-", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = OnSurface)
                                    }
                                    Text(
                                        text = "${calisDaysInput}x / sem",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TertiaryOrange
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceContainerHigh)
                                            .clickable { calisDaysInput = (calisDaysInput + 1).coerceAtMost(7) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = OnSurface)
                                    }
                                }
                            }
                        }

                        val weightDiff = abs(parsedWeightKg - parsedTargetWeightKg)
                        val goalDir = when {
                            parsedTargetWeightKg < parsedWeightKg -> String.format(Locale.US, "Objetivo: eliminar %.1f kg", weightDiff)
                            parsedTargetWeightKg > parsedWeightKg -> String.format(Locale.US, "Objetivo: ganhar %.1f kg de massa", weightDiff)
                            else -> "Objetivo: manutenção do peso corporal"
                        }
                        Text(
                            text = "$goalDir • Meta semanal total: ${runFreqInput + calisDaysInput} treinos",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryNeon
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            // Responsive Docked Bottom Save Bar (with extra bottom margin so it is never cut off by navigation bar or viewport edge)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceContainerLowest,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(
                        color = PrimaryNeon.copy(alpha = 0.25f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )

                    Button(
                        onClick = handleSaveProfile,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryNeon,
                            contentColor = OnPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .testTag("btn_save_initial_profile")
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Salvar Perfil",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEditingExisting) "SALVAR ALTERAÇÕES DO PERFIL" else "SALVAR PERFIL E COMEÇAR DO ZERO",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isEditingExisting) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 42.dp)
                        ) {
                            Text("Cancelar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
