package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.FakeSessionRepository
import com.pedro.treinoapp.feature.session.domain.TODAY
import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.session
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** As sessões são de quinta, 15/10/2026; a semana delas vai de 12/10 a 18/10. */
class LinkSessionToDayUseCaseTest {

    private val invalid = SessionResult.Failure(SessionError.InvalidMakeupDate)

    @Test
    fun `finished session can advance a later day or make up an earlier one of its week`() = runTest {
        val repository = FakeSessionRepository(listOf(session(1, TODAY, templateId = 1)))
        val link = LinkSessionToDayUseCase(repository)

        assertEquals(SessionResult.Success(Unit), link(1, TODAY.plusDays(1)))
        assertEquals(TODAY.plusDays(1), repository.get(1)!!.makeupForDate)

        assertEquals(SessionResult.Success(Unit), link(1, TODAY.minusDays(3)))
        assertEquals(TODAY.minusDays(3), repository.get(1)!!.makeupForDate)
    }

    @Test
    fun `null removes the link`() = runTest {
        val repository = FakeSessionRepository(listOf(session(1, TODAY, templateId = 1, makeupForDate = TODAY.plusDays(1))))

        assertEquals(SessionResult.Success(Unit), LinkSessionToDayUseCase(repository)(1, null))
        assertNull(repository.get(1)!!.makeupForDate)
    }

    @Test
    fun `day must be another day of the session week`() = runTest {
        val link = LinkSessionToDayUseCase(FakeSessionRepository(listOf(session(1, TODAY, templateId = 1))))

        assertEquals(invalid, link(1, TODAY))
        assertEquals(invalid, link(1, TODAY.minusDays(4)))
        assertEquals(invalid, link(1, TODAY.plusDays(4)))
    }

    @Test
    fun `free workout cannot be linked`() = runTest {
        val link = LinkSessionToDayUseCase(FakeSessionRepository(listOf(session(1, TODAY))))

        assertEquals(invalid, link(1, TODAY.plusDays(1)))
    }

    @Test
    fun `active or unknown session fails`() = runTest {
        val link = LinkSessionToDayUseCase(FakeSessionRepository(listOf(session(1, TODAY, templateId = 1, finished = false))))
        val notFound = SessionResult.Failure(SessionError.NotFound)

        assertEquals(notFound, link(1, TODAY.plusDays(1)))
        assertEquals(notFound, link(99, null))
    }
}
