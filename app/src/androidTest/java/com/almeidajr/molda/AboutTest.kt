package com.almeidajr.molda

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The installed version on About. On a device: only [MainActivity] reads the version name, and
 * Robolectric cannot start it, since it calls `Rive.init`.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AboutTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Test
    fun about_onAnInstalledApp_showsItsNameAndVersion() {
        // Given
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        composeRule.launchApp(frame = null)

        // When
        composeRule.openAbout()

        // Then
        composeRule.onNode(hasText("Molda") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Versão").assertIsDisplayed()
        composeRule.onNodeWithText(checkNotNull(versionName)).assertIsDisplayed()
    }
}
