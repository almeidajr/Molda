package com.almeidajr.molda

import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.ui.theme.MoldaTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The edge-to-edge window [MainActivity] sets up and the bar icons [MoldaTheme] picks. */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SystemBarsTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Test
    fun mainActivity_keepsNavigationBarContrastEnforced_forTheThreeButtonScrim() {
        // When
        composeRule.launchApp(frame = null)

        // Then
        composeRule.activityRule.scenario.onActivity {
            assertTrue(it.window.isNavigationBarContrastEnforced)
        }
    }

    @Test
    fun moldaTheme_followsLightAndDark_withContrastingBarIcons() {
        // When
        composeRule.activityRule.launch { MoldaTheme(darkTheme = false) {} }

        // Then
        assertBarIconsAreDark(true)

        // When
        composeRule.activityRule.show { MoldaTheme(darkTheme = true) {} }

        // Then
        assertBarIconsAreDark(false)
    }

    private fun assertBarIconsAreDark(expected: Boolean) {
        composeRule.waitForIdle()
        composeRule.activityRule.scenario.onActivity {
            val bars = WindowCompat.getInsetsController(it.window, it.window.decorView)
            assertEquals(expected, bars.isAppearanceLightStatusBars, "status bar")
            assertEquals(expected, bars.isAppearanceLightNavigationBars, "navigation bar")
        }
    }
}
