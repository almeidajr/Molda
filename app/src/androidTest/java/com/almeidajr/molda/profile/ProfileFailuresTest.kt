package com.almeidajr.molda.profile

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.almeidajr.molda.MainActivity
import com.almeidajr.molda.click
import com.almeidajr.molda.closeKeyboard
import com.almeidajr.molda.pressBack
import com.almeidajr.molda.showProfiles
import com.almeidajr.molda.testing.FakeProfileRepository
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test

class ProfileFailuresTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun save_whilePending_disablesEditor() {
        // Given
        composeRule.showProfiles(existingProfiles().apply { commandGate = CompletableDeferred() })
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
        composeRule.showProfiles(existingProfiles().apply { commandGate = gate })
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
    fun delete_whilePending_disablesEveryRowsActions() {
        // Given
        composeRule.showProfiles(existingProfiles().apply { commandGate = CompletableDeferred() })
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
        composeRule.showCalculator(existingProfiles(measurementDate = LocalDate.of(2026, 10, 14)))

        // When
        composeRule.click("Usar perfil")

        // Then
        composeRule.onNodeWithText("Usar perfil: Existente — 80 cm — 14/10/2026")
            .assertIsDisplayed()
    }

    @Test
    fun saveEdit_withStoredFutureDate_asksForCorrection() {
        // Given
        composeRule.showProfiles(existingProfiles(measurementDate = LocalDate.of(2026, 10, 14)))
        composeRule.click("Existente", useUnmergedTree = true)

        // When
        composeRule.click("Salvar alterações")

        // Then
        composeRule.onNodeWithText("A medida já deve ter sido tomada. Corrija a data futura.")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Data da medida (dd/mm/aaaa)").assertTextContains("14/10/2026")
    }

    @Test
    fun saveNew_whenStorageFails_keepsFormAndShowsError() {
        // Given
        val storage = existingProfiles().apply { failsCommands = true }
        composeRule.showProfiles(storage)
        composeRule.click("Novo perfil")
        composeRule.fillProfile("Nova", waist = "90")
        composeRule.closeKeyboard()

        // When
        composeRule.click("Salvar perfil")

        // Then
        composeRule.onNodeWithText("Não foi possível salvar o perfil.", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertDoesNotExist()
        composeRule.onNodeWithText("Nome ou apelido").performScrollTo().assertTextContains("Nova")
        assertEquals(2, storage.current.size)
    }

    @Test
    fun saveEdit_whenStorageFails_keepsDraftAndShowsError() {
        // Given
        val storage = existingProfiles().apply { failsCommands = true }
        val original = storage.current
        composeRule.showProfiles(storage)
        composeRule.click("Existente", useUnmergedTree = true)
        composeRule.edit("Nome ou apelido", "Alterado")
        composeRule.edit("Cintura do perfil (cm)", "82")
        composeRule.closeKeyboard()

        // When
        composeRule.click("Salvar alterações")

        // Then
        composeRule.onNodeWithText("Não foi possível salvar o perfil.", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Perfil salvo neste aparelho.").assertDoesNotExist()
        composeRule.onNodeWithText("Nome ou apelido").performScrollTo()
            .assertTextContains("Alterado")
        composeRule.onNodeWithText("Cintura do perfil (cm)").performScrollTo()
            .assertTextContains("82")
        assertEquals(original, storage.current)
    }

    @Test
    fun delete_whenStorageFails_keepsProfileAndShowsError() {
        // Given
        val storage = existingProfiles().apply { failsCommands = true }
        val original = storage.current
        composeRule.showProfiles(storage)
        composeRule.click("Ações: Existente")
        composeRule.click("Excluir perfil")

        // When
        composeRule.click("Confirmar exclusão")

        // Then
        composeRule.onNodeWithText("Não foi possível excluir o perfil.", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Perfil excluído deste aparelho.").assertDoesNotExist()
        composeRule.onNodeWithText("Existente").performScrollTo().assertIsDisplayed()
        assertEquals(original, storage.current)
    }

    @Test
    fun load_whenStorageFails_showsErrorInsteadOfEmptyList() {
        // Given
        val storage = existingProfiles().apply { failsLoad = true }

        // When
        composeRule.showProfiles(storage)

        // Then
        composeRule.onNodeWithText("Não foi possível carregar os perfis.", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum perfil salvo.", substring = true).assertDoesNotExist()
    }

    @Test
    fun loadRetry_afterFailure_showsProfiles() {
        // Given
        val storage = existingProfiles().apply { failsLoad = true }
        composeRule.showProfiles(storage)
        storage.failsLoad = false

        // When
        composeRule.click("Tentar carregar novamente")

        // Then
        composeRule.onNodeWithText("Existente").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun picker_whileProfilesLoad_showsLoading() {
        // Given
        composeRule.showCalculator(existingProfiles().apply { loadGate = CompletableDeferred() })

        // When
        composeRule.click("Usar perfil")

        // Then
        composeRule.onNodeWithText("Carregando perfis…").assertIsDisplayed()
    }

    @Test
    fun calculate_whileProfilesLoad_showsResults() {
        // Given
        composeRule.showCalculator(existingProfiles().apply { loadGate = CompletableDeferred() })
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
    fun wideWindow_deleteFailure_announcesOnce() {
        // Given
        composeRule.showProfileRoute(
            existingProfiles().apply {
                failsCommands = true
            },
            width = 1200,
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
        composeRule.showProfileRoute(
            existingProfiles().apply {
                failsCommands = true
            },
            width = 1200,
        )
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
