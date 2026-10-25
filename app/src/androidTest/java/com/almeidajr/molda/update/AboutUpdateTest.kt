package com.almeidajr.molda.update

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.clickInDialog
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.dismissSnackbar
import com.almeidajr.molda.launchApp
import com.almeidajr.molda.openAbout
import com.almeidajr.molda.profile.leaveUnsavedNewProfile
import com.almeidajr.molda.screenTitle
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.showProfiles
import com.almeidajr.molda.testing.Beta2
import com.almeidajr.molda.testing.FakeUpdateRepository
import com.almeidajr.molda.update.domain.UpdateCheck
import com.almeidajr.molda.update.domain.UpdateDownload
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The update status on About, through a fake of GitHub Releases. */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AboutUpdateTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Test
    fun about_withAnUpdate_showsItsVersionAndReleasePage() {
        // Given
        composeRule.launchApp(updates = FakeUpdateRepository(UpdateCheck.Available(Beta2)))

        // When
        openAboutPastTheStartupNotice()

        // Then
        composeRule.onNodeWithText("Versão 0.3.0-beta.2 disponível").assertIsDisplayed()
        composeRule.onNodeWithText("Ver a página da versão").assertHasClickAction()
        composeRule.onNodeWithText("Verificar atualizações").assertHasClickAction()
    }

    @Test
    fun about_whenUpToDate_saysSo_withoutAReleasePage() {
        // Given
        composeRule.launchApp(updates = FakeUpdateRepository(UpdateCheck.UpToDate))

        // When
        composeRule.openAbout()

        // Then
        composeRule.onNodeWithText("O Molda está atualizado").assertIsDisplayed()
        composeRule.onNodeWithText("Ver a página da versão").assertDoesNotExist()
    }

    @Test
    fun about_whenTheCheckFails_saysSoQuietly_andCanCheckAgain() {
        // Given
        val updates = FakeUpdateRepository(UpdateCheck.Failed)
        composeRule.launchApp(updates = updates)
        composeRule.openAbout()
        composeRule.onNodeWithText("Não foi possível verificar atualizações").assertIsDisplayed()
        updates.result = UpdateCheck.Available(Beta2)

        // When
        composeRule.onNodeWithText("Verificar atualizações").performClick()

        // Then
        composeRule.onNodeWithText("Versão 0.3.0-beta.2 disponível").assertIsDisplayed()
    }

    @Test
    fun about_whileChecking_saysSo_andCannotCheckAgain() {
        // Given
        composeRule.launchApp(
            updates = FakeUpdateRepository().apply { checkGate = CompletableDeferred() },
        )

        // When
        composeRule.openAbout()

        // Then
        composeRule.onNodeWithText("Verificando atualizações…").assertIsDisplayed()
        composeRule.onNodeWithText("Verificar atualizações").assertIsNotEnabled()
    }

    @Test
    fun about_onALocalBuild_offersNoCheck() {
        // Given
        composeRule.launchApp(updates = FakeUpdateRepository(UpdateCheck.NotChecked))

        // When
        composeRule.openAbout()

        // Then
        composeRule.screenTitle("Sobre o Molda").assertIsDisplayed()
        composeRule.onNodeWithText("Verificar atualizações").assertDoesNotExist()
    }

    @Test
    fun about_choosingAtualizar_downloadsTheUpdate() {
        // Given
        val updates = FakeUpdateRepository(UpdateCheck.Available(Beta2))
        composeRule.launchApp(updates = updates)
        openAboutPastTheStartupNotice()

        // When
        composeRule.onNodeWithText("Atualizar").performClick()

        // Then
        composeRule.onNodeWithText("Baixando a atualização…").assertIsDisplayed()
        assertEquals(UpdateDownload.Downloading(progress = null), updates.download)
    }

    @Test
    fun about_duringADownload_showsItsProgress_insteadOfAtualizar() {
        // Given
        composeRule.launchApp(
            updates = FakeUpdateRepository(
                UpdateCheck.Available(Beta2),
                UpdateDownload.Downloading(progress = 0.4f),
            ),
        )

        // When
        openAboutPastTheStartupNotice()

        // Then
        composeRule.onNodeWithText("Baixando a atualização…").assertIsDisplayed()
        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.4f, 0f..1f)))
            .assertIsDisplayed()
        composeRule.onNodeWithText("Atualizar").assertDoesNotExist()
    }

    @Test
    fun about_whenTheDownloadFinishesWhileShown_startsTheInstall() {
        // Given
        val updates = FakeUpdateRepository(
            UpdateCheck.Available(Beta2),
            UpdateDownload.Downloading(progress = 0.9f),
        )
        composeRule.launchApp(updates = updates)
        openAboutPastTheStartupNotice()
        composeRule.onNodeWithText("Baixando a atualização…").assertIsDisplayed()

        // When
        composeRule.runOnIdle { updates.download = UpdateDownload.Downloaded }

        // Then
        composeRule.onNodeWithText("Instalar").assertHasClickAction()
        composeRule.runOnIdle { assertEquals(listOf(Beta2), updates.installed) }
    }

    @Test
    fun about_withADownloadFinishedInTheBackground_offersInstalar_withoutInstalling() {
        // Given
        val updates = FakeUpdateRepository(UpdateCheck.Available(Beta2), UpdateDownload.Downloaded)
        composeRule.launchApp(updates = updates)
        openAboutPastTheStartupNotice()
        composeRule.onNodeWithText("Instalar").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(emptyList(), updates.installed) }

        // When
        composeRule.onNodeWithText("Instalar").performClick()

        // Then
        composeRule.runOnIdle { assertEquals(listOf(Beta2), updates.installed) }
    }

    @Test
    fun about_whenTheDownloadFails_pointsToTheConnection_andOffersARetry() {
        // Given
        val updates = FakeUpdateRepository(UpdateCheck.Available(Beta2), UpdateDownload.Failed)
        composeRule.launchApp(updates = updates)
        openAboutPastTheStartupNotice()
        composeRule.onNodeWithText("O download falhou").assertIsDisplayed()
        composeRule.onNodeWithText("Confira a conexão e tente de novo.").assertIsDisplayed()

        // When
        composeRule.onNodeWithText("Tentar novamente").performClick()

        // Then
        composeRule.onNodeWithText("Baixando a atualização…").assertIsDisplayed()
    }

    @Test
    fun about_whenVerificationFails_saysSo_andOffersARetry() {
        // Given
        val updates = FakeUpdateRepository(
            UpdateCheck.Available(Beta2),
            UpdateDownload.VerificationFailed,
        )
        composeRule.launchApp(updates = updates)
        openAboutPastTheStartupNotice()
        composeRule.onNodeWithText("O arquivo baixado não confere com o publicado.")
            .assertIsDisplayed()

        // When
        composeRule.onNodeWithText("Tentar novamente").performClick()

        // Then
        composeRule.onNodeWithText("Baixando a atualização…").assertIsDisplayed()
    }

    @Test
    fun instalar_overUnsavedChanges_asksToDiscardThemBeforeInstalling() {
        // Given
        val updates = FakeUpdateRepository(UpdateCheck.Available(Beta2), UpdateDownload.Downloaded)
        launchWithUnsavedProfileThenOpenAbout(updates)

        // When
        composeRule.onNodeWithText("Instalar").performClick()

        // Then
        composeRule.onNodeWithText("Descartar alterações?").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(emptyList(), updates.installed) }
    }

    @Test
    fun discardDialogOverInstalar_discard_installs() {
        // Given
        val updates = FakeUpdateRepository(UpdateCheck.Available(Beta2), UpdateDownload.Downloaded)
        launchWithUnsavedProfileThenOpenAbout(updates)
        composeRule.onNodeWithText("Instalar").performClick()

        // When
        composeRule.clickInDialog("Descartar")

        // Then
        composeRule.runOnIdle { assertEquals(listOf(Beta2), updates.installed) }
    }

    @Test
    fun discardDialogOverInstalar_continueEditing_keepsTheChanges_withoutInstalling() {
        // Given
        val updates = FakeUpdateRepository(UpdateCheck.Available(Beta2), UpdateDownload.Downloaded)
        launchWithUnsavedProfileThenOpenAbout(updates)
        composeRule.onNodeWithText("Instalar").performClick()

        // When
        composeRule.clickInDialog("Continuar editando")

        // Then
        composeRule.runOnIdle { assertEquals(emptyList(), updates.installed) }
        composeRule.showProfiles()
        composeRule.onNodeWithText("Nome ou apelido").assertTextContains("Rascunho")
    }

    /** Leaves a new profile's editor with unsaved changes in Perfis and opens About from Moldes. */
    private fun launchWithUnsavedProfileThenOpenAbout(updates: FakeUpdateRepository) {
        composeRule.launchApp(updates = updates)
        composeRule.dismissSnackbar()
        composeRule.leaveUnsavedNewProfile()
        composeRule.selectDestination("Moldes")
        composeRule.openAbout()
    }

    /** Opens About once the startup notice of the same update is gone, so its texts are About's. */
    private fun openAboutPastTheStartupNotice() {
        composeRule.dismissSnackbar()
        composeRule.openAbout()
    }
}
