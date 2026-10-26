package com.almeidajr.molda

import com.almeidajr.molda.di.ClockModule
import com.almeidajr.molda.di.RepositoryModule
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.testing.FakeProfileRepository
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.FakeUpdateRepository
import com.almeidajr.molda.testing.October13Clock
import com.almeidajr.molda.update.domain.UpdateRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.time.Clock

/**
 * The repositories and clock the app runs on in a device test: empty fakes, Molda up to date and
 * 13/10/2026 until [launchApp] seeds them. Starting the app freezes them, so a fake assigned too
 * late fails the test instead of being silently ignored. [MainActivityLauncher] resets them
 * before each test.
 */
internal object TestBindings {
    private var isFrozen = false

    var profiles: ProfileRepository = FakeProfileRepository()
        set(value) {
            checkNotFrozen()
            field = value
        }

    var projects: ProjectRepository = FakeProjectRepository()
        set(value) {
            checkNotFrozen()
            field = value
        }

    var updates: UpdateRepository = FakeUpdateRepository()
        set(value) {
            checkNotFrozen()
            field = value
        }

    var clock: Clock = October13Clock
        set(value) {
            checkNotFrozen()
            field = value
        }

    /** Rejects every later change: the app has started on the current bindings. */
    fun freeze() {
        isFrozen = true
    }

    /** Restores the defaults for the next test. */
    fun reset() {
        isFrozen = false
        profiles = FakeProfileRepository()
        projects = FakeProjectRepository()
        updates = FakeUpdateRepository()
        clock = October13Clock
    }

    private fun checkNotFrozen() =
        check(!isFrozen) { "The app has started: seed the bindings through launchApp" }
}

/** Gives every device test's app the repositories and clock held by [TestBindings]. */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class, ClockModule::class],
)
internal object TestBindingsModule {
    @Provides
    fun profileRepository(): ProfileRepository = TestBindings.profiles

    @Provides
    fun projectRepository(): ProjectRepository = TestBindings.projects

    @Provides
    fun updateRepository(): UpdateRepository = TestBindings.updates

    @Provides
    fun clock(): Clock = TestBindings.clock
}
