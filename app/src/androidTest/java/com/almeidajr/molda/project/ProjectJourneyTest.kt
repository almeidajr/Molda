package com.almeidajr.molda.project

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.almeidajr.molda.IsolatedStorage
import com.almeidajr.molda.click
import com.almeidajr.molda.closeKeyboard
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.di.RepositoryModule
import com.almeidajr.molda.dialogTextField
import com.almeidajr.molda.labelText
import com.almeidajr.molda.launchApp
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.screenTitle
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.showSkirtCalculator
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.testing.October18Clock
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * A project's whole life in the app: saved from a result, edited, copied and deleted. It runs the
 * production repositories on Room through [IsolatedStorage], so Compose waits for every query
 * without polling.
 */
@HiltAndroidTest
class ProjectJourneyTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Inject
    lateinit var storage: IsolatedStorage

    private val projects by lazy { RepositoryModule.projectRepository(storage.database) }

    @Before
    fun injectStorage() = hiltRule.inject()

    @After
    fun removeIsolatedStorage() = storage.remove(composeRule.activityRule.scenario)

    @Test
    fun projetos_throughCalculateSaveEditCopyAndDelete_stayInStep() = runTest {
        // Given
        composeRule.launchApp(
            profiles = RepositoryModule.profileRepository(storage.database),
            projects = projects,
            clock = October18Clock,
        )
        composeRule.showSkirtCalculator()
        listOf("80", "2", "60", "1", "2").zip(SkirtDraftField.entries).forEach { (text, field) ->
            editField(field, text)
        }
        composeRule.click("Calcular")

        // When: saved from the result
        composeRule.click("Salvar projeto")
        composeRule.dialogTextField().performTextReplacement("Saia da formatura")
        confirmDialog("Salvar")

        // Then
        composeRule.onNodeWithText("Projeto salvo em Projetos.").assertIsDisplayed()
        composeRule.selectDestination("Projetos")
        composeRule.onNodeWithText("Saia godê · 18/10/2026").assertIsDisplayed()

        // When: opened, edited and saved in place
        composeRule.clickProject("Saia da formatura")
        composeRule.click("Editar")
        editField(SkirtDraftField.Ease, "4")
        composeRule.click("Calcular")
        composeRule.click("Salvar")

        // Then
        composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText("Projeto atualizado.").assertIsDisplayed()
        assertEquals(listOf("Saia da formatura" to 4.0), storedNamesAndEase())

        // When: edited again and saved as a copy
        composeRule.click("Editar")
        editField(SkirtDraftField.Ease, "5")
        composeRule.click("Calcular")
        composeRule.click("Salvar como novo")
        composeRule.dialogTextField().performTextReplacement("Saia de verão")
        confirmDialog("Salvar")

        // Then
        composeRule.screenTitle("Saia de verão").assertIsDisplayed()
        assertEquals(
            listOf("Saia de verão" to 5.0, "Saia da formatura" to 4.0),
            storedNamesAndEase(),
        )

        // When: the copy is deleted
        composeRule.click("Ações: Saia de verão")
        composeRule.click("Excluir projeto")
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
        composeRule.closeKeyboard()
    }

    private fun confirmDialog(label: String) {
        composeRule.onNode(hasText(label) and hasAnyAncestor(isDialog())).performClick()
        // The name field's keyboard may outlive the dialog and cover the next target.
        composeRule.closeKeyboard()
    }
}

private fun Project.namesAndEase(): Pair<String, Double> =
    name.text to snapshot.draft.ease.centimeters
