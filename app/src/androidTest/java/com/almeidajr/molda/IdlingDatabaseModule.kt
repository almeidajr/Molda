package com.almeidajr.molda

import com.almeidajr.molda.data.room.MoldaDatabase
import com.almeidajr.molda.di.DatabaseModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Provides every device test's [IsolatedStorage] and opens the database on it, on Room that
 * Compose waits for, so no test touches `molda.db`. Journey tests build the production repositories
 * on that storage; screen tests run on fakes and never open it. A test that opens it injects the
 * storage and removes it afterward.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
internal object IdlingDatabaseModule {
    @Provides
    @Singleton
    fun storage(): IsolatedStorage = IsolatedStorage("journey-test.db")

    @Provides
    fun database(storage: IsolatedStorage): MoldaDatabase = storage.database
}
