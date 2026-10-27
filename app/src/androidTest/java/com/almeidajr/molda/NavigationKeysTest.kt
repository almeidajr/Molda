package com.almeidajr.molda

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Keyboard navigation through the navigation suite, and Back leaving the app. On a device: they
 * need keyboard (non-touch) mode or the activity finishing.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class NavigationKeysTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @get:Rule(order = 2)
    val keyboardMode = KeyboardModeRule(composeRule)

    private val destinations = listOf("Moldes", "Projetos", "Guias", "Perfis")

    @Before
    fun showApp() = composeRule.launchApp()

    @Test
    fun back_onCatalog_leavesTheApp() {
        // When
        composeRule.pressBack()

        // Then
        composeRule.awaitActivityFinishing()
    }

    @Test
    fun newProfileByKeyboard_opensEditorWithoutFocusingAField() {
        // Given
        composeRule.selectDestination("Perfis")
        // Out of touch mode, buttons join keyboard focus traversal.
        keyboardMode.enter()
        composeRule.onNodeWithContentDescription("Novo perfil").requestFocus()

        // When
        composeRule.pressKey(KeyEvent.KEYCODE_ENTER)

        // Then
        composeRule.onNodeWithText("Novo perfil de medidas").assertIsDisplayed()
        composeRule.assertNoTextFieldFocused()
    }

    @Test
    fun arrowKeys_onNavigationItems_moveFocus_andEnterOrSpaceSelects() {
        // Given: out of touch mode, navigation items join keyboard focus traversal.
        keyboardMode.enter()
        composeRule.destination("Moldes").requestFocus()

        // When/Then
        composeRule.pressKey(KeyEvent.KEYCODE_DPAD_RIGHT)
        composeRule.destination("Projetos").assertIsFocused()
        composeRule.pressKey(KeyEvent.KEYCODE_SPACE)
        composeRule.destination("Projetos").assertIsSelected()
        composeRule.destination("Projetos").requestFocus()
        composeRule.pressKey(KeyEvent.KEYCODE_DPAD_RIGHT)
        composeRule.destination("Guias").assertIsFocused()
        composeRule.pressKey(KeyEvent.KEYCODE_ENTER)
        composeRule.destination("Guias").assertIsSelected()
    }

    @Test
    fun tab_fromAnyNavigationItem_leavesTheItemsInOneStep() {
        // Given: out of touch mode, navigation items join keyboard focus traversal.
        keyboardMode.enter()

        listOf(0, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON).forEach { metaState ->
            composeRule.destination("Projetos").requestFocus()

            // When
            composeRule.pressKey(KeyEvent.KEYCODE_TAB, metaState)

            // Then
            destinations.forEach { composeRule.destination(it).assertIsNotFocused() }
        }
    }
}
