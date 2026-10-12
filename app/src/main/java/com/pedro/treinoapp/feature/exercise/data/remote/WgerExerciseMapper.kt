package com.pedro.treinoapp.feature.exercise.data.remote

import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import com.pedro.treinoapp.feature.exercise.domain.model.WgerExercise

private const val LANGUAGE_ENGLISH = 2
private const val LANGUAGE_PORTUGUESE = 7

/** Ids de `/api/v2/muscle/`. Serrátil anterior (3) fica de fora: não tem grupo equivalente. */
private val muscleGroupByMuscleId = mapOf(
    1 to MuscleGroup.BICEPS,
    2 to MuscleGroup.SHOULDERS,
    4 to MuscleGroup.CHEST,
    5 to MuscleGroup.TRICEPS,
    6 to MuscleGroup.ABS,
    7 to MuscleGroup.CALVES,
    8 to MuscleGroup.GLUTES,
    9 to MuscleGroup.BACK,
    10 to MuscleGroup.QUADRICEPS,
    11 to MuscleGroup.HAMSTRINGS,
    12 to MuscleGroup.BACK,
    13 to MuscleGroup.BICEPS,
    14 to MuscleGroup.ABS,
    15 to MuscleGroup.CALVES,
)

/** Ids de `/api/v2/exercisecategory/`. Braços, pernas e cardio são amplos demais para escolher um grupo. */
private val muscleGroupByCategoryId = mapOf(
    10 to MuscleGroup.ABS,
    11 to MuscleGroup.CHEST,
    12 to MuscleGroup.BACK,
    13 to MuscleGroup.SHOULDERS,
    14 to MuscleGroup.CALVES,
)

/**
 * Prefere a tradução em português; sem ela, usa a em inglês. A descrição segue a tradução
 * escolhida e, se estiver vazia, cai para a em inglês.
 *
 * @return nulo se o exercício não tem nenhuma das duas traduções.
 */
fun ExerciseInfoDto.toDomain(): WgerExercise? {
    val portuguese = translations.firstOrNull { it.language == LANGUAGE_PORTUGUESE && it.name.isNotBlank() }
    val english = translations.firstOrNull { it.language == LANGUAGE_ENGLISH && it.name.isNotBlank() }
    val chosen = portuguese ?: english ?: return null

    return WgerExercise(
        wgerId = id,
        name = chosen.name.trim(),
        isNameInPortuguese = chosen === portuguese,
        description = chosen.description?.htmlToPlainText() ?: english?.description?.htmlToPlainText(),
        muscleGroup = muscles.firstNotNullOfOrNull { muscleGroupByMuscleId[it.id] }
            ?: category?.let { muscleGroupByCategoryId[it.id] },
    )
}

private val lineBreakTags = Regex("(?i)<br\\s*/?>|</(p|li|div|h\\d)>")
private val listItemTag = Regex("(?i)<li[^>]*>")
private val anyTag = Regex("<[^>]+>")
private val whitespace = Regex("\\s+")

/**
 * As descrições do wger vêm em HTML simples (parágrafos e listas). Quebras de linha cruas
 * valem como espaço, como no navegador; só as tags quebram linha.
 *
 * @return nulo se ficar vazio.
 */
internal fun String.htmlToPlainText(): String? = this
    .replace(whitespace, " ")
    .replace(lineBreakTags, "\n")
    .replace(listItemTag, "• ")
    .replace(anyTag, "")
    .replace("&nbsp;", " ")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&amp;", "&")
    .lines()
    .map { it.replace(whitespace, " ").trim() }
    .filter { it.isNotEmpty() && it != "•" }
    .joinToString("\n")
    .ifEmpty { null }
