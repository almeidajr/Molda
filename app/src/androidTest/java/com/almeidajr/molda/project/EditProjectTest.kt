package com.almeidajr.molda.project

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import com.almeidajr.molda.IsolatedStorage
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.MoldaRoute
import com.almeidajr.molda.labelText
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.data.LocalProfileRepository
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.project.data.LocalProjectRepository
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.ui.ProjectsViewModel
import com.almeidajr.molda.screenTitle
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.showSkirtCalculator
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel
import com.almeidajr.molda.ui.theme.MoldaTheme
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test

/** Editing a saved project in the calculator: "Salvar", "Salvar como novo" and discarding. */
class EditProjectTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val storage = IsolatedStorage("edit-project-test.db")
    private val database = storage.database
    private val clock = Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC)
    private val projects = LocalProjectRepository(database.projects())

    @After
    fun removeIsolatedStorage() = storage.remove(composeRule.activityRule.scenario)

    @Test
    fun edit_loadsEveryInput_thenSalvarUpdatesTheProjectInPlace() {
        projects.storeProject(clock.instant())
        showRoute()
        openEditor()

        SAVED_INPUTS.forEach { (field, text) ->
            composeRule.onNodeWithText(field.labelText).performScrollTo().assert(hasText(text))
        }
        composeRule.onNodeWithText("Cintura copiada do perfil: Ana").performScrollTo()
            .assertIsDisplayed()
        editEase("4")
        click("Calcular")
        click("Salvar")

        // Storage runs off the main thread, outside Compose's idling.
        awaitTitle("Saia da formatura")
        composeRule.onNodeWithText("Projeto atualizado.").assertIsDisplayed()
        composeRule.onNode(hasText("Folga de cintura: 4,00", substring = true))
            .performScrollTo().assertIsDisplayed()
        val stored = storedProjects()
        assertEquals(1, stored.size)
        assertEquals(Length(4.0), stored.single().snapshot.draft.ease)
    }

    @Test
    fun saveAsNew_keepsTheOriginal_andShowsTheNewProject() {
        projects.storeProject(clock.instant())
        showRoute()
        openEditor()

        editEase("4")
        click("Calcular")
        click("Salvar como novo")
        composeRule.nameField()
            .performTextReplacement("Saia de verão")
        composeRule.onNode(hasText("Salvar") and hasAnyAncestor(isDialog())).performClick()

        awaitTitle("Saia de verão")
        val stored = storedProjects()
        assertEquals(listOf("Saia de verão", "Saia da formatura"), stored.map { it.name.text })
        assertEquals(Length(2.0), stored.last().snapshot.draft.ease)
        // The name field's keyboard may outlive the dialog; Back would only close it.
        closeSoftKeyboard()
        pressBack()
        composeRule.onNodeWithText("Saia da formatura").assertIsDisplayed()
    }

    @Test
    fun back_withUnsavedChanges_asksBeforeDiscarding() {
        projects.storeProject(clock.instant())
        showRoute()
        openEditor()

        editEase("4")
        pressBack()
        composeRule.onNodeWithText("Descartar alterações?").assertIsDisplayed()
        composeRule.onNodeWithText("As alterações deste projeto ainda não foram salvas.")
            .assertIsDisplayed()
        click("Continuar editando")
        composeRule.screenTitle("Editar Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Voltar").performClick()
        click("Descartar")

        composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
        assertEquals(Length(2.0), storedProjects().single().snapshot.draft.ease)
    }

    @Test
    fun back_withoutChanges_returnsToTheProject() {
        projects.storeProject(clock.instant())
        showRoute()
        openEditor()

        pressBack()

        composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText("Descartar alterações?").assertDoesNotExist()
    }

    @Test
    fun editing_leavesTheMoldesCalculatorDraftUntouched() {
        projects.storeProject(clock.instant())
        showRoute()
        composeRule.showSkirtCalculator()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).performTextReplacement("70")

        openEditor()
        editEase("4")
        composeRule.selectDestination("Moldes")

        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assert(hasText("70"))
        composeRule.onNodeWithText(SkirtDraftField.Ease.labelText).assert(hasText(""))
    }

    @Test
    fun useProfile_whileEditing_changesOnlyTheEditedDraft() {
        runBlocking {
            LocalProfileRepository(database.profiles()).create(
                ProfileMeasurements(ProfileName("Bia"), Length(95.0), LocalDate.of(2026, 10, 17)),
            ).getOrThrow()
        }
        projects.storeProject(clock.instant())
        showRoute()
        openEditor()

        click("Usar perfil")
        composeRule.onAllNodes(hasText("Usar perfil: Bia", substring = true))[0].performClick()

        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assert(hasText("95"))
        composeRule.showSkirtCalculator()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).assert(hasText(""))
    }

    @Test
    fun recreation_keepsTheEditedValues() {
        projects.storeProject(clock.instant())
        showRoute()
        openEditor()
        editEase("4")

        composeRule.activityRule.scenario.recreate()
        showRoute()

        composeRule.screenTitle("Editar Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText(SkirtDraftField.Ease.labelText).assert(hasText("4"))
    }

    private fun openEditor() {
        composeRule.selectDestination("Projetos")
        click("Saia da formatura")
        click("Editar")
        composeRule.screenTitle("Editar Saia da formatura").assertIsDisplayed()
    }

    private fun awaitTitle(title: String) {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText(title) and isHeading()).fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.screenTitle(title).assertIsDisplayed()
    }

    private fun editEase(text: String) {
        composeRule.onNodeWithText(SkirtDraftField.Ease.labelText).performScrollTo()
            .performTextReplacement(text)
        // The keyboard resizes the screen as it opens; let it settle before the next tap.
        closeSoftKeyboard()
    }

    private fun storedProjects(): List<Project> = runBlocking { projects.projects().first() }

    private fun showRoute() {
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                val profiles: ProfilesViewModel = viewModel(
                    key = "isolatedProfiles",
                    factory = viewModelFactory {
                        initializer {
                            ProfilesViewModel(
                                LocalProfileRepository(database.profiles()),
                                createSavedStateHandle(),
                                clock,
                            )
                        }
                    },
                )
                val projectsViewModel: ProjectsViewModel = viewModel(
                    key = "isolatedProjects",
                    factory = viewModelFactory {
                        initializer { ProjectsViewModel(projects, clock, createSavedStateHandle()) }
                    },
                )
                val calculator: SkirtCalculatorViewModel = viewModel()
                MoldaTheme {
                    MoldaRoute(
                        calculator = calculator,
                        profiles = profiles,
                        projects = projectsViewModel,
                    )
                }
            }
        }
    }

    private fun click(label: String) {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText(label) and isEnabled()).fetchSemanticsNodes()
                .isNotEmpty()
        }
        // Tap the label as a reader does: a small row's centre falls on its Ações button.
        val text = composeRule.onAllNodes(hasText(label), useUnmergedTree = true)[0]
        if (!text.isDisplayed()) text.performScrollTo()
        text.performClick()
    }

    private companion object {
        val SAVED_INPUTS = listOf(
            SkirtDraftField.Waist to "80",
            SkirtDraftField.Ease to "2",
            SkirtDraftField.FinishedLength to "60",
            SkirtDraftField.WaistSeamAllowance to "1",
            SkirtDraftField.HemAllowance to "2",
        )
    }
}
