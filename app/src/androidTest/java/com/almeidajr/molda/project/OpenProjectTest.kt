package com.almeidajr.molda.project

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.almeidajr.molda.AnimatorDurationScaleRule
import com.almeidajr.molda.click
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.di.ClockModule
import com.almeidajr.molda.di.RepositoryModule
import com.almeidajr.molda.dialogTextField
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.pressBack
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileName
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.recreate
import com.almeidajr.molda.screenTitle
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.showRoute
import com.almeidajr.molda.testing.FakeProfileRepository
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.October18Clock
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.time.Clock
import java.time.LocalDate
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

/** Opening a saved project: its stored result read-only, its guide and its source profile. */
@HiltAndroidTest
@UninstallModules(RepositoryModule::class, ClockModule::class)
class OpenProjectTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @get:Rule(order = 2)
    val animatorScale = AnimatorDurationScaleRule(composeRule)

    // Each test binds its own profiles, then launches the app on them.
    @BindValue @JvmField
    var profileRepository: ProfileRepository = FakeProfileRepository()

    @BindValue @JvmField
    val projectRepository: ProjectRepository =
        FakeProjectRepository(listOf(formaturaProject(October18Clock.instant())))

    @BindValue @JvmField
    val clock: Clock = October18Clock

    @Test
    fun open_afterItsSourceProfileChanged_showsTheStoredResultWithoutRecalculating() {
        // Given: Ana's profile, the project's source, has since become Bia with another waist.
        val bia = MeasurementProfile(
            ProfileId(1),
            ProfileMeasurements(ProfileName("Bia"), Length(95.0), LocalDate.of(2026, 10, 17)),
        )
        launch(profiles = FakeProfileRepository(listOf(bia)))
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
    fun open_afterSaving_showsTheExplanationAndFormulasFromItsStoredValues() {
        // Given
        launch()
        openProject()

        mapOf(
            "Como levar os raios ao papel" to listOf(
                "A margem de cintura de 1 cm avança para dentro; " +
                    "a margem de barra de 1,5 cm avança para fora.",
            ),
            // The stored radii, not the 13,05 cm and the rest a recalculation would give.
            "Fórmulas" to listOf(
                "r = (80 + 2) / (2π) = 11,11 cm",
                "r + 60 = 71,11 cm",
                "r − 1 = 10,11 cm",
                "r + 60 + 1,5 = 73,11 cm",
            ),
        ).forEach { (label, contents) ->
            // When
            composeRule.onNodeWithText(label).performScrollTo().performClick()

            // Then
            contents.forEach {
                composeRule.onNodeWithText(it, substring = true).performScrollTo()
                    .assertIsDisplayed()
            }
        }
    }

    @Test
    fun open_afterSaving_describesTheDraftingFigureWithTheStoredAllowances() {
        // Given: a 1,5 cm hem allowance, which a "1,50" reading of the snapshot would misstate.
        launch()

        // When
        openProject()

        // Then
        composeRule.onNode(hasContentDescription("margem de barra de 1,5 cm", substring = true))
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun guide_fromProject_showsItsStoredRadii() {
        // Given
        launch()
        openProject()

        // When
        openGuide()
        repeat(3) { composeRule.click("Próximo") }

        // Then
        composeRule.onNode(hasText("11,11 cm", substring = true)).performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun back_fromProjectGuide_returnsToTheProject() {
        // Given
        launch()
        openProject()
        openGuide()

        // When
        composeRule.pressBack()

        // Then
        composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
    }

    @Test
    fun wideWindow_withNoProjectOpen_showsThePlaceholderBesideTheList() {
        // Given
        launch(DpSize(1600.dp, 1000.dp))

        // When
        composeRule.selectDestination("Projetos")

        // Then
        composeRule.onNodeWithText("Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText("Escolha um projeto na lista.").assertIsDisplayed()
    }

    @Test
    fun wideWindow_openProject_showsItBesideTheListWithoutUpArrow() {
        // Given
        launch(DpSize(1600.dp, 1000.dp))
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
        launch()
        openProject()

        // When
        composeRule.recreate()
        composeRule.showRoute()

        // Then
        composeRule.screenTitle("Saia da formatura").assertIsDisplayed()
        composeRule.onNodeWithText("Medidas de Ana, tiradas em 12/10/2026.").assertIsDisplayed()
    }

    @Test
    fun rename_openedProject_updatesItsTitleAndTheList() {
        // Given
        launch()
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
        launch()
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
        launch(DpSize(1600.dp, 1000.dp))
        openProject()

        // When: the detail pane's own menu, beside its Editar button, not the list row's.
        composeRule.onNode(hasText("Ações: Saia da formatura") and hasAnySibling(hasText("Editar")))
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

    private fun openGuide() {
        // The guide's drawings hold still, so Compose goes idle.
        animatorScale.set(0f)
        composeRule.click("Guia de dobra do tecido")
    }

    /** Binds [profiles], then launches the app on them at [size] when given. */
    private fun launch(
        size: DpSize? = null,
        profiles: ProfileRepository = FakeProfileRepository(),
    ) {
        profileRepository = profiles
        composeRule.activityRule.launch()
        composeRule.showRoute(size)
    }

    private companion object {
        val STORED_RADII = listOf(
            "Costura da cintura: 11,11 cm",
            "Barra acabada: 71,11 cm",
            "Corte da cintura: 10,11 cm",
            "Corte da barra: 73,11 cm",
        )
    }
}
