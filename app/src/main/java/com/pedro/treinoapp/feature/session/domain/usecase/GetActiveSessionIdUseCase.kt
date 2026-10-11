package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import javax.inject.Inject

/** Sessão em andamento, para retomá-la ao reabrir o app. */
class GetActiveSessionIdUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    suspend operator fun invoke(): Long? = repository.getActiveId()
}
