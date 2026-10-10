package com.pedro.treinoapp.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pedro.treinoapp.core.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    /** Cada feature pode registrar callbacks do banco (ex.: seed) sem o core conhecê-las. */
    @Multibinds
    abstract fun databaseCallbacks(): Set<RoomDatabase.Callback>

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context,
            callbacks: Set<@JvmSuppressWildcards RoomDatabase.Callback>,
        ): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
                .apply { callbacks.forEach { addCallback(it) } }
                .build()
    }
}
