package com.pedro.treinoapp.feature.session.domain.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

sealed interface RestTimerState {
    data object Idle : RestTimerState

    /** `total` é a duração atual do descanso, já com os ajustes de ±15 s. */
    data class Running(val remaining: Duration, val total: Duration) : RestTimerState
    data class Paused(val remaining: Duration, val total: Duration) : RestTimerState

    /** Fica neste estado até o usuário dispensar ou começar outro descanso. */
    data object Finished : RestTimerState
}

/**
 * Timer de descanso entre séries. Vive enquanto o processo do app vive, para não parar ao trocar
 * de tela; notificação em segundo plano fica fora do MVP. A contagem usa o fim previsto, não a
 * soma de ticks, então atrasos de agendamento não acumulam erro.
 */
class RestTimer(
    private val scope: CoroutineScope,
    private val timeSource: TimeSource.WithComparableMarks,
) {
    private val _state = MutableStateFlow<RestTimerState>(RestTimerState.Idle)
    val state: StateFlow<RestTimerState> = _state.asStateFlow()

    private var job: Job? = null
    private var end: ComparableTimeMark? = null
    private var total: Duration = Duration.ZERO

    fun start(duration: Duration) {
        total = duration
        runUntil(timeSource.markNow() + duration)
    }

    fun pause() {
        val running = _state.value as? RestTimerState.Running ?: return
        val remaining = remaining() ?: running.remaining
        stopJob()
        _state.value = RestTimerState.Paused(remaining, total)
    }

    fun resume() {
        val paused = _state.value as? RestTimerState.Paused ?: return
        runUntil(timeSource.markNow() + paused.remaining)
    }

    /** Encerra o descanso sem vibrar; também dispensa o aviso de descanso concluído. */
    fun skip() {
        stopJob()
        _state.value = RestTimerState.Idle
    }

    /** Soma (ou subtrai, com [delta] negativo) tempo ao descanso. Chegar a zero conclui o descanso. */
    fun adjust(delta: Duration) {
        when (val current = _state.value) {
            is RestTimerState.Running -> {
                val newEnd = (end ?: return) + delta
                total = (total + delta).coerceAtLeast(Duration.ZERO)
                runUntil(newEnd)
            }
            is RestTimerState.Paused -> {
                val remaining = (current.remaining + delta).coerceAtLeast(Duration.ZERO)
                total = (total + delta).coerceAtLeast(remaining)
                if (remaining == Duration.ZERO) finish() else _state.value = RestTimerState.Paused(remaining, total)
            }
            RestTimerState.Idle, RestTimerState.Finished -> Unit
        }
    }

    private fun runUntil(newEnd: ComparableTimeMark) {
        stopJob()
        end = newEnd
        job = scope.launch {
            while (true) {
                val remaining = -newEnd.elapsedNow()
                if (remaining <= Duration.ZERO) {
                    finish()
                    break
                }
                _state.value = RestTimerState.Running(remaining, total)
                // Acorda na virada de cada segundo exibido.
                val untilNextSecond = (remaining.inWholeMilliseconds % 1000).milliseconds
                delay(if (untilNextSecond == Duration.ZERO) 1.seconds else untilNextSecond)
            }
        }
    }

    private fun finish() {
        stopJob()
        _state.value = RestTimerState.Finished
    }

    private fun remaining(): Duration? = end?.let { (-it.elapsedNow()).coerceAtLeast(Duration.ZERO) }

    private fun stopJob() {
        job?.cancel()
        job = null
        end = null
    }

    companion object {
        val ADJUST_STEP: Duration = 15.seconds
    }
}
