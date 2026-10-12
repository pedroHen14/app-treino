package com.pedro.treinoapp.feature.history.presentation.progress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import com.pedro.treinoapp.R
import com.pedro.treinoapp.databinding.FragmentExerciseProgressBinding
import com.pedro.treinoapp.databinding.ItemProgressBestRepsBinding
import com.pedro.treinoapp.databinding.ItemProgressExecutionBinding
import com.pedro.treinoapp.feature.history.domain.model.ExerciseExecution
import com.pedro.treinoapp.feature.history.domain.model.PersonalRecords
import com.pedro.treinoapp.feature.history.domain.model.ProgressPoint
import com.pedro.treinoapp.feature.history.presentation.formatChart
import com.pedro.treinoapp.feature.history.presentation.formatSet
import com.pedro.treinoapp.feature.history.presentation.formatShort
import com.pedro.treinoapp.feature.session.presentation.formatWeight
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Evolução de um exercício: gráfico de carga máxima ou volume por sessão, recordes vigentes e execuções. */
@AndroidEntryPoint
class ExerciseProgressFragment : Fragment() {

    private var _binding: FragmentExerciseProgressBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExerciseProgressViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentExerciseProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.metric.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            viewModel.selectMetric(
                if (checkedId == R.id.metric_volume) ProgressMetric.VOLUME else ProgressMetric.MAX_WEIGHT,
            )
        }
        setUpChart()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun setUpChart() = with(binding.chart) {
        val textColor = MaterialColors.getColor(this, MaterialR.attr.colorOnSurfaceVariant)
        val gridColor = MaterialColors.getColor(this, MaterialR.attr.colorOutlineVariant)
        description.isEnabled = false
        legend.isEnabled = false
        axisRight.isEnabled = false
        setTouchEnabled(true)
        setPinchZoom(false)
        isDoubleTapToZoomEnabled = false
        setScaleEnabled(false)
        setNoDataText("")
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            this.textColor = textColor
            axisLineColor = gridColor
        }
        axisLeft.apply {
            this.textColor = textColor
            this.gridColor = gridColor
            axisLineColor = gridColor
            axisMinimum = 0f
        }
    }

    private fun render(state: ExerciseProgressUiState) {
        if (state.isLoading) return
        binding.toolbar.title = state.exerciseName
        val isEmpty = state.executions.isEmpty()
        binding.emptyMessage.isVisible = isEmpty
        binding.content.isVisible = !isEmpty
        if (isEmpty) return

        val checkedId = when (state.metric) {
            ProgressMetric.MAX_WEIGHT -> R.id.metric_max_weight
            ProgressMetric.VOLUME -> R.id.metric_volume
        }
        if (binding.metric.checkedButtonId != checkedId) binding.metric.check(checkedId)
        renderChart(state.points, state.metric)
        renderRecords(state.records)
        renderExecutions(state.executions)
    }

    private fun renderChart(points: List<ProgressPoint>, metric: ProgressMetric) {
        val label = getString(
            when (metric) {
                ProgressMetric.MAX_WEIGHT -> R.string.progress_chart_max_weight
                ProgressMetric.VOLUME -> R.string.progress_chart_volume
            },
        )
        binding.chartLabel.text = label
        binding.chartHint.isVisible = points.size < 2
        binding.chart.contentDescription = resources.getQuantityString(
            R.plurals.progress_chart_description,
            points.size,
            label,
            points.size,
            points.first().date.formatShort(),
            points.last().date.formatShort(),
        )

        // Eixo X por sessão (índice), com a data como rótulo: sessões no mesmo dia não se sobrepõem.
        val entries = points.mapIndexed { index, point ->
            val value = when (metric) {
                ProgressMetric.MAX_WEIGHT -> point.maxWeightGrams / GRAMS_PER_KG
                ProgressMetric.VOLUME -> point.volume / GRAMS_PER_KG
            }
            Entry(index.toFloat(), value)
        }
        val lineColor = MaterialColors.getColor(binding.chart, androidx.appcompat.R.attr.colorPrimary)
        val dataSet = LineDataSet(entries, label).apply {
            color = lineColor
            setCircleColor(lineColor)
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawCircleHole(false)
            setDrawValues(false)
            highLightColor = lineColor
        }
        binding.chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String =
                points.getOrNull(value.toInt())?.date?.formatChart().orEmpty()
        }
        binding.chart.xAxis.setLabelCount(points.size.coerceAtMost(MAX_X_LABELS), false)
        binding.chart.data = LineData(dataSet)
        binding.chart.invalidate()
    }

    private fun renderRecords(records: PersonalRecords) {
        val maxWeight = records.maxWeight
        binding.maxWeight.text = maxWeight?.let { formatSet(it.weightGrams, it.reps, requireContext()) }
        binding.maxWeightDate.text = maxWeight?.date?.formatShort()
        binding.maxWeightCard.contentDescription = listOfNotNull(
            getString(R.string.progress_record_weight),
            binding.maxWeight.text,
            binding.maxWeightDate.text,
        ).joinToString(", ")

        binding.bestReps.removeAllViews()
        records.bestRepsByWeight.forEach { mark ->
            val row = ItemProgressBestRepsBinding.inflate(layoutInflater, binding.bestReps, true)
            row.root.text = getString(
                R.string.progress_mark_with_date,
                formatSet(mark.weightGrams, mark.reps, requireContext()),
                mark.date.formatShort(),
            )
        }
    }

    private fun renderExecutions(executions: List<ExecutionItem>) {
        binding.executions.removeAllViews()
        executions.forEach { item ->
            val row = ItemProgressExecutionBinding.inflate(layoutInflater, binding.executions, true)
            row.date.text = item.execution.date.formatShort()
            row.sets.text = item.execution.formatSets()
            row.record.isVisible = item.hasRecord
            row.record.setText(R.string.progress_execution_record)
            row.root.contentDescription = listOfNotNull(
                row.date.text,
                row.sets.text,
                row.record.text.takeIf { item.hasRecord },
            ).joinToString(". ")
        }
    }

    /** Séries seguidas com a mesma carga ficam juntas: "60 kg × 10, 9 · 62,5 kg × 8". */
    private fun ExerciseExecution.formatSets(): String {
        val groups = mutableListOf<Pair<Int, MutableList<Int>>>()
        sets.forEach { set ->
            val last = groups.lastOrNull()
            if (last != null && last.first == set.weightGrams) last.second += set.reps
            else groups += set.weightGrams to mutableListOf(set.reps)
        }
        return groups.joinToString(" · ") { (weight, reps) ->
            getString(R.string.progress_execution_sets, formatWeight(weight), reps.joinToString(", "))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_EXERCISE_ID = "exerciseId"

        private const val GRAMS_PER_KG = 1000f
        private const val MAX_X_LABELS = 6
    }
}
