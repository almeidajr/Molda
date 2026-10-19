package com.almeidajr.molda.project

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.MoldaRoute
import com.almeidajr.molda.data.room.openMoldaDatabase
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.data.LocalProfileRepository
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.project.data.LocalProjectRepository
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.project.ui.ProjectsViewModel
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel
import com.almeidajr.molda.ui.theme.MoldaTheme
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Renaming and deleting a project from the Projetos list. */
class ProjectManagementTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "project-management-test.db"
    private val database =
        openMoldaDatabase(context.also { it.deleteDatabase(databaseName) }, databaseName)
    private val clock = Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC)

    @After
    fun removeIsolatedStorage() {
        composeRule.activityRule.scenario.close()
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun rename_refusesBlankName_thenUpdatesTheList() {
        val projects = LocalProjectRepository(database.projects())
        runBlocking { projects.create(ProjectName("Saia antiga"), snapshot, clock.instant()) }
        showProjetos(projects)

        click("Ações: Saia antiga")
        click("Renomear projeto")
        nameField().performTextReplacement("   ")
        click("Renomear")
        composeRule.onNodeWithText("Informe um nome para o projeto.").assertIsDisplayed()
        nameField().performTextReplacement("Saia da formatura")
        click("Renomear")

        composeRule.onNodeWithText("Projeto renomeado.").assertIsDisplayed()
        composeRule.onNodeWithText("Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText("Saia antiga").assertDoesNotExist()
    }

    @Test
    fun delete_asksForConfirmation_thenRemovesTheProject() {
        val projects = LocalProjectRepository(database.projects())
        runBlocking { projects.create(ProjectName("Saia antiga"), snapshot, clock.instant()) }
        showProjetos(projects)

        click("Ações: Saia antiga")
        click("Excluir projeto")
        composeRule.onNodeWithText("Excluir projeto Saia antiga?").assertIsDisplayed()
        click("Cancelar")
        composeRule.onNodeWithText("Excluir projeto Saia antiga?").assertDoesNotExist()
        assertEquals(1, runBlocking { projects.projects().first() }.size)
        click("Ações: Saia antiga")
        click("Excluir projeto")
        click("Confirmar exclusão")

        composeRule.onNodeWithText("Projeto excluído deste aparelho.").assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum projeto salvo").assertIsDisplayed()
        assertEquals(emptyList<Project>(), runBlocking { projects.projects().first() })
    }

    @Test
    fun delete_whenStorageFails_showsFailureAndKeepsTheProject() {
        showProjetos(RefusingProjects)

        click("Ações: Saia antiga")
        click("Excluir projeto")
        click("Confirmar exclusão")

        composeRule.onNodeWithText("Não foi possível excluir o projeto", substring = true)
            .assertIsDisplayed()
        click("Cancelar")
        composeRule.onNodeWithText("Saia antiga").assertIsDisplayed()
    }

    private fun showProjetos(projectStorage: ProjectRepository) {
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
                        initializer { ProjectsViewModel(projectStorage, clock) }
                    },
                )
                val calculator: SkirtCalculatorViewModel = viewModel()
                MoldaTheme {
                    MoldaRoute(calculator = calculator, profiles = profiles, projects = projects)
                }
            }
        }
        composeRule.selectDestination("Projetos")
    }

    private fun nameField() = composeRule.onNode(hasSetTextAction() and hasAnyAncestor(isDialog()))

    private fun click(label: String) {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText(label) and isEnabled()).fetchSemanticsNodes()
                .isNotEmpty()
        }
        val node = composeRule.onNode(hasText(label) and isEnabled())
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
    }
}

private val snapshot = SkirtSnapshot(
    SkirtDraft(Length(80.0), Length(2.0), Length(60.0), Length(1.0), Length(2.0)),
    SkirtRadii(Length(13.05), Length(73.05), Length(12.05), Length(75.05)),
    source = null,
)

/** Storage that lists one project and refuses every change to it. */
private object RefusingProjects : ProjectRepository {
    private val stored = Project(
        ProjectId(1),
        ProjectName("Saia antiga"),
        snapshot,
        Instant.parse("2026-10-01T12:00:00Z"),
        Instant.parse("2026-10-01T12:00:00Z"),
    )

    override fun projects(): Flow<List<Project>> = flowOf(listOf(stored))

    override suspend fun create(name: ProjectName, snapshot: SkirtSnapshot, savedAt: Instant) =
        Result.failure<Project>(IllegalStateException("Controlled storage failure"))

    override suspend fun rename(id: ProjectId, name: ProjectName, renamedAt: Instant) =
        Result.failure<Unit>(IllegalStateException("Controlled storage failure"))

    override suspend fun delete(id: ProjectId) =
        Result.failure<Unit>(IllegalStateException("Controlled storage failure"))
}
