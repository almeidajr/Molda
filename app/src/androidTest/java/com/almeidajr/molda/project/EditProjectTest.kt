package com.almeidajr.molda.project

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.almeidajr.molda.awaitDialogFocus
import com.almeidajr.molda.click
import com.almeidajr.molda.clickInDialog
import com.almeidajr.molda.closeKeyboard
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.di.ClockModule
import com.almeidajr.molda.di.RepositoryModule
import com.almeidajr.molda.dialogTextField
import com.almeidajr.molda.labelText
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.pressBack
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.profile.fillProfile
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.recreate
import com.almeidajr.molda.screenTitle
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.showRoute
import com.almeidajr.molda.showSkirtCalculator
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.testing.FakeProfileRepository
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.October18Clock
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.time.Clock
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

/** Editing a saved project in the calculator: "Salvar", "Salvar como novo" and discarding. */
@HiltAndroidTest
@UninstallModules(RepositoryModule::class, ClockModule::class)
class EditProjectTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    private val projects = FakeProjectRepository(listOf(formaturaProject(October18Clock.instant())))

    // Each test binds its own profiles, then launches the app on them.
    @BindValue @JvmField
    var profileRepository: ProfileRepository = FakeProfileRepository()

    @BindValue @JvmField
    var projectRepository: ProjectRepository = projects

    @BindValue @JvmField
    val clock: Clock = October18Clock

    @Test
    fun edit_savedProject_loadsEveryInputAndItsSource() {
        // Given
        launch()

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
        launch()
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
        launch()
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
    fun saveAsNew_closedByBack_storesNothing() {
        // Given: reached by taps only, so no keyboard's Back callback can take the Back press
        launch()
        openEditor()
        composeRule.click("Calcular")
        composeRule.click("Salvar como novo")
        composeRule.awaitDialogFocus()

        // When
        composeRule.pressBack()

        // Then
        composeRule.onNode(isDialog()).assertDoesNotExist()
        assertEquals(listOf("Saia da formatura"), projects.current.map { it.name.text })
    }

    @Test
    fun back_withUnsavedChanges_asksBeforeDiscarding() {
        // Given
        launch()
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
        launch()
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
        launch()
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
    fun discard_inDiscardDialogAfterRecreation_returnsToTheUnchangedProject() {
        // Given
        openDiscardDialogAfterRecreation()

        // When
        composeRule.clickInDialog("Descartar")

        // Then
        composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
        assertEquals(Length(2.0), projects.current.single().snapshot.draft.ease)
    }

    @Test
    fun continueEditing_inDiscardDialogAfterRecreation_keepsTheEditedValues() {
        // Given
        openDiscardDialogAfterRecreation()

        // When
        composeRule.clickInDialog("Continuar editando")

        // Then
        composeRule.screenTitle("Editar Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText(SkirtDraftField.Ease.labelText).assert(hasText("4"))
    }

    @Test
    fun back_withoutChanges_returnsToTheProject() {
        // Given
        launch()
        openEditor()

        // When
        composeRule.pressBack()

        // Then
        composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText("Descartar alterações?").assertDoesNotExist()
    }

    @Test
    fun edit_anotherProjectAfterClosingAnEditor_loadsThatProjectsDraft() {
        // Given
        val formatura = formaturaProject(October18Clock.instant())
        val verao = formatura.copy(
            id = ProjectId(2),
            name = ProjectName("Saia de verão"),
            snapshot = formatura.snapshot.copy(
                draft = formatura.snapshot.draft.copy(waist = Length(90.0)),
            ),
        )
        projectRepository = FakeProjectRepository(listOf(formatura, verao))
        launch()
        openEditor()
        composeRule.pressBack()
        composeRule.pressBack()

        // When
        composeRule.clickProject("Saia de verão")
        composeRule.click("Editar")

        // Then
        composeRule.screenTitle("Editar Saia de verão").assertIsDisplayed()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assert(hasText("90"))
    }

    @Test
    fun editing_leavesTheMoldesCalculatorDraftUntouched() {
        // Given
        launch()
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
        launch(FakeProfileRepository(listOf(BIA)))
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
        launch()
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
        launch()
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
        launch(FakeProfileRepository(listOf(BIA)))
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
        launch(FakeProfileRepository(listOf(BIA)))
        openEditor()
        composeRule.click("Usar perfil")

        // When
        composeRule.recreate()
        composeRule.showRoute()
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
        launch()
        openEditor()
        editEase("4")

        // When
        composeRule.recreate()
        composeRule.showRoute()

        // Then
        composeRule.screenTitle("Editar Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText(SkirtDraftField.Ease.labelText).assert(hasText("4"))
    }

    private fun openEditor() {
        composeRule.selectDestination("Projetos")
        composeRule.clickProject("Saia da formatura")
        composeRule.click("Editar")
    }

    private fun openDiscardDialogAfterRecreation() {
        launch()
        openEditor()
        editEase("4")
        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.recreate()
        composeRule.showRoute()
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

    /** Binds [profiles], then launches the app on them. */
    private fun launch(profiles: ProfileRepository = FakeProfileRepository()) {
        profileRepository = profiles
        composeRule.activityRule.launch()
        composeRule.showRoute()
    }

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
