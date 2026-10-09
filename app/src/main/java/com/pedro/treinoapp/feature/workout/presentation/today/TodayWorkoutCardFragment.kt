package com.pedro.treinoapp.feature.workout.presentation.today

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pedro.treinoapp.databinding.FragmentTodayWorkoutCardBinding
import dagger.hilt.android.AndroidEntryPoint

/** Card "Treino do dia" da tela Hoje. O conteúdo real entra no PBI 4. */
@AndroidEntryPoint
class TodayWorkoutCardFragment : Fragment() {

    private var _binding: FragmentTodayWorkoutCardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTodayWorkoutCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
