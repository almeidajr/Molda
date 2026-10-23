package com.almeidajr.molda.di

import com.almeidajr.molda.data.room.MoldaDatabase
import com.almeidajr.molda.profile.data.LocalProfileRepository
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.project.data.LocalProjectRepository
import com.almeidajr.molda.project.domain.ProjectRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The repositories on the app's database. */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun profileRepository(database: MoldaDatabase): ProfileRepository =
        LocalProfileRepository(database.profiles())

    @Provides
    @Singleton
    fun projectRepository(database: MoldaDatabase): ProjectRepository =
        LocalProjectRepository(database.projects())
}
