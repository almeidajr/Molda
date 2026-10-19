package com.almeidajr.molda.di

import android.content.Context
import com.almeidajr.molda.data.room.MoldaDatabase
import com.almeidajr.molda.data.room.openMoldaDatabase
import com.almeidajr.molda.profile.data.LocalProfileRepository
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.project.data.LocalProjectRepository
import com.almeidajr.molda.project.domain.ProjectRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): MoldaDatabase = openMoldaDatabase(context)

    @Provides
    @Singleton
    fun profileRepository(database: MoldaDatabase): ProfileRepository =
        LocalProfileRepository(database.profiles())

    @Provides
    @Singleton
    fun projectRepository(database: MoldaDatabase): ProjectRepository =
        LocalProjectRepository(database.projects())

    @Provides
    fun clock(): Clock = Clock.systemDefaultZone()
}
