package com.almeidajr.molda.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.Call
import okhttp3.OkHttpClient

/** The app's one HTTP client, apart from the repositories so tests can replace either. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun callFactory(): Call.Factory = OkHttpClient()
}
