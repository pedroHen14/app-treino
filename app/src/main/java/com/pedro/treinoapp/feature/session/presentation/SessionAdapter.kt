package com.pedro.treinoapp.feature.session.presentation

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pedro.treinoapp.R
import com.pedro.treinoapp.databinding.ItemSessionExerciseFooterBinding
import com.pedro.treinoapp.databinding.ItemSessionExerciseHeaderBinding
import com.pedro.treinoapp.databinding.ItemSessionSetBinding
import com.pedro.treinoapp.feature.history.presentation.labelRes
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import com.pedro.treinoapp.feature.workout.presentation.format

class SessionAdapter(
    private val onExerciseMenu: (SessionListItem.Header, View) -> Unit,
    private val onWeightChange: (setId: Long, String) -> Unit,
    private val onRepsChange: (setId: Long, String) -> Unit,
    private val onStepWeight: (setId: Long, deltaGrams: Int) -> Unit,
    private val onCompletedClick: (setId: Long) -> Unit,
    private val onRemoveSet: (setId: Long) -> Unit,
    private val onAddSet: (sessionExerciseId: Long) -> Unit,
) : ListAdapter<SessionListItem, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is SessionListItem.Header -> TYPE_HEADER
        is SessionListItem.SetRow -> TYPE_SET
        is SessionListItem.Footer -> TYPE_FOOTER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderHolder(ItemSessionExerciseHeaderBinding.inflate(inflater, parent, false))
            TYPE_SET -> SetHolder(ItemSessionSetBinding.inflate(inflater, parent, false))
            else -> FooterHolder(ItemSessionExerciseFooterBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SessionListItem.Header -> (holder as HeaderHolder).bind(item)
            is SessionListItem.SetRow -> (holder as SetHolder).bind(item)
            is SessionListItem.Footer -> (holder as FooterHolder).bind(item)
        }
    }

    inner class HeaderHolder(private val binding: ItemSessionExerciseHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SessionListItem.Header) {
            val context = binding.root.context
            binding.name.text = item.exerciseName
            binding.target.isVisible = item.targetReps != null
            binding.target.text = item.targetReps?.let { context.getString(R.string.session_target_reps, it.format()) }
            binding.menu.contentDescription = context.getString(R.string.session_exercise_options, item.exerciseName)
            binding.menu.setOnClickListener { onExerciseMenu(item, it) }
        }
    }

    inner class SetHolder(private val binding: ItemSessionSetBinding) : RecyclerView.ViewHolder(binding.root) {

        private var setId: Long = 0
        private var isCompleted = false
        private var weightRevision = 0

        /** Evita que o texto escrito pelo próprio bind volte como edição do usuário. */
        private var isBinding = false

        init {
            binding.weight.watch { onWeightChange(setId, it) }
            binding.reps.watch { onRepsChange(setId, it) }
            binding.weightMinus.setOnClickListener { onStepWeight(setId, -WorkoutSet.WEIGHT_STEP_GRAMS) }
            binding.weightPlus.setOnClickListener { onStepWeight(setId, WorkoutSet.WEIGHT_STEP_GRAMS) }
            binding.remove.setOnClickListener { onRemoveSet(setId) }
            binding.done.setOnClickListener {
                binding.done.isChecked = isCompleted
                onCompletedClick(setId)
            }
        }

        fun bind(item: SessionListItem.SetRow) {
            val context = binding.root.context
            // Holder reaproveitado para outra série: os campos precisam receber os valores dela.
            val isOtherSet = item.setId != setId
            isBinding = true
            setId = item.setId
            isCompleted = item.isCompleted
            binding.number.text = item.number.toString()
            binding.weight.setTextIfChanged(item.weightText, force = isOtherSet || item.weightRevision != weightRevision)
            weightRevision = item.weightRevision
            binding.reps.setTextIfChanged(item.repsText, force = isOtherSet)
            isBinding = false

            binding.repsLayout.placeholderText = item.targetReps?.format()
            binding.weightLayout.placeholderText = context.getString(R.string.session_weight_placeholder)
            // O estado vem sempre do ViewModel: o toque desfaz a marcação local até a mudança chegar.
            binding.done.isChecked = item.isCompleted
            binding.done.isVisible = item.canToggleCompleted
            binding.done.contentDescription = context.getString(R.string.session_set_done, item.number)
            val record = item.records.firstOrNull()
            binding.record.isVisible = record != null
            binding.record.text = record?.let { context.getString(it.labelRes()) }
            binding.remove.contentDescription = context.getString(R.string.session_remove_set, item.number)
            binding.root.isActivated = item.isCompleted && item.canToggleCompleted
            binding.error.isVisible = item.error != null
            binding.error.text = item.error?.message(context)
        }

        /**
         * Campo em foco é de quem está digitando: a lista chega de forma assíncrona e pode trazer um
         * texto já ultrapassado, que moveria o cursor. Só [force] (botões ±, outra série) escreve nele.
         */
        private fun EditText.setTextIfChanged(value: String, force: Boolean) {
            if (text?.toString() == value || (hasFocus() && !force)) return
            setText(value)
            if (hasFocus()) setSelection(value.length)
        }

        private fun EditText.watch(onChange: (String) -> Unit) = addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!isBinding && bindingAdapterPosition != RecyclerView.NO_POSITION) onChange(s?.toString().orEmpty())
            }
        })
    }

    inner class FooterHolder(private val binding: ItemSessionExerciseFooterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SessionListItem.Footer) {
            binding.addSet.isEnabled = item.canAddSet
            binding.addSet.setOnClickListener { onAddSet(item.sessionExerciseId) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SessionListItem>() {
        override fun areItemsTheSame(oldItem: SessionListItem, newItem: SessionListItem) = oldItem.key == newItem.key

        override fun areContentsTheSame(oldItem: SessionListItem, newItem: SessionListItem) = oldItem == newItem
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_SET = 1
        const val TYPE_FOOTER = 2
    }
}
