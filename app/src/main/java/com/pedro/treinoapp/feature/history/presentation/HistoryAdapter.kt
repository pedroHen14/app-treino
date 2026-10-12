package com.pedro.treinoapp.feature.history.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pedro.treinoapp.R
import com.pedro.treinoapp.databinding.ItemHistoryExerciseBinding
import com.pedro.treinoapp.databinding.ItemHistoryMonthBinding
import com.pedro.treinoapp.databinding.ItemHistorySessionBinding

class HistoryAdapter(
    private val onSessionClick: (sessionId: Long) -> Unit,
    private val onExerciseClick: (exerciseId: Long) -> Unit,
) : ListAdapter<HistoryListItem, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is HistoryListItem.Month -> TYPE_MONTH
        is HistoryListItem.Session -> TYPE_SESSION
        is HistoryListItem.Exercise -> TYPE_EXERCISE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_MONTH -> MonthHolder(ItemHistoryMonthBinding.inflate(inflater, parent, false))
            TYPE_SESSION -> SessionHolder(ItemHistorySessionBinding.inflate(inflater, parent, false))
            else -> ExerciseHolder(ItemHistoryExerciseBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HistoryListItem.Month -> (holder as MonthHolder).bind(item)
            is HistoryListItem.Session -> (holder as SessionHolder).bind(item)
            is HistoryListItem.Exercise -> (holder as ExerciseHolder).bind(item)
        }
    }

    class MonthHolder(private val binding: ItemHistoryMonthBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryListItem.Month) {
            binding.root.text = item.month.atDay(1).formatMonth()
        }
    }

    inner class SessionHolder(private val binding: ItemHistorySessionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistoryListItem.Session) {
            val context = binding.root.context
            val session = item.summary
            binding.dayOfWeek.text = session.date.formatDayOfWeek()
            binding.dayOfMonth.text = session.date.dayOfMonth.toString()
            binding.name.text = session.templateName ?: context.getString(R.string.history_free_workout)
            binding.details.text = listOf(
                context.resources.getQuantityString(R.plurals.history_exercise_count, session.exerciseCount, session.exerciseCount),
                session.duration.formatDuration(context),
            ).joinToString(" · ")
            binding.records.isVisible = session.recordCount > 0
            binding.records.text =
                context.resources.getQuantityString(R.plurals.history_record_count, session.recordCount, session.recordCount)
            binding.makeup.isVisible = session.makeupForDate != null
            binding.makeup.text = session.makeupForDate?.let {
                context.getString(R.string.history_makeup, it.formatDayOfWeek().lowercase())
            }
            binding.root.contentDescription = listOfNotNull(
                session.date.formatLong(),
                binding.name.text,
                binding.details.text,
                binding.records.text.takeIf { session.recordCount > 0 },
                binding.makeup.text.takeIf { session.makeupForDate != null },
            ).joinToString(". ")
            binding.root.setOnClickListener { onSessionClick(session.id) }
        }
    }

    inner class ExerciseHolder(private val binding: ItemHistoryExerciseBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistoryListItem.Exercise) {
            val context = binding.root.context
            val exercise = item.exercise
            binding.name.text = exercise.name
            binding.details.text = listOf(
                context.resources.getQuantityString(R.plurals.history_session_count, exercise.sessionCount, exercise.sessionCount),
                context.getString(R.string.history_last_done, exercise.lastDate.formatShort()),
            ).joinToString(" · ")
            binding.root.setOnClickListener { onExerciseClick(exercise.exerciseId) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<HistoryListItem>() {
        override fun areItemsTheSame(oldItem: HistoryListItem, newItem: HistoryListItem) = oldItem.key == newItem.key

        override fun areContentsTheSame(oldItem: HistoryListItem, newItem: HistoryListItem) = oldItem == newItem
    }

    private companion object {
        const val TYPE_MONTH = 0
        const val TYPE_SESSION = 1
        const val TYPE_EXERCISE = 2
    }
}
