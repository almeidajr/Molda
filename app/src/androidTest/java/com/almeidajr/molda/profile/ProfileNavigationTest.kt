package com.almeidajr.molda.profile

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.almeidajr.molda.MainActivity
import org.junit.Rule
import org.junit.Test

class ProfileNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun calculatorLaunch_doesNotInlineProfileManagement() {
        composeRule.onNodeWithText("Novo perfil").assertDoesNotExist()
    }

    @Test
    fun calculatorProfilesAndPicker_exposeSectionHeadings() {
        val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        composeRule.onNodeWithText("Saia godê inteira").assert(heading)
        composeRule.onNodeWithText("Perfis de medidas").performClick()
        composeRule.onNodeWithText("Perfis de medidas (opcional)").assert(heading)
        composeRule.onNodeWithText("Voltar ao cálculo").performClick()
        composeRule.onNodeWithText("Usar perfil").performScrollTo().performClick()
        composeRule.onNode(hasText("Usar perfil") and heading).assertIsDisplayed()
    }
}
