package com.pedro.treinoapp.feature.history.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import com.pedro.treinoapp.R
import com.pedro.treinoapp.databinding.FragmentHistoryBinding
import com.pedro.treinoapp.feature.history.presentation.detail.SessionDetailFragment
import com.pedro.treinoapp.feature.history.presentation.progress.ExerciseProgressFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()

    private val adapter = HistoryAdapter(
        onSessionClick = { sessionId ->
            findNavController().navigate(
                R.id.action_history_to_sessionDetail,
                bundleOf(SessionDetailFragment.ARG_SESSION_ID to sessionId),
            )
        },
        onExerciseClick = { exerciseId ->
            findNavController().navigate(
                R.id.action_history_to_exerciseProgress,
                bundleOf(ExerciseProgressFragment.ARG_EXERCISE_ID to exerciseId),
            )
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.list.adapter = adapter
        binding.tabs.selectTab(binding.tabs.getTabAt(viewModel.uiState.value.tab.ordinal))
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = viewModel.selectTab(HistoryTab.entries[tab.position])
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = binding.list.smoothScrollToPosition(0)
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: HistoryUiState) {
        if (state.isLoading) return
        if (binding.tabs.selectedTabPosition != state.tab.ordinal) {
            binding.tabs.selectTab(binding.tabs.getTabAt(state.tab.ordinal))
        }
        adapter.submitList(state.items)
        binding.emptyMessage.isVisible = state.items.isEmpty()
        binding.emptyMessage.setText(
            when (state.tab) {
                HistoryTab.SESSIONS -> R.string.history_sessions_empty
                HistoryTab.EXERCISES -> R.string.history_exercises_empty
            },
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.list.adapter = null
        _binding = null
    }
}
