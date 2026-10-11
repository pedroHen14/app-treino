package com.pedro.treinoapp.feature.session.presentation.today

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.pedro.treinoapp.R
import com.pedro.treinoapp.databinding.FragmentTodayWorkoutCardBinding
import com.pedro.treinoapp.feature.session.domain.model.TodayPlan
import com.pedro.treinoapp.feature.session.presentation.message
import com.pedro.treinoapp.feature.session.presentation.openSession
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Card "Treino do dia" da tela Hoje: o treino previsto na agenda, ou descanso, com o botão para
 * iniciar. Com uma sessão em andamento, o card leva de volta a ela.
 */
@AndroidEntryPoint
class TodayWorkoutCardFragment : Fragment() {

    private var _binding: FragmentTodayWorkoutCardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TodayWorkoutCardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTodayWorkoutCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.secondaryButton.setOnClickListener { findNavController().navigate(R.id.action_global_startWorkout) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.plan.collect(::render) }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is TodayWorkoutCardEvent.OpenSession -> findNavController().openSession(event.sessionId)
                            is TodayWorkoutCardEvent.ShowError ->
                                Snackbar.make(binding.root, event.error.message(requireContext()), Snackbar.LENGTH_SHORT)
                                    .show()
                        }
                    }
                }
            }
        }
    }

    private fun render(plan: TodayPlan?) {
        binding.root.isInvisible = plan == null
        plan ?: return
        when {
            plan.activeSessionId != null -> {
                binding.title.text = plan.activeSessionName ?: getString(R.string.session_free_title)
                binding.subtitle.setText(R.string.today_workout_in_progress)
                showPrimary(R.string.today_workout_resume) { viewModel.resume() }
                binding.secondaryButton.isVisible = false
            }
            plan.isRestDay -> {
                binding.title.setText(R.string.schedule_rest)
                binding.subtitle.setText(R.string.today_workout_rest)
                binding.primaryButton.isVisible = false
                binding.secondaryButton.isVisible = true
            }
            plan.isScheduledDone -> {
                binding.title.text = plan.scheduledTemplateName
                binding.subtitle.setText(R.string.today_workout_done)
                binding.primaryButton.isVisible = false
                binding.secondaryButton.isVisible = true
            }
            else -> {
                binding.title.text = plan.scheduledTemplateName
                binding.subtitle.setText(R.string.today_workout_scheduled)
                showPrimary(R.string.today_workout_start) { viewModel.startScheduled() }
                binding.secondaryButton.isVisible = true
            }
        }
    }

    private fun showPrimary(textRes: Int, onClick: () -> Unit) {
        binding.primaryButton.isVisible = true
        binding.primaryButton.setText(textRes)
        binding.primaryButton.setOnClickListener { onClick() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
