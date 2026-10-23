package com.almeidajr.molda

import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule
import androidx.test.core.app.ActivityScenario
import org.junit.rules.ExternalResource

/**
 * Launches [MainActivity] only on [launch], so a test can first choose the fakes it binds into
 * Hilt: the activity's ViewModels read them as it starts. Closes the activity after the test.
 */
class MainActivityLauncher internal constructor() : ExternalResource() {
    private var launched: ActivityScenario<MainActivity>? = null

    /** The launched activity's scenario. */
    val scenario: ActivityScenario<MainActivity>
        get() = checkNotNull(launched) { "MainActivity is not launched yet" }

    /** Launches the activity; once per test. */
    fun launch() {
        check(launched == null) { "MainActivity is already launched" }
        launched = ActivityScenario.launch(MainActivity::class.java)
    }

    override fun after() {
        launched?.close()
    }
}

/** A compose rule on [MainActivity] that starts it only on [MainActivityLauncher.launch]. */
internal fun createLaunchLaterComposeRule() =
    AndroidComposeTestRule(MainActivityLauncher()) { launcher ->
        var activity: MainActivity? = null
        launcher.scenario.onActivity { activity = it }
        checkNotNull(activity) { "MainActivity is not running" }
    }
