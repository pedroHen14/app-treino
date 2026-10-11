package com.pedro.treinoapp.feature.session.domain

import com.pedro.treinoapp.feature.session.domain.model.NewSessionExercise
import com.pedro.treinoapp.feature.session.domain.model.NewWorkoutSession
import com.pedro.treinoapp.feature.session.domain.model.SessionDay
import com.pedro.treinoapp.feature.session.domain.model.SessionExercise
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSession
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate

/** Em memória; o nome do exercício é sempre "Exercício <id>", como nas fixtures de modelo. */
class FakeSessionRepository(initial: List<WorkoutSession> = emptyList()) : SessionRepository {

    private val sessions = MutableStateFlow(initial)
    private var nextId = 1000L

    val current: List<WorkoutSession> get() = sessions.value

    override fun observeActive(): Flow<WorkoutSession?> = sessions.map { list -> list.find { it.isActive } }

    override suspend fun getActiveId(): Long? = sessions.value.find { it.isActive }?.id

    override fun observe(id: Long): Flow<WorkoutSession?> = sessions.map { list -> list.find { it.id == id } }

    override suspend fun get(id: Long): WorkoutSession? = sessions.value.find { it.id == id }

    override fun observeFinishedTemplateIdsOn(date: LocalDate): Flow<List<Long?>> =
        sessions.map { list -> list.filter { it.date == date && !it.isActive }.map { it.templateId } }

    override suspend fun getSessionDays(from: LocalDate, to: LocalDate): List<SessionDay> =
        sessions.value.filter { it.date in from..to }.map { SessionDay(it.date, it.templateId, it.makeupForDate) }

    override suspend fun getLastCompletedSets(exerciseId: Long): List<WorkoutSet> =
        sessions.value
            .filter { !it.isActive }
            .sortedWith(compareByDescending<WorkoutSession> { it.date }.thenByDescending { it.startedAt })
            .firstNotNullOfOrNull { session ->
                session.exercises.lastOrNull { e -> e.exerciseId == exerciseId && e.sets.any { it.isCompleted } }
            }
            ?.sets?.filter { it.isCompleted }
            .orEmpty()

    override suspend fun create(session: NewWorkoutSession): Long {
        val id = nextId++
        val created = WorkoutSession(
            id = id,
            templateId = session.templateId,
            templateName = session.templateName,
            date = session.date,
            startedAt = session.startedAt,
            makeupForDate = session.makeupForDate,
            exercises = session.exercises.map { it.toSessionExercise() },
        )
        sessions.update { it + created }
        return id
    }

    override suspend fun updateSet(setId: Long, weightGrams: Int?, reps: Int?) =
        editSet(setId) { it.copy(weightGrams = weightGrams, reps = reps) }

    override suspend fun setCompleted(setId: Long, completedAt: Instant?) =
        editSet(setId) { it.copy(isCompleted = completedAt != null, completedAt = completedAt) }

    override suspend fun addSet(sessionExerciseId: Long, set: WorkoutSet): Long {
        val id = nextId++
        editExercise(sessionExerciseId) { it.copy(sets = it.sets + set.copy(id = id)) }
        return id
    }

    override suspend fun removeSet(setId: Long) = editExercises { exercise ->
        exercise.copy(sets = exercise.sets.filterNot { it.id == setId })
    }

    override suspend fun addExercise(sessionId: Long, exercise: NewSessionExercise): Long {
        val created = exercise.toSessionExercise()
        editSession(sessionId) { it.copy(exercises = it.exercises + created) }
        return created.id
    }

    override suspend fun removeExercise(sessionExerciseId: Long) = sessions.update { list ->
        list.map { session -> session.copy(exercises = session.exercises.filterNot { it.id == sessionExerciseId }) }
    }

    override suspend fun replaceExercise(sessionExerciseId: Long, exerciseId: Long, sets: List<WorkoutSet>) =
        editExercise(sessionExerciseId) {
            it.copy(exerciseId = exerciseId, exerciseName = exerciseName(exerciseId), sets = sets.withIds())
        }

    override suspend fun finish(sessionId: Long, finishedAt: Instant) = editSession(sessionId) { session ->
        session.copy(
            finishedAt = finishedAt,
            exercises = session.exercises
                .map { exercise -> exercise.copy(sets = exercise.sets.filter { it.isCompleted }) }
                .filter { it.sets.isNotEmpty() },
        )
    }

    override suspend fun delete(sessionId: Long) = sessions.update { list -> list.filterNot { it.id == sessionId } }

    private fun NewSessionExercise.toSessionExercise() = SessionExercise(
        id = nextId++,
        exerciseId = exerciseId,
        exerciseName = exerciseName(exerciseId),
        targetReps = targetReps,
        sets = sets.withIds(),
    )

    private fun List<WorkoutSet>.withIds() = map { it.copy(id = nextId++) }

    private fun editSession(id: Long, change: (WorkoutSession) -> WorkoutSession) =
        sessions.update { list -> list.map { if (it.id == id) change(it) else it } }

    private fun editExercises(change: (SessionExercise) -> SessionExercise) =
        sessions.update { list -> list.map { session -> session.copy(exercises = session.exercises.map(change)) } }

    private fun editExercise(id: Long, change: (SessionExercise) -> SessionExercise) =
        editExercises { if (it.id == id) change(it) else it }

    private fun editSet(id: Long, change: (WorkoutSet) -> WorkoutSet) =
        editExercises { exercise -> exercise.copy(sets = exercise.sets.map { if (it.id == id) change(it) else it }) }

    private fun exerciseName(id: Long) = "Exercício $id"
}
