package com.pedro.treinoapp.feature.exercise.data.seed

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Carrega o catálogo inicial quando o banco é criado, ou seja, só na primeira execução.
 * Insere direto no [SupportSQLiteDatabase] para ficar na mesma transação da criação:
 * se o app morrer no meio, o banco não fica criado e vazio.
 */
class ExerciseCatalogSeedCallback @Inject constructor(
    @ApplicationContext private val context: Context,
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        val json = context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
        ExerciseSeedParser.parse(json).forEach { seed ->
            val values = ContentValues().apply {
                put("name", seed.name)
                put("muscle_group", seed.muscleGroup.name)
                put("is_custom", false)
                put("is_archived", false)
            }
            db.insert("exercise", SQLiteDatabase.CONFLICT_IGNORE, values)
        }
    }

    companion object {
        const val ASSET_FILE = "exercises_seed.json"
    }
}
