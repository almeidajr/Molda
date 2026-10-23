package com.almeidajr.molda.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/** The device's clock, which tests replace with a fixed one. */
@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    @Provides
    fun clock(): Clock = Clock.systemDefaultZone()
}
