package com.pedro.treinoapp.feature.history.presentation.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.pedro.treinoapp.R
import com.pedro.treinoapp.databinding.FragmentSessionDetailBinding
import com.pedro.treinoapp.databinding.ItemDetailExerciseBinding
import com.pedro.treinoapp.databinding.ItemDetailSetBinding
import com.pedro.treinoapp.feature.history.presentation.formatDayOfWeek
import com.pedro.treinoapp.feature.history.presentation.formatDuration
import com.pedro.treinoapp.feature.history.presentation.formatLong
import com.pedro.treinoapp.feature.history.presentation.formatSet
import com.pedro.treinoapp.feature.history.presentation.formatVolumeKg
import com.pedro.treinoapp.feature.history.presentation.labelRes
import com.pedro.treinoapp.feature.history.presentation.progress.ExerciseProgressFragment
import com.pedro.treinoapp.feature.session.presentation.message
import com.pedro.treinoapp.feature.session.presentation.openSession
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Sessão finalizada, só leitura. Editar abre a tela da sessão no modo de edição. */
@AndroidEntryPoint
class SessionDetailFragment : Fragment() {

    private var _binding: FragmentSessionDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSessionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> findNavController().openSession(viewModel.sessionId)
                R.id.action_delete -> confirmDelete()
                else -> return@setOnMenuItemClickListener false
            }
            true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: SessionDetailUiState) {
        if (state.isLoading) return
        if (state.isClosed) {
            leave()
            return
        }
        val date = state.date ?: return
        binding.toolbar.title = state.templateName ?: getString(R.string.history_free_workout)
        binding.date.text = date.formatLong()
        binding.summary.text = listOf(
            getString(R.string.session_detail_duration, state.duration.formatDuration(requireContext())),
            resources.getQuantityString(R.plurals.history_exercise_count, state.exercises.size, state.exercises.size),
        ).joinToString(" · ")
        binding.makeup.isVisible = state.makeupForDate != null
        binding.makeup.text = state.makeupForDate?.let { getString(R.string.history_makeup, it.formatDayOfWeek().lowercase()) }
        renderExercises(state.exercises)
    }

    // Uma sessão tem poucos exercícios e séries, então as linhas são recriadas a cada mudança.
    private fun renderExercises(exercises: List<DetailExercise>) {
        binding.exercises.removeAllViews()
        exercises.forEach { exercise ->
            val card = ItemDetailExerciseBinding.inflate(layoutInflater, binding.exercises, true)
            card.name.text = exercise.name
            card.progress.contentDescription = getString(R.string.session_detail_progress, exercise.name)
            card.progress.setOnClickListener { openProgress(exercise.exerciseId) }
            card.volume.text = getString(R.string.history_volume, formatVolumeKg(exercise.volume, requireContext()))
            exercise.sets.forEach { set ->
                val row = ItemDetailSetBinding.inflate(layoutInflater, card.sets, true)
                row.number.text = getString(R.string.session_detail_set_number, set.number)
                row.values.text = formatSet(set.weightGrams, set.reps, requireContext())
                val record = set.records.firstOrNull()
                row.record.isVisible = record != null
                row.record.text = record?.let { getString(it.labelRes()) }
                row.root.contentDescription =
                    listOfNotNull(row.number.text, row.values.text, row.record.text.takeIf { record != null }).joinToString(", ")
            }
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.session_detail_delete_title)
            .setMessage(R.string.session_detail_delete_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete() }
            .show()
    }

    private fun handleEvent(event: SessionDetailEvent) {
        when (event) {
            is SessionDetailEvent.ShowError ->
                Snackbar.make(binding.root, event.error.message(requireContext()), Snackbar.LENGTH_SHORT).show()
            SessionDetailEvent.Deleted -> {
                Toast.makeText(requireContext(), R.string.session_detail_deleted, Toast.LENGTH_SHORT).show()
                leave()
            }
        }
    }

    private fun openProgress(exerciseId: Long) {
        findNavController().navigate(
            R.id.action_sessionDetail_to_exerciseProgress,
            bundleOf(ExerciseProgressFragment.ARG_EXERCISE_ID to exerciseId),
        )
    }

    /** Volta uma vez só, mesmo que o fechamento chegue por estado e por evento. */
    private fun leave() {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.sessionDetailFragment) navController.navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_SESSION_ID = "sessionId"
    }
}
