package com.almeidajr.molda.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.SkirtTitle
import com.almeidajr.molda.clickInDialog
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.destination
import com.almeidajr.molda.launchApp
import com.almeidajr.molda.pressBack
import com.almeidajr.molda.recreate
import com.almeidajr.molda.screenTitle
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.showSkirtCalculator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ProfileNavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Before
    fun openSkirtCalculator() {
        composeRule.launchApp(frame = profileFrame())
        composeRule.showSkirtCalculator()
    }

    @Test
    fun picker_title_isAHeading() {
        // When
        composeRule.onNodeWithText("Usar perfil").performScrollTo().performClick()

        // Then
        composeRule.onNode(hasText("Usar perfil") and isHeading()).assertIsDisplayed()
    }

    @Test
    fun profileEditor_afterRecreation_keepsItsWayBack() {
        // Given
        composeRule.selectDestination("Perfis")
        composeRule.onNodeWithContentDescription("Novo perfil").performClick()
        composeRule.onNodeWithText("Nome ou apelido").performTextReplacement("Bia")
        composeRule.recreate()

        // When
        composeRule.pressBack()
        composeRule.clickInDialog("Descartar")
        composeRule.pressBack()

        // Then
        composeRule.screenTitle(SkirtTitle).assertIsDisplayed()
    }

    @Test
    fun perfisNewProfile_up_returnsToProfiles() {
        // Given
        composeRule.selectDestination("Perfis")
        composeRule.onNodeWithContentDescription("Novo perfil").performClick()
        composeRule.screenTitle("Novo perfil de medidas").assertIsDisplayed()

        // When
        composeRule.onNodeWithContentDescription("Voltar").performClick()

        // Then
        composeRule.screenTitle("Novo perfil de medidas").assertDoesNotExist()
        composeRule.screenTitle("Perfis de medidas").assertIsDisplayed()
    }

    @Test
    fun pickerNewProfile_cancel_returnsToPickerInMoldesWithoutSelecting() {
        // Given
        openPickerEditor()

        // When
        composeRule.onNodeWithText("Cancelar").performScrollTo().performClick()

        // Then
        composeRule.onNodeWithText("Novo perfil de medidas").assertDoesNotExist()
        composeRule.onNodeWithText("Cancelar seleção").assertIsDisplayed()
        composeRule.destination("Moldes").assertIsSelected()
        composeRule.onNodeWithText("Cintura copiada do perfil", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun pickerNewProfile_discardDirtyDraft_returnsToPicker() {
        // Given
        openPickerEditor()
        composeRule.onNodeWithText("Nome ou apelido").performTextReplacement("Rascunho")
        composeRule.onNodeWithText("Cancelar").performScrollTo().performClick()

        // When
        composeRule.clickInDialog("Descartar")

        // Then
        composeRule.onNodeWithText("Novo perfil de medidas").assertDoesNotExist()
        composeRule.onNodeWithText("Cancelar seleção").assertIsDisplayed()
    }

    @Test
    fun discard_inDiscardDialogAfterRecreation_closesTheEditor() {
        // Given
        composeRule.selectDestination("Perfis")
        composeRule.onNodeWithContentDescription("Novo perfil").performClick()
        composeRule.onNodeWithText("Nome ou apelido").performTextReplacement("Bia")
        composeRule.onNodeWithText("Cancelar").performScrollTo().performClick()
        composeRule.recreate()

        // When
        composeRule.clickInDialog("Descartar")

        // Then
        composeRule.onNodeWithText("Novo perfil de medidas").assertDoesNotExist()
        composeRule.screenTitle("Perfis de medidas").assertIsDisplayed()
    }

    private fun openPickerEditor() {
        composeRule.onNodeWithText("Usar perfil").performScrollTo().performClick()
        composeRule.onNodeWithText("Novo perfil").performScrollTo().performClick()
        composeRule.onNodeWithText("Novo perfil de medidas").assertIsDisplayed()
    }
}
