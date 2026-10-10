package com.pedro.treinoapp.feature.exercise.presentation.form

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pedro.treinoapp.R
import com.pedro.treinoapp.databinding.BottomSheetExerciseFormBinding
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import com.pedro.treinoapp.feature.exercise.presentation.labelRes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExerciseFormBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetExerciseFormBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExerciseFormViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetExerciseFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (dialog as BottomSheetDialog).behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        binding.muscleGroup.setSimpleItems(MuscleGroup.entries.map { getString(it.labelRes()) }.toTypedArray())
        binding.muscleGroup.setOnItemClickListener { _, _, position, _ ->
            viewModel.onMuscleGroupChange(MuscleGroup.entries[position])
        }
        binding.name.doAfterTextChanged { viewModel.onNameChange(it?.toString().orEmpty()) }
        binding.name.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) viewModel.save()
            false
        }
        binding.cancelButton.setOnClickListener { dismiss() }
        binding.saveButton.setOnClickListener { viewModel.save() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: ExerciseFormUiState) {
        if (state.isSaved) {
            dismiss()
            return
        }
        binding.title.setText(if (state.isEditing) R.string.exercise_form_edit_title else R.string.exercise_form_new_title)
        binding.progress.isVisible = state.isLoading

        // Só escreve no campo quando o valor veio de fora (carga da edição), para não mexer no cursor.
        if (binding.name.text?.toString() != state.name) binding.name.setText(state.name)
        val groupLabel = state.muscleGroup?.let { getString(it.labelRes()) }.orEmpty()
        if (binding.muscleGroup.text?.toString() != groupLabel) binding.muscleGroup.setText(groupLabel, false)

        binding.nameLayout.error = state.nameError?.let(::getString)
        binding.muscleGroupLayout.error = state.muscleGroupError?.let(::getString)
        binding.generalError.isVisible = state.generalError != null
        state.generalError?.let(binding.generalError::setText)

        val enabled = !state.isLoading && !state.isSaving
        binding.nameLayout.isEnabled = enabled
        binding.muscleGroupLayout.isEnabled = enabled
        binding.saveButton.isEnabled = enabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_EXERCISE_ID = "exerciseId"
        const val NEW_EXERCISE = -1L
    }
}
