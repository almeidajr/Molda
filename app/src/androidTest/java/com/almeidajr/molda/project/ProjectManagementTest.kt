package com.almeidajr.molda.project

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
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
import com.almeidajr.molda.recreate
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

/** Renaming and deleting a project from the Projetos list. */
@HiltAndroidTest
class ProjectManagementTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Test
    fun rename_withBlankName_showsNameErrorAndKeepsTheName() {
        // Given
        val projects = FakeProjectRepository(listOf(saiaAntiga))
        showProjetos(projects)
        click("Ações: Saia antiga")
        click("Renomear projeto")

        // When
        composeRule.dialogTextField().performTextReplacement("   ")
        click("Renomear")

        // Then
        composeRule.onNodeWithText("Informe um nome para o projeto.").assertIsDisplayed()
        assertEquals(listOf(saiaAntiga), projects.current)
    }

    @Test
    fun rename_withNewName_updatesTheList() {
        // Given
        showProjetos(FakeProjectRepository(listOf(saiaAntiga)))
        click("Ações: Saia antiga")
        click("Renomear projeto")

        // When
        composeRule.dialogTextField().performTextReplacement("Saia da formatura")
        click("Renomear")

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
        click("Ações: Saia antiga")
        click("Renomear projeto")
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
        click("Ações: Saia antiga")
        click("Renomear projeto")
        composeRule.dialogTextField().performTextReplacement("Saia da formatura")

        // When
        click("Renomear")

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
        click("Ações: Saia antiga")
        click("Renomear projeto")

        // When
        composeRule.pressBack()

        // Then
        composeRule.onNodeWithText("Renomear").assertDoesNotExist()
        assertEquals(listOf(saiaAntiga), projects.current)
    }

    @Test
    fun rename_whenStorageFails_showsFailureAndKeepsTheName() {
        // Given
        val projects = FakeProjectRepository(listOf(saiaAntiga)).apply { failsCommands = true }
        showProjetos(projects)
        click("Ações: Saia antiga")
        click("Renomear projeto")

        // When
        composeRule.dialogTextField().performTextReplacement("Saia da formatura")
        click("Renomear")

        // Then
        composeRule.onNodeWithText("Não foi possível renomear o projeto", substring = true)
            .assertIsDisplayed()
        assertEquals(listOf(saiaAntiga), projects.current)
    }

    @Test
    fun rename_whenTheProjectFailsToLoad_showsTheFailureInADialogThatCancels() {
        // Given
        val projects = FakeProjectRepository(listOf(saiaAntiga))
        showProjetos(projects)
        // Projetos composes its list, which starts loading, only after the destination's click.
        composeRule.runOnIdle { projects.failsLoad = true }
        click("Ações: Saia antiga")

        // When
        click("Renomear projeto")

        // Then
        composeRule.onNode(isDialog()).assertExists()
        composeRule.onNodeWithText("Não foi possível carregar os projetos", substring = true)
            .assertIsDisplayed()
        click("Cancelar")
        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun rename_afterRecreation_keepsTheProjectAndTheTypedName() {
        // Given
        val projects = FakeProjectRepository(listOf(saiaAntiga, saiaNova))
        showProjetos(projects)
        click("Ações: Saia antiga")
        click("Renomear projeto")
        composeRule.dialogTextField().performTextReplacement("Saia da formatura")

        // When
        composeRule.recreate()
        click("Renomear")

        // Then
        composeRule.onNodeWithText("Projeto renomeado.").assertIsDisplayed()
        assertEquals(
            listOf(ProjectName("Saia da formatura"), ProjectName("Saia nova")),
            projects.current.map { it.name },
        )
    }

    @Test
    fun cancel_duringTheRename_keepsTheDialog() {
        // Given
        val gate = CompletableDeferred<Unit>()
        val projects = FakeProjectRepository(listOf(saiaAntiga)).apply { commandGate = gate }
        showProjetos(projects)
        click("Ações: Saia antiga")
        click("Renomear projeto")
        composeRule.dialogTextField().performTextReplacement("Saia da formatura")
        click("Renomear")

        // When
        composeRule.onNodeWithText("Cancelar").performClick()

        // Then
        composeRule.onNode(isDialog()).assertExists()
        composeRule.onNodeWithText("Cancelar").assertIsNotEnabled()
        gate.complete(Unit)
        composeRule.onNodeWithText("Projeto renomeado.").assertIsDisplayed()
    }

    @Test
    fun recreation_duringTheRename_renamesAndClosesTheDialog() {
        // Given
        val gate = CompletableDeferred<Unit>()
        val projects = FakeProjectRepository(listOf(saiaAntiga)).apply { commandGate = gate }
        showProjetos(projects)
        click("Ações: Saia antiga")
        click("Renomear projeto")
        composeRule.dialogTextField().performTextReplacement("Saia da formatura")
        click("Renomear")
        composeRule.recreate()

        // When
        gate.complete(Unit)

        // Then
        composeRule.onNodeWithText("Projeto renomeado.").assertIsDisplayed()
        composeRule.onNode(isDialog()).assertDoesNotExist()
        assertEquals(listOf(ProjectName("Saia da formatura")), projects.current.map { it.name })
    }

    @Test
    fun recreation_duringTheDeletion_deletesAndClosesTheDialog() {
        // Given
        val gate = CompletableDeferred<Unit>()
        val projects = FakeProjectRepository(listOf(saiaAntiga)).apply { commandGate = gate }
        showProjetos(projects)
        click("Ações: Saia antiga")
        click("Excluir projeto")
        click("Confirmar exclusão")
        composeRule.recreate()

        // When
        gate.complete(Unit)

        // Then
        composeRule.onNodeWithText("Projeto excluído deste aparelho.").assertIsDisplayed()
        composeRule.onNode(isDialog()).assertDoesNotExist()
        assertEquals(emptyList(), projects.current)
    }

    @Test
    fun delete_cancelled_keepsTheProject() {
        // Given
        val projects = FakeProjectRepository(listOf(saiaAntiga))
        showProjetos(projects)
        click("Ações: Saia antiga")
        click("Excluir projeto")

        // When
        click("Cancelar")

        // Then
        composeRule.onNodeWithText("Excluir projeto Saia antiga?").assertDoesNotExist()
        assertEquals(listOf(saiaAntiga), projects.current)
    }

    @Test
    fun delete_closedByBack_keepsTheProject() {
        // Given
        val projects = FakeProjectRepository(listOf(saiaAntiga))
        showProjetos(projects)
        click("Ações: Saia antiga")
        click("Excluir projeto")

        // When
        composeRule.pressBack()

        // Then
        composeRule.onNodeWithText("Excluir projeto Saia antiga?").assertDoesNotExist()
        assertEquals(listOf(saiaAntiga), projects.current)
    }

    @Test
    fun delete_afterRecreation_keepsTheProjectToDelete() {
        // Given
        val projects = FakeProjectRepository(listOf(saiaAntiga, saiaNova))
        showProjetos(projects)
        click("Ações: Saia antiga")
        click("Excluir projeto")

        // When
        composeRule.recreate()
        click("Confirmar exclusão")

        // Then
        composeRule.onNodeWithText("Projeto excluído deste aparelho.").assertIsDisplayed()
        assertEquals(listOf(saiaNova), projects.current)
    }

    @Test
    fun delete_confirmed_removesTheProject() {
        // Given
        val projects = FakeProjectRepository(listOf(saiaAntiga))
        showProjetos(projects)
        click("Ações: Saia antiga")
        click("Excluir projeto")

        // When
        click("Confirmar exclusão")

        // Then
        composeRule.onNodeWithText("Projeto excluído deste aparelho.").assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum projeto salvo").assertIsDisplayed()
        assertEquals(emptyList(), projects.current)
    }

    @Test
    fun delete_ofProjectDeletedMeanwhile_closesTheDialogWithTheMessage() {
        // Given
        val projects = FakeProjectRepository(listOf(saiaAntiga, saiaNova))
        showProjetos(projects)
        click("Ações: Saia antiga")
        click("Excluir projeto")

        // When: deleted elsewhere while the dialog is open.
        composeRule.runOnIdle { projects.forget(saiaAntiga.id) }

        // Then
        composeRule.onNodeWithText("Este projeto foi excluído.").assertIsDisplayed()
        composeRule.onNode(isDialog()).assertDoesNotExist()
        composeRule.onNodeWithText("Projeto excluído deste aparelho.").assertDoesNotExist()
        assertEquals(listOf(saiaNova), projects.current)
    }

    @Test
    fun delete_foundDeletedByTheStorage_closesTheDialogWithTheMessage() {
        // Given
        val projects = FakeProjectRepository(listOf(saiaAntiga))
            .apply { commandFailure = NoLongerExistsException("Deleted meanwhile") }
        showProjetos(projects)
        click("Ações: Saia antiga")
        click("Excluir projeto")

        // When
        click("Confirmar exclusão")

        // Then
        composeRule.onNodeWithText("Este projeto foi excluído.").assertIsDisplayed()
        composeRule.onNode(isDialog()).assertDoesNotExist()
        composeRule.onNodeWithText("Não foi possível excluir o projeto", substring = true)
            .assertDoesNotExist()
        composeRule.onNodeWithText("Projeto excluído deste aparelho.").assertDoesNotExist()
    }

    @Test
    fun delete_whenStorageFails_showsFailureAndKeepsTheProject() {
        // Given
        showProjetos(FakeProjectRepository(listOf(saiaAntiga)).apply { failsCommands = true })
        click("Ações: Saia antiga")
        click("Excluir projeto")

        // When
        click("Confirmar exclusão")

        // Then
        composeRule.onNodeWithText("Não foi possível excluir o projeto", substring = true)
            .assertIsDisplayed()
        click("Cancelar")
        composeRule.onNodeWithText("Saia antiga").assertIsDisplayed()
    }

    /** Launches the app on [projects] and selects Projetos. */
    private fun showProjetos(projects: ProjectRepository) {
        composeRule.launchApp(projects = projects, clock = October18Clock)
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
    SkirtSnapshot(
        SkirtDraft(Length(80.0), Length(2.0), Length(60.0), Length(1.0), Length(2.0)),
        SkirtRadii(Length(13.05), Length(73.05), Length(12.05), Length(75.05)),
        source = null,
    ),
    createdAt = Instant.parse("2026-10-01T12:00:00Z"),
    updatedAt = Instant.parse("2026-10-01T12:00:00Z"),
)

private val saiaNova = saiaAntiga.copy(id = ProjectId(2), name = ProjectName("Saia nova"))
