package com.pedro.treinoapp.feature.exercise.presentation.wger

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pedro.treinoapp.R
import com.pedro.treinoapp.databinding.ItemWgerExerciseBinding
import com.pedro.treinoapp.feature.exercise.domain.model.WgerExercise
import com.pedro.treinoapp.feature.exercise.presentation.labelRes

class WgerExerciseAdapter(
    private val onClick: (WgerExercise) -> Unit,
) : ListAdapter<WgerExercise, WgerExerciseAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemWgerExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemWgerExerciseBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: WgerExercise) {
            val context = binding.root.context
            binding.name.text = exercise.name
            binding.details.text = buildList {
                add(context.getString(exercise.muscleGroup?.labelRes() ?: R.string.wger_muscle_group_unknown))
                if (!exercise.isNameInPortuguese) add(context.getString(R.string.wger_label_english))
            }.joinToString(" · ")
            binding.description.text = exercise.description
            binding.description.isVisible = exercise.description != null
            binding.root.setOnClickListener { onClick(exercise) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<WgerExercise>() {
        override fun areItemsTheSame(oldItem: WgerExercise, newItem: WgerExercise) = oldItem.wgerId == newItem.wgerId

        override fun areContentsTheSame(oldItem: WgerExercise, newItem: WgerExercise) = oldItem == newItem
    }
}
