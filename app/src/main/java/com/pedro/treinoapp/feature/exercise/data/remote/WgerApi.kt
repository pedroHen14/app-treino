package com.pedro.treinoapp.feature.exercise.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface WgerApi {

    /** Busca aproximada pelo nome nas traduções dos idiomas pedidos, ordenada por relevância. */
    @GET("exerciseinfo/")
    suspend fun searchExercises(
        @Query("name__search") term: String,
        @Query("language__code") languages: String = "pt,en",
        @Query("limit") limit: Int = 20,
    ): PagedResponseDto<ExerciseInfoDto>

    companion object {
        const val BASE_URL = "https://wger.de/api/v2/"
    }
}
