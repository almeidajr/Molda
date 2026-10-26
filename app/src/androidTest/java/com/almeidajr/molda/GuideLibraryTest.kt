package com.almeidajr.molda

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** `Guias`: the library of guides and the fabric-folding guide opened from it or from a result. */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class GuideLibraryTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @get:Rule(order = 2)
    val animatorScale = AnimatorDurationScaleRule(composeRule)

    private val guideTitle = "Guia de dobra do tecido"
    private val foldingSteps = 6
    private val markingStep = 3

    @Before
    fun showApp() {
        // The guide's drawings hold still, so Compose goes idle.
        animatorScale.set(0f)
        composeRule.launchApp()
    }

    @Test
    fun guias_listsFabricFoldingGuide_whichOpensOnExampleDimensionsLabeledOnEveryStep() {
        // Given
        composeRule.selectDestination("Guias")
        guideEntry().assertTextContains("Dobre", substring = true)

        // When
        guideEntry()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        // Then
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
    }

    @Test
    fun back_fromExampleGuide_returnsToGuias() {
        // Given
        composeRule.selectDestination("Guias")
        guideEntry().performClick()

        // When
        composeRule.pressBack()

        // Then
        composeRule.screenTitle("Guias").assertIsDisplayed()
    }

    @Test
    fun exampleGuide_calculateMySkirt_opensCalculatorInMoldes_keepingTheDraft() {
        // Given
        composeRule.openSkirtCalculator()
        waistField().performTextReplacement("90")
        composeRule.closeKeyboard()
        composeRule.selectDestination("Guias")
        guideEntry().performClick()

        // When
        composeRule.onNodeWithText("Calcular a minha saia").performClick()

        // Then
        composeRule.destination("Moldes").assertIsSelected()
        composeRule.screenTitle(SkirtTitle).assertIsDisplayed()
        waistField().assertTextContains("90")
    }

    @Test
    fun resultGuide_opensOnMoldesWithCalculatedRadiiUnlabeled_andBackReturnsToResults() {
        // Given
        composeRule.openSkirtCalculator()
        SkirtDraftField.entries.zip(listOf("100", "0", "50", "1", "1")).forEach { (field, value) ->
            composeRule.onNodeWithText(field.labelText).performScrollTo()
                .performTextReplacement(value)
        }
        composeRule.closeKeyboard()
        composeRule.onNodeWithText("Calcular").performScrollTo().performClick()

        // When
        composeRule.onNodeWithText(guideTitle).performScrollTo().performClick()
        repeat(markingStep) { composeRule.onNodeWithText("Próximo").performClick() }

        // Then
        // 100 cm of waist plus no ease: 100 / 2π.
        composeRule.onNodeWithText("1. Costura da cintura: 15,92 cm").assertExists()
        composeRule.onNodeWithText("Exemplo — não são as suas medidas").assertDoesNotExist()
        composeRule.pressBack()
        composeRule.onNodeWithText(guideTitle).assertExists()
        composeRule.screenTitle(SkirtTitle).assertIsDisplayed()
        composeRule.destination("Moldes").assertIsSelected()
    }

    @Test
    fun wideWindow_showsGuideBesideTheList_withPlaceholderUntilChosen() {
        // Given
        composeRule.showRoute(DpSize(1280.dp, 800.dp))
        composeRule.selectDestination("Guias")
        composeRule.onNodeWithText("Escolha um guia na lista.").assertIsDisplayed()

        // When
        guideEntry().assertIsNotSelected().performClick()

        // Then
        val list = composeRule.screenTitle("Guias").assertIsDisplayed().getUnclippedBoundsInRoot()
        val label = composeRule.onNodeWithText("Exemplo — não são as suas medidas")
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(list.right <= label.left, "guide not beside the list: $list $label")
        guideEntry().assertIsSelected()
        composeRule.onNodeWithText("Escolha um guia na lista.").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Fechar guia").assertDoesNotExist()
        composeRule.destination("Guias").assertIsDisplayed()
    }

    private fun waistField() = composeRule.onNodeWithText(SkirtDraftField.Waist.labelText)

    private fun guideEntry() = composeRule.onNode(hasText(guideTitle) and !isHeading())
}
