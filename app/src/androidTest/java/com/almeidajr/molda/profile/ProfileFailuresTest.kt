package com.almeidajr.molda.profile

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.almeidajr.molda.click
import com.almeidajr.molda.closeKeyboard
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.di.ClockModule
import com.almeidajr.molda.di.RepositoryModule
import com.almeidajr.molda.pressBack
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.recreate
import com.almeidajr.molda.showProfiles
import com.almeidajr.molda.showSkirtCalculator
import com.almeidajr.molda.testing.FakeProfileRepository
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.October13Clock
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.time.Clock
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@UninstallModules(RepositoryModule::class, ClockModule::class)
class ProfileFailuresTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    // Each test binds its own profiles, then launches the app on them.
    @BindValue @JvmField
    var profileRepository: ProfileRepository = FakeProfileRepository()

    @BindValue @JvmField
    val projectRepository: ProjectRepository = FakeProjectRepository()

    @BindValue @JvmField
    val clock: Clock = October13Clock

    @Test
    fun save_whilePending_disablesEditor() {
        // Given
        profileRepository = existingProfiles().apply { commandGate = CompletableDeferred() }
        composeRule.launchProfiles()
        composeRule.click("Novo perfil")
        composeRule.fillProfile("Nova", waist = "90")
        composeRule.closeKeyboard()

        // When
        composeRule.click("Salvar perfil")

        // Then
        composeRule.onNodeWithText("Salvando…").assertIsNotEnabled()
        composeRule.onNodeWithText("Cancelar").assertIsNotEnabled()
        composeRule.onNodeWithText("Nome ou apelido").assertIsNotEnabled()
        composeRule.onNodeWithText("Data da medida (dd/mm/aaaa)").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Escolher data no calendário").assertIsNotEnabled()
    }

    @Test
    fun back_whileSaving_keepsEditorUntilSaved() {
        // Given
        val gate = CompletableDeferred<Unit>()
        profileRepository = existingProfiles().apply { commandGate = gate }
        composeRule.launchProfiles()
        composeRule.click("Novo perfil")
        composeRule.fillProfile("Nova", waist = "90")
        // Back must reach the editor, not close the keyboard.
        composeRule.closeKeyboard()
        composeRule.click("Salvar perfil")

        // When
        composeRule.pressBack()

        // Then
        composeRule.onNodeWithText("Novo perfil de medidas").assertIsDisplayed()
        gate.complete(Unit)
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertIsDisplayed()
        composeRule.onNodeWithText("Nova").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun pickerNewProfile_afterRecreationWithHiddenSaveHeld_opensItsOwnEditor() {
        // Given
        profileRepository = existingProfiles().apply { commandGate = CompletableDeferred() }
        // The app's own content, so the recreated activity composes the same route.
        composeRule.activityRule.launch()
        composeRule.showProfiles()
        composeRule.click("Novo perfil")
        composeRule.fillProfile("Nova", waist = "90")
        composeRule.closeKeyboard()
        composeRule.click("Salvar perfil")
        composeRule.showSkirtCalculator()
        composeRule.recreate()
        composeRule.click("Usar perfil")

        // When
        composeRule.click("Novo perfil")

        // Then
        composeRule.onNodeWithText("Descartar alterações?").assertDoesNotExist()
        composeRule.onNodeWithText("Novo perfil de medidas").assertIsDisplayed()
        // Its own blank form, not the Perfis draft being saved.
        composeRule.onNodeWithText("Nova").assertDoesNotExist()
    }

    @Test
    fun saveHeld_afterRecreation_storesOnceAndShowsTheMessageOnce() {
        // Given
        val gate = CompletableDeferred<Unit>()
        val storage = existingProfiles().apply { commandGate = gate }
        profileRepository = storage
        // The app's own content, so the recreated activity composes the same route.
        composeRule.activityRule.launch()
        composeRule.showProfiles()
        composeRule.click("Novo perfil")
        composeRule.fillProfile("Nova", waist = "90")
        composeRule.closeKeyboard()
        composeRule.click("Salvar perfil")
        composeRule.recreate()

        // When
        gate.complete(Unit)

        // Then
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertIsDisplayed()
        composeRule.onNodeWithText("Novo perfil de medidas").assertDoesNotExist()
        assertEquals(3, storage.current.size)
        // A new activity has a new snackbar host: the message shows again only if sent again.
        composeRule.recreate()
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertDoesNotExist()
    }

    @Test
    fun delete_whilePending_disablesEveryRowsActions() {
        // Given
        profileRepository = existingProfiles().apply { commandGate = CompletableDeferred() }
        composeRule.launchProfiles()
        composeRule.click("Ações: Existente")
        composeRule.click("Excluir perfil")

        // When
        composeRule.click("Confirmar exclusão")

        // Then
        composeRule.onNodeWithText("Excluindo Existente…").assertIsDisplayed()
        composeRule.onNodeWithText("Ações: Existente").assertIsNotEnabled()
        composeRule.onNodeWithText("Ações: Outra pessoa").assertIsNotEnabled()
    }

    @Test
    fun pickerRow_withStoredFutureDate_staysListed() {
        // Given
        profileRepository = existingProfiles(measurementDate = LocalDate.of(2026, 10, 14))
        composeRule.launchCalculator()

        // When
        composeRule.click("Usar perfil")

        // Then
        composeRule.onNodeWithText("Usar perfil: Existente — 80 cm — 14/10/2026")
            .assertIsDisplayed()
    }

    @Test
    fun saveEdit_withStoredFutureDate_asksForCorrection() {
        // Given
        profileRepository = existingProfiles(measurementDate = LocalDate.of(2026, 10, 14))
        composeRule.launchProfiles()
        composeRule.click("Existente", useUnmergedTree = true)

        // When
        composeRule.click("Salvar alterações")

        // Then
        composeRule.onNodeWithText("A medida já deve ter sido tomada. Corrija a data futura.")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Data da medida (dd/mm/aaaa)").assertTextContains("14/10/2026")
    }

    @Test
    fun saveNew_whenStorageFails_showsError() {
        // Given
        profileRepository = existingProfiles().apply { failsCommands = true }
        composeRule.launchProfiles()
        composeRule.click("Novo perfil")
        composeRule.fillProfile("Nova", waist = "90")
        composeRule.closeKeyboard()

        // When
        composeRule.click("Salvar perfil")

        // Then
        composeRule.onNodeWithText("Não foi possível salvar o perfil.", substring = true)
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun load_whenStorageFails_showsErrorInsteadOfEmptyList() {
        // Given
        val storage = existingProfiles().apply { failsLoad = true }

        // When
        profileRepository = storage
        composeRule.launchProfiles()

        // Then
        composeRule.onNodeWithText("Não foi possível carregar os perfis.", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum perfil salvo.", substring = true).assertDoesNotExist()
    }

    @Test
    fun loadRetry_afterFailure_showsProfiles() {
        // Given
        val storage = existingProfiles().apply { failsLoad = true }
        profileRepository = storage
        composeRule.launchProfiles()
        // Perfis composes its list, which starts loading, only after the destination's click.
        composeRule.runOnIdle { storage.failsLoad = false }

        // When
        composeRule.click("Tentar carregar novamente")

        // Then
        composeRule.onNodeWithText("Existente").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun picker_whileProfilesLoad_showsLoading() {
        // Given
        profileRepository = existingProfiles().apply { loadGate = CompletableDeferred() }
        composeRule.launchCalculator()

        // When
        composeRule.click("Usar perfil")

        // Then
        composeRule.onNodeWithText("Carregando perfis…").assertIsDisplayed()
    }

    @Test
    fun calculate_whileProfilesLoad_showsResults() {
        // Given
        profileRepository = existingProfiles().apply { loadGate = CompletableDeferred() }
        composeRule.launchCalculator()
        composeRule.edit("Circunferência da cintura (cm)", "80")
        composeRule.edit("Folga de cintura (cm)", "0")
        composeRule.edit("Comprimento acabado (cm)", "60")
        composeRule.edit("Margem de costura da cintura (cm)", "0")
        composeRule.edit("Margem de barra (cm)", "0")

        // When
        composeRule.click("Calcular")

        // Then
        composeRule.onNodeWithText("Raios para o molde").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun wideWindow_whileTheEditorSaves_disablesOpeningAnother() {
        // Given
        profileRepository = existingProfiles().apply { commandGate = CompletableDeferred() }
        composeRule.launchProfileRoute(width = 1200)
        composeRule.showProfiles()
        composeRule.click("Existente", useUnmergedTree = true)
        composeRule.edit("Nome ou apelido", "Rascunho")
        composeRule.closeKeyboard()

        // When
        composeRule.click("Salvar alterações")

        // Then
        composeRule.onNodeWithContentDescription("Novo perfil").assertIsNotEnabled()
        composeRule.onNodeWithText("Ações: Outra pessoa").assertIsNotEnabled()
    }

    @Test
    fun wideWindow_deleteFailure_announcesOnce() {
        // Given
        profileRepository = existingProfiles().apply { failsCommands = true }
        composeRule.launchProfileRoute(width = 1200)
        composeRule.showProfiles()
        composeRule.click("Existente", useUnmergedTree = true)
        composeRule.click("Ações: Outra pessoa")
        composeRule.click("Excluir perfil")

        // When
        composeRule.click("Confirmar exclusão")

        // Then
        composeRule.onAllNodesWithText("Não foi possível excluir o perfil.", substring = true)
            .assertCountEquals(1)
        composeRule.onNodeWithText("Não foi possível excluir o perfil.", substring = true)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
            )
    }

    @Test
    fun wideWindow_deleteFailure_keepsOtherProfilesDraft() {
        // Given
        profileRepository = existingProfiles().apply { failsCommands = true }
        composeRule.launchProfileRoute(width = 1200)
        composeRule.showProfiles()
        composeRule.click("Existente", useUnmergedTree = true)
        composeRule.edit("Nome ou apelido", "Rascunho")
        composeRule.closeKeyboard()
        composeRule.click("Ações: Outra pessoa")
        composeRule.click("Excluir perfil")

        // When
        composeRule.click("Confirmar exclusão")

        // Then
        composeRule.onNodeWithText("Nome ou apelido").performScrollTo()
            .assertTextContains("Rascunho")
        composeRule.onNodeWithText("Outra pessoa").performScrollTo().assertIsDisplayed()
    }
}

private fun existingProfiles(measurementDate: LocalDate = LocalDate.of(2026, 10, 12)) =
    FakeProfileRepository(
        listOf(
            profile(1, "Existente", date = measurementDate),
            profile(2, "Outra pessoa", waist = 95.0, date = LocalDate.of(2026, 10, 11)),
        ),
    )
