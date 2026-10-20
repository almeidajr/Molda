package com.almeidajr.molda

import android.view.KeyEvent
import android.view.WindowInsets
import androidx.activity.compose.setContent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.then
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.ui.theme.MoldaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The navigation suite: Moldes, Projetos, Guias and Perfis, each with its own back stack. */
@RunWith(AndroidJUnit4::class)
class NavigationShellTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val destinations = listOf("Moldes", "Projetos", "Guias", "Perfis")

    @Test
    fun launch_landsOnMoldesCatalog_withSkirtEntryAndFourDestinations() {
        composeRule.screenTitle("Moldes").assertIsDisplayed()
        composeRule.onNode(hasText(SkirtTitle) and !isHeading())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTextContains("Tecido plano", substring = true)
        composeRule.destination("Moldes").assertIsSelected()
        destinations.drop(1).forEach { composeRule.destination(it).assertIsNotSelected() }
        // The skirt app bar no longer carries profile management.
        composeRule.openSkirtCalculator()
        composeRule.onNodeWithText("Perfis de medidas").assertDoesNotExist()
    }

    @Test
    fun catalogToCalculator_backReturnsToCatalog_thenBackExits() {
        composeRule.openSkirtCalculator()
        pressBack()
        composeRule.screenTitle("Moldes").assertIsDisplayed()
        pressBack()
        composeRule.waitUntil(5_000) { composeRule.activity.isFinishing }
    }

    @Test
    fun calculatorUp_returnsToCatalog() {
        composeRule.openSkirtCalculator()
        composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Voltar")),
        ).performClick()
        composeRule.screenTitle("Moldes").assertIsDisplayed()
    }

    @Test
    fun switchingDestinations_keepsTypedDraft() {
        composeRule.openSkirtCalculator()
        waistField().performTextReplacement("80")
        closeSoftKeyboard()
        listOf("Perfis", "Guias", "Projetos").forEach { composeRule.selectDestination(it) }
        composeRule.selectDestination("Moldes")
        composeRule.screenTitle(SkirtTitle).assertIsDisplayed()
        waistField().assertTextContains("80")
    }

    @Test
    fun reselectingCurrentDestination_returnsToItsRoot() {
        composeRule.openSkirtCalculator()
        composeRule.selectDestination("Moldes")
        composeRule.screenTitle("Moldes").assertIsDisplayed()
        composeRule.selectDestination("Perfis")
        composeRule.onNodeWithContentDescription("Novo perfil").performClick()
        composeRule.onNodeWithText("Novo perfil de medidas").assertIsDisplayed()
        composeRule.selectDestination("Perfis")
        composeRule.onNodeWithText("Novo perfil de medidas").assertDoesNotExist()
        composeRule.screenTitle("Perfis de medidas").assertIsDisplayed()
    }

    @Test
    fun backAtAnotherDestinationRoot_goesToMoldes() {
        composeRule.openSkirtCalculator()
        listOf("Projetos" to "Projetos", "Guias" to "Guias", "Perfis" to "Perfis de medidas")
            .forEach { (destination, title) ->
                composeRule.selectDestination(destination)
                composeRule.screenTitle(title).assertIsDisplayed()
                composeRule.destination(destination).assertIsSelected()
                pressBack()
                composeRule.screenTitle(SkirtTitle).assertIsDisplayed()
                composeRule.destination("Moldes").assertIsSelected()
            }
    }

    @Test
    fun projetos_showsEmptyStateAndOpensMoldes() {
        composeRule.selectDestination("Projetos")
        composeRule.onNode(hasText("Nenhum projeto salvo") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Um projeto guarda", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Ver moldes").performClick()
        composeRule.screenTitle("Moldes").assertIsDisplayed()
        composeRule.destination("Moldes").assertIsSelected()
    }

    @Test
    fun recreation_keepsSelectedDestinationAndEachStack() {
        composeRule.openSkirtCalculator()
        waistField().performTextReplacement("80")
        closeSoftKeyboard()
        composeRule.selectDestination("Perfis")
        composeRule.onNodeWithContentDescription("Novo perfil").performClick()
        composeRule.activityRule.scenario.recreate()
        composeRule.destination("Perfis").assertIsSelected()
        composeRule.onNodeWithText("Novo perfil de medidas").assertIsDisplayed()
        composeRule.selectDestination("Moldes")
        composeRule.screenTitle(SkirtTitle).assertIsDisplayed()
        waistField().assertTextContains("80")
    }

    @Test
    fun keyboard_opensProfileEditorWithoutFocusingAField() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        composeRule.selectDestination("Perfis")
        // A real key event leaves touch mode, so buttons join keyboard focus traversal.
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
        composeRule.onNodeWithContentDescription("Novo perfil").requestFocus().assertIsFocused()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
        composeRule.onNodeWithText("Novo perfil de medidas").assertIsDisplayed()
        composeRule.onNodeWithText("Nome ou apelido").assertIsNotFocused()
        assertTrue("keyboard opened by itself", !isKeyboardVisible())
    }

    @Test
    fun navigationSuite_isBarOnCompactAndRailOnWide() {
        listOf(DpSize(400.dp, 800.dp) to false, DpSize(1000.dp, 800.dp) to true)
            .forEach { (size, isRail) ->
                composeRule.runOnUiThread {
                    composeRule.activity.setContent {
                        DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
                            MoldaTheme { MoldaRoute() }
                        }
                    }
                }
                val items = destinations.map {
                    composeRule.destination(it).assertIsDisplayed().getUnclippedBoundsInRoot()
                }
                val title = composeRule.screenTitle("Moldes").getUnclippedBoundsInRoot()
                if (isRail) {
                    assertTrue(
                        "$size items not in a column",
                        items.all {
                            it.left == items[0].left
                        },
                    )
                    assertTrue("$size rail not at the start", items[0].right <= title.left)
                } else {
                    assertTrue("$size items not in a row", items.all { it.top == items[0].top })
                    assertTrue("$size bar not below content", items[0].top > title.bottom)
                }
                assertEquals(destinations.size, items.distinct().size)
            }
    }

    @Test
    fun doubleTextOnPhoneBar_labelsFitTheirItems() {
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 640.dp)) then
                        DeviceConfigurationOverride.FontScale(2f),
                ) { MoldaTheme { MoldaRoute() } }
            }
        }
        destinations.forEach { label ->
            val layouts = mutableListOf<TextLayoutResult>()
            composeRule.onNode(hasText(label) and hasAnyAncestor(isSelectable()), true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                    assertTrue(it(layouts))
                }
            assertTrue("$label overflows its item", !layouts.single().hasVisualOverflow)
        }
    }

    @Test
    fun switchingDestination_movesFocusToTheTitle() {
        listOf(
            "Projetos" to "Projetos",
            "Guias" to "Guias",
            "Perfis" to "Perfis de medidas",
            "Moldes" to "Moldes",
        ).forEach { (destination, title) ->
            composeRule.selectDestination(destination)
            composeRule.screenTitle(title).assertIsFocused()
        }
        pressKey(KeyEvent.KEYCODE_TAB)
        composeRule.destination("Perfis").requestFocus()
        pressKey(KeyEvent.KEYCODE_ENTER)
        composeRule.screenTitle("Perfis de medidas").assertIsFocused()
        pressBack()
        composeRule.screenTitle("Moldes").assertIsFocused()
    }

    @Test
    fun switchingToAnOpenEditor_focusesItsHeadingNotAField() {
        composeRule.selectDestination("Perfis")
        composeRule.onNodeWithContentDescription("Novo perfil").performClick()
        composeRule.selectDestination("Moldes")
        composeRule.selectDestination("Perfis")
        composeRule.onNode(hasText("Novo perfil de medidas") and isHeading()).assertIsFocused()
        composeRule.onNodeWithText("Nome ou apelido").assertIsNotFocused()
        assertTrue("keyboard opened by itself", !isKeyboardVisible())
    }

    @Test
    fun keyboard_navigationItemsAreOneFocusGroup_arrowsMove_enterAndSpaceSelect() {
        // A real key event leaves touch mode, so navigation items join keyboard focus traversal.
        pressKey(KeyEvent.KEYCODE_TAB)
        composeRule.destination("Moldes").requestFocus().assertIsFocused()
        pressKey(KeyEvent.KEYCODE_DPAD_RIGHT)
        composeRule.destination("Projetos").assertIsFocused()
        pressKey(KeyEvent.KEYCODE_SPACE)
        composeRule.destination("Projetos").assertIsSelected()
        composeRule.destination("Projetos").requestFocus()
        pressKey(KeyEvent.KEYCODE_DPAD_RIGHT)
        composeRule.destination("Guias").assertIsFocused()
        pressKey(KeyEvent.KEYCODE_ENTER)
        composeRule.destination("Guias").assertIsSelected()
        // Tab and Shift+Tab leave the items in one step, wherever focus sits among them.
        listOf(false, true).forEach { isShifted ->
            composeRule.destination("Projetos").requestFocus()
            pressKey(KeyEvent.KEYCODE_TAB, isShifted)
            destinations.forEach { composeRule.destination(it).assertIsNotFocused() }
        }
    }

    private fun waistField() = composeRule.onNodeWithText(SkirtDraftField.Waist.labelText)

    private fun pressBack() = pressKey(KeyEvent.KEYCODE_BACK)

    private fun pressKey(keyCode: Int, isShifted: Boolean = false) {
        val metaState = if (isShifted) KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON else 0
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP).forEach { action ->
            instrumentation.sendKeySync(KeyEvent(0, 0, action, keyCode, 0, metaState))
        }
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
