package com.almeidajr.molda.profile

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.almeidajr.molda.assertSurfaceStandsOut
import com.almeidajr.molda.click
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.launchApp
import com.almeidajr.molda.showRoute
import com.almeidajr.molda.showSkirtCalculator
import com.almeidajr.molda.testing.FakeProfileRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

/** The picker's row surfaces. On a device: the check reads rendered pixels. */
@HiltAndroidTest
class ProfilePresentationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Test
    fun pickerRows_inEveryScheme_standOutFromTheSheet() {
        // Given
        val storage = FakeProfileRepository(listOf(profile(1, "Ana"), profile(2, "Bia", 72.0)))
        val schemes = listOf(false, true)

        composeRule.launchApp(profiles = storage, frame = null)

        // When/Then
        schemes.forEach { isDarkTheme ->
            composeRule.showRoute(frame = profileFrame(isDarkTheme = isDarkTheme))
            composeRule.showSkirtCalculator()
            composeRule.click("Usar perfil")
            listOf("Ana — 80", "Bia — 72").forEach {
                val label = "Usar perfil: $it cm — 12/10/2026"
                composeRule.onNodeWithText(label).performScrollTo()
                    .assertSurfaceStandsOut("$label dark=$isDarkTheme row")
            }
            composeRule.click("Cancelar seleção")
        }
    }
}
