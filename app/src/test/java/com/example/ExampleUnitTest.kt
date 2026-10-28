package com.example

import com.example.data.model.ElasticStrengthExercises
import com.example.data.model.HiitExercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun hiit_routine_has_27_exercises_and_elastic_routine_has_7_items() {
        val hiitRoutine = HiitExercises.routine27
        assertEquals(27, hiitRoutine.size)

        hiitRoutine.forEachIndexed { index, exercise ->
            assertEquals(index + 1, exercise.id)
            assertTrue("Exercise ${exercise.id} name should not be blank", exercise.name.isNotBlank())
            assertTrue("Exercise ${exercise.id} instruction should not be blank", exercise.instruction.isNotBlank())
            assertTrue("Exercise ${exercise.id} muscleGroup should not be blank", exercise.muscleGroup.isNotBlank())
            assertTrue("Exercise ${exercise.id} duration should be positive", exercise.durationSeconds > 0)
            assertTrue("Exercise ${exercise.id} rest should be >= 0", exercise.restSeconds >= 0)
            assertNotEquals("Exercise ${exercise.id} must have a valid drawable resource", 0, exercise.drawableResId)
        }

        val elasticRoutine = ElasticStrengthExercises.routine7
        assertEquals(7, elasticRoutine.size)
        assertEquals(22, ElasticStrengthExercises.totalTargetSets)
    }
}

