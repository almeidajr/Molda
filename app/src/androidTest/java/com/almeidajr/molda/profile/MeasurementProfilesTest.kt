package com.almeidajr.molda.profile

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import com.almeidajr.molda.awaitKeyboardVisible
import com.almeidajr.molda.click
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.launchApp
import com.almeidajr.molda.showProfiles
import com.almeidajr.molda.testing.FakeProfileRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

/** Measurement profiles with the keyboard open. On a device: Robolectric has no IME. */
@HiltAndroidTest
class MeasurementProfilesTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Test
    fun largeText_withKeyboardOpen_saveStillWorks() {
        // Given: a saved profile, since the calendar a new one needs leaves the keyboard unable to
        // open for a while after it closes on a slow emulator.
        val storage = FakeProfileRepository(listOf(profile(1, "Ana")))
        composeRule.launchApp(profiles = storage, frame = profileFrame(fontScale = 2f))
        composeRule.showProfiles()
        // At this size a tap on the row's center lands on the Novo perfil button above it.
        composeRule.onNodeWithText("Ana").performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithText("Observações (opcional)").performScrollTo().performClick()
            .performTextInput("Cós")
        composeRule.awaitKeyboardVisible()

        // When
        composeRule.click("Salvar alterações")

        // Then
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertIsDisplayed()
        assertEquals("Cós", storage.current.single().measurements.notes)
    }
}
