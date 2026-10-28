package com.example

import com.example.data.model.CalisthenicsExercises
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
    fun calisthenics_routine_has_20_exercises_with_valid_drawables() {
        val routine = CalisthenicsExercises.routine20
        assertEquals(20, routine.size)

        routine.forEachIndexed { index, exercise ->
            assertEquals(index + 1, exercise.id)
            assertTrue("Exercise ${exercise.id} name should not be blank", exercise.name.isNotBlank())
            assertTrue("Exercise ${exercise.id} instruction should not be blank", exercise.instruction.isNotBlank())
            assertTrue("Exercise ${exercise.id} muscleGroup should not be blank", exercise.muscleGroup.isNotBlank())
            assertTrue("Exercise ${exercise.id} duration should be positive", exercise.durationSeconds > 0)
            assertTrue("Exercise ${exercise.id} rest should be positive", exercise.restSeconds > 0)
            assertNotEquals("Exercise ${exercise.id} must have a valid drawable resource", 0, exercise.drawableResId)
        }
    }
}
