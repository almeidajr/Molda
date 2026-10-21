package com.almeidajr.molda.project

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.click
import com.almeidajr.molda.dialogTextField
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.recreate
import com.almeidajr.molda.screenTitle
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.showRoute
import com.almeidajr.molda.testing.FakeProfileRepository
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.October18Clock
import java.time.LocalDate
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

/** Opening a saved project: its stored result read-only, its guide and its source profile. */
class OpenProjectTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val projects = FakeProjectRepository(listOf(formaturaProject(October18Clock.instant())))

    @Test
    fun open_afterItsSourceProfileChanged_showsTheStoredResultWithoutRecalculating() {
        // Given: Ana's profile, the project's source, has since become Bia with another waist.
        val bia = MeasurementProfile(
            ProfileId(1),
            ProfileMeasurements(ProfileName("Bia"), Length(95.0), LocalDate.of(2026, 10, 17)),
        )
        showRoute(profiles = FakeProfileRepository(listOf(bia)))
        composeRule.selectDestination("Projetos")

        // When
        composeRule.clickProject("Saia da formatura")

        // Then
        composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText("Medidas de Ana, tiradas em 12/10/2026.").assertIsDisplayed()
        composeRule.onNode(hasText("Circunferência da cintura: 80,00", substring = true))
            .performScrollTo().assertIsDisplayed()
        // Stored radii that the draft would not calculate: shown as saved, never recomputed.
        STORED_RADII.forEach {
            composeRule.onNode(hasContentDescription(it, substring = true)).performScrollTo()
                .assertIsDisplayed()
        }
        composeRule.onNodeWithText("Calcular").assertDoesNotExist()
    }

    @Test
    fun guide_fromProject_showsItsStoredRadii() {
        // Given
        showRoute()
        openProject()

        // When
        composeRule.click("Guia de dobra do tecido")
        repeat(3) { composeRule.click("Próximo") }

        // Then
        composeRule.onNode(hasText("11,11 cm", substring = true)).performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun back_fromProjectGuide_returnsToTheProject() {
        // Given
        showRoute()
        openProject()
        composeRule.click("Guia de dobra do tecido")

        // When
        pressBack()

        // Then
        composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
    }

    @Test
    fun wideWindow_withNoProjectOpen_showsThePlaceholderBesideTheList() {
        // Given
        showRoute(DpSize(1600.dp, 1000.dp))

        // When
        composeRule.selectDestination("Projetos")

        // Then
        composeRule.onNodeWithText("Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText("Escolha um projeto na lista.").assertIsDisplayed()
    }

    @Test
    fun wideWindow_openProject_showsItBesideTheListWithoutUpArrow() {
        // Given
        showRoute(DpSize(1600.dp, 1000.dp))
        composeRule.selectDestination("Projetos")

        // When
        composeRule.clickProject("Saia da formatura")

        // Then
        val row = composeRule.onNode(hasText("Saia da formatura") and hasClickAction())
        row.assertIsDisplayed().assertIsSelected()
        val title = composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
        assertTrue(
            title.getBoundsInRoot().left >= row.getBoundsInRoot().right,
            "project not beside the list",
        )
        composeRule.onNodeWithText("Escolha um projeto na lista.").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Voltar").assertDoesNotExist()
    }

    @Test
    fun recreation_keepsTheOpenedProject() {
        // Given
        showRoute()
        openProject()

        // When
        composeRule.recreate()
        showRoute()

        // Then
        composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText("Medidas de Ana, tiradas em 12/10/2026.").assertIsDisplayed()
    }

    @Test
    fun rename_openedProject_updatesItsTitleAndTheList() {
        // Given
        showRoute()
        openProject()

        // When
        composeRule.click("Ações: Saia da formatura")
        composeRule.click("Renomear projeto")
        composeRule.dialogTextField().performTextReplacement("Saia de verão")
        composeRule.click("Renomear")

        // Then
        composeRule.screenTitle("Saia de verão").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.onNodeWithText("Saia de verão").assertIsDisplayed()
        composeRule.onNodeWithText("Saia da formatura").assertDoesNotExist()
    }

    @Test
    fun delete_openedProject_returnsToTheEmptyList() {
        // Given
        showRoute()
        openProject()

        // When
        composeRule.click("Ações: Saia da formatura")
        composeRule.click("Excluir projeto")
        composeRule.click("Confirmar exclusão")

        // Then
        composeRule.screenTitle("Projetos").assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum projeto salvo").assertIsDisplayed()
    }

    @Test
    fun wideWindow_deleteOpenedProject_returnsToThePlaceholder() {
        // Given
        showRoute(DpSize(1600.dp, 1000.dp))
        openProject()

        // When: the detail pane's own menu, the right-hand one of the two beside each other.
        composeRule.onAllNodes(hasText("Ações: Saia da formatura")).assertCountEquals(2)
            .fetchSemanticsNodes()
            .maxBy { it.boundsInRoot.left }
            .let { composeRule.onNode(hasText("Ações: Saia da formatura") and isAt(it.id)) }
            .performClick()
        composeRule.click("Excluir projeto")
        composeRule.click("Confirmar exclusão")

        // Then
        composeRule.onNodeWithText("Escolha um projeto na lista.").assertIsDisplayed()
        composeRule.screenTitle("Saia da formatura").assertDoesNotExist()
    }

    private fun openProject() {
        composeRule.selectDestination("Projetos")
        composeRule.clickProject("Saia da formatura")
    }

    private fun isAt(id: Int) = SemanticsMatcher("node $id") { it.id == id }

    private fun showRoute(
        size: DpSize? = null,
        profiles: FakeProfileRepository = FakeProfileRepository(),
    ) = composeRule.showRoute(profiles, projects, October18Clock, size)

    private companion object {
        val STORED_RADII = listOf(
            "Costura da cintura: 11,11 cm",
            "Barra acabada: 71,11 cm",
            "Corte da cintura: 10,11 cm",
            "Corte da barra: 73,11 cm",
        )
    }
}
