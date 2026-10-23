package com.almeidajr.molda.project

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.almeidajr.molda.awaitDialogFocus
import com.almeidajr.molda.clickInDialog
import com.almeidajr.molda.closeKeyboard
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.di.ClockModule
import com.almeidajr.molda.di.RepositoryModule
import com.almeidajr.molda.dialogTextField
import com.almeidajr.molda.labelText
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.measurement.MeasurementSource
import com.almeidajr.molda.pressBack
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.recreate
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

/** Saving a calculated skirt as a project, from the result to the Projetos list. */
@HiltAndroidTest
@UninstallModules(RepositoryModule::class, ClockModule::class)
class ProjectSavingTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    // Each test binds its own profiles and projects, then launches the app.
    @BindValue @JvmField
    var profileRepository: ProfileRepository = FakeProfileRepository()

    @BindValue @JvmField
    var projectRepository: ProjectRepository = FakeProjectRepository()

    @BindValue @JvmField
    val clock: Clock = October18Clock

    @Test
    fun save_fromProfileResultWithSuggestedName_storesItsSourceAndListsIt() {
        // Given
        val ana = MeasurementProfile(
            ProfileId(1),
            ProfileMeasurements(ProfileName("Ana"), Length(80.0), LocalDate.of(2026, 10, 12)),
        )
        val projects = FakeProjectRepository()
        profileRepository = FakeProfileRepository(listOf(ana))
        projectRepository = projects
        composeRule.activityRule.launch()
        composeRule.showRoute()
        composeRule.showSkirtCalculator()
        click("Usar perfil")
        composeRule.onAllNodes(hasText("Usar perfil: Ana", substring = true))[0].performClick()
        calculate(listOf("2", "60", "1", "2"))

        // When
        click("Salvar projeto")
        click("Salvar")

        // Then
        composeRule.onNodeWithText("Projeto salvo em Projetos.").assertIsDisplayed()
        val stored = projects.current.single()
        assertEquals(MeasurementSource("Ana", LocalDate.of(2026, 10, 12)), stored.snapshot.source)
        assertEquals(Length(80.0), stored.snapshot.draft.waist)
        composeRule.selectDestination("Projetos")
        composeRule.onNodeWithText("Saia godê · Ana · 18/10/2026").assertIsDisplayed()
    }

    @Test
    fun save_whenStorageFails_showsFailureAndKeepsTheResult() {
        // Given
        showCalculatedHandTypedSkirt(FakeProjectRepository().apply { failsCommands = true })

        // When
        click("Salvar projeto")
        click("Salvar")

        // Then
        composeRule.onNodeWithText("Não foi possível salvar o projeto", substring = true)
            .assertIsDisplayed()
        composeRule.dialogTextField().assertTextContains("Saia godê · 18/10/2026")
        click("Cancelar")
        composeRule.onNodeWithText("Raios para o molde").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun save_closedByBack_storesNothing() {
        // Given
        val projects = FakeProjectRepository()
        showCalculatedHandTypedSkirt(projects)
        // Back must reach the dialog, not close the keyboard.
        composeRule.closeKeyboard()
        click("Salvar projeto")
        composeRule.awaitDialogFocus()

        // When
        composeRule.pressBack()

        // Then
        composeRule.onNode(isDialog()).assertDoesNotExist()
        assertEquals(emptyList(), projects.current)
    }

    @Test
    fun save_withBlankName_explainsAndStoresNothing() {
        // Given
        val projects = FakeProjectRepository()
        showCalculatedHandTypedSkirt(projects)
        click("Salvar projeto")
        composeRule.dialogTextField().performTextReplacement("   ")

        // When
        composeRule.clickInDialog("Salvar")

        // Then
        composeRule.onNodeWithText("Informe um nome para o projeto.").assertIsDisplayed()
        assertEquals(emptyList(), projects.current)
    }

    @Test
    fun recreation_withSaveDialogOpen_keepsItsSnapshotAndTypedName() {
        // Given
        val projects = FakeProjectRepository()
        showCalculatedHandTypedSkirt(projects)
        click("Salvar projeto")
        composeRule.dialogTextField().performTextReplacement("Saia de verão")

        // When
        composeRule.recreate()
        composeRule.showRoute()
        composeRule.clickInDialog("Salvar")

        // Then
        composeRule.onNodeWithText("Projeto salvo em Projetos.").assertIsDisplayed()
        val stored = projects.current.single()
        assertEquals("Saia de verão", stored.name.text)
        assertEquals(Length(70.0), stored.snapshot.draft.waist)
    }

    /** Shows the calculator's result for a 70 cm waist typed by hand, stored in [projects]. */
    private fun showCalculatedHandTypedSkirt(projects: FakeProjectRepository) {
        projectRepository = projects
        composeRule.activityRule.launch()
        composeRule.showRoute()
        composeRule.showSkirtCalculator()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).performScrollTo()
            .performTextReplacement("70")
        calculate(listOf("0", "45", "0", "0"))
    }

    /** Fills every field after the waist and calculates. */
    private fun calculate(values: List<String>) {
        SkirtDraftField.entries.drop(1).zip(values).forEach { (field, value) ->
            composeRule.onNodeWithText(
                field.labelText,
            ).performScrollTo().performTextReplacement(value)
        }
        click("Calcular")
    }

    private fun click(label: String) {
        val node = composeRule.onNode(hasText(label) and isEnabled())
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
    }
}
