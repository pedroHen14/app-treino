package com.pedro.treinoapp.feature.weeklysummary.presentation

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.pedro.treinoapp.R
import com.pedro.treinoapp.databinding.FragmentWeeklySummaryCardBinding
import com.pedro.treinoapp.databinding.ItemWeekDayBinding
import com.pedro.treinoapp.feature.history.presentation.formatDayOfWeek
import com.pedro.treinoapp.feature.history.presentation.formatVolumeKg
import com.pedro.treinoapp.feature.weeklysummary.domain.model.DayStatus
import com.pedro.treinoapp.feature.weeklysummary.domain.model.WeekDay
import com.pedro.treinoapp.feature.weeklysummary.domain.model.WeeklySummary
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Card "Esta semana" da tela Hoje: treinos realizados contra planejados, o status de cada dia,
 * volume e recordes da semana corrente.
 */
@AndroidEntryPoint
class WeeklySummaryCardFragment : Fragment() {

    private var _binding: FragmentWeeklySummaryCardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WeeklySummaryCardViewModel by viewModels()

    private var dayViews: List<ItemWeekDayBinding> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWeeklySummaryCardBinding.inflate(inflater, container, false)
        dayViews = List(7) { ItemWeekDayBinding.inflate(inflater, binding.days, true) }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.summary.collect(::render)
            }
        }
    }

    private fun render(summary: WeeklySummary?) {
        binding.root.isInvisible = summary == null
        summary ?: return

        binding.progress.text = if (summary.plannedCount == 0) {
            getString(R.string.weekly_summary_no_plan)
        } else {
            resources.getQuantityString(
                R.plurals.weekly_summary_progress,
                summary.plannedCount,
                summary.doneCount,
                summary.plannedCount,
            )
        }
        binding.extra.isVisible = summary.extraCount > 0
        binding.extra.text =
            resources.getQuantityString(R.plurals.weekly_summary_extra, summary.extraCount, summary.extraCount)

        summary.days.zip(dayViews).forEach { (day, view) -> bindDay(view, day, day.date == summary.today) }

        binding.volume.text = formatVolumeKg(summary.volume, requireContext())
        binding.records.text = summary.recordCount.toString()
        binding.volumeBox.contentDescription =
            "${getString(R.string.weekly_summary_volume)}: ${binding.volume.text}"
        binding.recordsBox.contentDescription =
            "${getString(R.string.weekly_summary_records)}: ${summary.recordCount}"
    }

    private fun bindDay(view: ItemWeekDayBinding, day: WeekDay, isToday: Boolean) {
        val style = day.status.style()
        view.statusIcon.setBackgroundResource(style.background)
        view.statusIcon.backgroundTintList = style.backgroundColor?.let { ColorStateList.valueOf(color(it)) }
        view.statusIcon.setImageResource(style.icon ?: 0)
        view.statusIcon.imageTintList = style.iconColor?.let { ColorStateList.valueOf(color(it)) }

        view.dayLabel.text = day.date.formatDayOfWeek()
        view.dayLabel.setTextColor(color(if (isToday) R.color.ink else R.color.ink_muted))

        val dayName = day.date.fullDayName()
        val status = getString(style.label)
        view.root.contentDescription = if (day.templateName != null) {
            getString(R.string.weekly_summary_day_with_template_description, dayName, day.templateName, status)
        } else {
            getString(R.string.weekly_summary_day_description, dayName, status)
        }
    }

    private fun color(@ColorRes res: Int): Int = requireContext().getColor(res)

    override fun onDestroyView() {
        super.onDestroyView()
        dayViews = emptyList()
        _binding = null
    }
}

/** Cor e ícone do círculo do dia; sem cor de fundo, o círculo é vazado. */
private data class DayStyle(
    @DrawableRes val background: Int,
    @ColorRes val backgroundColor: Int?,
    @DrawableRes val icon: Int?,
    @ColorRes val iconColor: Int?,
    @StringRes val label: Int,
)

private fun DayStatus.style(): DayStyle = when (this) {
    DayStatus.DONE -> DayStyle(
        R.drawable.bg_week_day, R.color.ink, R.drawable.ic_check, R.color.surface,
        R.string.weekly_summary_status_done,
    )
    DayStatus.MADE_UP -> DayStyle(
        R.drawable.bg_week_day, R.color.rule, R.drawable.ic_undo, R.color.ink,
        R.string.weekly_summary_status_made_up,
    )
    DayStatus.MISSED -> DayStyle(
        R.drawable.bg_week_day, R.color.perigo, R.drawable.ic_close, R.color.surface,
        R.string.weekly_summary_status_missed,
    )
    DayStatus.PENDING -> DayStyle(
        R.drawable.bg_week_day_outline, null, null, null,
        R.string.weekly_summary_status_pending,
    )
    DayStatus.REST -> DayStyle(
        R.drawable.bg_week_day, R.color.surface, R.drawable.ic_remove, R.color.ink_muted,
        R.string.weekly_summary_status_rest,
    )
}

/** "Segunda-feira". */
private fun LocalDate.fullDayName(): String {
    val locale = Locale.forLanguageTag("pt-BR")
    return dayOfWeek.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.titlecase(locale) }
}
