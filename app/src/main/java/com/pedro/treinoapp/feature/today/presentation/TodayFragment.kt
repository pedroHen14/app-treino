package com.pedro.treinoapp.feature.today.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pedro.treinoapp.R
import com.pedro.treinoapp.databinding.FragmentTodayBinding
import com.pedro.treinoapp.feature.settings.domain.model.ThemeMode
import com.pedro.treinoapp.feature.settings.presentation.ThemeViewModel
import com.pedro.treinoapp.feature.settings.presentation.labelRes
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    private val themeViewModel: ThemeViewModel by viewModels()

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
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_theme -> {
                    showThemeDialog()
                    true
                }
                else -> false
            }
        }
        addMissingCards()
    }

    private fun showThemeDialog() {
        val modes = ThemeMode.entries
        val labels = modes.map { getString(it.labelRes) }.toTypedArray()
        val checked = modes.indexOf(themeViewModel.themeMode.value)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.theme_title)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                themeViewModel.select(modes[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
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
