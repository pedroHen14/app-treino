package com.pedro.treinoapp.feature.workout.presentation.editor

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pedro.treinoapp.R
import com.pedro.treinoapp.databinding.ItemTemplateExerciseBinding
import com.pedro.treinoapp.feature.workout.presentation.message

class TemplateExerciseAdapter(
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
    private val onRemove: (EditorExercise) -> Unit,
    private val onSetsChange: (exerciseId: Long, String) -> Unit,
    private val onRepsMinChange: (exerciseId: Long, String) -> Unit,
    private val onRepsMaxChange: (exerciseId: Long, String) -> Unit,
) : ListAdapter<EditorExercise, TemplateExerciseAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemTemplateExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    @SuppressLint("ClickableViewAccessibility")
    inner class ViewHolder(private val binding: ItemTemplateExerciseBinding) : RecyclerView.ViewHolder(binding.root) {

        private var exerciseId: Long = 0

        /** Evita que o texto escrito pelo próprio bind volte como edição do usuário. */
        private var isBinding = false

        init {
            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag(this)
                false
            }
            binding.sets.watch { onSetsChange(exerciseId, it) }
            binding.repsMin.watch { onRepsMinChange(exerciseId, it) }
            binding.repsMax.watch { onRepsMaxChange(exerciseId, it) }
        }

        fun bind(item: EditorExercise) {
            val context = binding.root.context
            isBinding = true
            exerciseId = item.exerciseId
            binding.name.text = item.exerciseName
            binding.sets.setTextIfChanged(item.sets)
            binding.repsMin.setTextIfChanged(item.repsMin)
            binding.repsMax.setTextIfChanged(item.repsMax)
            isBinding = false
            binding.error.isVisible = item.error != null
            binding.error.text = item.error?.message(context)
            binding.remove.contentDescription = context.getString(R.string.template_remove_exercise, item.exerciseName)
            binding.remove.setOnClickListener { onRemove(item) }
        }

        /** Escreve só quando o valor mudou fora do campo, para não mexer no cursor de quem digita. */
        private fun EditText.setTextIfChanged(value: String) {
            if (text?.toString() != value) setText(value)
        }

        private fun EditText.watch(onChange: (String) -> Unit) = addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!isBinding && bindingAdapterPosition != RecyclerView.NO_POSITION) onChange(s?.toString().orEmpty())
            }
        })
    }

    private object DiffCallback : DiffUtil.ItemCallback<EditorExercise>() {
        override fun areItemsTheSame(oldItem: EditorExercise, newItem: EditorExercise) =
            oldItem.exerciseId == newItem.exerciseId

        override fun areContentsTheSame(oldItem: EditorExercise, newItem: EditorExercise) = oldItem == newItem
    }
}
