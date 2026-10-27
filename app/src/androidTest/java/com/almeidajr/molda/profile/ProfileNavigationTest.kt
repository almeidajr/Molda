package com.almeidajr.molda.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.almeidajr.molda.AnimatorDurationScaleRule
import com.almeidajr.molda.SkirtTitle
import com.almeidajr.molda.clickInDialog
import com.almeidajr.molda.closeKeyboard
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.launchApp
import com.almeidajr.molda.screenTitle
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.showSkirtCalculator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** The guide's Calcular a minha saia over the picker's editor. On a device: the guide runs Rive. */
@HiltAndroidTest
class ProfileNavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @get:Rule(order = 2)
    val animatorScale = AnimatorDurationScaleRule(composeRule)

    @Before
    fun openSkirtCalculator() {
        composeRule.launchApp(frame = profileFrame())
        composeRule.showSkirtCalculator()
    }

    @Test
    fun guideCalculateMySkirt_overDirtyPickerEditor_asksToDiscardAndLeavesNoDraft() {
        // Given
        openPickerEditor()
        composeRule.onNodeWithText("Nome ou apelido").performTextReplacement("Rascunho")

        // When
        calculateMySkirtFromGuias()
        composeRule.clickInDialog("Descartar")

        // Then
        composeRule.screenTitle(SkirtTitle).assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar seleção").assertDoesNotExist()
        assertPerfisShowsNoEditor()
    }

    @Test
    fun guideCalculateMySkirt_overCleanPickerEditor_closesItWithoutAsking() {
        // Given
        openPickerEditor()

        // When
        calculateMySkirtFromGuias()

        // Then
        composeRule.screenTitle(SkirtTitle).assertIsDisplayed()
        composeRule.onNodeWithText("Descartar alterações?").assertDoesNotExist()
        composeRule.onNodeWithText("Cancelar seleção").assertDoesNotExist()
        assertPerfisShowsNoEditor()
    }

    private fun openPickerEditor() {
        composeRule.onNodeWithText("Usar perfil").performScrollTo().performClick()
        composeRule.onNodeWithText("Novo perfil").performScrollTo().performClick()
        composeRule.onNodeWithText("Novo perfil de medidas").assertIsDisplayed()
    }

    private fun calculateMySkirtFromGuias() {
        composeRule.closeKeyboard()
        // The guide's drawings hold still, so Compose goes idle.
        animatorScale.set(0f)
        composeRule.selectDestination("Guias")
        composeRule.onNode(hasText("Guia de dobra do tecido") and !isHeading()).performClick()
        composeRule.onNodeWithText("Calcular a minha saia").performClick()
    }

    private fun assertPerfisShowsNoEditor() {
        composeRule.selectDestination("Perfis")
        composeRule.screenTitle("Perfis de medidas").assertIsDisplayed()
        composeRule.onNodeWithText("Novo perfil de medidas").assertDoesNotExist()
        composeRule.onNodeWithText("Rascunho").assertDoesNotExist()
    }
}
