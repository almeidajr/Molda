package com.almeidajr.molda

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule
import androidx.test.core.app.ActivityScenario
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.rules.ExternalResource

/**
 * Launches [MainActivity] only on [launch], after the test has seeded [TestBindings], and shows the
 * test's content in every activity it creates, recreated ones included, before their first frame.
 * Resets the bindings before each test and closes the activity after it.
 */
class MainActivityLauncher internal constructor() : ExternalResource() {
    private var launched: ActivityScenario<MainActivity>? = null
    private var content: (@Composable () -> Unit)? = null

    // Held here: the monitor keeps its callbacks weakly. Replacing the content right after
    // onCreate, before the window attaches, composes only the test's content.
    private val showContent = ActivityLifecycleCallback { activity, stage ->
        val shown = content
        if (stage == Stage.CREATED && activity is MainActivity && shown != null) {
            activity.setContent(content = shown)
        }
    }

    /** The launched activity's scenario. */
    val scenario: ActivityScenario<MainActivity>
        get() = checkNotNull(launched) { "MainActivity is not launched yet" }

    override fun before() {
        TestBindings.reset()
        ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(showContent)
    }

    /**
     * Freezes [TestBindings] and launches the activity on [content], or on the app's own content
     * when null; once per test.
     */
    internal fun launch(content: (@Composable () -> Unit)?) {
        check(launched == null) { "MainActivity is already launched" }
        this.content = content
        TestBindings.freeze()
        launched = ActivityScenario.launch(MainActivity::class.java)
    }

    /** Shows [content] in the running activity and in every recreated one. */
    internal fun show(content: @Composable () -> Unit) {
        this.content = content
        scenario.onActivity { it.setContent(content = content) }
    }

    override fun after() {
        launched?.close()
        ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(showContent)
    }
}

/** A compose rule on [MainActivity] that starts it only on [launchApp]. */
internal fun createLaunchLaterComposeRule() =
    AndroidComposeTestRule(MainActivityLauncher()) { launcher ->
        var activity: MainActivity? = null
        launcher.scenario.onActivity { activity = it }
        checkNotNull(activity) { "MainActivity is not running" }
    }
