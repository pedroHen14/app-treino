package com.pedro.treinoapp.feature.session.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSession
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import com.pedro.treinoapp.feature.session.domain.timer.RestTimer
import com.pedro.treinoapp.feature.session.domain.timer.RestTimerState
import com.pedro.treinoapp.feature.session.domain.usecase.AddSessionExercisesUseCase
import com.pedro.treinoapp.feature.session.domain.usecase.AddSetUseCase
import com.pedro.treinoapp.feature.session.domain.usecase.CompleteSetUseCase
import com.pedro.treinoapp.feature.session.domain.usecase.DiscardWorkoutSessionUseCase
import com.pedro.treinoapp.feature.session.domain.usecase.FinishWorkoutSessionUseCase
import com.pedro.treinoapp.feature.session.domain.usecase.ObserveRestTimerDurationUseCase
import com.pedro.treinoapp.feature.session.domain.usecase.ObserveSessionUseCase
import com.pedro.treinoapp.feature.session.domain.usecase.RemoveSessionExerciseUseCase
import com.pedro.treinoapp.feature.session.domain.usecase.RemoveSetUseCase
import com.pedro.treinoapp.feature.session.domain.usecase.ReplaceSessionExerciseUseCase
import com.pedro.treinoapp.feature.session.domain.usecase.SetRestTimerDurationUseCase
import com.pedro.treinoapp.feature.session.domain.usecase.UncompleteSetUseCase
import com.pedro.treinoapp.feature.session.domain.usecase.UpdateSetUseCase
import com.pedro.treinoapp.feature.workout.domain.model.RepRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration

/** Linhas da lista da sessão: cabeçalho do exercício, uma linha por série e o rodapé com "adicionar série". */
sealed interface SessionListItem {
    val key: String

    data class Header(
        val sessionExerciseId: Long,
        val exerciseName: String,
        val targetReps: RepRange?,
    ) : SessionListItem {
        override val key get() = "h$sessionExerciseId"
    }

    /** Textos de carga e repetições são o que está no campo, que pode ainda não ter sido salvo. */
    data class SetRow(
        val sessionExerciseId: Long,
        val setId: Long,
        val number: Int,
        val weightText: String,
        /** Muda quando a carga é alterada pelos botões ±, para o campo aceitar o valor mesmo em foco. */
        val weightRevision: Int,
        val repsText: String,
        val targetReps: RepRange?,
        val isCompleted: Boolean,
        val error: SessionError?,
    ) : SessionListItem {
        override val key get() = "s$setId"
    }

    data class Footer(val sessionExerciseId: Long, val canAddSet: Boolean) : SessionListItem {
        override val key get() = "f$sessionExerciseId"
    }
}

data class SessionUiState(
    val isLoading: Boolean = true,
    /** Sessão finalizada, descartada ou inexistente: a tela fecha. */
    val isClosed: Boolean = false,
    val templateName: String? = null,
    val items: List<SessionListItem> = emptyList(),
    val exerciseIds: List<Long> = emptyList(),
    val completedSetCount: Int = 0,
    val pendingSetCount: Int = 0,
    val timer: RestTimerState = RestTimerState.Idle,
    val defaultRest: Duration = SetRestTimerDurationUseCase.DEFAULT,
)

sealed interface SessionEvent {
    data class ShowError(val error: SessionError) : SessionEvent

    /** Pede confirmação antes de salvar; `pendingSetCount` séries não concluídas serão descartadas. */
    data class ConfirmFinish(val pendingSetCount: Int) : SessionEvent

    /** Nenhuma série concluída: não há o que salvar, só descartar. */
    data object OfferDiscard : SessionEvent
    data object Saved : SessionEvent
    data object Discarded : SessionEvent
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observeSession: ObserveSessionUseCase,
    observeRestDuration: ObserveRestTimerDurationUseCase,
    private val restTimer: RestTimer,
    private val setRestDuration: SetRestTimerDurationUseCase,
    private val updateSet: UpdateSetUseCase,
    private val completeSet: CompleteSetUseCase,
    private val uncompleteSet: UncompleteSetUseCase,
    private val addSet: AddSetUseCase,
    private val removeSet: RemoveSetUseCase,
    private val addExercises: AddSessionExercisesUseCase,
    private val removeExercise: RemoveSessionExerciseUseCase,
    private val replaceExercise: ReplaceSessionExerciseUseCase,
    private val finishSession: FinishWorkoutSessionUseCase,
    private val discardSession: DiscardWorkoutSessionUseCase,
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle[SessionFragment.ARG_SESSION_ID])

    /** Exercício da sessão a ser trocado pelo que voltar do catálogo; nulo quando a escolha é para adicionar. */
    var pendingReplacement: Long?
        get() = savedStateHandle[KEY_PENDING_REPLACEMENT]
        set(value) {
            savedStateHandle[KEY_PENDING_REPLACEMENT] = value
        }

    /** Texto dos campos enquanto o usuário digita, por série; some quando o valor vem de fora (botões ±). */
    private val drafts = MutableStateFlow<Map<Long, SetDraft>>(emptyMap())
    private val errors = MutableStateFlow<Map<Long, SessionError>>(emptyMap())

    private var session: WorkoutSession? = null

    private val _events = Channel<SessionEvent>(Channel.BUFFERED)
    val events: Flow<SessionEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<SessionUiState> = combine(
        observeSession(sessionId),
        drafts,
        errors,
        restTimer.state,
        observeRestDuration(),
    ) { session, drafts, errors, timer, defaultRest ->
        this.session = session
        if (session == null || !session.isActive) {
            SessionUiState(isLoading = false, isClosed = true)
        } else {
            SessionUiState(
                isLoading = false,
                templateName = session.templateName,
                items = session.toItems(drafts, errors),
                exerciseIds = session.exercises.map { it.exerciseId },
                completedSetCount = session.completedSetCount,
                pendingSetCount = session.pendingSetCount,
                timer = timer,
                defaultRest = defaultRest,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionUiState())

    fun onWeightChange(setId: Long, text: String) {
        drafts.update { it + (setId to (it[setId] ?: SetDraft()).copy(weight = text)) }
        saveDraft(setId)
    }

    fun onRepsChange(setId: Long, text: String) {
        drafts.update { it + (setId to (it[setId] ?: SetDraft()).copy(reps = text)) }
        saveDraft(setId)
    }

    /** Soma [deltaGrams] à carga da série (botões −/+ 0,5 kg). */
    fun stepWeight(setId: Long, deltaGrams: Int) {
        val set = session?.findSet(setId) ?: return
        val current = drafts.value[setId]?.weight?.let { parseWeight(it).getOrNull() } ?: set.weightGrams ?: 0
        val next = ((current + deltaGrams) / WorkoutSet.WEIGHT_STEP_GRAMS * WorkoutSet.WEIGHT_STEP_GRAMS).coerceAtLeast(0)
        drafts.update { drafts ->
            val draft = drafts[setId] ?: SetDraft()
            drafts + (setId to draft.copy(weight = null, weightRevision = draft.weightRevision + 1))
        }
        val reps = drafts.value[setId]?.reps?.let { parseReps(it).getOrNull() } ?: set.reps
        save(setId, next, reps)
    }

    fun onCompletedClick(setId: Long) = viewModelScope.launch {
        val set = session?.findSet(setId) ?: return@launch
        if (set.isCompleted) {
            uncompleteSet(sessionId, setId).onFailure()
            return@launch
        }
        errors.value[setId]?.let {
            _events.send(SessionEvent.ShowError(it))
            return@launch
        }
        completeSet(sessionId, setId).onFailure()
    }

    fun onAddSet(sessionExerciseId: Long) = viewModelScope.launch {
        addSet(sessionId, sessionExerciseId).onFailure()
    }

    fun onRemoveSet(setId: Long) = viewModelScope.launch {
        removeSet(sessionId, setId).onFailure()
    }

    fun onRemoveExercise(sessionExerciseId: Long) = viewModelScope.launch {
        removeExercise(sessionId, sessionExerciseId).onFailure()
    }

    /** Ids escolhidos no catálogo: trocam o exercício pendente ou entram no fim da sessão. */
    fun onExercisesSelected(ids: LongArray) = viewModelScope.launch {
        val replacing = pendingReplacement
        pendingReplacement = null
        if (replacing != null) {
            ids.firstOrNull()?.let { replaceExercise(sessionId, replacing, it).onFailure() }
        } else {
            addExercises(sessionId, ids.toList()).onFailure()
        }
    }

    fun pauseRest() = restTimer.pause()

    fun resumeRest() = restTimer.resume()

    fun skipRest() = restTimer.skip()

    fun adjustRest(add: Boolean) = restTimer.adjust(if (add) RestTimer.ADJUST_STEP else -RestTimer.ADJUST_STEP)

    fun setDefaultRest(duration: Duration) = viewModelScope.launch {
        setRestDuration(duration).onFailure()
    }

    /** Pede confirmação: com séries pendentes avisa que serão descartadas; sem séries concluídas oferece descartar. */
    fun requestFinish() = viewModelScope.launch {
        val session = session ?: return@launch
        _events.send(
            when {
                session.completedSetCount == 0 -> SessionEvent.OfferDiscard
                else -> SessionEvent.ConfirmFinish(session.pendingSetCount)
            },
        )
    }

    fun finish() = viewModelScope.launch {
        when (val result = finishSession(sessionId)) {
            is SessionResult.Success -> _events.send(SessionEvent.Saved)
            is SessionResult.Failure -> handleFailure(result.error)
        }
    }

    fun discard() = viewModelScope.launch {
        when (val result = discardSession(sessionId)) {
            is SessionResult.Success -> _events.send(SessionEvent.Discarded)
            is SessionResult.Failure -> handleFailure(result.error)
        }
    }

    private fun saveDraft(setId: Long) {
        val set = session?.findSet(setId) ?: return
        val draft = drafts.value[setId] ?: return
        val weight = if (draft.weight == null) {
            set.weightGrams
        } else {
            parseWeight(draft.weight).getOrElse { return setError(setId, SessionError.InvalidWeight) }
        }
        val reps = if (draft.reps == null) {
            set.reps
        } else {
            parseReps(draft.reps).getOrElse { return setError(setId, SessionError.InvalidReps) }
        }
        save(setId, weight, reps)
    }

    private fun save(setId: Long, weightGrams: Int?, reps: Int?) {
        val set = session?.findSet(setId) ?: return
        if (weightGrams == set.weightGrams && reps == set.reps) {
            setError(setId, null)
            return
        }
        viewModelScope.launch {
            when (val result = updateSet(sessionId, setId, weightGrams, reps)) {
                is SessionResult.Success -> setError(setId, null)
                is SessionResult.Failure -> setError(setId, result.error)
            }
        }
    }

    private fun setError(setId: Long, error: SessionError?) =
        errors.update { if (error == null) it - setId else it + (setId to error) }

    private suspend fun SessionResult<*>.onFailure() {
        if (this is SessionResult.Failure) handleFailure(error)
    }

    private suspend fun handleFailure(error: SessionError) {
        if (error == SessionError.NoCompletedSets) {
            _events.send(SessionEvent.OfferDiscard)
        } else {
            _events.send(SessionEvent.ShowError(error))
        }
    }

    private fun WorkoutSession.toItems(drafts: Map<Long, SetDraft>, errors: Map<Long, SessionError>) =
        exercises.flatMap { exercise ->
            val header = SessionListItem.Header(exercise.id, exercise.exerciseName, exercise.targetReps)
            val rows = exercise.sets.mapIndexed { index, set ->
                val draft = drafts[set.id]
                SessionListItem.SetRow(
                    sessionExerciseId = exercise.id,
                    setId = set.id,
                    number = index + 1,
                    weightText = draft?.weight ?: set.weightGrams?.let(::formatWeight).orEmpty(),
                    weightRevision = draft?.weightRevision ?: 0,
                    repsText = draft?.reps ?: set.reps?.toString().orEmpty(),
                    targetReps = exercise.targetReps,
                    isCompleted = set.isCompleted,
                    error = errors[set.id],
                )
            }
            val footer = SessionListItem.Footer(exercise.id, canAddSet = exercise.sets.size < WorkoutSet.MAX_SETS)
            listOf(header) + rows + footer
        }

    private data class SetDraft(val weight: String? = null, val reps: String? = null, val weightRevision: Int = 0)

    private companion object {
        const val KEY_PENDING_REPLACEMENT = "pending_replacement"
    }
}
