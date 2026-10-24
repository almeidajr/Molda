package com.almeidajr.molda.profile

import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almeidajr.molda.KeyboardModeRule
import com.almeidajr.molda.assertDrawnHeightIsAtLeast
import com.almeidajr.molda.assertNoTextFieldFocused
import com.almeidajr.molda.assertReadableTextContrast
import com.almeidajr.molda.assertSurfaceStandsOut
import com.almeidajr.molda.click
import com.almeidajr.molda.closeKeyboard
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.launchApp
import com.almeidajr.molda.pressKey
import com.almeidajr.molda.showProfiles
import com.almeidajr.molda.showRoute
import com.almeidajr.molda.showSkirtCalculator
import com.almeidajr.molda.testing.FakeProfileRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ProfilePresentationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @get:Rule(order = 2)
    val keyboardMode = KeyboardModeRule(composeRule)

    @Test
    fun profileRows_withSameName_showTheirNotes() {
        // When
        composeRule.launchProfiles(sameNameWithNotes())

        // Then
        composeRule.onNodeWithText("Medida com roupa leve").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Conferida antes do corte").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun pickerRows_withSameName_showTheirNotes() {
        // Given
        composeRule.launchCalculator(sameNameWithNotes())

        // When
        composeRule.click("Usar perfil")

        // Then
        composeRule.onNodeWithText("Medida com roupa leve").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Conferida antes do corte").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun newProfileEditor_explainsDateAndNotes() {
        // Given
        composeRule.launchProfiles()

        // When
        composeRule.click("Novo perfil")

        // Then
        composeRule.onNodeWithText("Identificação e medidas").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Escolha a data em que a medida foi tomada, sem usar uma data futura.",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Use observações para distinguir perfis com o mesmo nome.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun newProfileEditor_dateField_startsEmpty() {
        // Given
        composeRule.launchProfiles()

        // When
        composeRule.click("Novo perfil")

        // Then
        composeRule.onNodeWithText("Data da medida (dd/mm/aaaa)").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")),
        )
    }

    @Test
    fun profileTexts_useAgreedTypography() {
        // Given
        composeRule.launchProfiles(FakeProfileRepository(listOf(profile(1, "Ana"))))

        // When/Then
        assertTypography("Ana", 16, 24, FontWeight.Medium)
        composeRule.click("Novo perfil")
        listOf(
            "Nomes repetidos são permitidos.",
            "Escolha a data em que a medida foi tomada, sem usar uma data futura.",
        ).forEach { assertTypography(it, 14, 20) }
        composeRule.edit("Cintura do perfil (cm)", "NaN")
        composeRule.closeKeyboard()
        composeRule.click("Salvar perfil")
        assertTypography("Informe uma cintura positiva e finita, como 80,5.", 14, 20)
        composeRule.click("Cancelar")
        composeRule.click("Descartar")
        composeRule.showSkirtCalculator()
        composeRule.click("Usar perfil")
        assertTypography("Usar perfil: Ana — 80 cm — 12/10/2026", 16, 24, FontWeight.Medium)
    }

    @Test
    fun pickerRows_inEveryScheme_standOutFromTheSheet() {
        // Given
        val storage = FakeProfileRepository(listOf(profile(1, "Ana"), profile(2, "Bia", 72.0)))
        val schemes = listOf(false to false, true to false, false to true, true to true)

        composeRule.launchApp(profiles = storage, frame = null)

        // When/Then
        schemes.forEach { (dark, dynamic) ->
            composeRule.showRoute(frame = profileFrame(dark = dark, dynamic = dynamic))
            composeRule.showSkirtCalculator()
            composeRule.click("Usar perfil")
            listOf("Ana — 80", "Bia — 72").forEach {
                val label = "Usar perfil: $it cm — 12/10/2026"
                val context = "$label dark=$dark dynamic=$dynamic"
                composeRule.onNodeWithText(label).performScrollTo()
                    .assertSurfaceStandsOut("$context row")
                composeRule.onNodeWithText(label, useUnmergedTree = true)
                    .assertReadableTextContrast(context)
            }
            composeRule.click("Cancelar seleção")
        }
    }

    @Test
    fun profileButtons_atDefaultTextSize_drawAtLeast48dpTall() {
        // Given
        composeRule.launchProfiles(FakeProfileRepository(listOf(profile(1, "Ana"))))

        // When/Then
        assertDrawnHeight("Perfis", "Ações: Ana")
        composeRule.click("Ações: Ana")
        composeRule.click("Excluir perfil")
        assertDrawnHeight("Confirmar exclusão", "Cancelar")
        composeRule.click("Cancelar")
        composeRule.click("Novo perfil")
        assertDrawnHeight("Salvar perfil", "Cancelar")
        composeRule.click("Cancelar")
        composeRule.showSkirtCalculator()
        composeRule.click("Usar perfil")
        assertDrawnHeight("Cancelar seleção", "Novo perfil")
    }

    @Test
    fun profileRow_openedWithKeyboard_focusesNoField() {
        // Given
        composeRule.launchProfiles(FakeProfileRepository(listOf(profile(1, "Ana"))))
        // Out of touch mode, the row joins keyboard focus traversal.
        keyboardMode.enter()
        composeRule.onNodeWithText("Ana").requestFocus().assertIsFocused()

        // When
        composeRule.pressKey(KeyEvent.KEYCODE_ENTER)

        // Then
        composeRule.onNodeWithText("Editar perfil de medidas").assertIsDisplayed()
        // The list leaves after the editor arrives; losing its focused row must not refocus a
        // field.
        composeRule.assertNoTextFieldFocused()
    }

    @Test
    fun largeText_inEveryScheme_keepsProfileRowsAndActionsReachable() {
        // Given
        val name = "Ana Maria da Silva — medidas conferidas para a saia"
        val notes = "Conferida antes do corte"
        val storage = FakeProfileRepository(listOf(profile(1, name, notes = notes)))
        val schemes = listOf(false to false, true to false, false to true, true to true)

        composeRule.launchApp(profiles = storage, frame = null)

        // When/Then
        schemes.forEach { (dark, dynamic) ->
            composeRule.showRoute(
                frame = profileFrame(fontScale = 2f, dark = dark, dynamic = dynamic),
            )
            composeRule.showProfiles()
            composeRule.onNodeWithText(name).performScrollTo().assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
            composeRule.onNodeWithText(notes).assertIsDisplayed()
            listOf(name, notes).forEach { label ->
                composeRule.onNodeWithText(label, useUnmergedTree = true)
                    .assertReadableTextContrast("$label dark=$dark dynamic=$dynamic")
            }
            composeRule.onNodeWithText("Ações: $name").performScrollTo().assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            composeRule.onNodeWithContentDescription("Novo perfil").assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            composeRule.click("Ações: $name")
            composeRule.click("Editar perfil")
            composeRule.onNodeWithText("Salvar alterações").performScrollTo().assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            composeRule.click("Cancelar")
        }
    }

    private fun sameNameWithNotes() = FakeProfileRepository(
        listOf(
            profile(1, "Ana", notes = "Medida com roupa leve"),
            profile(2, "Ana", notes = "Conferida antes do corte"),
        ),
    )

    private fun assertDrawnHeight(vararg labels: String) = labels.forEach {
        val node = composeRule.onNodeWithText(it)
        if (!node.isDisplayed()) node.performScrollTo()
        node.assertDrawnHeightIsAtLeast(48.dp)
    }

    private fun assertTypography(
        label: String,
        size: Int,
        height: Int,
        weight: FontWeight? = null,
    ) {
        val text = composeRule.onNodeWithText(label, useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            assertTrue(it(layouts))
        }
        val style = layouts.single().layoutInput.style
        assertEquals(size.sp, style.fontSize, "$label size")
        assertEquals(height.sp, style.lineHeight, "$label line height")
        if (weight != null) assertEquals(weight, style.fontWeight, "$label weight")
    }
}
