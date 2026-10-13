package com.pedro.treinoapp.feature.session.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.pedro.treinoapp.core.ui.bindPrimaryAction
import com.pedro.treinoapp.core.ui.primaryActionButton
import com.pedro.treinoapp.databinding.FragmentSessionBinding
import com.pedro.treinoapp.feature.exercise.presentation.catalog.ExerciseCatalogFragment
import com.pedro.treinoapp.feature.history.presentation.progress.ExerciseProgressFragment
import com.pedro.treinoapp.feature.session.domain.timer.RestTimerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Execução do treino. Sair da tela não encerra a sessão: ela continua em andamento e é retomada
 * pela tela Hoje ou ao reabrir o app.
 */
@AndroidEntryPoint
class SessionFragment : Fragment() {

    private var _binding: FragmentSessionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionViewModel by viewModels()

    /** Na edição do histórico, voltar salva: séries vazias não podem ficar numa sessão finalizada. */
    private val saveEditOnBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            viewModel.saveEdit()
        }
    }

    private val adapter = SessionAdapter(
        onExerciseMenu = ::showExerciseMenu,
        onWeightChange = { id, text -> viewModel.onWeightChange(id, text) },
        onRepsChange = { id, text -> viewModel.onRepsChange(id, text) },
        onStepWeight = { id, delta -> viewModel.stepWeight(id, delta) },
        onCompletedClick = { viewModel.onCompletedClick(it) },
        onRemoveSet = { viewModel.onRemoveSet(it) },
        onAddSet = { viewModel.onAddSet(it) },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, saveEditOnBack)
        binding.toolbar.setNavigationOnClickListener {
            if (viewModel.uiState.value.isEditing) viewModel.saveEdit() else findNavController().navigateUp()
        }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_finish -> finishOrSave()
                R.id.action_discard -> confirmDiscard()
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        binding.toolbar.bindPrimaryAction(R.id.action_finish, ::finishOrSave)
        binding.list.adapter = adapter
        // Cada digitação gera uma nova lista; sem animação de troca os campos não piscam.
        binding.list.itemAnimator = null
        binding.addExercise.setOnClickListener { openExercisePicker(replacing = null) }
        setUpTimerPanel()

        observeSelectedExercises()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun finishOrSave() {
        if (viewModel.uiState.value.isEditing) viewModel.saveEdit() else viewModel.requestFinish()
    }

    private fun setUpTimerPanel() {
        binding.timerPause.setOnClickListener {
            if (viewModel.uiState.value.timer is RestTimerState.Paused) viewModel.resumeRest() else viewModel.pauseRest()
        }
        binding.timerMinus.setOnClickListener { viewModel.adjustRest(add = false) }
        binding.timerPlus.setOnClickListener { viewModel.adjustRest(add = true) }
        binding.timerSkip.setOnClickListener { viewModel.skipRest() }
        binding.timerDismiss.setOnClickListener { viewModel.skipRest() }
        binding.timerDefault.setOnClickListener { chooseDefaultRest(viewModel.uiState.value.defaultRest) }
    }

    private fun observeSelectedExercises() {
        val handle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        handle.getLiveData<LongArray>(ExerciseCatalogFragment.RESULT_SELECTED_IDS)
            .observe(viewLifecycleOwner) { ids ->
                handle.remove<LongArray>(ExerciseCatalogFragment.RESULT_SELECTED_IDS)
                viewModel.onExercisesSelected(ids)
            }
    }

    private fun render(state: SessionUiState) {
        if (state.isClosed) {
            leave()
            return
        }
        binding.progress.isVisible = state.isLoading
        renderMode(state)
        adapter.submitList(state.items)
        binding.emptyMessage.isVisible = !state.isLoading && state.items.isEmpty()
        binding.addExercise.isEnabled = !state.isLoading
        if (!state.isEditing) renderTimer(state.timer, state.defaultRest)
    }

    private fun renderMode(state: SessionUiState) {
        val templateTitle = state.templateName ?: getString(R.string.session_free_title)
        binding.toolbar.title = if (state.isEditing) getString(R.string.session_edit_title) else templateTitle
        binding.toolbar.subtitle = if (state.isEditing) templateTitle else null
        val finishTitle = if (state.isEditing) R.string.action_save else R.string.session_finish
        binding.toolbar.menu.findItem(R.id.action_finish)?.setTitle(finishTitle)
        binding.toolbar.primaryActionButton(R.id.action_finish)?.setText(finishTitle)
        binding.toolbar.menu.findItem(R.id.action_discard)?.isVisible = !state.isEditing
        binding.timerPanel.isVisible = !state.isEditing
        saveEditOnBack.isEnabled = state.isEditing
    }

    private fun renderTimer(timer: RestTimerState, defaultRest: Duration) {
        val active = timer is RestTimerState.Running || timer is RestTimerState.Paused
        binding.timerControls.isVisible = active
        binding.timerProgress.isVisible = active
        binding.timerDefault.isVisible = timer is RestTimerState.Idle
        binding.timerDismiss.isVisible = timer is RestTimerState.Finished
        binding.timerDefault.text = getString(R.string.session_rest_default, defaultRest.formatClock())

        when (timer) {
            RestTimerState.Idle -> binding.timerLabel.setText(R.string.session_rest_idle)
            RestTimerState.Finished -> binding.timerLabel.setText(R.string.session_rest_finished)
            is RestTimerState.Running -> renderCountdown(timer.remaining, timer.total, paused = false)
            is RestTimerState.Paused -> renderCountdown(timer.remaining, timer.total, paused = true)
        }
    }

    private fun renderCountdown(remaining: Duration, total: Duration, paused: Boolean) {
        val clock = remaining.formatClock()
        binding.timerLabel.text =
            getString(if (paused) R.string.session_rest_paused else R.string.session_rest_running, clock)
        binding.timerProgress.max = total.inWholeSeconds.toInt().coerceAtLeast(1)
        binding.timerProgress.progress = remaining.inWholeSeconds.toInt()
        binding.timerPause.setIconResource(if (paused) R.drawable.ic_play else R.drawable.ic_pause)
        binding.timerPause.contentDescription =
            getString(if (paused) R.string.session_rest_resume else R.string.session_rest_pause)
    }

    private fun handleEvent(event: SessionEvent) {
        when (event) {
            is SessionEvent.ShowError -> Snackbar.make(binding.root, event.error.message(requireContext()), Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.addExercise)
                .show()

            is SessionEvent.ConfirmFinish -> confirmFinish(event.pendingSetCount)
            SessionEvent.OfferDiscard -> offerDiscard()
            SessionEvent.Saved -> {
                Toast.makeText(requireContext(), R.string.session_saved, Toast.LENGTH_SHORT).show()
                leave()
            }
            SessionEvent.Discarded -> {
                Toast.makeText(requireContext(), R.string.session_discarded, Toast.LENGTH_SHORT).show()
                leave()
            }
            SessionEvent.EditSaved -> {
                Toast.makeText(requireContext(), R.string.session_edit_saved, Toast.LENGTH_SHORT).show()
                leave()
            }
            SessionEvent.OfferDelete -> offerDelete()
            SessionEvent.Deleted -> {
                Toast.makeText(requireContext(), R.string.session_detail_deleted, Toast.LENGTH_SHORT).show()
                leave()
            }
        }
    }

    private fun confirmFinish(pendingSetCount: Int) {
        val message = if (pendingSetCount > 0) {
            resources.getQuantityString(R.plurals.session_finish_pending_message, pendingSetCount, pendingSetCount)
        } else {
            getString(R.string.session_finish_message)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.session_finish_title)
            .setMessage(message)
            .setNegativeButton(R.string.session_keep_training, null)
            .setPositiveButton(R.string.session_finish) { _, _ -> viewModel.finish() }
            .show()
    }

    private fun offerDiscard() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.session_no_completed_title)
            .setMessage(R.string.session_no_completed_message)
            .setNegativeButton(R.string.session_keep_training, null)
            .setPositiveButton(R.string.session_discard) { _, _ -> viewModel.discard() }
            .show()
    }

    private fun offerDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.session_edit_empty_title)
            .setMessage(R.string.session_edit_empty_message)
            .setNegativeButton(R.string.session_keep_editing, null)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete() }
            .show()
    }

    private fun confirmDiscard() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.session_discard_title)
            .setMessage(R.string.session_discard_message)
            .setNegativeButton(R.string.session_keep_training, null)
            .setPositiveButton(R.string.session_discard) { _, _ -> viewModel.discard() }
            .show()
    }

    private fun chooseDefaultRest(current: Duration) {
        val options = REST_OPTIONS
        val labels = options.map { it.formatClock() }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.session_rest_duration)
            .setSingleChoiceItems(labels, options.indexOf(current)) { dialog, which ->
                viewModel.setDefaultRest(options[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showExerciseMenu(header: SessionListItem.Header, anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            inflate(R.menu.menu_session_exercise)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_progress -> findNavController().navigate(
                        R.id.action_session_to_exerciseProgress,
                        bundleOf(ExerciseProgressFragment.ARG_EXERCISE_ID to header.exerciseId),
                    )
                    R.id.action_replace -> openExercisePicker(replacing = header.sessionExerciseId)
                    R.id.action_remove -> confirmRemoveExercise(header)
                }
                true
            }
        }.show()
    }

    private fun confirmRemoveExercise(header: SessionListItem.Header) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.session_remove_exercise_title, header.exerciseName))
            .setMessage(R.string.session_remove_exercise_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.session_remove_exercise) { _, _ ->
                viewModel.onRemoveExercise(header.sessionExerciseId)
            }
            .show()
    }

    /** Com [replacing], o catálogo abre em seleção única e a escolha troca esse exercício da sessão. */
    private fun openExercisePicker(replacing: Long?) {
        viewModel.pendingReplacement = replacing
        findNavController().navigate(
            R.id.action_session_to_exerciseCatalog,
            bundleOf(
                ExerciseCatalogFragment.ARG_SELECTION_MODE to true,
                ExerciseCatalogFragment.ARG_SINGLE_SELECTION to (replacing != null),
                ExerciseCatalogFragment.ARG_EXCLUDED_IDS to viewModel.uiState.value.exerciseIds.toLongArray(),
            ),
        )
    }

    /** Volta para a tela anterior uma vez só, mesmo que o fechamento chegue por estado e por evento. */
    private fun leave() {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.sessionFragment) navController.navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.list.adapter = null
        _binding = null
    }

    companion object {
        const val ARG_SESSION_ID = "sessionId"

        private val REST_OPTIONS: List<Duration> =
            listOf(30, 45, 60, 75, 90, 120, 150, 180, 240, 300).map { it.seconds }
    }
}
