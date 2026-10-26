package com.almeidajr.molda.di

import android.content.Context
import com.almeidajr.molda.data.room.MoldaDatabase
import com.almeidajr.molda.profile.data.LocalProfileRepository
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.project.data.LocalProjectRepository
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.update.data.GitHubReleases
import com.almeidajr.molda.update.data.SystemUpdateRepository
import com.almeidajr.molda.update.domain.UpdateRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The repositories on the app's database, and the updates on GitHub Releases. */
@Module
@InstallIn(SingletonComponent::class)
internal object RepositoryModule {
    @Provides
    @Singleton
    fun profileRepository(database: MoldaDatabase): ProfileRepository =
        LocalProfileRepository(database.profiles())

    @Provides
    @Singleton
    fun projectRepository(database: MoldaDatabase): ProjectRepository =
        LocalProjectRepository(database.projects())

    @Provides
    @Singleton
    fun updateRepository(
        @ApplicationContext context: Context,
        releases: GitHubReleases,
    ): UpdateRepository = SystemUpdateRepository(context, releases)
}
