package com.pedro.treinoapp.feature.exercise.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class PagedResponseDto<T>(
    val results: List<T> = emptyList(),
)

@Serializable
data class ExerciseInfoDto(
    val id: Long,
    val category: CategoryDto? = null,
    val muscles: List<MuscleDto> = emptyList(),
    val translations: List<TranslationDto> = emptyList(),
)

@Serializable
data class CategoryDto(
    val id: Int,
)

@Serializable
data class MuscleDto(
    val id: Int,
)

@Serializable
data class TranslationDto(
    val name: String,
    val language: Int,
    /** HTML. */
    val description: String? = null,
)
