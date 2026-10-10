package com.pedro.treinoapp.feature.today.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.pedro.treinoapp.databinding.FragmentTodayBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addMissingCards()
    }

    // Busca por tag para não duplicar cards restaurados pelo FragmentManager
    // (rotação ou volta de outra aba).
    private fun addMissingCards() {
        val missing = todayCards.filter { childFragmentManager.findFragmentByTag(it.tag) == null }
        if (missing.isEmpty()) return
        childFragmentManager.commit {
            setReorderingAllowed(true)
            missing.forEach { add(binding.cardsContainer.id, it.create(), it.tag) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
