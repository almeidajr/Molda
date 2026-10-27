package com.almeidajr.molda.project

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.click
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.dialogTextField
import com.almeidajr.molda.domain.NoLongerExistsException
import com.almeidajr.molda.launchApp
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.pressBack
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.October18Clock
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Renaming and deleting a project from the Projetos list. */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ProjectManagementTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Test
    fun rename_withNewName_updatesTheList() {
        // Given
        showProjetos(FakeProjectRepository(listOf(saiaAntiga)))
        composeRule.click("Ações: Saia antiga")
        composeRule.click("Renomear projeto")

        // When
        composeRule.dialogTextField().performTextReplacement("Saia da formatura")
        composeRule.click("Renomear")

        // Then
        composeRule.onNodeWithText("Projeto renomeado.").assertIsDisplayed()
        composeRule.onNodeWithText("Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText("Saia antiga").assertDoesNotExist()
    }

    @Test
    fun rename_ofProjectDeletedMeanwhile_closesTheDialogWithTheMessage() {
        // Given
        val projects = FakeProjectRepository(listOf(saiaAntiga, saiaNova))
        showProjetos(projects)
        composeRule.click("Ações: Saia antiga")
        composeRule.click("Renomear projeto")
        composeRule.dialogTextField().performTextReplacement("Saia da formatura")

        // When: deleted elsewhere while the dialog is open.
        composeRule.runOnIdle { projects.forget(saiaAntiga.id) }

        // Then
        composeRule.onNodeWithText("Este projeto foi excluído.").assertIsDisplayed()
        composeRule.onNode(isDialog()).assertDoesNotExist()
        composeRule.onNodeWithText("Não foi possível renomear o projeto", substring = true)
            .assertDoesNotExist()
        assertEquals(listOf(saiaNova), projects.current)
    }

    @Test
    fun rename_foundDeletedByTheStorage_closesTheDialogWithTheMessage() {
        // Given
        val projects = FakeProjectRepository(listOf(saiaAntiga))
            .apply { commandFailure = NoLongerExistsException("Deleted meanwhile") }
        showProjetos(projects)
        composeRule.click("Ações: Saia antiga")
        composeRule.click("Renomear projeto")
        composeRule.dialogTextField().performTextReplacement("Saia da formatura")

        // When
        composeRule.click("Renomear")

        // Then
        composeRule.onNodeWithText("Este projeto foi excluído.").assertIsDisplayed()
        composeRule.onNode(isDialog()).assertDoesNotExist()
        composeRule.onNodeWithText("Não foi possível renomear o projeto", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun rename_closedByBack_keepsTheName() {
        // Given
        val projects = FakeProjectRepository(listOf(saiaAntiga))
        showProjetos(projects)
        composeRule.click("Ações: Saia antiga")
        composeRule.click("Renomear projeto")

        // When
        composeRule.pressBack()

        // Then
        composeRule.onNodeWithText("Renomear").assertDoesNotExist()
        assertEquals(listOf(saiaAntiga), projects.current)
    }

    @Test
    fun rename_whenStorageFails_showsTheFailure() {
        // Given
        showProjetos(FakeProjectRepository(listOf(saiaAntiga)).apply { failsCommands = true })
        composeRule.click("Ações: Saia antiga")
        composeRule.click("Renomear projeto")

        // When
        composeRule.dialogTextField().performTextReplacement("Saia da formatura")
        composeRule.click("Renomear")

        // Then
        composeRule.onNodeWithText("Não foi possível renomear o projeto", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun rename_whenTheProjectFailsToLoad_showsTheFailureInADialogThatCancels() {
        // Given
        val projects = FakeProjectRepository(listOf(saiaAntiga))
        showProjetos(projects)
        // Projetos composes its list, which starts loading, only after the destination's click.
        composeRule.runOnIdle { projects.failsLoad = true }
        composeRule.click("Ações: Saia antiga")

        // When
        composeRule.click("Renomear projeto")

        // Then
        composeRule.onNode(isDialog()).assertExists()
        composeRule.onNodeWithText("Não foi possível carregar os projetos", substring = true)
            .assertIsDisplayed()
        composeRule.click("Cancelar")
        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun cancel_duringTheRename_keepsTheDialog() {
        // Given
        val gate = CompletableDeferred<Unit>()
        val projects = FakeProjectRepository(listOf(saiaAntiga)).apply { commandGate = gate }
        showProjetos(projects)
        composeRule.click("Ações: Saia antiga")
        composeRule.click("Renomear projeto")
        composeRule.dialogTextField().performTextReplacement("Saia da formatura")
        composeRule.click("Renomear")

        // When
        composeRule.onNodeWithText("Cancelar").performClick()

        // Then
        composeRule.onNode(isDialog()).assertExists()
        composeRule.onNodeWithText("Cancelar").assertIsNotEnabled()
        gate.complete(Unit)
        composeRule.onNodeWithText("Projeto renomeado.").assertIsDisplayed()
    }

    @Test
    fun delete_canceled_keepsTheProject() {
        // Given
        val projects = FakeProjectRepository(listOf(saiaAntiga))
        showProjetos(projects)
        composeRule.click("Ações: Saia antiga")
        composeRule.click("Excluir projeto")

        // When
        composeRule.click("Cancelar")

        // Then
        composeRule.onNodeWithText("Excluir projeto Saia antiga?").assertDoesNotExist()
        assertEquals(listOf(saiaAntiga), projects.current)
    }

    @Test
    fun delete_ofProjectDeletedMeanwhile_closesTheDialogWithTheMessage() {
        // Given
        val projects = FakeProjectRepository(listOf(saiaAntiga, saiaNova))
        showProjetos(projects)
        composeRule.click("Ações: Saia antiga")
        composeRule.click("Excluir projeto")

        // When: deleted elsewhere while the dialog is open.
        composeRule.runOnIdle { projects.forget(saiaAntiga.id) }

        // Then
        composeRule.onNodeWithText("Este projeto foi excluído.").assertIsDisplayed()
        composeRule.onNode(isDialog()).assertDoesNotExist()
        composeRule.onNodeWithText("Projeto excluído deste aparelho.").assertDoesNotExist()
        assertEquals(listOf(saiaNova), projects.current)
    }

    /** Launches the app on [projects] and selects Projetos. */
    private fun showProjetos(projects: ProjectRepository) {
        composeRule.launchApp(projects = projects, clock = October18Clock)
        composeRule.selectDestination("Projetos")
    }
}

private val saiaAntiga = Project(
    ProjectId(1),
    ProjectName("Saia antiga"),
    SkirtSnapshot(
        SkirtDraft(Length(80.0), Length(2.0), Length(60.0), Length(1.0), Length(2.0)),
        SkirtRadii(Length(13.05), Length(73.05), Length(12.05), Length(75.05)),
        source = null,
    ),
    createdAt = Instant.parse("2026-10-01T12:00:00Z"),
    updatedAt = Instant.parse("2026-10-01T12:00:00Z"),
)

private val saiaNova = saiaAntiga.copy(id = ProjectId(2), name = ProjectName("Saia nova"))
