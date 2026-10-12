package com.pedro.treinoapp.feature.history.domain.usecase

import com.pedro.treinoapp.feature.history.domain.DAY_1
import com.pedro.treinoapp.feature.history.domain.DAY_2
import com.pedro.treinoapp.feature.history.domain.DAY_3
import com.pedro.treinoapp.feature.history.domain.FakeHistoryRepository
import com.pedro.treinoapp.feature.history.domain.finishedSession
import com.pedro.treinoapp.feature.history.domain.model.ProgressPoint
import com.pedro.treinoapp.feature.history.domain.model.RecordMark
import com.pedro.treinoapp.feature.history.domain.model.RecordType.MAX_REPS_AT_WEIGHT
import com.pedro.treinoapp.feature.history.domain.model.RecordType.MAX_WEIGHT
import com.pedro.treinoapp.feature.history.domain.recordedSet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class HistoryUseCasesTest {

    /** Supino (1) e agachamento (2) em três sessões; a 2 e a 3 batem recordes de supino. */
    private fun repository() = FakeHistoryRepository(
        sessions = listOf(
            finishedSession(1, DAY_1, exerciseCount = 2),
            finishedSession(2, DAY_2),
            finishedSession(3, DAY_3),
        ),
        sets = listOf(
            recordedSet(11, 60_000, 10, DAY_1, sessionId = 1, exerciseId = 1),
            recordedSet(12, 60_000, 9, DAY_1, sessionId = 1, exerciseId = 1),
            recordedSet(13, 100_000, 5, DAY_1, sessionId = 1, exerciseId = 2),
            recordedSet(21, 62_500, 8, DAY_2, sessionId = 2, exerciseId = 1),
            recordedSet(31, 62_500, 9, DAY_3, sessionId = 3, exerciseId = 1),
        ),
    )

    @Test
    fun `history lists sessions newest first with duration, exercise count and records`() = runTest {
        val history = ObserveSessionHistoryUseCase(repository())().first()

        assertEquals(listOf(3L, 2L, 1L), history.map { it.id })
        assertEquals(Duration.ofHours(1), history.last().duration)
        assertEquals(2, history.last().exerciseCount)
        assertEquals(listOf(1, 1, 0), history.map { it.recordCount })
    }

    @Test
    fun `deleting a session recalculates the records of the following ones`() = runTest {
        val repository = repository()
        val useCase = ObserveSessionHistoryUseCase(repository)

        repository.deleteSession(2)

        // Sem a sessão 2, a sessão 3 passa a ser a primeira com 62,5 kg: recorde de carga, não de reps.
        val history = useCase().first()
        assertEquals(listOf(3L, 1L), history.map { it.id })
        assertEquals(listOf(1, 0), history.map { it.recordCount })
    }

    @Test
    fun `editing a set recalculates records`() = runTest {
        val repository = repository()

        repository.updateSet(21, weightGrams = 60_000, reps = 8)

        val records = ObserveSessionRecordsUseCase(repository)(sessionId = 3).first()
        assertEquals(mapOf(31L to setOf(MAX_WEIGHT)), records)
        assertEquals(0, ObserveSessionHistoryUseCase(repository)().first().first { it.id == 2L }.recordCount)
    }

    @Test
    fun `session records only include sets of that session`() = runTest {
        val records = ObserveSessionRecordsUseCase(repository())(sessionId = 3).first()

        assertEquals(mapOf(31L to setOf(MAX_REPS_AT_WEIGHT)), records)
    }

    @Test
    fun `session records consider a session still in progress`() = runTest {
        val repository = FakeHistoryRepository(
            sessions = listOf(finishedSession(1, DAY_1)),
            sets = listOf(
                recordedSet(11, 60_000, 10, DAY_1, sessionId = 1),
                recordedSet(91, 65_000, 6, DAY_2, sessionId = 9),
            ),
        )

        val records = ObserveSessionRecordsUseCase(repository)(sessionId = 9).first()

        assertEquals(mapOf(91L to setOf(MAX_WEIGHT)), records)
        assertTrue(ObserveSessionHistoryUseCase(repository)().first().all { it.recordCount == 0 })
    }

    @Test
    fun `progress lists executions newest first and chart points in chronological order`() = runTest {
        val progress = ObserveExerciseProgressUseCase(repository())(exerciseId = 1).first()

        assertEquals(listOf(3L, 2L, 1L), progress.executions.map { it.sessionId })
        assertEquals(
            listOf(
                ProgressPoint(DAY_1, maxWeightGrams = 60_000, volume = 1_140_000),
                ProgressPoint(DAY_2, maxWeightGrams = 62_500, volume = 500_000),
                ProgressPoint(DAY_3, maxWeightGrams = 62_500, volume = 562_500),
            ),
            progress.points,
        )
        assertEquals(RecordMark(62_500, 8, DAY_2), progress.records.maxWeight)
        assertEquals(
            listOf(RecordMark(62_500, 9, DAY_3), RecordMark(60_000, 10, DAY_1)),
            progress.records.bestRepsByWeight,
        )
        assertEquals(setOf(21L, 31L), progress.recordsBySet.keys)
    }

    @Test
    fun `progress of an exercise never done is empty`() = runTest {
        val progress = ObserveExerciseProgressUseCase(repository())(exerciseId = 99).first()

        assertTrue(progress.executions.isEmpty())
        assertTrue(progress.points.isEmpty())
        assertEquals(null, progress.records.maxWeight)
    }
}
