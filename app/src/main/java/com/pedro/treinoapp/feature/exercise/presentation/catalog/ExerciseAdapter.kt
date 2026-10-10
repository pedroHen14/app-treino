package com.pedro.treinoapp.feature.exercise.presentation.catalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pedro.treinoapp.R
import com.pedro.treinoapp.databinding.ItemExerciseBinding
import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.presentation.labelRes

class ExerciseAdapter(
    private val onClick: (Exercise) -> Unit,
    private val onMenuClick: (Exercise, View) -> Unit,
) : ListAdapter<Exercise, ExerciseAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemExerciseBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: Exercise) {
            val context = binding.root.context
            binding.name.text = exercise.name
            binding.details.text = buildList {
                add(context.getString(exercise.muscleGroup.labelRes()))
                if (exercise.isCustom) add(context.getString(R.string.exercise_label_custom))
                if (exercise.isArchived) add(context.getString(R.string.exercise_label_archived))
            }.joinToString(" · ")
            binding.root.alpha = if (exercise.isArchived) ARCHIVED_ALPHA else 1f
            binding.root.setOnClickListener { onClick(exercise) }
            binding.menu.setOnClickListener { onMenuClick(exercise, it) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise) = oldItem == newItem
    }

    private companion object {
        const val ARCHIVED_ALPHA = 0.6f
    }
}
