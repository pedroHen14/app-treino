package com.pedro.treinoapp.feature.exercise.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["wger_id"], unique = true),
    ],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name", collate = ColumnInfo.NOCASE) val name: String,
    @ColumnInfo(name = "muscle_group") val muscleGroup: String,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "wger_id") val wgerId: Long? = null,
)
