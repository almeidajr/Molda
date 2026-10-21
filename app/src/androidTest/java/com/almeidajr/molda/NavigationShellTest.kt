package com.almeidajr.molda

import android.view.KeyEvent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.ui.theme.MoldaTheme
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The navigation suite: Moldes, Projetos, Guias and Perfis, each with its own back stack. */
@RunWith(AndroidJUnit4::class)
class NavigationShellTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val destinations = listOf("Moldes", "Projetos", "Guias", "Perfis")

    @Before
    fun showApp() = composeRule.showEmptyApp()

    @Test
    fun start_showsMoldesSelected_withSkirtEntry() {
        // When/Then
        composeRule.screenTitle("Moldes").assertIsDisplayed()
        composeRule.onNode(hasText(SkirtTitle) and !isHeading())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTextContains("Tecido plano", substring = true)
        composeRule.destination("Moldes").assertIsSelected()
        destinations.drop(1).forEach { composeRule.destination(it).assertIsNotSelected() }
    }

    @Test
    fun back_fromCalculator_returnsToCatalog() {
        // Given
        composeRule.openSkirtCalculator()

        // When
        pressBack()

        // Then
        composeRule.screenTitle("Moldes").assertIsDisplayed()
    }

    @Test
    fun back_onCatalog_leavesTheApp() {
        // When
        pressBack()

        // Then
        composeRule.awaitActivityFinishing()
    }

    @Test
    fun up_fromCalculator_returnsToCatalog() {
        // Given
        composeRule.openSkirtCalculator()

        // When
        composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Voltar")),
        ).performClick()

        // Then
        composeRule.screenTitle("Moldes").assertIsDisplayed()
    }

    @Test
    fun switchingDestinations_keepsCalculatorDraft() {
        // Given
        composeRule.openSkirtCalculator()
        waistField().performTextReplacement("80")
        closeSoftKeyboard()

        // When
        listOf("Perfis", "Guias", "Projetos", "Moldes").forEach {
            composeRule.selectDestination(it)
        }

        // Then
        composeRule.screenTitle(SkirtTitle).assertIsDisplayed()
        waistField().assertTextContains("80")
    }

    @Test
    fun reselectingCurrentDestination_returnsToItsRoot() {
        // Given
        composeRule.openSkirtCalculator()

        // When
        composeRule.selectDestination("Moldes")

        // Then
        composeRule.screenTitle("Moldes").assertIsDisplayed()
    }

    @Test
    fun back_atAnotherDestinationRoot_returnsToMoldes() {
        // Given
        composeRule.openSkirtCalculator()
        val roots = listOf(
            "Projetos" to "Projetos",
            "Guias" to "Guias",
            "Perfis" to "Perfis de medidas",
        )

        roots.forEach { (destination, title) ->
            composeRule.selectDestination(destination)
            composeRule.screenTitle(title).assertIsDisplayed()

            // When
            pressBack()

            // Then
            composeRule.screenTitle(SkirtTitle).assertIsDisplayed()
            composeRule.destination("Moldes").assertIsSelected()
        }
    }

    @Test
    fun projetos_withNoProjects_offersMoldes() {
        // Given
        composeRule.selectDestination("Projetos")
        composeRule.onNode(hasText("Nenhum projeto salvo") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Um projeto guarda", substring = true).assertIsDisplayed()

        // When
        composeRule.onNodeWithText("Ver moldes").performClick()

        // Then
        composeRule.screenTitle("Moldes").assertIsDisplayed()
        composeRule.destination("Moldes").assertIsSelected()
    }

    @Test
    fun recreation_keepsSelectedDestinationAndEachStack() {
        // Given
        composeRule.openSkirtCalculator()
        waistField().performTextReplacement("80")
        closeSoftKeyboard()
        composeRule.selectDestination("Perfis")
        composeRule.onNodeWithContentDescription("Novo perfil").performClick()

        // When
        composeRule.activityRule.scenario.recreate()
        composeRule.showEmptyApp()

        // Then
        composeRule.destination("Perfis").assertIsSelected()
        composeRule.onNodeWithText("Novo perfil de medidas").assertIsDisplayed()
        composeRule.selectDestination("Moldes")
        composeRule.screenTitle(SkirtTitle).assertIsDisplayed()
        waistField().assertTextContains("80")
    }

    @Test
    fun newProfileByKeyboard_opensEditorWithoutFocusingAField() {
        // Given
        composeRule.selectDestination("Perfis")
        // A real key event leaves touch mode, so buttons join keyboard focus traversal.
        pressKey(KeyEvent.KEYCODE_TAB)
        composeRule.onNodeWithContentDescription("Novo perfil").requestFocus()

        // When
        pressKey(KeyEvent.KEYCODE_ENTER)

        // Then
        composeRule.onNodeWithText("Novo perfil de medidas").assertIsDisplayed()
        composeRule.onNodeWithText("Nome ou apelido").assertIsNotFocused()
        assertFalse(composeRule.isKeyboardVisible(), "keyboard opened by itself")
    }

    @Test
    fun navigationSuite_isBarOnCompactAndRailOnWide() {
        listOf(DpSize(400.dp, 800.dp) to false, DpSize(1000.dp, 800.dp) to true)
            .forEach { (size, isRail) ->
                // When
                composeRule.showEmptyApp(size)

                // Then
                val items = destinations.map {
                    composeRule.destination(it).assertIsDisplayed().getUnclippedBoundsInRoot()
                }
                val title = composeRule.screenTitle("Moldes").getUnclippedBoundsInRoot()
                if (isRail) {
                    assertTrue(
                        items.all {
                            it.left == items[0].left
                        },
                        "$size items not in a column",
                    )
                    assertTrue(items[0].right <= title.left, "$size rail not at the start")
                } else {
                    assertTrue(items.all { it.top == items[0].top }, "$size items not in a row")
                    assertTrue(items[0].top > title.bottom, "$size bar not below content")
                }
                assertEquals(destinations.size, items.distinct().size)
            }
    }

    @Test
    fun phoneBar_withDoubleText_labelsFitTheirItems() {
        // When
        composeRule.showEmptyApp(DpSize(360.dp, 640.dp)) { route ->
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                MoldaTheme(content = route)
            }
        }

        // Then
        destinations.forEach { label ->
            val layouts = mutableListOf<TextLayoutResult>()
            composeRule.onNode(hasText(label) and hasAnyAncestor(isSelectable()), true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                    assertTrue(it(layouts))
                }
            assertFalse(layouts.single().hasVisualOverflow, "$label overflows its item")
        }
    }

    @Test
    fun selectingDestination_focusesItsTitle() {
        listOf(
            "Projetos" to "Projetos",
            "Guias" to "Guias",
            "Perfis" to "Perfis de medidas",
            "Moldes" to "Moldes",
        ).forEach { (destination, title) ->
            // When
            composeRule.selectDestination(destination)

            // Then
            composeRule.screenTitle(title).assertIsFocused()
        }
    }

    @Test
    fun back_toMoldes_focusesItsTitle() {
        // Given
        composeRule.selectDestination("Perfis")

        // When
        pressBack()

        // Then
        composeRule.screenTitle("Moldes").assertIsFocused()
    }

    @Test
    fun returningToOpenProfileEditor_focusesItsHeadingNotAField() {
        // Given
        composeRule.selectDestination("Perfis")
        composeRule.onNodeWithContentDescription("Novo perfil").performClick()
        composeRule.selectDestination("Moldes")

        // When
        composeRule.selectDestination("Perfis")

        // Then
        composeRule.onNode(hasText("Novo perfil de medidas") and isHeading()).assertIsFocused()
        composeRule.onNodeWithText("Nome ou apelido").assertIsNotFocused()
        assertFalse(composeRule.isKeyboardVisible(), "keyboard opened by itself")
    }

    @Test
    fun arrowKeys_onNavigationItems_moveFocus_andEnterOrSpaceSelects() {
        // Given: a real key event leaves touch mode, so navigation items join focus traversal.
        pressKey(KeyEvent.KEYCODE_TAB)
        composeRule.destination("Moldes").requestFocus()

        // When/Then
        pressKey(KeyEvent.KEYCODE_DPAD_RIGHT)
        composeRule.destination("Projetos").assertIsFocused()
        pressKey(KeyEvent.KEYCODE_SPACE)
        composeRule.destination("Projetos").assertIsSelected()
        composeRule.destination("Projetos").requestFocus()
        pressKey(KeyEvent.KEYCODE_DPAD_RIGHT)
        composeRule.destination("Guias").assertIsFocused()
        pressKey(KeyEvent.KEYCODE_ENTER)
        composeRule.destination("Guias").assertIsSelected()
    }

    @Test
    fun tab_fromAnyNavigationItem_leavesTheItemsInOneStep() {
        // Given: a real key event leaves touch mode, so navigation items join focus traversal.
        pressKey(KeyEvent.KEYCODE_TAB)

        listOf(false, true).forEach { isShifted ->
            composeRule.destination("Projetos").requestFocus()

            // When
            pressKey(KeyEvent.KEYCODE_TAB, isShifted)

            // Then
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
}
