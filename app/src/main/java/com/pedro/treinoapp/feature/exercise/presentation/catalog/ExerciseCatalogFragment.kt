package com.pedro.treinoapp.feature.exercise.presentation.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.pedro.treinoapp.R
import com.pedro.treinoapp.databinding.FragmentExerciseCatalogBinding
import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import com.pedro.treinoapp.feature.exercise.presentation.form.ExerciseFormBottomSheet
import com.pedro.treinoapp.feature.exercise.presentation.labelRes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExerciseCatalogFragment : Fragment() {

    private var _binding: FragmentExerciseCatalogBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExerciseCatalogViewModel by viewModels()

    private val adapter by lazy {
        ExerciseAdapter(
            selectionMode = viewModel.isSelectionMode,
            onClick = {
                when {
                    viewModel.isSelectionMode -> viewModel.toggleSelection(it)
                    it.isCustom -> openForm(it.id)
                }
            },
            onMenuClick = ::showItemMenu,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentExerciseCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpToolbar()
        setUpMuscleGroupChips()
        binding.list.adapter = adapter
        binding.search.doAfterTextChanged { viewModel.onQueryChange(it?.toString().orEmpty()) }
        if (viewModel.isSelectionMode) binding.addButton.isVisible = false
        binding.addButton.setOnClickListener {
            if (viewModel.isSelectionMode) returnSelection() else openForm(exerciseId = null)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        adapter.submitList(state.items)
                        binding.emptyMessage.visibility =
                            if (!state.isLoading && state.items.isEmpty()) View.VISIBLE else View.GONE
                        if (viewModel.isSelectionMode) renderSelectionButton(state.selectedCount)
                    }
                }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun renderSelectionButton(selectedCount: Int) {
        binding.addButton.text = getString(R.string.exercise_add_selected, selectedCount)
        if (selectedCount > 0) binding.addButton.show() else binding.addButton.hide()
    }

    private fun returnSelection() {
        val navController = findNavController()
        navController.previousBackStackEntry?.savedStateHandle?.set(RESULT_SELECTED_IDS, viewModel.selection)
        navController.navigateUp()
    }

    private fun setUpToolbar() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        if (viewModel.isSelectionMode) {
            binding.toolbar.setTitle(R.string.exercise_select_title)
            binding.toolbar.menu.clear()
            return
        }
        val showArchived = binding.toolbar.menu.findItem(R.id.action_show_archived)
        showArchived.isChecked = viewModel.filter.value.includeArchived
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId != R.id.action_show_archived) return@setOnMenuItemClickListener false
            item.isChecked = !item.isChecked
            viewModel.onShowArchivedChange(item.isChecked)
            true
        }
    }

    private fun setUpMuscleGroupChips() {
        val selected = viewModel.filter.value.muscleGroup
        val options = listOf<MuscleGroup?>(null) + MuscleGroup.entries
        options.forEach { group ->
            val chip = layoutInflater.inflate(R.layout.chip_muscle_group, binding.muscleGroups, false) as Chip
            chip.id = View.generateViewId()
            chip.tag = group
            chip.text = getString(group?.labelRes() ?: R.string.exercise_filter_all)
            chip.isChecked = group == selected
            binding.muscleGroups.addView(chip)
        }
        binding.muscleGroups.setOnCheckedStateChangeListener { group, checkedIds ->
            val chip = checkedIds.firstOrNull()?.let { group.findViewById<Chip>(it) }
            viewModel.onMuscleGroupSelected(chip?.tag as MuscleGroup?)
        }
    }

    private fun showItemMenu(exercise: Exercise, anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            inflate(R.menu.menu_exercise_item)
            menu.findItem(R.id.action_edit).isVisible = exercise.isCustom
            menu.findItem(R.id.action_delete).isVisible = exercise.isCustom
            menu.findItem(R.id.action_archive).isVisible = !exercise.isArchived
            menu.findItem(R.id.action_unarchive).isVisible = exercise.isArchived
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> openForm(exercise.id)
                    R.id.action_archive -> viewModel.archive(exercise)
                    R.id.action_unarchive -> viewModel.unarchive(exercise)
                    R.id.action_delete -> confirmDelete(exercise)
                }
                true
            }
        }.show()
    }

    private fun confirmDelete(exercise: Exercise) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.exercise_delete_title, exercise.name))
            .setMessage(R.string.exercise_delete_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete(exercise) }
            .show()
    }

    private fun handleEvent(event: ExerciseCatalogEvent) {
        when (event) {
            is ExerciseCatalogEvent.ShowMessage ->
                Snackbar.make(binding.root, event.messageRes, Snackbar.LENGTH_SHORT).show()

            is ExerciseCatalogEvent.Archived ->
                Snackbar.make(binding.root, R.string.exercise_archived, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_undo) { viewModel.unarchive(event.exercise) }
                    .show()

            is ExerciseCatalogEvent.OfferArchive ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.exercise_cannot_delete_title)
                    .setMessage(event.reasonRes)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.action_archive) { _, _ -> viewModel.archive(event.exercise) }
                    .show()
        }
    }

    private fun openForm(exerciseId: Long?) {
        findNavController().navigate(
            R.id.action_exerciseCatalog_to_exerciseForm,
            bundleOf(ExerciseFormBottomSheet.ARG_EXERCISE_ID to (exerciseId ?: ExerciseFormBottomSheet.NEW_EXERCISE)),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.list.adapter = null
        _binding = null
    }

    companion object {
        /** Abre o catálogo para escolher exercícios; a escolha volta em [RESULT_SELECTED_IDS]. */
        const val ARG_SELECTION_MODE = "selectionMode"

        /** Exercícios que não devem aparecer no modo seleção (ex.: os que o modelo já tem). */
        const val ARG_EXCLUDED_IDS = "excludedIds"

        /** `LongArray` gravado no `SavedStateHandle` de quem abriu o catálogo. */
        const val RESULT_SELECTED_IDS = "selectedExerciseIds"
    }
}
