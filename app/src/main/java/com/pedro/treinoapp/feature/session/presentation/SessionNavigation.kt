package com.pedro.treinoapp.feature.session.presentation

import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.navOptions
import com.pedro.treinoapp.R

/** Abre a tela da sessão. Com [popUpTo], tira essa tela da pilha (ex.: a escolha de treino que levou até aqui). */
fun NavController.openSession(sessionId: Long, @IdRes popUpTo: Int? = null) {
    navigate(
        R.id.action_global_session,
        bundleOf(SessionFragment.ARG_SESSION_ID to sessionId),
        popUpTo?.let { destination -> navOptions { popUpTo(destination) { inclusive = true } } },
    )
}
