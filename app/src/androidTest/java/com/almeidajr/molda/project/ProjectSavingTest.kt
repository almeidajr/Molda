package com.almeidajr.molda.project

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.almeidajr.molda.IsolatedStorage
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.MoldaRoute
import com.almeidajr.molda.labelText
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.measurement.MeasurementSource
import com.almeidajr.molda.profile.data.LocalProfileRepository
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.project.data.LocalProjectRepository
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.project.ui.ProjectsViewModel
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

/** Saving a calculated skirt as a project, from the result to the Projetos list. */
class ProjectSavingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val storage = IsolatedStorage("project-ui-test.db")
    private val database = storage.database
    private val clock = Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC)

    @After
    fun removeIsolatedStorage() = storage.remove(composeRule.activityRule.scenario)

    @Test
    fun calculateFromProfile_saveWithEditedName_listsItInProjetosWithItsSource() {
        val profiles = LocalProfileRepository(database.profiles())
        runBlocking {
            profiles.create(
                ProfileMeasurements(ProfileName("Ana"), Length(80.0), LocalDate.of(2026, 10, 12)),
            ).getOrThrow()
        }
        val projects = LocalProjectRepository(database.projects())
        showRoute(projects)
        composeRule.showSkirtCalculator()
        click("Usar perfil")
        composeRule.onAllNodes(hasText("Usar perfil: Ana", substring = true))[0].performClick()
        calculate(listOf("2", "60", "1", "2"))

        click("Salvar projeto")
        composeRule.nameField().assertTextContains("Saia godê · Ana · 18/10/2026")
        composeRule.nameField().performTextReplacement("   ")
        click("Salvar")
        composeRule.onNodeWithText("Informe um nome para o projeto.").assertIsDisplayed()
        composeRule.nameField().performTextReplacement("Saia da formatura")
        click("Salvar")

        composeRule.onNodeWithText("Projeto salvo em Projetos.").assertIsDisplayed()
        composeRule.onNodeWithText("Raios para o molde").performScrollTo().assertIsDisplayed()
        val stored = runBlocking { projects.projects().first() }.single()
        assertEquals(MeasurementSource("Ana", LocalDate.of(2026, 10, 12)), stored.snapshot.source)
        assertEquals(Length(80.0), stored.snapshot.draft.waist)
        composeRule.selectDestination("Projetos")
        composeRule.onNodeWithText("Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText("Saia godê · 18/10/2026").assertIsDisplayed()
        composeRule.onNode(hasText("Nenhum projeto salvo") and isHeading()).assertDoesNotExist()
    }

    @Test
    fun save_whenStorageFails_showsFailureAndKeepsTheResult() {
        showRoute(RefusingProjects())
        composeRule.showSkirtCalculator()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).performScrollTo()
            .performTextReplacement("70")
        calculate(listOf("0", "45", "0", "0"))

        click("Salvar projeto")
        composeRule.nameField().assertTextContains("Saia godê · 18/10/2026")
        click("Salvar")

        composeRule.onNodeWithText("Não foi possível salvar o projeto", substring = true)
            .assertIsDisplayed()
        composeRule.nameField().assertTextContains("Saia godê · 18/10/2026")
        click("Cancelar")
        composeRule.onNodeWithText("Raios para o molde").performScrollTo().assertIsDisplayed()
    }

    private fun showRoute(projectStorage: ProjectRepository) {
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
                val projects: ProjectsViewModel = viewModel(
                    key = "isolatedProjects",
                    factory = viewModelFactory {
                        initializer {
                            ProjectsViewModel(projectStorage, clock, createSavedStateHandle())
                        }
                    },
                )
                val calculator: SkirtCalculatorViewModel = viewModel()
                MoldaTheme {
                    MoldaRoute(calculator = calculator, profiles = profiles, projects = projects)
                }
            }
        }
    }

    /** Fills every field after the waist and calculates. */
    private fun calculate(values: List<String>) {
        SkirtDraftField.entries.drop(1).zip(values).forEach { (field, value) ->
            composeRule.onNodeWithText(
                field.labelText,
            ).performScrollTo().performTextReplacement(value)
        }
        click("Calcular")
        composeRule.onNodeWithText("Raios para o molde").assertIsDisplayed()
    }

    private fun click(label: String) {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(
                hasText(label) and isEnabled(),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        val node = composeRule.onNode(hasText(label) and isEnabled())
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
    }
}
