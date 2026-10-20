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
import com.almeidajr.molda.labelText
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.screenTitle
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.showSkirtCalculator
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.testing.FakeProfileRepository
import com.almeidajr.molda.testing.FakeProjectRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** Opening a saved project: its stored result read-only, its guide and its source profile. */
class OpenProjectTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val clock = Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC)
    private val projects = FakeProjectRepository(listOf(formaturaProject(clock.instant())))

    @Test
    fun openProject_showsTheStoredResultAndItsSource_withoutRecalculating() {
        showRoute()
        composeRule.selectDestination("Projetos")

        click("Saia da formatura")

        composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText("Medidas de Ana, tiradas em 12/10/2026.").assertIsDisplayed()
        // Stored radii that the draft would not calculate: shown as saved, never recomputed.
        STORED_RADII.forEach {
            composeRule.onNode(hasContentDescription(it, substring = true)).performScrollTo()
                .assertIsDisplayed()
        }
        composeRule.onNodeWithText("Calcular").assertDoesNotExist()
    }

    @Test
    fun guideFromProject_usesItsRadii_andBackReturnsToTheProject() {
        showRoute()
        composeRule.selectDestination("Projetos")
        click("Saia da formatura")

        click("Guia de dobra do tecido")
        repeat(3) { click("Próximo") }
        composeRule.onNode(hasText("11,11\u00A0cm", substring = true)).performScrollTo()
            .assertIsDisplayed()
        pressBack()

        composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
    }

    @Test
    fun savedFromProfile_profileEditedThenDeleted_projectKeepsOriginalMeasurementsAndName() {
        val ana = MeasurementProfile(
            ProfileId(1),
            ProfileMeasurements(ProfileName("Ana"), Length(80.0), LocalDate.of(2026, 10, 12)),
        )
        val profiles = FakeProfileRepository(listOf(ana))
        showRoute(profiles = profiles)
        composeRule.showSkirtCalculator()
        click("Usar perfil")
        composeRule.onAllNodes(hasText("Usar perfil: Ana", substring = true))[0].performClick()
        SkirtDraftField.entries.drop(1).zip(listOf("2", "60", "1", "2")).forEach { (field, value) ->
            composeRule.onNodeWithText(field.labelText).performScrollTo()
                .performTextReplacement(value)
        }
        click("Calcular")
        click("Salvar projeto")
        click("Salvar")
        composeRule.onNodeWithText("Projeto salvo em Projetos.").assertIsDisplayed()

        // A change made elsewhere in the app while the project stays saved.
        runTest {
            val edited =
                ProfileMeasurements(ProfileName("Bia"), Length(95.0), LocalDate.of(2026, 10, 17))
            profiles.update(ana.copy(measurements = edited)).getOrThrow()
            profiles.delete(ana.id).getOrThrow()
        }
        composeRule.selectDestination("Projetos")
        click("Saia godê · Ana · 18/10/2026")

        composeRule.onNodeWithText("Medidas de Ana, tiradas em 12/10/2026.").assertIsDisplayed()
        composeRule.onNode(hasText("Circunferência da cintura:\u00A080,00", substring = true))
            .performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasContentDescription("Costura da cintura: 13,05 cm", substring = true))
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun wideWindow_showsListBesideThePlaceholderThenBesideTheOpenedProject() {
        showRoute(DpSize(1600.dp, 1000.dp))
        composeRule.selectDestination("Projetos")
        composeRule.onNodeWithText("Escolha um projeto na lista.").assertIsDisplayed()

        click("Saia da formatura")

        val row = composeRule.onNode(hasText("Saia da formatura") and hasClickAction())
        row.assertIsDisplayed().assertIsSelected()
        val title = composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
        assertTrue(
            title.getBoundsInRoot().left >= row.getBoundsInRoot().right,
            "project not beside the list",
        )
        composeRule.onNodeWithText("Escolha um projeto na lista.").assertDoesNotExist()
        // A pane beside its list has no up arrow.
        composeRule.onNodeWithContentDescription("Voltar").assertDoesNotExist()
    }

    @Test
    fun recreation_keepsTheOpenedProject() {
        showRoute()
        composeRule.selectDestination("Projetos")
        click("Saia da formatura")

        composeRule.activityRule.scenario.recreate()
        showRoute()

        composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText("Medidas de Ana, tiradas em 12/10/2026.").assertIsDisplayed()
    }

    @Test
    fun openedProject_rename_updatesItsTitleAndTheList() {
        showRoute()
        composeRule.selectDestination("Projetos")
        click("Saia da formatura")

        click("Ações: Saia da formatura")
        click("Renomear projeto")
        composeRule.nameField()
            .performTextReplacement("Saia de verão")
        click("Renomear")

        composeRule.screenTitle("Saia de verão").assertIsDisplayed()
        pressBack()
        composeRule.onNodeWithText("Saia de verão").assertIsDisplayed()
        composeRule.onNodeWithText("Saia da formatura").assertDoesNotExist()
    }

    @Test
    fun openedProject_delete_returnsToTheList() {
        showRoute()
        composeRule.selectDestination("Projetos")
        click("Saia da formatura")

        click("Ações: Saia da formatura")
        click("Excluir projeto")
        click("Confirmar exclusão")

        composeRule.screenTitle("Projetos").assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum projeto salvo").assertIsDisplayed()
    }

    @Test
    fun wideWindow_deletingTheOpenedProject_returnsToThePlaceholder() {
        showRoute(DpSize(1600.dp, 1000.dp))
        composeRule.selectDestination("Projetos")
        click("Saia da formatura")
        composeRule.screenTitle("Saia da formatura").assertIsDisplayed()

        // The detail pane's own menu: the right-hand one of the two beside each other.
        composeRule.onAllNodes(hasText("Ações: Saia da formatura")).assertCountEquals(2)
            .fetchSemanticsNodes()
            .maxBy { it.boundsInRoot.left }
            .let { composeRule.onNode(hasText("Ações: Saia da formatura") and isAt(it.id)) }
            .performClick()
        click("Excluir projeto")
        click("Confirmar exclusão")

        composeRule.onNodeWithText("Escolha um projeto na lista.").assertIsDisplayed()
        composeRule.screenTitle("Saia da formatura").assertDoesNotExist()
    }

    private fun isAt(id: Int) = SemanticsMatcher("node $id") { it.id == id }

    private fun showRoute(
        size: DpSize? = null,
        profiles: FakeProfileRepository = FakeProfileRepository(),
    ) = composeRule.showRoute(profiles, projects, clock, size)

    private fun click(label: String) {
        // Tap the label as a reader does: a small row's centre falls on its Ações button.
        val text = composeRule.onAllNodes(hasText(label), useUnmergedTree = true)[0]
        if (!text.isDisplayed()) text.performScrollTo()
        text.performClick()
    }

    private companion object {
        val STORED_RADII = listOf(
            "Costura da cintura: 11,11\u00A0cm",
            "Barra acabada: 71,11\u00A0cm",
            "Corte da cintura: 10,11\u00A0cm",
            "Corte da barra: 73,11\u00A0cm",
        )
    }
}
