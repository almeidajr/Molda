package com.almeidajr.molda

import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.ui.theme.MoldaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Guias: the library of guides and the fabric-folding guide opened from it or from a result. */
@RunWith(AndroidJUnit4::class)
class GuideLibraryTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val guideTitle = "Guia de dobra do tecido"
    private val foldingSteps = 6
    private val markingStep = 3

    @Test
    fun guias_listsFabricFoldingGuide_whichOpensOnExampleDimensionsLabelledOnEveryStep() {
        composeRule.selectDestination("Guias")
        guideEntry()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        // On a compact window the guide's step bar is the only bottom control.
        composeRule.destination("Guias").assertIsNotDisplayed()
        repeat(foldingSteps) { step ->
            composeRule.onNodeWithText("Exemplo — não são as suas medidas").assertIsDisplayed()
            // The marking step reads the example's radii: waist 80 + ease 2 cm.
            if (step == markingStep) {
                composeRule.onNodeWithText("1. Costura da cintura: 13,05\u00A0cm").assertExists()
            }
            if (step < foldingSteps - 1) composeRule.onNodeWithText("Próximo").performClick()
        }
        pressBack()
        composeRule.screenTitle("Guias").assertIsDisplayed()
        guideEntry().assertTextContains("Dobre", substring = true)
    }

    @Test
    fun exampleGuide_calculateMySkirt_opensCalculatorInMoldes_keepingTheDraft() {
        composeRule.openSkirtCalculator()
        waistField().performTextReplacement("90")
        closeSoftKeyboard()
        composeRule.selectDestination("Guias")
        guideEntry().performClick()
        composeRule.onNodeWithText("Calcular a minha saia").performClick()
        composeRule.destination("Moldes").assertIsSelected()
        composeRule.screenTitle(SkirtTitle).assertIsDisplayed()
        waistField().assertTextContains("90")
    }

    @Test
    fun resultGuide_opensOnMoldesWithCalculatedRadiiUnlabelled_andBackReturnsToResults() {
        composeRule.openSkirtCalculator()
        SkirtDraftField.entries.zip(listOf("100", "0", "50", "1", "1")).forEach { (field, value) ->
            composeRule.onNodeWithText(field.labelText).performScrollTo()
                .performTextReplacement(value)
        }
        closeSoftKeyboard()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()
        composeRule.onNodeWithText(guideTitle).performScrollTo().performClick()
        repeat(markingStep) { composeRule.onNodeWithText("Próximo").performClick() }
        // 100 cm of waist plus no ease: 100 / 2π.
        composeRule.onNodeWithText("1. Costura da cintura: 15,92 cm").assertExists()
        composeRule.onNodeWithText("Exemplo — não são as suas medidas").assertDoesNotExist()
        pressBack()
        composeRule.onNodeWithText(guideTitle).assertExists()
        composeRule.screenTitle(SkirtTitle).assertIsDisplayed()
        composeRule.destination("Moldes").assertIsSelected()
    }

    @Test
    fun wideWindow_showsGuideBesideTheList_withPlaceholderUntilChosen() {
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(DpSize(1280.dp, 800.dp)),
                ) { MoldaTheme { MoldaRoute() } }
            }
        }
        composeRule.selectDestination("Guias")
        composeRule.onNodeWithText("Escolha um guia na lista.").assertIsDisplayed()
        guideEntry().assertIsNotSelected().performClick()
        val list = composeRule.screenTitle("Guias").assertIsDisplayed().getUnclippedBoundsInRoot()
        val label = composeRule.onNodeWithText("Exemplo — não são as suas medidas")
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("guide not beside the list: $list $label", list.right <= label.left)
        guideEntry().assertIsSelected()
        composeRule.onNodeWithText("Escolha um guia na lista.").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Fechar guia").assertDoesNotExist()
        composeRule.destination("Guias").assertIsDisplayed()
    }

    private fun waistField() = composeRule.onNodeWithText(SkirtDraftField.Waist.labelText)

    private fun guideEntry() = composeRule.onNode(hasText(guideTitle) and !isHeading())

    private fun pressBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    }
}
