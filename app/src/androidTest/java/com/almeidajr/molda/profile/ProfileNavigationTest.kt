package com.almeidajr.molda.profile

import android.view.KeyEvent
import android.view.WindowInsets
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.SkirtTitle
import com.almeidajr.molda.destination
import com.almeidajr.molda.screenTitle
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.showSkirtCalculator
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProfileNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun openSkirtCalculator() = composeRule.showSkirtCalculator()

    @Test
    fun calculatorLaunch_doesNotInlineProfileManagement() {
        composeRule.onNodeWithText("Novo perfil").assertDoesNotExist()
    }

    @Test
    fun navigationSuite_opensProfilesFromKeyboardAndReturnsToCalculator() {
        composeRule.destination("Perfis").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        // A real key event leaves touch mode, so navigation items join keyboard focus traversal.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
        composeRule.destination("Perfis").requestFocus().assertIsFocused()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
        composeRule.screenTitle("Perfis de medidas").assertIsDisplayed()
        composeRule.destination("Perfis").assertIsSelected()
        composeRule.screenTitle(SkirtTitle).assertDoesNotExist()
        composeRule.destination("Moldes").requestFocus().assertIsFocused()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
        composeRule.screenTitle(SkirtTitle).assertIsDisplayed()
    }

    @Test
    fun calculatorProfilesAndPicker_exposeSectionHeadings() {
        val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        composeRule.onNodeWithText(SkirtTitle).assert(heading)
        composeRule.selectDestination("Perfis")
        composeRule.onNodeWithText("Perfis de medidas").assert(heading)
        composeRule.selectDestination("Moldes")
        composeRule.onNodeWithText("Usar perfil").performScrollTo().performClick()
        composeRule.onNode(hasText("Usar perfil") and heading).assertIsDisplayed()
    }

    @Test
    fun recreation_keepsDirtyProfileEditorAndItsWayBack() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        composeRule.selectDestination("Perfis")
        composeRule.onNodeWithText("Novo perfil").performClick()
        composeRule.onNodeWithText("Nome ou apelido").performTextReplacement("Bia")
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithText("Nome ou apelido").assertTextContains("Bia")
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        clickInDialog("Descartar")
        composeRule.screenTitle("Perfis de medidas").assertIsDisplayed()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.screenTitle(SkirtTitle).assertIsDisplayed()
    }

    @Test
    fun pickerCreation_cancelReturnsToPickerWithoutSelecting() {
        val waist = composeRule.onNodeWithText("Circunferência da cintura (cm)")
        waist.performScrollTo().performTextReplacement("95")
        composeRule.onNodeWithText("Usar perfil").performScrollTo().performClick()
        composeRule.onNodeWithText("Novo perfil").performScrollTo().performClick()
        composeRule.onNodeWithText("Novo perfil de medidas").assertIsDisplayed()
        composeRule.destination("Moldes").assertIsSelected()
        composeRule.onNodeWithText("Cancelar").performScrollTo().performClick()
        composeRule.onNodeWithText("Novo perfil de medidas").assertDoesNotExist()
        composeRule.onNodeWithText("Cancelar seleção").assertIsDisplayed().performClick()
        waist.assertTextContains("95")
        composeRule.onNodeWithText("Medida manual").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun pickerCreation_dirtyCancelAsksToDiscard_thenReturnsToPicker() {
        composeRule.onNodeWithText("Usar perfil").performScrollTo().performClick()
        composeRule.onNodeWithText("Novo perfil").performScrollTo().performClick()
        composeRule.onNodeWithText("Nome ou apelido").performTextReplacement("Rascunho")
        // The keyboard resizing the editor mid-scroll would move Cancelar from under the touch.
        closeSoftKeyboard()
        composeRule.onNodeWithText("Cancelar").performScrollTo().performClick()
        val confirmation = composeRule.onNodeWithText("Descartar alterações?")
        composeRule.waitUntil(5_000) { confirmation.isDisplayed() }
        clickInDialog("Descartar")
        composeRule.onNodeWithText("Novo perfil de medidas").assertDoesNotExist()
        composeRule.onNodeWithText("Cancelar seleção").assertIsDisplayed()
        composeRule.onNodeWithText("Novo perfil").performScrollTo().performClick()
        composeRule.onNodeWithText("Novo perfil de medidas").assertIsDisplayed()
        composeRule.onNodeWithText("Rascunho").assertDoesNotExist()
    }

    @Test
    fun exampleGuideCalculateMySkirt_asksToDiscardDirtyPickerEditor_andLeavesNoDraftInPerfis() {
        openPickerEditor()
        composeRule.onNodeWithText("Nome ou apelido").performTextReplacement("Rascunho")
        calculateMySkirtFromGuias()
        clickInDialog("Descartar")
        composeRule.screenTitle(SkirtTitle).assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar seleção").assertDoesNotExist()
        assertPerfisShowsNoEditor()
    }

    @Test
    fun exampleGuideCalculateMySkirt_cancelsCleanPickerEditor_andLeavesNoDraftInPerfis() {
        openPickerEditor()
        calculateMySkirtFromGuias()
        composeRule.screenTitle(SkirtTitle).assertIsDisplayed()
        composeRule.onNodeWithText("Descartar alterações?").assertDoesNotExist()
        composeRule.onNodeWithText("Cancelar seleção").assertDoesNotExist()
        assertPerfisShowsNoEditor()
    }

    @Test
    fun reselectingMoldes_whileDiscardDialogIsOpen_keepsAsking_andLeavesNoDraftInPerfis() {
        openPickerEditor()
        composeRule.onNodeWithText("Nome ou apelido").performTextReplacement("Rascunho")
        // The keyboard resizing the editor mid-scroll would move Cancelar from under the touch.
        closeSoftKeyboard()
        composeRule.onNodeWithText("Cancelar").performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onNodeWithText("Descartar alterações?").isDisplayed()
        }
        composeRule.selectDestination("Moldes")
        clickInDialog("Descartar")
        composeRule.onNodeWithText("Cancelar seleção").assertIsDisplayed()
        assertPerfisShowsNoEditor()
    }

    @Test
    fun recreation_keepsOpenPicker() {
        composeRule.onNodeWithText("Usar perfil").performScrollTo().performClick()
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithText("Cancelar seleção").performScrollTo().performClick()
        composeRule.onNodeWithText("Cancelar seleção").assertDoesNotExist()
    }

    @Test
    fun recreation_keepsDiscardConfirmation() {
        composeRule.selectDestination("Perfis")
        composeRule.onNodeWithText("Novo perfil").performClick()
        composeRule.onNodeWithText("Nome ou apelido").performTextReplacement("Bia")
        composeRule.onNodeWithText("Cancelar").performScrollTo().performClick()
        composeRule.activityRule.scenario.recreate()
        clickInDialog("Continuar editando")
        composeRule.onNodeWithText("Nome ou apelido").assertTextContains("Bia")
    }

    @Test
    fun backWithKeyboardOpen_closesKeyboardAndKeepsProfileEditor() {
        composeRule.selectDestination("Perfis")
        composeRule.onNodeWithText("Novo perfil").performClick()
        composeRule.onNodeWithText("Nome ou apelido").performClick()
        composeRule.waitUntil(5_000) { isKeyboardVisible() }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.waitUntil(5_000) { !isKeyboardVisible() }
        composeRule.onNodeWithText("Nome ou apelido").assertIsDisplayed()
    }

    private fun openPickerEditor() {
        composeRule.onNodeWithText("Usar perfil").performScrollTo().performClick()
        composeRule.onNodeWithText("Novo perfil").performScrollTo().performClick()
        composeRule.onNodeWithText("Novo perfil de medidas").assertIsDisplayed()
    }

    private fun calculateMySkirtFromGuias() {
        closeSoftKeyboard()
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

    private fun clickInDialog(label: String) {
        // A semantics click needs no window focus, unlike a touch on a dialog still attaching.
        val button = hasText(label) and hasClickAction()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(button).fetchSemanticsNodes().size == 1
        }
        composeRule.onNode(button).performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun isKeyboardVisible(): Boolean {
        var isVisible = false
        composeRule.runOnUiThread {
            isVisible = composeRule.activity.window.decorView.rootWindowInsets
                .isVisible(WindowInsets.Type.ime())
        }
        return isVisible
    }
}
