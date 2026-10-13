package com.pedro.treinoapp.feature.workout.presentation.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pedro.treinoapp.R
import com.pedro.treinoapp.core.ui.bindPrimaryAction
import com.pedro.treinoapp.core.ui.primaryActionButton
import com.pedro.treinoapp.databinding.FragmentTemplateEditorBinding
import com.pedro.treinoapp.feature.exercise.presentation.catalog.ExerciseCatalogFragment
import com.pedro.treinoapp.feature.workout.presentation.message
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TemplateEditorFragment : Fragment() {

    private var _binding: FragmentTemplateEditorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TemplateEditorViewModel by viewModels()

    private val touchHelper = ItemTouchHelper(DragCallback())

    private val adapter = TemplateExerciseAdapter(
        onStartDrag = { touchHelper.startDrag(it) },
        onRemove = { viewModel.removeExercise(it.exerciseId) },
        onSetsChange = { id, value -> viewModel.onSetsChange(id, value) },
        onRepsMinChange = { id, value -> viewModel.onRepsMinChange(id, value) },
        onRepsMaxChange = { id, value -> viewModel.onRepsMaxChange(id, value) },
    )

    private val discardCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = confirmDiscard()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTemplateEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, discardCallback)

        binding.toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId != R.id.action_save) return@setOnMenuItemClickListener false
            viewModel.save()
            true
        }
        binding.toolbar.bindPrimaryAction(R.id.action_save, viewModel::save)
        binding.name.doAfterTextChanged { viewModel.onNameChange(it?.toString().orEmpty()) }
        binding.list.adapter = adapter
        // Cada tecla atualiza a linha; a animação de "change" trocaria o ViewHolder e o campo perderia o foco e o cursor.
        (binding.list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        touchHelper.attachToRecyclerView(binding.list)
        binding.addExercise.setOnClickListener { openExercisePicker() }

        observeSelectedExercises()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun observeSelectedExercises() {
        val handle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        handle.getLiveData<LongArray>(ExerciseCatalogFragment.RESULT_SELECTED_IDS)
            .observe(viewLifecycleOwner) { ids ->
                handle.remove<LongArray>(ExerciseCatalogFragment.RESULT_SELECTED_IDS)
                viewModel.addExercises(ids)
            }
    }

    private fun render(state: TemplateEditorUiState) {
        if (state.isSaved) {
            discardCallback.isEnabled = false
            findNavController().navigateUp()
            return
        }
        val context = requireContext()
        binding.toolbar.setTitle(if (state.isEditing) R.string.template_edit_title else R.string.template_new_title)
        binding.progress.isVisible = state.isLoading

        // Só escreve no campo quando o valor veio de fora (carga da edição), para não mexer no cursor.
        if (binding.name.text?.toString() != state.name) binding.name.setText(state.name)
        binding.nameLayout.error = state.nameError?.message(context)

        adapter.submitList(state.exercises)
        binding.emptyMessage.isVisible = !state.isLoading && state.exercises.isEmpty()
        binding.listError.isVisible = state.listError != null
        binding.listError.text = state.listError?.message(context)
        binding.generalError.isVisible = state.generalError != null
        binding.generalError.text = state.generalError?.message(context)

        val enabled = !state.isLoading && !state.isSaving
        binding.toolbar.menu.findItem(R.id.action_save).isEnabled = enabled
        binding.toolbar.primaryActionButton(R.id.action_save)?.isEnabled = enabled
        binding.addExercise.isEnabled = enabled
        discardCallback.isEnabled = state.hasChanges
    }

    private fun openExercisePicker() {
        findNavController().navigate(
            R.id.action_templateEditor_to_exerciseCatalog,
            bundleOf(
                ExerciseCatalogFragment.ARG_SELECTION_MODE to true,
                ExerciseCatalogFragment.ARG_EXCLUDED_IDS to viewModel.exerciseIds,
            ),
        )
    }

    private fun confirmDiscard() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.template_discard_title)
            .setMessage(R.string.template_discard_message)
            .setNegativeButton(R.string.template_keep_editing, null)
            .setPositiveButton(R.string.template_discard) { _, _ ->
                discardCallback.isEnabled = false
                findNavController().navigateUp()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        touchHelper.attachToRecyclerView(null)
        binding.list.adapter = null
        _binding = null
    }

    /** Arraste só pela alça (o toque longo fica livre para os campos de texto). */
    private inner class DragCallback : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

        override fun isLongPressDragEnabled() = false

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            viewModel.moveExercise(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
    }

    companion object {
        const val ARG_TEMPLATE_ID = "templateId"
        const val NEW_TEMPLATE = -1L
    }
}
