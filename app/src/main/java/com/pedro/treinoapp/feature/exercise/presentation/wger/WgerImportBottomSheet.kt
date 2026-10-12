package com.pedro.treinoapp.feature.exercise.presentation.wger

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pedro.treinoapp.databinding.BottomSheetWgerImportBinding
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import com.pedro.treinoapp.feature.exercise.presentation.labelRes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Revisão do exercício antes de importar. O estado fica no ViewModel da tela de busca. */
@AndroidEntryPoint
class WgerImportBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetWgerImportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WgerSearchViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetWgerImportBinding.inflate(inflater, container, false)
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
        binding.cancelButton.setOnClickListener { viewModel.onImportDismissed() }
        binding.importButton.setOnClickListener { viewModel.import() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.importState.collect { form ->
                    // Importado, bloqueado ou cancelado: o ViewModel limpa o formulário e a folha fecha.
                    if (form == null) dismissAllowingStateLoss() else render(form)
                }
            }
        }
    }

    private fun render(state: WgerImportUiState) {
        // Só escreve no campo quando o valor veio de fora, para não mexer no cursor.
        if (binding.name.text?.toString() != state.name) binding.name.setText(state.name)
        val groupLabel = state.muscleGroup?.let { getString(it.labelRes()) }.orEmpty()
        if (binding.muscleGroup.text?.toString() != groupLabel) binding.muscleGroup.setText(groupLabel, false)

        binding.englishNameWarning.isVisible = !state.exercise.isNameInPortuguese
        binding.unknownGroupWarning.isVisible = state.exercise.muscleGroup == null
        binding.description.isVisible = state.exercise.description != null
        binding.description.text = state.exercise.description

        binding.nameLayout.error = state.nameError?.let(::getString)
        binding.muscleGroupLayout.error = state.muscleGroupError?.let(::getString)
        binding.generalError.isVisible = state.generalError != null
        state.generalError?.let(binding.generalError::setText)

        binding.nameLayout.isEnabled = !state.isSaving
        binding.muscleGroupLayout.isEnabled = !state.isSaving
        binding.importButton.isEnabled = !state.isSaving
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        viewModel.onImportDismissed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "WgerImportBottomSheet"
    }
}
