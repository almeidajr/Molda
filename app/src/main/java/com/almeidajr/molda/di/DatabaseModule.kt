package com.almeidajr.molda.di

import android.content.Context
import com.almeidajr.molda.data.room.MoldaDatabase
import com.almeidajr.molda.data.room.openMoldaDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The app's one Room database, apart from the repositories so tests can replace either. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): MoldaDatabase = openMoldaDatabase(context)
}
