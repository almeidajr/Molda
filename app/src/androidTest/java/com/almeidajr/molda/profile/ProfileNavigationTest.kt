package com.almeidajr.molda.profile

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.requestFocus
import com.almeidajr.molda.AnimatorDurationScaleRule
import com.almeidajr.molda.KeyboardModeRule
import com.almeidajr.molda.SkirtTitle
import com.almeidajr.molda.awaitActivityFocus
import com.almeidajr.molda.awaitKeyboardHidden
import com.almeidajr.molda.awaitKeyboardVisible
import com.almeidajr.molda.clickInDialog
import com.almeidajr.molda.closeKeyboard
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.destination
import com.almeidajr.molda.launchApp
import com.almeidajr.molda.pressBack
import com.almeidajr.molda.pressKey
import com.almeidajr.molda.recreate
import com.almeidajr.molda.screenTitle
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.showSkirtCalculator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ProfileNavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @get:Rule(order = 2)
    val keyboardMode = KeyboardModeRule(composeRule)

    @get:Rule(order = 3)
    val animatorScale = AnimatorDurationScaleRule(composeRule)

    @Before
    fun openSkirtCalculator() {
        composeRule.launchApp(frame = profileFrame())
        composeRule.showSkirtCalculator()
    }

    @Test
    fun calculator_whenOpened_inlinesNoProfileManagement() {
        // When/Then: profile management's FAB belongs to Perfis, not to the calculator.
        composeRule.onNodeWithContentDescription("Novo perfil").assertDoesNotExist()
    }

    @Test
    fun navigationSuite_withKeyboard_opensPerfis() {
        // Given
        // Out of touch mode, navigation items join keyboard focus traversal.
        keyboardMode.enter()

        // When
        composeRule.destination("Perfis").requestFocus().assertIsFocused()
        composeRule.pressKey(KeyEvent.KEYCODE_ENTER)

        // Then
        composeRule.screenTitle("Perfis de medidas").assertIsDisplayed()
        composeRule.destination("Perfis").assertIsSelected()
        composeRule.screenTitle(SkirtTitle).assertDoesNotExist()
    }

    @Test
    fun calculatorPerfisAndPicker_titles_areHeadings() {
        // When/Then
        composeRule.onNode(hasText(SkirtTitle) and isHeading()).assertIsDisplayed()
        composeRule.selectDestination("Perfis")
        composeRule.onNode(hasText("Perfis de medidas") and isHeading()).assertIsDisplayed()
        composeRule.selectDestination("Moldes")
        composeRule.onNodeWithText("Usar perfil").performScrollTo().performClick()
        composeRule.onNode(hasText("Usar perfil") and isHeading()).assertIsDisplayed()
    }

    @Test
    fun profileEditor_afterRecreation_keepsItsWayBack() {
        // Given
        composeRule.selectDestination("Perfis")
        composeRule.onNodeWithContentDescription("Novo perfil").performClick()
        composeRule.onNodeWithText("Nome ou apelido").performTextReplacement("Bia")
        composeRule.recreate()

        // When A key reaches only the focused window: the recreated activity, then again after the
        // dialog.
        composeRule.awaitActivityFocus()
        composeRule.pressBack()
        composeRule.clickInDialog("Descartar")
        composeRule.awaitActivityFocus()
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
        // The keyboard resizing the editor mid-scroll would move Cancelar from under the touch.
        composeRule.closeKeyboard()
        composeRule.onNodeWithText("Cancelar").performScrollTo().performClick()

        // When
        composeRule.clickInDialog("Descartar")

        // Then
        composeRule.onNodeWithText("Novo perfil de medidas").assertDoesNotExist()
        composeRule.onNodeWithText("Cancelar seleção").assertIsDisplayed()
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

    @Test
    fun reselectMoldes_withDiscardDialogOpen_keepsAsking() {
        // Given
        openPickerEditor()
        composeRule.onNodeWithText("Nome ou apelido").performTextReplacement("Rascunho")
        // The keyboard resizing the editor mid-scroll would move Cancelar from under the touch.
        composeRule.closeKeyboard()
        composeRule.onNodeWithText("Cancelar").performScrollTo().performClick()

        // When
        composeRule.selectDestination("Moldes")

        // Then
        composeRule.onNodeWithText("Descartar alterações?").assertIsDisplayed()
    }

    @Test
    fun picker_afterRecreation_staysOpen() {
        // Given
        composeRule.onNodeWithText("Usar perfil").performScrollTo().performClick()

        // When
        composeRule.recreate()

        // Then
        composeRule.onNodeWithText("Cancelar seleção").assertIsDisplayed()
    }

    @Test
    fun discardDialog_afterRecreation_staysOpen() {
        // Given
        composeRule.selectDestination("Perfis")
        composeRule.onNodeWithContentDescription("Novo perfil").performClick()
        composeRule.onNodeWithText("Nome ou apelido").performTextReplacement("Bia")
        // The opening keyboard resizes the editor, so a tap racing it can miss Cancelar.
        composeRule.closeKeyboard()
        composeRule.onNodeWithText("Cancelar").performScrollTo().performClick()

        // When
        composeRule.recreate()

        // Then
        composeRule.onNodeWithText("Descartar alterações?").assertIsDisplayed()
        composeRule.onNodeWithText("Nome ou apelido").assertTextContains("Bia")
    }

    @Test
    fun discard_inDiscardDialogAfterRecreation_closesTheEditor() {
        // Given
        openDiscardDialogAfterRecreation()

        // When
        composeRule.clickInDialog("Descartar")

        // Then
        composeRule.onNodeWithText("Novo perfil de medidas").assertDoesNotExist()
        composeRule.screenTitle("Perfis de medidas").assertIsDisplayed()
    }

    @Test
    fun continueEditing_inDiscardDialogAfterRecreation_keepsTheEdits() {
        // Given
        openDiscardDialogAfterRecreation()

        // When
        composeRule.clickInDialog("Continuar editando")

        // Then
        composeRule.onNodeWithText("Descartar alterações?").assertDoesNotExist()
        composeRule.onNodeWithText("Nome ou apelido").assertTextContains("Bia")
    }

    @Test
    fun back_withKeyboardOpen_closesOnlyTheKeyboard() {
        // Given
        composeRule.selectDestination("Perfis")
        composeRule.onNodeWithContentDescription("Novo perfil").performClick()
        composeRule.onNodeWithText("Nome ou apelido").performClick()
        composeRule.awaitKeyboardVisible()

        // When
        composeRule.pressBack()

        // Then
        composeRule.awaitKeyboardHidden()
        composeRule.onNodeWithText("Nome ou apelido").assertIsDisplayed()
    }

    private fun openDiscardDialogAfterRecreation() {
        composeRule.selectDestination("Perfis")
        composeRule.onNodeWithContentDescription("Novo perfil").performClick()
        composeRule.onNodeWithText("Nome ou apelido").performTextReplacement("Bia")
        // The opening keyboard resizes the editor, so a tap racing it can miss Cancelar.
        composeRule.closeKeyboard()
        composeRule.onNodeWithText("Cancelar").performScrollTo().performClick()
        composeRule.recreate()
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
