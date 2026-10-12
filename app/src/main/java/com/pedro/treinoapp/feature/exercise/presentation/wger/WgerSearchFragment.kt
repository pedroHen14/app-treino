package com.pedro.treinoapp.feature.exercise.presentation.wger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.pedro.treinoapp.R
import com.pedro.treinoapp.databinding.FragmentWgerSearchBinding
import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.presentation.catalog.ExerciseCatalogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WgerSearchFragment : Fragment() {

    private var _binding: FragmentWgerSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WgerSearchViewModel by viewModels()

    private val adapter by lazy { WgerExerciseAdapter(onClick = viewModel::onExerciseSelected) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWgerSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.list.adapter = adapter
        binding.search.doAfterTextChanged { viewModel.onQueryChange(it?.toString().orEmpty()) }
        binding.retryButton.setOnClickListener { viewModel.retry() }
        if (savedInstanceState == null) binding.search.requestFocus()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.importState.collect { form ->
                        if (form != null) showImportSheet()
                    }
                }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: WgerSearchUiState) {
        adapter.submitList((state as? WgerSearchUiState.Results)?.items.orEmpty())
        binding.progress.isVisible = state == WgerSearchUiState.Loading

        val message = when (state) {
            WgerSearchUiState.Idle -> getString(R.string.wger_search_idle)
            WgerSearchUiState.Empty -> getString(R.string.wger_search_empty)
            is WgerSearchUiState.Error -> getString(state.messageRes)
            WgerSearchUiState.Loading, is WgerSearchUiState.Results -> null
        }
        binding.messageContainer.isVisible = message != null
        binding.message.text = message
        binding.retryButton.isVisible = state is WgerSearchUiState.Error
    }

    private fun showImportSheet() {
        val fragmentManager = childFragmentManager
        if (fragmentManager.isStateSaved || fragmentManager.findFragmentByTag(WgerImportBottomSheet.TAG) != null) return
        WgerImportBottomSheet().show(fragmentManager, WgerImportBottomSheet.TAG)
    }

    private fun handleEvent(event: WgerSearchEvent) {
        when (event) {
            is WgerSearchEvent.Imported -> {
                Snackbar.make(binding.root, getString(R.string.wger_imported, event.name), Snackbar.LENGTH_SHORT).show()
                // No modo seleção, o catálogo marca os importados ao voltar.
                findNavController().previousBackStackEntry?.savedStateHandle
                    ?.set(ExerciseCatalogFragment.RESULT_CREATED_IDS, event.importedIds.toLongArray())
            }

            is WgerSearchEvent.AlreadyInCatalog -> showAlreadyInCatalog(event.existing)
        }
    }

    private fun showAlreadyInCatalog(existing: Exercise) {
        val message = if (existing.isArchived) R.string.wger_already_in_catalog_archived else R.string.wger_already_in_catalog
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.wger_already_in_catalog_title)
            .setMessage(getString(message, existing.name))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.wger_show_existing) { _, _ -> showInCatalog(existing) }
            .show()
    }

    /** Volta ao catálogo já filtrado pelo exercício existente. */
    private fun showInCatalog(existing: Exercise) {
        val navController = findNavController()
        navController.previousBackStackEntry?.savedStateHandle?.apply {
            set(ExerciseCatalogFragment.RESULT_SUGGESTED_ARCHIVED, existing.isArchived)
            set(ExerciseCatalogFragment.RESULT_SUGGESTED_NAME, existing.name)
        }
        navController.navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.list.adapter = null
        _binding = null
    }
}
