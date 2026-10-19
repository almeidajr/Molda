package com.almeidajr.molda

import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The About screen, opened from every root top app bar, and its open-source licences. */
@RunWith(AndroidJUnit4::class)
class AboutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val roots = listOf(
        "Moldes" to "Moldes",
        "Projetos" to "Projetos",
        "Guias" to "Guias",
        "Perfis" to "Perfis de medidas",
    )

    @Test
    fun aboutAction_onEveryRoot_opensAboutWithVersion_andBackReturnsThere() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        roots.forEach { (destination, title) ->
            composeRule.selectDestination(destination)
            aboutAction()
                .assertHasClickAction()
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
                .performClick()
            composeRule.screenTitle("Sobre o Molda").assertIsDisplayed()
            composeRule.onNode(hasText("Molda") and isHeading()).assertIsDisplayed()
            composeRule.onNodeWithText("Versão").assertIsDisplayed()
            composeRule.onNodeWithText(checkNotNull(versionName)).assertIsDisplayed()
            // Pushed screens carry an up arrow, not the About action.
            aboutAction().assertDoesNotExist()
            // Profile management's FAB belongs to its own screens.
            composeRule.onNodeWithText("Novo perfil").assertDoesNotExist()
            pressBack()
            composeRule.screenTitle(title).assertIsDisplayed()
            composeRule.destination(destination).assertIsSelected()
        }
    }

    @Test
    fun pushedScreen_hasNoAboutAction() {
        composeRule.openSkirtCalculator()
        aboutAction().assertDoesNotExist()
    }

    @Test
    fun licencesRow_opensOpenSourceLicences_andUpReturnsToAbout() {
        aboutAction().performClick()
        composeRule.onNodeWithText("Licenças de código aberto").performClick()
        composeRule.screenTitle("Licenças de código aberto").assertIsDisplayed()
        // A dependency the app ships with, listed from its build-time metadata.
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Hilt Android"))
        composeRule.onNodeWithText("Hilt Android").assertIsDisplayed()
        composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Voltar")),
        ).performClick()
        composeRule.screenTitle("Sobre o Molda").assertIsDisplayed()
    }

    private fun aboutAction() = composeRule.onNode(
        SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription,
            listOf("Sobre o Molda"),
        ),
    )

    private fun pressBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    }
}
