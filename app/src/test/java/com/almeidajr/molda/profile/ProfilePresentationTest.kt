package com.almeidajr.molda.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.click
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.launchApp
import com.almeidajr.molda.showProfiles
import com.almeidajr.molda.testing.FakeProfileRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ProfilePresentationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Test
    fun profileRows_withSameName_showTheirNotes() {
        // When
        composeRule.launchProfiles(sameNameWithNotes())

        // Then
        composeRule.onNodeWithText("Medida com roupa leve").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Conferida antes do corte").performScrollTo().assertIsDisplayed()
    }

    // The picker opens half expanded; below a phone's height, its rows start off screen.
    @Config(qualifiers = "w411dp-h923dp")
    @Test
    fun pickerRows_withSameName_showTheirNotes() {
        // Given
        composeRule.launchCalculator(sameNameWithNotes())

        // When
        composeRule.click("Usar perfil")

        // Then
        composeRule.onNodeWithText("Medida com roupa leve").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Conferida antes do corte").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun profileEditor_forANewProfile_explainsDateAndNotes() {
        // Given
        composeRule.launchProfiles()

        // When
        composeRule.click("Novo perfil")

        // Then
        composeRule.onNodeWithText("Identificação e medidas").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Escolha a data em que a medida foi tomada, sem usar uma data futura.",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Use observações para distinguir perfis com o mesmo nome.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun profiles_atDoubleFontScale_keepRowsAndActionsReachable() {
        // Given
        val name = "Ana Maria da Silva — medidas conferidas para a saia"
        val notes = "Conferida antes do corte"
        val storage = FakeProfileRepository(listOf(profile(1, name, notes = notes)))

        // When
        composeRule.launchApp(profiles = storage, frame = profileFrame(fontScale = 2f))
        composeRule.showProfiles()

        // Then
        composeRule.onNodeWithText(name).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(notes).assertIsDisplayed()
        composeRule.onNodeWithText("Ações: $name").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Novo perfil").assertIsDisplayed()
        composeRule.click("Ações: $name")
        composeRule.click("Editar perfil")
        composeRule.onNodeWithText("Salvar alterações").performScrollTo().assertIsDisplayed()
    }

    private fun sameNameWithNotes() = FakeProfileRepository(
        listOf(
            profile(1, "Ana", notes = "Medida com roupa leve"),
            profile(2, "Ana", notes = "Conferida antes do corte"),
        ),
    )
}
