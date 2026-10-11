package com.pedro.treinoapp.core.di

import android.os.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import kotlin.time.AbstractLongTimeSource
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()

    /** Tempo monotônico para contagens; continua andando com o aparelho em suspensão. */
    @Provides
    fun provideTimeSource(): TimeSource.WithComparableMarks = ElapsedRealtimeTimeSource

    private object ElapsedRealtimeTimeSource : AbstractLongTimeSource(DurationUnit.NANOSECONDS) {
        override fun read(): Long = SystemClock.elapsedRealtimeNanos()
    }
}
