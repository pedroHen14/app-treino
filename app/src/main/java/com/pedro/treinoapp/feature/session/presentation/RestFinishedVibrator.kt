package com.pedro.treinoapp.feature.session.presentation

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.pedro.treinoapp.feature.session.domain.timer.RestTimer
import com.pedro.treinoapp.feature.session.domain.timer.RestTimerState
import kotlinx.coroutines.launch

/**
 * Vibra quando o descanso termina com o app aberto, em qualquer tela. Com o app em segundo
 * plano não vibra (notificação fica fora do MVP); ao voltar, o painel mostra "descanso concluído".
 */
class RestFinishedVibrator(private val context: Context) {

    fun observe(owner: LifecycleOwner, timer: RestTimer) {
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Só a passagem de contagem para concluído vibra; reabrir o app com o descanso já
                // concluído não vibra de novo.
                var previous: RestTimerState? = null
                timer.state.collect { state ->
                    if (state == RestTimerState.Finished && previous is RestTimerState.Running) vibrate()
                    previous = state
                }
            }
        }
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        if (vibrator?.hasVibrator() != true) return
        vibrator.vibrate(VibrationEffect.createWaveform(PATTERN, -1))
    }

    private companion object {
        val PATTERN = longArrayOf(0, 400, 200, 400)
    }
}
