package com.almeidajr.molda

/** A compose rule on [HiltTestActivity] that starts it only on [launchApp]. */
internal fun createLaunchLaterComposeRule() = createAppComposeRule(HiltTestActivity::class.java)
