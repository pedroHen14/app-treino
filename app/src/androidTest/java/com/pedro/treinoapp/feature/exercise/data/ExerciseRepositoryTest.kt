package com.pedro.treinoapp.feature.exercise.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedro.treinoapp.core.database.AppDatabase
import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ExerciseRepositoryImpl

    private val imported = Exercise(
        name = "Supino com halteres",
        muscleGroup = MuscleGroup.CHEST,
        isCustom = true,
        description = "Deite no banco.",
        wgerId = 75,
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = ExerciseRepositoryImpl(database.exerciseDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun storesDescriptionAndWgerId() = runBlocking {
        val id = repository.insert(imported)

        assertEquals(imported.copy(id = id), repository.getById(id))
    }

    @Test
    fun updateKeepsDescriptionAndWgerId() = runBlocking {
        val id = repository.insert(imported)

        repository.update(imported.copy(id = id, name = "Supino reto com halteres"))

        assertEquals(imported.copy(id = id, name = "Supino reto com halteres"), repository.getById(id))
    }

    @Test(expected = SQLiteConstraintException::class)
    fun wgerIdIsUnique(): Unit = runBlocking {
        repository.insert(imported)
        repository.insert(imported.copy(name = "Outro nome"))
    }
}
