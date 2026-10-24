package com.almeidajr.molda

import com.almeidajr.molda.testing.FakeProfileRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.Test

/** The device tests' bindings holder, which the app reads only as it starts. */
@HiltAndroidTest
class TestBindingsTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Test
    fun assigningAFake_afterLaunch_throws() {
        // Given
        composeRule.launchApp()

        // When/Then
        assertFailsWith<IllegalStateException> { TestBindings.profiles = FakeProfileRepository() }
    }
}
