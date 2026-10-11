package com.pedro.treinoapp.feature.exercise.data.seed

import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import org.json.JSONArray

data class ExerciseSeed(val name: String, val muscleGroup: MuscleGroup)

/** Formato: `[{"name": "Supino reto", "muscleGroup": "CHEST"}, ...]`. */
object ExerciseSeedParser {

    fun parse(json: String): List<ExerciseSeed> {
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            ExerciseSeed(
                name = item.getString("name"),
                muscleGroup = MuscleGroup.valueOf(item.getString("muscleGroup")),
            )
        }
    }
}
