package com.pedro.treinoapp.feature.exercise.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercise")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Insert
    suspend fun insert(exercise: ExerciseEntity): Long

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Query("DELETE FROM exercise WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM session_exercise WHERE exercise_id = :id)")
    suspend fun isUsedInSession(id: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM template_exercise WHERE exercise_id = :id)")
    suspend fun isUsedInTemplate(id: Long): Boolean
}
