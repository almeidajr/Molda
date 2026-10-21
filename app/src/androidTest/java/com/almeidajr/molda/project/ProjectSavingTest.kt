package com.almeidajr.molda.project

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.labelText
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.measurement.MeasurementSource
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.showRoute
import com.almeidajr.molda.showSkirtCalculator
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.testing.FakeProfileRepository
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.October18Clock
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

/** Saving a calculated skirt as a project, from the result to the Projetos list. */
class ProjectSavingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun save_fromProfileResultWithSuggestedName_storesItsSourceAndListsIt() {
        // Given
        val ana = MeasurementProfile(
            ProfileId(1),
            ProfileMeasurements(ProfileName("Ana"), Length(80.0), LocalDate.of(2026, 10, 12)),
        )
        val projects = FakeProjectRepository()
        composeRule.showRoute(FakeProfileRepository(listOf(ana)), projects, October18Clock)
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
        composeRule.showRoute(
            FakeProfileRepository(),
            FakeProjectRepository().apply { failsCommands = true },
            October18Clock,
        )
        composeRule.showSkirtCalculator()
        composeRule.onNodeWithText(SkirtDraftField.Waist.labelText).performScrollTo()
            .performTextReplacement("70")
        calculate(listOf("0", "45", "0", "0"))

        // When
        click("Salvar projeto")
        click("Salvar")

        // Then
        composeRule.onNodeWithText("Não foi possível salvar o projeto", substring = true)
            .assertIsDisplayed()
        composeRule.nameField().assertTextContains("Saia godê · 18/10/2026")
        click("Cancelar")
        composeRule.onNodeWithText("Raios para o molde").performScrollTo().assertIsDisplayed()
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
