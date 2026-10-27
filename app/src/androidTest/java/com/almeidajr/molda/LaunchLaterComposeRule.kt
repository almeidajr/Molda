package com.almeidajr.molda

/** A compose rule on [MainActivity] that starts it only on [launchApp]. */
internal fun createLaunchLaterComposeRule() = createAppComposeRule(MainActivity::class.java) {
    KeyboardAnimations.observe(it.window.decorView)
}
