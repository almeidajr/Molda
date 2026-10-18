package com.almeidajr.molda.di

import android.content.Context
import com.almeidajr.molda.profile.data.LocalProfileRepository
import com.almeidajr.molda.profile.domain.ProfileRepository
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
    fun profileRepository(@ApplicationContext context: Context): ProfileRepository =
        LocalProfileRepository(context)

    @Provides
    fun clock(): Clock = Clock.systemDefaultZone()
}
