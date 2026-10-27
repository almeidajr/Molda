package com.almeidajr.molda

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The About screen, opened from every root top app bar, and its open-source licenses. */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AboutTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Before
    fun showApp() = composeRule.launchApp()

    @Test
    fun aboutAction_onEveryRoot_opensAbout_andUpReturnsThere() {
        listOf(
            "Moldes" to "Moldes",
            "Projetos" to "Projetos",
            "Guias" to "Guias",
            "Perfis" to "Perfis de medidas",
        ).forEach { (destination, title) ->
            // Given
            composeRule.selectDestination(destination)

            // When
            composeRule.aboutAction().assertHasClickAction().performClick()

            // Then
            composeRule.screenTitle("Sobre o Molda").assertIsDisplayed()
            // Pushed screens carry an up arrow, not the About action.
            composeRule.aboutAction().assertDoesNotExist()
            // Profile management's FAB belongs to its own screens.
            composeRule.onNodeWithContentDescription("Novo perfil").assertDoesNotExist()
            up().performClick()
            composeRule.screenTitle(title).assertIsDisplayed()
            composeRule.destination(destination).assertIsSelected()
        }
    }

    @Test
    fun licensesRow_opensOpenSourceLicenses_andUpReturnsToAbout() {
        // Given
        composeRule.openAbout()

        // When
        composeRule.onNodeWithText("Licenças de código aberto").performScrollTo().performClick()

        // Then
        composeRule.screenTitle("Licenças de código aberto").assertIsDisplayed()
        up().performClick()
        composeRule.screenTitle("Sobre o Molda").assertIsDisplayed()
    }

    private fun up() = composeRule.onNode(
        SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Voltar")),
    )
}
