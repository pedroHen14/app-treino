package com.pedro.treinoapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.pedro.treinoapp.databinding.ActivityMainBinding
import com.pedro.treinoapp.feature.session.domain.timer.RestTimer
import com.pedro.treinoapp.feature.session.domain.usecase.GetActiveSessionIdUseCase
import com.pedro.treinoapp.feature.session.presentation.RestFinishedVibrator
import com.pedro.treinoapp.feature.session.presentation.openSession
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var restTimer: RestTimer

    @Inject
    lateinit var getActiveSessionId: GetActiveSessionIdUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // A BottomNavigationView já trata o inset inferior por conta própria.
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        RestFinishedVibrator(applicationContext).observe(this, restTimer)
        // Só na abertura do app: rotação e recriação já restauram a pilha de navegação.
        if (savedInstanceState == null) resumeActiveSession(navController)
    }

    /** Sessão em andamento (ex.: o app foi fechado no meio do treino) volta a ser a tela aberta. */
    private fun resumeActiveSession(navController: NavController) {
        lifecycleScope.launch {
            val sessionId = getActiveSessionId() ?: return@launch
            navController.openSession(sessionId)
        }
    }
}
