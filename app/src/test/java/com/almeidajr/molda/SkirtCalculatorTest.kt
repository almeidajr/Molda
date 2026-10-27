package com.almeidajr.molda

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.testing.October18Clock
import com.almeidajr.molda.ui.theme.MoldaTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SkirtCalculatorTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Before
    fun openSkirtCalculator() {
        composeRule.launchApp(clock = October18Clock)
        composeRule.showSkirtCalculator()
    }

    @Test
    fun form_beforeCalculation_explainsMeasuringAndExplicitZeros() {
        // Then
        listOf(
            "Meça a circunferência da cintura",
            "na posição desejada do cós",
            "Folga é o acréscimo à cintura para vestir; informe zero se não desejar folga.",
            "a partir da linha de costura da cintura; exclua a altura do cós.",
            "Tecido reservado para costurar a cintura; informe zero se não houver margem.",
            "Tecido reservado para acabar a barra; informe zero se não houver margem.",
        ).forEach {
            composeRule.onNodeWithText(it, substring = true).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun waistSource_onForm_sharesRowWithUseProfileJustAboveWaist() {
        // When
        val waist = composeRule.onNodeWithText(SkirtDraftField.Waist.labelText)
            .getUnclippedBoundsInRoot()
        val source = composeRule.onNodeWithText("Medida manual").getUnclippedBoundsInRoot()
        val useProfile = composeRule.onNodeWithText("Usar perfil").getUnclippedBoundsInRoot()

        // Then
        assertTrue(source.top >= useProfile.top && source.bottom <= useProfile.bottom)
        assertTrue(useProfile.bottom <= waist.top && waist.top - useProfile.bottom < 16.dp)
        assertTrue(useProfile.right <= waist.right + 1.dp)
    }

    @Test
    fun calculate_validDraft_showsAnnouncedRadiiSummaryAndLimitsInReadingOrder() {
        // Given
        composeRule.fillSkirtDraft()

        // When
        calculate()

        // Then
        listOf("Raios para o molde", "Linhas de referência", "Linhas de corte").forEach {
            composeRule.onNode(hasText(it) and isHeading()).performScrollTo().assertIsDisplayed()
        }
        composeRule.onAllNodes(
            hasText("Raios para o molde") and
                SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion),
        ).assertCountEquals(1)
        ReferenceRadii.forEach { radius ->
            composeRule.onNodeWithContentDescription(radius).performScrollTo().assertIsDisplayed()
            composeRule.onAllNodes(hasText(radius.substringAfterLast(": "))).assertCountEquals(1)
        }
        SkirtDraftField.entries.zip(ReferenceInputs).forEach { (field, value) ->
            composeRule.onNodeWithText("${field.summaryLabelText}: $value,00", substring = true)
                .performScrollTo().assertIsDisplayed()
        }
        listOf("Para saia godê inteira", "O comprimento acabado exclui a altura do cós.").forEach {
            composeRule.onNodeWithText(it, substring = true).performScrollTo().assertIsDisplayed()
        }
        assertResultsReadInOrder()
    }

    @Test
    fun disclosures_clicked_expandWithTheirContent() {
        // Given
        composeRule.fillSkirtDraft()
        calculate()

        mapOf(
            "Como levar os raios ao papel" to "Transfira os raios ao papel",
            "Fórmulas" to "r = (80 + 2) / (2π)",
        ).forEach { (label, content) ->
            val disclosure = composeRule.onNode(hasText(label) and hasClickAction())
            disclosure.performScrollTo().assert(hasStateDescription("Recolhido"))

            // When
            disclosure.performClick()

            // Then
            disclosure.assert(hasStateDescription("Expandido"))
            composeRule.onNodeWithText(content, substring = true).performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun editMeasurements_afterCalculation_focusesWaist() {
        // Given
        composeRule.fillSkirtDraft()
        calculate()

        // When
        composeRule.onNodeWithText("Editar medidas").performScrollTo().performClick()

        // Then
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText)
            .assertIsFocused().assertIsDisplayed()
    }

    @Test
    fun imeAction_afterFirstCalculation_revalidatesTheFieldLeft() {
        // Given
        calculate()
        edit(SkirtDraftField.Waist, "-1")

        // When
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).performImeAction()

        // Then
        composeRule.onNodeWithText("Informe um valor maior que zero.").assertExists()
        edit(SkirtDraftField.Waist, "80,5")
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).performImeAction()
        composeRule.onNodeWithText("Informe um valor maior que zero.").assertDoesNotExist()
    }

    @Test
    fun imeNext_onEachField_focusesTheNextField() {
        SkirtDraftField.entries.zipWithNext().forEach { (field, next) ->
            // When
            composeRule.onNodeWithText(field.labelText).performScrollTo().performClick()
                .performImeAction()

            // Then
            composeRule.onNodeWithText(next.labelText).assertIsFocused().assertIsDisplayed()
        }
    }

    @Test
    fun tab_onEase_focusesFinishedLength() {
        // Given
        val ease = composeRule.onNodeWithText(SkirtDraftField.Ease.labelText)
            .performScrollTo().performClick()

        // When
        ease.performKeyInput { pressKey(Key.Tab) }

        // Then
        composeRule.onNodeWithText(SkirtDraftField.FinishedLength.labelText)
            .assertIsFocused().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1200dp-h900dp-xxhdpi")
    fun wideCalculator_editAfterCalculation_bringsBackPlaceholderBesideInputs() {
        // Given
        composeRule.onNodeWithText("Preencha as medidas e toque em Calcular.").assertIsDisplayed()
        composeRule.fillSkirtDraft()
        calculate()
        composeRule.onNodeWithText("Raios para o molde").assertIsDisplayed()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assertIsDisplayed()

        // When
        edit(SkirtDraftField.Waist, "90")

        // Then
        composeRule.onNodeWithText("Raios para o molde").assertDoesNotExist()
        composeRule.onNodeWithText("Preencha as medidas e toque em Calcular.").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1200dp-h900dp-xxhdpi")
    fun resizingAndLargeText_afterCalculation_keepDraftAndResults() {
        // Given
        val width = mutableIntStateOf(1200)
        val font = mutableFloatStateOf(1f)
        showCalculator { route ->
            val window = LocalWindowInfo.current.containerSize
            val height = maxOf(900f, width.intValue * window.height.toFloat() / window.width)
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(font.floatValue)) {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(DpSize(width.intValue.dp, height.dp)),
                ) { MoldaTheme(content = route) }
            }
        }
        composeRule.fillSkirtDraft()
        calculate()

        // When
        composeRule.runOnIdle { width.intValue = 400 }

        // Then
        composeRule.onNodeWithContentDescription(ReferenceRadii.last())
            .performScrollTo().assertIsDisplayed()

        // When
        composeRule.runOnIdle {
            width.intValue = 1000
            font.floatValue = 2f
        }

        // Then
        composeRule.onNodeWithText("Editar medidas").performScrollTo().performClick()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText)
            .assertIsFocused().assertTextContains("80")
    }

    /** Shows the app inside [frame], on the calculator. */
    private fun showCalculator(frame: RouteFrame) {
        composeRule.showRoute(frame = frame)
        composeRule.showSkirtCalculator()
    }

    /** Card with its diagram, the action row, summary, limits, then the two disclosures. */
    private fun assertResultsReadInOrder() {
        val card = composeRule.onNode(hasAnyChild(hasText("Raios para o molde")))
            .getUnclippedBoundsInRoot()
        val diagram = composeRule.onNodeWithContentDescription(
            "Diagrama esquemático",
            substring = true,
        ).getUnclippedBoundsInRoot()
        assertTrue(
            diagram.top >= card.top && diagram.bottom <= card.bottom,
            "diagram outside the card",
        )
        val guide = composeRule.onNodeWithText("Guia de dobra do tecido")
            .getUnclippedBoundsInRoot()
        val edit = composeRule.onNodeWithText("Editar medidas").getUnclippedBoundsInRoot()
        assertTrue(guide.top >= card.bottom && edit.top >= card.bottom, "actions above the card")
        val below = listOf(
            "Medidas utilizadas (cm)",
            "Limites do cálculo",
            "Como levar os raios ao papel",
            "Fórmulas",
        ).map { composeRule.onNodeWithText(it).getUnclippedBoundsInRoot() }
        (listOf(maxOf(guide.bottom, edit.bottom)) + below.map { it.top })
            .zipWithNext().forEach { (upper, lower) ->
                assertTrue(upper <= lower, "results out of order: $upper > $lower")
            }
    }

    private fun edit(field: SkirtDraftField, value: String) {
        composeRule.onNodeWithText(field.labelText).performScrollTo().performTextReplacement(value)
    }

    private fun calculate() = composeRule.click("Calcular")
}
