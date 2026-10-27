package com.almeidajr.molda

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule
import androidx.test.core.app.ActivityScenario
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.rules.ExternalResource

/**
 * Launches [activityClass] only on [launch], after the test has seeded [TestBindings], and shows
 * the test's content in every activity it creates, recreated ones included, before their first
 * frame; [onCreated] runs on each of them too. Resets the bindings before each test and closes the
 * activity after it.
 */
class AppLauncher<A : ComponentActivity>(
    private val activityClass: Class<A>,
    onCreated: (A) -> Unit,
) : ExternalResource() {
    private var launched: ActivityScenario<A>? = null
    private var content: (@Composable () -> Unit)? = null

    // Held here: the monitor keeps its callbacks weakly. Replacing the content right after
    // onCreate, before the window attaches, composes only the test's content.
    private val showContent = ActivityLifecycleCallback { activity, stage ->
        val created = activity.takeIf { stage == Stage.CREATED && activityClass.isInstance(it) }
            ?.let(activityClass::cast)
            ?: return@ActivityLifecycleCallback
        content?.let { created.setContent(content = it) }
        onCreated(created)
    }

    /** The launched activity's scenario. */
    val scenario: ActivityScenario<A>
        get() = checkNotNull(launched) { "${activityClass.simpleName} is not launched yet" }

    override fun before() {
        TestBindings.reset()
        ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(showContent)
    }

    /**
     * Freezes [TestBindings] and launches the activity on [content], or on its own content when
     * null; once per test.
     */
    fun launch(content: (@Composable () -> Unit)?) {
        check(launched == null) { "${activityClass.simpleName} is already launched" }
        this.content = content
        TestBindings.freeze()
        launched = ActivityScenario.launch(activityClass)
    }

    /** Shows [content] in the running activity and in every recreated one. */
    fun show(content: @Composable () -> Unit) {
        this.content = content
        scenario.onActivity { it.setContent(content = content) }
    }

    override fun after() {
        try {
            launched?.close()
        } finally {
            ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(showContent)
        }
    }
}

/** A compose rule on [activityClass] that starts it only on [launchApp]. */
fun <A : ComponentActivity> createAppComposeRule(
    activityClass: Class<A>,
    onCreated: (A) -> Unit = {},
) = AndroidComposeTestRule(AppLauncher(activityClass, onCreated)) { launcher ->
    var activity: A? = null
    launcher.scenario.onActivity { activity = it }
    checkNotNull(activity) { "${activityClass.simpleName} is not running" }
}
