package com.pedro.treinoapp.feature.workout.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.pedro.treinoapp.R
import com.pedro.treinoapp.databinding.FragmentWorkoutBinding
import com.pedro.treinoapp.databinding.ItemScheduleDayBinding
import com.pedro.treinoapp.databinding.ItemWorkoutTemplateBinding
import com.pedro.treinoapp.feature.workout.domain.model.ScheduleDay
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplateSummary
import com.pedro.treinoapp.feature.workout.presentation.editor.TemplateEditorFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WorkoutFragment : Fragment() {

    private var _binding: FragmentWorkoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkoutViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId != R.id.action_exercise_catalog) return@setOnMenuItemClickListener false
            findNavController().navigate(R.id.action_workout_to_exerciseCatalog)
            true
        }
        binding.addTemplate.setOnClickListener { openEditor(templateId = null) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: WorkoutUiState) {
        if (state.isLoading) return
        renderSchedule(state.schedule, state.templates)
        renderTemplates(state.templates)
    }

    // As listas são curtas (7 dias, poucos modelos), então as linhas são recriadas a cada mudança.
    private fun renderSchedule(days: List<ScheduleDay>, templates: List<WorkoutTemplateSummary>) {
        binding.schedule.removeAllViews()
        days.forEach { day ->
            val row = ItemScheduleDayBinding.inflate(layoutInflater, binding.schedule, true)
            val dayLabel = getString(day.dayOfWeek.labelRes())
            val templateLabel = day.templateName ?: getString(R.string.schedule_rest)
            row.day.text = dayLabel
            row.template.text = templateLabel
            row.template.setTextColor(
                MaterialColors.getColor(
                    row.template,
                    if (day.isRest) MaterialR.attr.colorOnSurfaceVariant
                    else MaterialR.attr.colorOnSurface,
                ),
            )
            row.root.contentDescription = getString(R.string.schedule_day_description, dayLabel, templateLabel)
            row.root.setOnClickListener { pickTemplateFor(day, templates) }
        }
    }

    private fun renderTemplates(templates: List<WorkoutTemplateSummary>) {
        binding.templates.removeAllViews()
        binding.templatesEmpty.isVisible = templates.isEmpty()
        templates.forEach { template ->
            val card = ItemWorkoutTemplateBinding.inflate(layoutInflater, binding.templates, true)
            card.name.text = template.name
            card.details.text = buildList {
                add(resources.getQuantityString(R.plurals.template_exercise_count, template.exerciseCount, template.exerciseCount))
                if (template.scheduledDays.isNotEmpty()) {
                    add(template.scheduledDays.joinToString(", ") { getString(it.shortLabelRes()) })
                }
            }.joinToString(" · ")
            card.root.setOnClickListener { openEditor(template.id) }
            card.menu.contentDescription = getString(R.string.template_item_options, template.name)
            card.menu.setOnClickListener { showTemplateMenu(template, it) }
        }
    }

    /** O primeiro item é descanso; os demais são os modelos, na ordem da lista. */
    private fun pickTemplateFor(day: ScheduleDay, templates: List<WorkoutTemplateSummary>) {
        val options = listOf(getString(R.string.schedule_rest)) + templates.map { it.name }
        val checked = if (day.isRest) 0 else templates.indexOfFirst { it.id == day.templateId } + 1
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(day.dayOfWeek.labelRes())
            .setSingleChoiceItems(options.toTypedArray(), checked) { dialog, which ->
                viewModel.setDay(day.dayOfWeek, templates.getOrNull(which - 1)?.id)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showTemplateMenu(template: WorkoutTemplateSummary, anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            inflate(R.menu.menu_template_item)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> openEditor(template.id)
                    R.id.action_duplicate -> viewModel.duplicate(
                        template,
                        CopyNameFormats(
                            first = getString(R.string.template_copy_name),
                            numbered = getString(R.string.template_copy_name_numbered),
                        ),
                    )
                    R.id.action_delete -> viewModel.requestDelete(template)
                }
                true
            }
        }.show()
    }

    private fun handleEvent(event: WorkoutEvent) {
        when (event) {
            is WorkoutEvent.ShowError ->
                Snackbar.make(binding.root, event.error.message(requireContext()), Snackbar.LENGTH_SHORT).show()

            is WorkoutEvent.Duplicated ->
                Snackbar.make(binding.root, R.string.template_duplicated, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_edit) { openEditor(event.templateId) }
                    .show()

            WorkoutEvent.Deleted ->
                Snackbar.make(binding.root, R.string.template_deleted, Snackbar.LENGTH_SHORT).show()

            is WorkoutEvent.ConfirmDelete -> confirmDelete(event)
        }
    }

    private fun confirmDelete(event: WorkoutEvent.ConfirmDelete) {
        val days = event.scheduledDays
        val message = if (days.isEmpty()) {
            getString(R.string.template_delete_message)
        } else {
            resources.getQuantityString(R.plurals.template_delete_message_scheduled, days.size, days.joinLabels(requireContext()))
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.template_delete_title, event.template.name))
            .setMessage(message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete(event.template) }
            .show()
    }

    private fun openEditor(templateId: Long?) {
        findNavController().navigate(
            R.id.action_workout_to_templateEditor,
            bundleOf(TemplateEditorFragment.ARG_TEMPLATE_ID to (templateId ?: TemplateEditorFragment.NEW_TEMPLATE)),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
