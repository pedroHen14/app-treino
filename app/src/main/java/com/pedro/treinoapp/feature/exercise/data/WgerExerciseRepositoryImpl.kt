package com.pedro.treinoapp.feature.exercise.data

import com.pedro.treinoapp.feature.exercise.data.remote.WgerApi
import com.pedro.treinoapp.feature.exercise.data.remote.toDomain
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.model.WgerExercise
import com.pedro.treinoapp.feature.exercise.domain.repository.WgerExerciseRepository
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class WgerExerciseRepositoryImpl @Inject constructor(
    private val api: WgerApi,
) : WgerExerciseRepository {

    override suspend fun search(term: String): ExerciseResult<List<WgerExercise>> = try {
        val response = api.searchExercises(term)
        ExerciseResult.Success(response.results.mapNotNull { it.toDomain() })
    } catch (e: IOException) {
        ExerciseResult.Failure(ExerciseError.NoConnection)
    } catch (e: HttpException) {
        ExerciseResult.Failure(ExerciseError.ServiceUnavailable)
    } catch (e: SerializationException) {
        ExerciseResult.Failure(ExerciseError.ServiceUnavailable)
    }
}
