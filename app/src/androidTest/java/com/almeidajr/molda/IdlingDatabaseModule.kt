package com.almeidajr.molda

import com.almeidajr.molda.data.room.MoldaDatabase
import com.almeidajr.molda.di.DatabaseModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Opens the database of every device test on [IsolatedStorage], so journey tests run the real
 * repositories on Room that Compose waits for. Screen tests replace the repositories instead and
 * never open it. A test that opens it injects the storage and removes it afterwards.
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
