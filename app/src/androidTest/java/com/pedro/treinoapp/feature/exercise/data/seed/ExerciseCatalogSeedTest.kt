package com.pedro.treinoapp.feature.exercise.data.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedro.treinoapp.core.database.AppDatabase
import com.pedro.treinoapp.core.text.toNameKey
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseCatalogSeedTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: AppDatabase

    private val seeds by lazy {
        val json = context.assets.open(ExerciseCatalogSeedCallback.ASSET_FILE).bufferedReader().use { it.readText() }
        ExerciseSeedParser.parse(json)
    }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(ExerciseCatalogSeedCallback(context))
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun seedFileHasUniqueNonBlankNames() {
        assertTrue(seeds.isNotEmpty())
        assertTrue(seeds.none { it.name.isBlank() })
        assertEquals(seeds.size, seeds.map { it.name.toNameKey() }.toSet().size)
    }

    @Test
    fun seedFileCoversEveryMuscleGroup() {
        assertEquals(MuscleGroup.entries.toSet(), seeds.map { it.muscleGroup }.toSet())
    }

    @Test
    fun databaseCreationLoadsWholeCatalogAsActiveSeedExercises() = runBlocking {
        val stored = database.exerciseDao().getAll()

        assertEquals(seeds.map { it.name }.toSet(), stored.map { it.name }.toSet())
        assertTrue(stored.none { it.isCustom || it.isArchived })
    }
}
