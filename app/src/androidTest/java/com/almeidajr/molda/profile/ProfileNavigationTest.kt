package com.almeidajr.molda.profile

import android.view.KeyEvent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
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
    fun calculatorAppBar_opensProfilesFromKeyboardAndReturnsToCalculator() {
        val button = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        composeRule.onNodeWithText("Perfis de medidas")
            .assert(button).assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        // A real key event leaves touch mode, so buttons join keyboard focus traversal.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
        composeRule.onNodeWithText("Perfis de medidas").requestFocus().assertIsFocused()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
        composeRule.onNodeWithText("Perfis de medidas (opcional)").assertIsDisplayed()
        composeRule.onNodeWithText("Saia godê inteira").assertDoesNotExist()
        composeRule.onNodeWithText("Voltar ao cálculo").performClick()
        composeRule.onNodeWithText("Saia godê inteira").assertIsDisplayed()
        composeRule.onNodeWithText("Perfis de medidas").assertIsDisplayed()
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

    @Test
    fun recreation_keepsDirtyProfileEditorAndItsWayBack() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        composeRule.onNodeWithText("Perfis de medidas").performClick()
        composeRule.onNodeWithText("Novo perfil").performClick()
        composeRule.onNodeWithText("Nome ou apelido").performTextReplacement("Bia")
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithText("Nome ou apelido").assertTextContains("Bia")
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.onNodeWithText("Descartar").performClick()
        composeRule.onNodeWithText("Voltar ao cálculo").assertIsDisplayed()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.onNodeWithText("Saia godê inteira").assertIsDisplayed()
    }
}
