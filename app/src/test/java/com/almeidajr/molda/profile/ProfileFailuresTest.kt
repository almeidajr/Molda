package com.almeidajr.molda.profile

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.click
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.launchApp
import com.almeidajr.molda.pressBack
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.recreate
import com.almeidajr.molda.showProfiles
import com.almeidajr.molda.showSkirtCalculator
import com.almeidajr.molda.testing.FakeProfileRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ProfileFailuresTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Test
    fun save_whilePending_disablesEditor() {
        // Given
        composeRule.launchProfiles(existingProfiles().apply { commandGate = CompletableDeferred() })
        composeRule.click("Novo perfil")
        composeRule.fillProfile("Nova", waist = "90")

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
        composeRule.launchProfiles(existingProfiles().apply { commandGate = gate })
        composeRule.click("Novo perfil")
        composeRule.fillProfile("Nova", waist = "90")
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
        composeRule.launchApp(
            profiles = existingProfiles().apply { commandGate = CompletableDeferred() },
        )
        composeRule.showProfiles()
        composeRule.click("Novo perfil")
        composeRule.fillProfile("Nova", waist = "90")
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

    /**
     * A configuration change keeps the entry's editor, so this checks the held result's delivery,
     * not a restore after process death (ProfileEditorViewModelTest covers that).
     */
    @Test
    fun pickerNewProfile_savedWhileHiddenAcrossConfigurationChange_reachesTheCalculatorOnce() {
        // Given
        val gate = CompletableDeferred<Unit>()
        val storage = existingProfiles().apply { commandGate = gate }
        composeRule.launchApp(profiles = storage)
        composeRule.showSkirtCalculator()
        composeRule.click("Usar perfil")
        composeRule.click("Novo perfil")
        composeRule.fillProfile("Nova", waist = "90")
        composeRule.click("Salvar perfil")
        composeRule.showProfiles()
        composeRule.recreate()
        gate.complete(Unit)

        // When
        composeRule.showSkirtCalculator()

        // Then
        composeRule.onNodeWithText("Cintura copiada do perfil: Nova").assertIsDisplayed()
        composeRule.onNodeWithText("Novo perfil de medidas").assertDoesNotExist()
        assertEquals(3, storage.current.size)
        // A new activity has a new snackbar host: the message shows again only if sent again.
        composeRule.recreate()
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertDoesNotExist()
    }

    @Test
    fun delete_whilePending_keepsTheDialogBusy() {
        // Given
        composeRule.launchProfiles(existingProfiles().apply { commandGate = CompletableDeferred() })
        composeRule.click("Ações: Existente")
        composeRule.click("Excluir perfil")

        // When
        composeRule.click("Confirmar exclusão")

        // Then
        composeRule.onNodeWithText("Excluindo Existente…").assertIsNotEnabled()
        composeRule.onNodeWithText("Cancelar").assertIsNotEnabled()
    }

    @Test
    fun back_whileDeleting_keepsTheDialog() {
        // Given
        composeRule.launchProfiles(existingProfiles().apply { commandGate = CompletableDeferred() })
        composeRule.click("Ações: Existente")
        composeRule.click("Excluir perfil")
        composeRule.click("Confirmar exclusão")

        // When
        composeRule.pressBack()

        // Then
        composeRule.onNodeWithText("Excluir perfil Existente?").assertIsDisplayed()
    }

    @Test
    fun deleteDialog_whenProfilesFailToLoad_showsTheFailureAndItsRetry() {
        // Given
        val storage = existingProfiles()
        composeRule.launchProfiles(storage)
        // The list has loaded; only the dialog's own load fails.
        composeRule.runOnIdle { storage.failsLoad = true }
        composeRule.click("Ações: Existente")
        composeRule.click("Excluir perfil")
        composeRule.onNodeWithText("Não foi possível carregar os perfis.", substring = true)
            .assertIsDisplayed()
        composeRule.runOnIdle { storage.failsLoad = false }

        // When
        composeRule.click("Tentar carregar novamente")

        // Then
        composeRule.onNodeWithText("Excluir perfil Existente?").assertIsDisplayed()
    }

    @Test
    fun pickerRow_withStoredFutureDate_staysListed() {
        // Given
        composeRule.launchCalculator(existingProfiles(measurementDate = LocalDate.of(2026, 10, 14)))

        // When
        composeRule.click("Usar perfil")

        // Then
        composeRule.onNodeWithText("Usar perfil: Existente — 80 cm — 14/10/2026")
            .assertIsDisplayed()
    }

    @Test
    fun saveEdit_ofProfileDeletedMeanwhile_closesTheEditorWithTheMessage() {
        // Given
        val storage = existingProfiles()
        composeRule.launchProfiles(storage)
        composeRule.click("Existente", useUnmergedTree = true)
        composeRule.edit("Nome ou apelido", "Rascunho")
        // Deleted elsewhere after the editor loaded it.
        composeRule.runOnIdle { storage.forget(ProfileId(1)) }

        // When
        composeRule.click("Salvar alterações")

        // Then
        composeRule.onNodeWithText("Este perfil foi excluído.").assertIsDisplayed()
        composeRule.onNodeWithText("Editar perfil de medidas").assertDoesNotExist()
        composeRule.onNodeWithText("Não foi possível salvar o perfil.", substring = true)
            .assertDoesNotExist()
        assertEquals(listOf(ProfileId(2)), storage.current.map { it.id })
    }

    @Test
    fun wideWindow_deleteTheEditedProfile_closesItsEditorWithTheMessage() {
        // Given
        val storage = existingProfiles()
        composeRule.launchApp(profiles = storage, frame = profileFrame(width = 1200))
        composeRule.showProfiles()
        composeRule.click("Existente", useUnmergedTree = true)
        composeRule.edit("Nome ou apelido", "Rascunho")
        composeRule.click("Ações: Existente")
        composeRule.click("Excluir perfil")

        // When
        composeRule.click("Confirmar exclusão")

        // Then
        composeRule.onNodeWithText("Este perfil foi excluído.").assertIsDisplayed()
        composeRule.onNodeWithText("Editar perfil de medidas").assertDoesNotExist()
        composeRule.onNodeWithText("Descartar alterações?").assertDoesNotExist()
        assertEquals(listOf(ProfileId(2)), storage.current.map { it.id })
    }

    @Test
    fun saveNew_whenStorageFails_showsError() {
        // Given
        composeRule.launchProfiles(existingProfiles().apply { failsCommands = true })
        composeRule.click("Novo perfil")
        composeRule.fillProfile("Nova", waist = "90")

        // When
        composeRule.click("Salvar perfil")

        // Then
        composeRule.onNodeWithText("Não foi possível salvar o perfil.", substring = true)
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loadRetry_afterFailure_showsProfiles() {
        // Given
        val storage = existingProfiles().apply { failsLoad = true }
        composeRule.launchProfiles(storage)
        // Perfis composes its list, which starts loading, only after the destination's click.
        composeRule.runOnIdle { storage.failsLoad = false }

        // When
        composeRule.click("Tentar carregar novamente")

        // Then
        composeRule.onNodeWithText("Existente").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun editorLoad_whenStorageFails_keepsFieldsReadOnlyUntilRetried() {
        // Given
        val storage = existingProfiles()
        composeRule.launchProfiles(storage)
        // The list has loaded; only the editor's own load fails.
        composeRule.runOnIdle { storage.failsLoad = true }
        composeRule.click("Existente", useUnmergedTree = true)
        composeRule.onNodeWithText("Nome ou apelido").assertIsNotEnabled()
        composeRule.onNodeWithText("Não foi possível carregar os perfis.", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle { storage.failsLoad = false }

        // When
        composeRule.click("Tentar carregar novamente")

        // Then
        composeRule.onNodeWithText("Nome ou apelido").assertIsEnabled()
            .assertTextContains("Existente")
        composeRule.onNodeWithText("Tentar carregar novamente").assertDoesNotExist()
    }

    // The picker opens half expanded; below a phone's height, its rows start off screen.
    @Config(qualifiers = "w411dp-h923dp")
    @Test
    fun picker_whileProfilesLoad_showsLoading() {
        // Given
        composeRule.launchCalculator(existingProfiles().apply { loadGate = CompletableDeferred() })

        // When
        composeRule.click("Usar perfil")

        // Then
        composeRule.onNodeWithText("Carregando perfis…").assertIsDisplayed()
    }

    @Test
    fun calculate_whileProfilesLoad_showsResults() {
        // Given
        composeRule.launchCalculator(existingProfiles().apply { loadGate = CompletableDeferred() })
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
        composeRule.launchApp(
            profiles = existingProfiles().apply { commandGate = CompletableDeferred() },
            frame = profileFrame(width = 1200),
        )
        composeRule.showProfiles()
        composeRule.click("Existente", useUnmergedTree = true)
        composeRule.edit("Nome ou apelido", "Rascunho")

        // When
        composeRule.click("Salvar alterações")

        // Then
        composeRule.onNodeWithContentDescription("Novo perfil").assertIsNotEnabled()
        composeRule.onNodeWithText("Ações: Outra pessoa").assertIsNotEnabled()
    }

    @Test
    fun wideWindow_deleteFailure_announcesOnce() {
        // Given
        composeRule.launchApp(
            profiles = existingProfiles().apply { failsCommands = true },
            frame = profileFrame(width = 1200),
        )
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
        composeRule.launchApp(
            profiles = existingProfiles().apply { failsCommands = true },
            frame = profileFrame(width = 1200),
        )
        composeRule.showProfiles()
        composeRule.click("Existente", useUnmergedTree = true)
        composeRule.edit("Nome ou apelido", "Rascunho")
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
