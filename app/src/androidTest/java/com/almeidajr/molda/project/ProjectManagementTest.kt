package com.almeidajr.molda.project

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.project.domain.PatternDraftKind
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.testing.FakeProfileRepository
import com.almeidajr.molda.testing.FakeProjectRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

/** Renaming and deleting a project from the Projetos list. */
class ProjectManagementTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val clock = Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun rename_refusesBlankName_thenUpdatesTheList() {
        val projects = FakeProjectRepository(listOf(saiaAntiga))
        showProjetos(projects)

        click("Ações: Saia antiga")
        click("Renomear projeto")
        composeRule.nameField().performTextReplacement("   ")
        click("Renomear")
        composeRule.onNodeWithText("Informe um nome para o projeto.").assertIsDisplayed()
        composeRule.nameField().performTextReplacement("Saia da formatura")
        click("Renomear")

        composeRule.onNodeWithText("Projeto renomeado.").assertIsDisplayed()
        composeRule.onNodeWithText("Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText("Saia antiga").assertDoesNotExist()
    }

    @Test
    fun delete_asksForConfirmation_thenRemovesTheProject() {
        val projects = FakeProjectRepository(listOf(saiaAntiga))
        showProjetos(projects)

        click("Ações: Saia antiga")
        click("Excluir projeto")
        composeRule.onNodeWithText("Excluir projeto Saia antiga?").assertIsDisplayed()
        click("Cancelar")
        composeRule.onNodeWithText("Excluir projeto Saia antiga?").assertDoesNotExist()
        assertEquals(1, projects.current.size)
        click("Ações: Saia antiga")
        click("Excluir projeto")
        click("Confirmar exclusão")

        composeRule.onNodeWithText("Projeto excluído deste aparelho.").assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum projeto salvo").assertIsDisplayed()
        assertEquals(emptyList<Project>(), projects.current)
    }

    @Test
    fun delete_whenStorageFails_showsFailureAndKeepsTheProject() {
        showProjetos(FakeProjectRepository(listOf(saiaAntiga)).apply { failsCommands = true })

        click("Ações: Saia antiga")
        click("Excluir projeto")
        click("Confirmar exclusão")

        composeRule.onNodeWithText("Não foi possível excluir o projeto", substring = true)
            .assertIsDisplayed()
        click("Cancelar")
        composeRule.onNodeWithText("Saia antiga").assertIsDisplayed()
    }

    private fun showProjetos(projects: ProjectRepository) {
        composeRule.showRoute(FakeProfileRepository(), projects, clock)
        composeRule.selectDestination("Projetos")
    }

    private fun click(label: String) {
        val node = composeRule.onNode(hasText(label) and isEnabled())
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
    }
}

private val saiaAntiga = Project(
    ProjectId(1),
    ProjectName("Saia antiga"),
    PatternDraftKind.FullCircleSkirt,
    SkirtSnapshot(
        SkirtDraft(Length(80.0), Length(2.0), Length(60.0), Length(1.0), Length(2.0)),
        SkirtRadii(Length(13.05), Length(73.05), Length(12.05), Length(75.05)),
        source = null,
    ),
    createdAt = Instant.parse("2026-10-01T12:00:00Z"),
    updatedAt = Instant.parse("2026-10-01T12:00:00Z"),
)
