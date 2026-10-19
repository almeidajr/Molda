package com.almeidajr.molda.project

import androidx.activity.compose.setContent
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
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
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.almeidajr.molda.IsolatedStorage
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.MoldaRoute
import com.almeidajr.molda.labelText
import com.almeidajr.molda.profile.data.LocalProfileRepository
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
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** A project's whole life in the app: saved from a result, edited, copied and deleted. */
class ProjectJourneyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val storage = IsolatedStorage("project-journey-test.db")
    private val database = storage.database
    private val clock = Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC)
    private val projects = LocalProjectRepository(database.projects())

    @After
    fun removeIsolatedStorage() = storage.remove(composeRule.activityRule.scenario)

    @Test
    fun calculateSaveEditCopyAndDelete_keepsProjetosInStep() {
        showRoute()
        composeRule.showSkirtCalculator()
        listOf("80", "2", "60", "1", "2").zip(SkirtDraftField.entries).forEach { (text, field) ->
            editField(field, text)
        }
        click("Calcular")
        click("Salvar projeto")
        composeRule.nameField().performTextReplacement("Saia da formatura")
        confirmDialog("Salvar")
        await(hasText("Projeto salvo em Projetos."))

        composeRule.selectDestination("Projetos")
        composeRule.onNodeWithText("Saia godê · 18/10/2026").assertIsDisplayed()
        click("Saia da formatura")
        awaitTitle("Saia da formatura")

        click("Editar")
        editField(SkirtDraftField.Ease, "4")
        click("Calcular")
        click("Salvar")
        awaitTitle("Saia da formatura")
        composeRule.onNodeWithText("Projeto atualizado.").assertIsDisplayed()
        assertEquals(listOf("Saia da formatura" to 4.0), storedNamesAndEase())

        click("Editar")
        editField(SkirtDraftField.Ease, "5")
        click("Calcular")
        click("Salvar como novo")
        composeRule.nameField().performTextReplacement("Saia de verão")
        confirmDialog("Salvar")
        awaitTitle("Saia de verão")
        assertEquals(
            listOf("Saia de verão" to 5.0, "Saia da formatura" to 4.0),
            storedNamesAndEase(),
        )

        click("Ações: Saia de verão")
        click("Excluir projeto")
        composeRule.onNodeWithText("Excluir projeto Saia de verão?").assertIsDisplayed()
        confirmDialog("Confirmar exclusão")
        await(hasText("Projeto excluído deste aparelho."))
        composeRule.onNodeWithText("Saia de verão").assertDoesNotExist()
        composeRule.onNodeWithText("Saia da formatura").assertIsDisplayed()
        assertEquals(listOf("Saia da formatura" to 4.0), storedNamesAndEase())
    }

    private fun storedNamesAndEase(): List<Pair<String, Double>> = runBlocking {
        projects.projects().first().map(Project::namesAndEase)
    }

    private fun editField(field: SkirtDraftField, text: String) {
        composeRule.onNodeWithText(field.labelText).performScrollTo().performTextReplacement(text)
        // The keyboard resizes the screen as it opens; let it settle before the next tap.
        closeSoftKeyboard()
    }

    private fun confirmDialog(label: String) {
        composeRule.onNode(hasText(label) and hasAnyAncestor(isDialog())).performClick()
        // The name field's keyboard may outlive the dialog and cover the next target.
        closeSoftKeyboard()
    }

    private fun awaitTitle(title: String) {
        await(hasText(title) and isHeading())
        composeRule.screenTitle(title).assertIsDisplayed()
    }

    // Storage runs off the main thread, outside Compose's idling.
    private fun await(matcher: SemanticsMatcher) = composeRule.waitUntil(5_000) {
        composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
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
}

private fun Project.namesAndEase(): Pair<String, Double> =
    name.text to snapshot.draft.ease.centimetres
