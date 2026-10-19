package com.almeidajr.molda.profile

import android.view.KeyEvent
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inspector.WindowInspector
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
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
        composeRule.onNodeWithText("Cancelar").performScrollTo().performClick()
        composeRule.onNodeWithText("Descartar alterações?").assertIsDisplayed()
        clickInDialog("Descartar")
        composeRule.onNodeWithText("Novo perfil de medidas").assertDoesNotExist()
        composeRule.onNodeWithText("Cancelar seleção").assertIsDisplayed()
        composeRule.onNodeWithText("Novo perfil").performScrollTo().performClick()
        composeRule.onNodeWithText("Novo perfil de medidas").assertIsDisplayed()
        composeRule.onNodeWithText("Rascunho").assertDoesNotExist()
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

    private fun clickInDialog(label: String) {
        // Touches reach a dialog only once its window has focus.
        composeRule.waitUntil(5_000) {
            WindowInspector.getGlobalWindowViews().any {
                it.hasWindowFocus() &&
                    (it.layoutParams as WindowManager.LayoutParams).type ==
                    WindowManager.LayoutParams.TYPE_APPLICATION
            }
        }
        composeRule.onNodeWithText(label).performClick()
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
