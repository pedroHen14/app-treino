package com.pedro.treinoapp.feature.exercise.presentation.catalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pedro.treinoapp.R
import com.pedro.treinoapp.databinding.ItemExerciseBinding
import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.presentation.labelRes

/** Em [selectionMode] cada item mostra uma caixa de seleção no lugar do menu de opções. */
class ExerciseAdapter(
    private val selectionMode: Boolean,
    private val onClick: (Exercise) -> Unit,
    private val onMenuClick: (Exercise, View) -> Unit,
) : ListAdapter<ExerciseListItem, ExerciseAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemExerciseBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExerciseListItem) {
            val exercise = item.exercise
            val context = binding.root.context
            binding.name.text = exercise.name
            binding.details.text = buildList {
                add(context.getString(exercise.muscleGroup.labelRes()))
                if (exercise.isCustom) add(context.getString(R.string.exercise_label_custom))
                if (exercise.isArchived) add(context.getString(R.string.exercise_label_archived))
            }.joinToString(" · ")
            binding.root.alpha = if (exercise.isArchived) ARCHIVED_ALPHA else 1f
            binding.root.setOnClickListener { onClick(exercise) }
            binding.menu.isVisible = !selectionMode
            binding.menu.setOnClickListener { onMenuClick(exercise, it) }
            binding.check.isVisible = selectionMode
            binding.check.isChecked = item.isSelected
            if (selectionMode) {
                val state = if (item.isSelected) R.string.exercise_state_selected else R.string.exercise_state_not_selected
                ViewCompat.setStateDescription(binding.root, context.getString(state))
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ExerciseListItem>() {
        override fun areItemsTheSame(oldItem: ExerciseListItem, newItem: ExerciseListItem) =
            oldItem.exercise.id == newItem.exercise.id

        override fun areContentsTheSame(oldItem: ExerciseListItem, newItem: ExerciseListItem) = oldItem == newItem
    }

    private companion object {
        const val ARCHIVED_ALPHA = 0.6f
    }
}
