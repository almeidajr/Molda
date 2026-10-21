package com.almeidajr.molda.project

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.almeidajr.molda.IsolatedStorage
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.labelText
import com.almeidajr.molda.profile.data.LocalProfileRepository
import com.almeidajr.molda.project.data.LocalProjectRepository
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.screenTitle
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.showSkirtCalculator
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

/**
 * A project's whole life in the app: saved from a result, edited, copied and deleted. It runs on
 * real Room through [IsolatedStorage], so Compose waits for every query without polling.
 */
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
    fun calculateSaveEditCopyAndDelete_keepsProjetosInStep() = runTest {
        // Given
        showRoute()
        composeRule.showSkirtCalculator()
        listOf("80", "2", "60", "1", "2").zip(SkirtDraftField.entries).forEach { (text, field) ->
            editField(field, text)
        }
        click("Calcular")

        // When: saved from the result
        click("Salvar projeto")
        composeRule.nameField().performTextReplacement("Saia da formatura")
        confirmDialog("Salvar")

        // Then
        composeRule.onNodeWithText("Projeto salvo em Projetos.").assertIsDisplayed()
        composeRule.selectDestination("Projetos")
        composeRule.onNodeWithText("Saia godê · 18/10/2026").assertIsDisplayed()

        // When: opened, edited and saved in place
        click("Saia da formatura")
        click("Editar")
        editField(SkirtDraftField.Ease, "4")
        click("Calcular")
        click("Salvar")

        // Then
        composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText("Projeto atualizado.").assertIsDisplayed()
        assertEquals(listOf("Saia da formatura" to 4.0), storedNamesAndEase())

        // When: edited again and saved as a copy
        click("Editar")
        editField(SkirtDraftField.Ease, "5")
        click("Calcular")
        click("Salvar como novo")
        composeRule.nameField().performTextReplacement("Saia de verão")
        confirmDialog("Salvar")

        // Then
        composeRule.screenTitle("Saia de verão").assertIsDisplayed()
        assertEquals(
            listOf("Saia de verão" to 5.0, "Saia da formatura" to 4.0),
            storedNamesAndEase(),
        )

        // When: the copy is deleted
        click("Ações: Saia de verão")
        click("Excluir projeto")
        confirmDialog("Confirmar exclusão")

        // Then
        composeRule.onNodeWithText("Projeto excluído deste aparelho.").assertIsDisplayed()
        composeRule.onNodeWithText("Saia de verão").assertDoesNotExist()
        composeRule.onNodeWithText("Saia da formatura").assertIsDisplayed()
        assertEquals(listOf("Saia da formatura" to 4.0), storedNamesAndEase())
    }

    private suspend fun storedNamesAndEase(): List<Pair<String, Double>> =
        projects.projects().first().map(Project::namesAndEase)

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

    private fun click(label: String) {
        // Tap the label as a reader does: a small row's centre falls on its Ações button.
        val text = composeRule.onAllNodes(hasText(label), useUnmergedTree = true)[0]
        if (!text.isDisplayed()) text.performScrollTo()
        text.performClick()
    }

    private fun showRoute() =
        composeRule.showRoute(LocalProfileRepository(database.profiles()), projects, clock)
}

private fun Project.namesAndEase(): Pair<String, Double> =
    name.text to snapshot.draft.ease.centimetres
