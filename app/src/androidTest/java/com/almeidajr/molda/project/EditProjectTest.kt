package com.almeidajr.molda.project

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.click
import com.almeidajr.molda.closeKeyboard
import com.almeidajr.molda.dialogTextField
import com.almeidajr.molda.labelText
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.pressBack
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.profile.fillProfile
import com.almeidajr.molda.recreate
import com.almeidajr.molda.screenTitle
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.showRoute
import com.almeidajr.molda.showSkirtCalculator
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.testing.FakeProfileRepository
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.October18Clock
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

/** Editing a saved project in the calculator: "Salvar", "Salvar como novo" and discarding. */
class EditProjectTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val projects = FakeProjectRepository(listOf(formaturaProject(October18Clock.instant())))

    @Test
    fun edit_savedProject_loadsEveryInputAndItsSource() {
        // Given
        showRoute()

        // When
        openEditor()

        // Then
        SAVED_INPUTS.forEach { (field, text) ->
            composeRule.onNodeWithText(field.labelText).performScrollTo().assert(hasText(text))
        }
        composeRule.onNodeWithText("Cintura copiada do perfil: Ana").performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun salvar_afterEditing_updatesTheProjectInPlace() {
        // Given
        showRoute()
        openEditor()
        editEase("4")
        composeRule.click("Calcular")

        // When
        composeRule.click("Salvar")

        // Then
        composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText("Projeto atualizado.").assertIsDisplayed()
        composeRule.onNode(hasText("Folga de cintura: 4,00", substring = true))
            .performScrollTo().assertIsDisplayed()
        assertEquals(Length(4.0), projects.current.single().snapshot.draft.ease)
    }

    @Test
    fun saveAsNew_afterEditing_keepsTheOriginalAndShowsTheNewProject() {
        // Given
        showRoute()
        openEditor()
        editEase("4")
        composeRule.click("Calcular")

        // When
        composeRule.click("Salvar como novo")
        composeRule.dialogTextField().performTextReplacement("Saia de verão")
        composeRule.onNode(hasText("Salvar") and hasAnyAncestor(isDialog())).performClick()

        // Then
        composeRule.screenTitle("Saia de verão").assertIsDisplayed()
        val stored = projects.current
        assertEquals(listOf("Saia de verão", "Saia da formatura"), stored.map { it.name.text })
        assertEquals(listOf(Length(4.0), Length(2.0)), stored.map { it.snapshot.draft.ease })
    }

    @Test
    fun back_withUnsavedChanges_asksBeforeDiscarding() {
        // Given
        showRoute()
        openEditor()
        editEase("4")

        // When
        composeRule.pressBack()

        // Then
        composeRule.onNodeWithText("Descartar alterações?").assertIsDisplayed()
        composeRule.onNodeWithText("As alterações deste projeto ainda não foram salvas.")
            .assertIsDisplayed()
    }

    @Test
    fun continueEditing_inDiscardDialog_keepsTheEditedValues() {
        // Given
        showRoute()
        openEditor()
        editEase("4")
        composeRule.onNodeWithContentDescription("Voltar").performClick()

        // When
        composeRule.click("Continuar editando")

        // Then
        composeRule.screenTitle("Editar Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText(SkirtDraftField.Ease.labelText).assert(hasText("4"))
    }

    @Test
    fun discard_inDiscardDialog_returnsToTheUnchangedProject() {
        // Given
        showRoute()
        openEditor()
        editEase("4")
        composeRule.onNodeWithContentDescription("Voltar").performClick()

        // When
        composeRule.click("Descartar")

        // Then
        composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
        assertEquals(Length(2.0), projects.current.single().snapshot.draft.ease)
    }

    @Test
    fun back_withoutChanges_returnsToTheProject() {
        // Given
        showRoute()
        openEditor()

        // When
        composeRule.pressBack()

        // Then
        composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText("Descartar alterações?").assertDoesNotExist()
    }

    @Test
    fun editing_leavesTheMoldesCalculatorDraftUntouched() {
        // Given
        showRoute()
        composeRule.showSkirtCalculator()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).performTextReplacement("70")
        composeRule.closeKeyboard()

        // When
        openEditor()
        editEase("4")
        composeRule.selectDestination("Moldes")

        // Then
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assert(hasText("70"))
        composeRule.onNodeWithText(SkirtDraftField.Ease.labelText).assert(hasText(""))
    }

    @Test
    fun useProfile_whileEditing_changesOnlyTheEditedDraft() {
        // Given
        showRoute(FakeProfileRepository(listOf(BIA)))
        openEditor()

        // When
        composeRule.click("Usar perfil")
        pickBia()

        // Then
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assert(hasText("95"))
        composeRule.showSkirtCalculator()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assert(hasText(""))
    }

    @Test
    fun createProfile_fromTheEditorsPicker_fillsTheEditedDraft() {
        // Given
        showRoute()
        openEditor()
        composeRule.click("Usar perfil")
        composeRule.click("Novo perfil")
        composeRule.fillProfile("Clara", waist = "95")

        // When
        saveProfile()

        // Then
        composeRule.screenTitle("Editar Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assert(hasText("95"))
        composeRule.showSkirtCalculator()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assert(hasText(""))
    }

    @Test
    fun createProfile_afterSwitchingTabAndBack_fillsTheEditedDraft() {
        // Given
        showRoute()
        openEditor()
        composeRule.click("Usar perfil")
        composeRule.click("Novo perfil")
        composeRule.showSkirtCalculator()
        composeRule.selectDestination("Projetos")
        composeRule.fillProfile("Clara", waist = "95")

        // When
        saveProfile()

        // Then
        composeRule.screenTitle("Editar Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assert(hasText("95"))
        composeRule.showSkirtCalculator()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assert(hasText(""))
    }

    @Test
    fun useProfile_inMoldes_leavesTheEditedDraftUntouched() {
        // Given
        showRoute(FakeProfileRepository(listOf(BIA)))
        openEditor()
        composeRule.showSkirtCalculator()

        // When
        composeRule.click("Usar perfil")
        pickBia()

        // Then
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assert(hasText("95"))
        composeRule.selectDestination("Projetos")
        composeRule.screenTitle("Editar Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assert(hasText("80"))
    }

    @Test
    fun useProfile_afterRecreationWithThePickerOpen_fillsTheEditedDraft() {
        // Given
        showRoute(FakeProfileRepository(listOf(BIA)))
        openEditor()
        composeRule.click("Usar perfil")

        // When
        composeRule.recreate()
        showRoute(FakeProfileRepository(listOf(BIA)))
        pickBia()

        // Then
        composeRule.screenTitle("Editar Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assert(hasText("95"))
        composeRule.showSkirtCalculator()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assert(hasText(""))
    }

    @Test
    fun recreation_keepsTheEditedValues() {
        // Given
        showRoute()
        openEditor()
        editEase("4")

        // When
        composeRule.recreate()
        showRoute()

        // Then
        composeRule.screenTitle("Editar Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText(SkirtDraftField.Ease.labelText).assert(hasText("4"))
    }

    private fun openEditor() {
        composeRule.selectDestination("Projetos")
        composeRule.clickProject("Saia da formatura")
        composeRule.click("Editar")
    }

    private fun editEase(text: String) {
        composeRule.onNodeWithText(SkirtDraftField.Ease.labelText).performScrollTo()
            .performTextReplacement(text)
        // The keyboard resizes the screen as it opens; let it settle before the next tap.
        composeRule.closeKeyboard()
    }

    private fun saveProfile() {
        composeRule.closeKeyboard()
        composeRule.click("Salvar perfil")
    }

    private fun pickBia() =
        composeRule.onAllNodes(hasText("Usar perfil: Bia", substring = true))[0].performClick()

    private fun showRoute(profiles: FakeProfileRepository = FakeProfileRepository()) =
        composeRule.showRoute(profiles, projects, October18Clock)

    private companion object {
        val BIA = MeasurementProfile(
            ProfileId(1),
            ProfileMeasurements(ProfileName("Bia"), Length(95.0), LocalDate.of(2026, 10, 17)),
        )

        val SAVED_INPUTS = listOf(
            SkirtDraftField.Waist to "80",
            SkirtDraftField.Ease to "2",
            SkirtDraftField.FinishedLength to "60",
            SkirtDraftField.WaistSeamAllowance to "1",
            SkirtDraftField.HemAllowance to "1,5",
        )
    }
}
