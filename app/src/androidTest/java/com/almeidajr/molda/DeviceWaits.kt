// The one home of wall-clock waits: config/detekt.yml bans waitUntil* in every other test file.
package com.almeidajr.molda

import android.animation.ValueAnimator
import android.view.WindowManager
import android.view.inspector.WindowInspector
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream
import kotlin.time.Duration.Companion.seconds
import org.junit.rules.ExternalResource

/**
 * Wall-clock budget for state Compose cannot see. Compose-driven state needs no wait:
 * rely on automatic synchronization or `mainClock` instead.
 */
internal val DeviceWaitTimeout = 10.seconds

/** Waits for the soft keyboard, whose insets can arrive after Compose has become idle. */
internal fun AndroidComposeTestRule<*, *>.awaitKeyboardVisible() =
    waitUntil("keyboard visible", DeviceWaitTimeout.inWholeMilliseconds) { isKeyboardVisible() }

/** Waits for the soft keyboard to close, which can resize the window after Compose is idle. */
internal fun AndroidComposeTestRule<*, *>.awaitKeyboardHidden() =
    waitUntil("keyboard hidden", DeviceWaitTimeout.inWholeMilliseconds) { !isKeyboardVisible() }

/** Whether the activity window currently shows the soft keyboard. */
private fun AndroidComposeTestRule<*, *>.isKeyboardVisible(): Boolean = runOnUiThread {
    ViewCompat.getRootWindowInsets(activity.window.decorView)
        ?.isVisible(WindowInsetsCompat.Type.ime()) == true
}

/**
 * Closes the soft keyboard and waits until the window no longer shows it. Call it after typing and
 * before tapping a button low in an `imePadding` layout: an opening keyboard shrinks the layout
 * frame by frame, which can clip the button between scrolling to it and tapping it.
 */
internal fun AndroidComposeTestRule<*, *>.closeKeyboard() {
    closeSoftKeyboard()
    awaitKeyboardHidden()
}

/**
 * Waits until a dialog window holds input focus with the keyboard closed, so a key event
 * such as Back reaches the dialog rather than the window or keyboard behind it.
 */
internal fun AndroidComposeTestRule<*, *>.awaitDialogFocus() =
    waitUntil("dialog window focused", DeviceWaitTimeout.inWholeMilliseconds) {
        runOnUiThread {
            WindowInspector.getGlobalWindowViews().any {
                it.hasWindowFocus() &&
                    ViewCompat.getRootWindowInsets(it)
                        ?.isVisible(WindowInsetsCompat.Type.ime()) != true &&
                    (it.layoutParams as? WindowManager.LayoutParams)?.type ==
                    WindowManager.LayoutParams.TYPE_APPLICATION
            }
        }
    }

/** Waits until the activity window gets input focus back, for example after a dialog closes. */
internal fun AndroidComposeTestRule<*, *>.awaitActivityFocus() =
    waitUntil("activity window focused", DeviceWaitTimeout.inWholeMilliseconds) {
        runOnUiThread { activity.hasWindowFocus() }
    }

/** Waits for the activity to start finishing, for example after Back leaves the app. */
internal fun AndroidComposeTestRule<*, *>.awaitActivityFinishing() =
    waitUntil("activity finishing", DeviceWaitTimeout.inWholeMilliseconds) { activity.isFinishing }

/**
 * Waits until the Rive drawing labelled [description] shows a frame that [isExpected] accepts, by
 * default any frame that is not blank, and returns that frame, never one covered by the frame's
 * loading indicator. Rive loads and draws on its own threads, which Compose cannot see. It reads
 * the unmerged tree: a playing drawing's frame is a toggle that merges the drawing, its badge and
 * the indicator into one node. A playing drawing keeps Compose busy, so tests turn
 * `mainClock.autoAdvance` off while one plays; each check then advances one frame itself.
 */
@OptIn(ExperimentalTestApi::class)
internal fun AndroidComposeTestRule<*, *>.awaitRiveFrame(
    description: String,
    isExpected: (ImageBitmap) -> Boolean = { it.isNotBlank() },
): ImageBitmap {
    val timeout = DeviceWaitTimeout.inWholeMilliseconds
    // Shows the result of the last action, such as a step change, before looking for the drawing.
    if (!mainClock.autoAdvance) mainClock.advanceTimeByFrame()
    waitUntilAtLeastOneExists(hasContentDescription(description), timeout)
    var frame: ImageBitmap? = null
    val indicator = hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate) and
        hasAnyAncestor(hasContentDescription(description))
    waitUntil("$description rendered as expected", timeout) {
        if (!mainClock.autoAdvance) mainClock.advanceTimeByFrame()
        // The indicator can appear after the first check, so it is part of every frame's check.
        val isCovered = onAllNodes(indicator, useUnmergedTree = true).fetchSemanticsNodes()
            .isNotEmpty()
        frame = onNodeWithContentDescription(description, useUnmergedTree = true).captureToImage()
            .takeIf { !isCovered && isExpected(it) }
        frame != null
    }
    return checkNotNull(frame)
}

/** Whether the image holds more than one colour, as a drawn frame does and an empty one not. */
private fun ImageBitmap.isNotBlank(): Boolean {
    val pixels = IntArray(width * height).also { readPixels(it) }
    return pixels.any { it != pixels.first() }
}

/**
 * Owns the system animator duration scale for one test: [set] changes it and waits until the app
 * sees the change, and the original value comes back after the test, even when it fails. Order it
 * inside [composeRule] (a higher `order`), whose waits it uses on both ends.
 */
class AnimatorDurationScaleRule internal constructor(
    private val composeRule: AndroidComposeTestRule<*, *>,
) : ExternalResource() {
    private var original = "null"

    override fun before() {
        original = shell("settings get global animator_duration_scale")
    }

    override fun after() {
        if (original == "null") {
            shell("settings delete global animator_duration_scale")
            awaitScale(1f)
        } else {
            set(original.toFloat())
        }
    }

    fun set(scale: Float) {
        shell("settings put global animator_duration_scale $scale")
        awaitScale(scale)
    }

    // The app process learns of a settings change asynchronously.
    private fun awaitScale(scale: Float) = composeRule.waitUntil(
        "animator duration scale $scale",
        DeviceWaitTimeout.inWholeMilliseconds,
    ) {
        ValueAnimator.getDurationScale() == scale
    }

    private fun shell(command: String): String =
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
            .use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).bufferedReader().use {
                    it.readText().trim()
                }
            }
}

/**
 * Takes the device out of touch mode for one test, as a keyboard user leaves it, and puts it back
 * after the test, even when it fails. Touch mode leaves buttons unfocusable. Touch mode is global
 * and Compose test clicks do not restore it, so never leave it with a navigation or typing key
 * (Tab, Enter, arrows): every later test would run as a keyboard user, whose returning window
 * focus raises the keyboard on a text field. Order it inside [composeRule], whose wait it uses.
 */
class KeyboardModeRule internal constructor(
    private val composeRule: AndroidComposeTestRule<*, *>,
) : ExternalResource() {
    private var isChanged = false

    /** Leaves touch mode until the test ends, once the activity window has left it. */
    fun enter() {
        isChanged = true
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
        // The window learns of the change asynchronously.
        composeRule.waitUntil("touch mode left", DeviceWaitTimeout.inWholeMilliseconds) {
            composeRule.runOnUiThread { !composeRule.activity.window.decorView.isInTouchMode }
        }
    }

    override fun after() {
        if (isChanged) InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
    }
}
