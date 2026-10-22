package com.pedro.treinoapp.feature.session.presentation.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pedro.treinoapp.R
import com.pedro.treinoapp.databinding.BottomSheetStartWorkoutBinding
import com.pedro.treinoapp.databinding.ItemStartWorkoutOptionBinding
import com.pedro.treinoapp.feature.session.presentation.message
import com.pedro.treinoapp.feature.session.presentation.openSession
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplateSummary
import com.pedro.treinoapp.feature.workout.presentation.labelRes
import com.pedro.treinoapp.feature.workout.presentation.shortLabelRes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Escolha do treino avulso, fora da agenda: qualquer modelo ou uma sessão vazia. */
@AndroidEntryPoint
class StartWorkoutBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetStartWorkoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StartWorkoutViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetStartWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.templates.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    // Poucos modelos: as linhas são recriadas a cada mudança.
    private fun render(templates: List<WorkoutTemplateSummary>?) {
        binding.progress.isVisible = templates == null
        binding.emptyMessage.isVisible = templates?.isEmpty() == true
        binding.options.removeAllViews()
        templates ?: return
        templates.forEach { template ->
            addOption(template.name, details(template)) { viewModel.onTemplateChosen(template) }
        }
        addOption(getString(R.string.start_workout_empty), getString(R.string.start_workout_empty_details)) {
            viewModel.onEmptySessionChosen()
        }
    }

    private fun addOption(name: String, details: String, onClick: () -> Unit) {
        val row = ItemStartWorkoutOptionBinding.inflate(layoutInflater, binding.options, true)
        row.name.text = name
        row.details.text = details
        row.root.setOnClickListener { onClick() }
    }

    private fun details(template: WorkoutTemplateSummary): String {
        val count = resources.getQuantityString(R.plurals.template_exercise_count, template.exerciseCount, template.exerciseCount)
        if (template.scheduledDays.isEmpty()) return count
        return "$count · " + template.scheduledDays.joinToString(", ") { getString(it.shortLabelRes()) }
    }

    private fun handleEvent(event: StartWorkoutEvent) {
        when (event) {
            is StartWorkoutEvent.AskMakeup -> askMakeup(event.template, event.date)
            is StartWorkoutEvent.AskAdvance -> askAdvance(event.template, event.date)
            is StartWorkoutEvent.OpenSession ->
                findNavController().openSession(event.sessionId, popUpTo = R.id.startWorkoutBottomSheet)
            is StartWorkoutEvent.ShowError ->
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(event.error.message(requireContext()))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
        }
    }

    private fun askMakeup(template: WorkoutTemplateSummary, date: LocalDate) = askLink(
        template, date,
        R.string.start_workout_makeup_title, R.string.start_workout_makeup_message, R.string.start_workout_makeup_yes,
    )

    private fun askAdvance(template: WorkoutTemplateSummary, date: LocalDate) = askLink(
        template, date,
        R.string.start_workout_advance_title, R.string.start_workout_advance_message, R.string.start_workout_advance_yes,
    )

    private fun askLink(template: WorkoutTemplateSummary, date: LocalDate, title: Int, message: Int, yes: Int) {
        val day = getString(date.dayOfWeek.labelRes()).lowercase(resources.configuration.locales[0])
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(getString(message, template.name, day))
            .setNegativeButton(R.string.start_workout_makeup_no) { _, _ -> viewModel.onLinkAnswered(template.id, null) }
            .setPositiveButton(yes) { _, _ -> viewModel.onLinkAnswered(template.id, date) }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
