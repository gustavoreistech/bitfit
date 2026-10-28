package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CalisthenicsExercises
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.TertiaryOrange

fun getExerciseDrawableRes(exerciseId: Int): Int {
    val found = CalisthenicsExercises.routine20.firstOrNull { it.id == exerciseId }?.drawableResId
    if (found != null && found != 0) return found
    return when (exerciseId) {
        1 -> R.drawable.ex_11
        2 -> R.drawable.ex_12
        3 -> R.drawable.ex_14
        4 -> R.drawable.ex_13
        5 -> R.drawable.ex_17
        6 -> R.drawable.ex_1
        7 -> R.drawable.ex_2
        8 -> R.drawable.ex_21_superman
        9 -> R.drawable.ex_3
        10 -> R.drawable.ex_7
        11 -> R.drawable.ex_15
        12 -> R.drawable.ex_19
        13 -> R.drawable.ex_8
        14 -> R.drawable.ex_9
        15 -> R.drawable.ex_20
        16 -> R.drawable.ex_16
        17 -> R.drawable.ex_18
        18 -> R.drawable.ex_4
        19 -> R.drawable.ex_5
        20 -> R.drawable.ex_6
        21 -> R.drawable.ex_10
        else -> R.drawable.ex_1
    }
}

@Composable
fun ExerciseIllustration(
    exerciseId: Int,
    modifier: Modifier = Modifier,
    isRest: Boolean = false,
    cornerRadius: Dp = 14.dp,
    drawableResId: Int = 0
) {
    val resId = if (drawableResId != 0) drawableResId else getExerciseDrawableRes(exerciseId)
    val borderColor = if (isRest) TertiaryOrange.copy(alpha = 0.75f) else PrimaryNeon.copy(alpha = 0.65f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White)
            .border(1.5.dp, borderColor, RoundedCornerShape(cornerRadius))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = "Demonstração do Exercício $exerciseId",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
