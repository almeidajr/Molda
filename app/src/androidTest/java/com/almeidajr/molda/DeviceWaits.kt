package com.almeidajr.molda

import android.view.WindowManager
import android.view.inspector.WindowInspector
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.time.Duration.Companion.seconds

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
internal fun AndroidComposeTestRule<*, *>.isKeyboardVisible(): Boolean = runOnUiThread {
    ViewCompat.getRootWindowInsets(activity.window.decorView)
        ?.isVisible(WindowInsetsCompat.Type.ime()) == true
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
