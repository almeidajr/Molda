package com.almeidajr.molda.update

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.click
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.dismissSnackbar
import com.almeidajr.molda.launchApp
import com.almeidajr.molda.recreate
import com.almeidajr.molda.screenTitle
import com.almeidajr.molda.selectDestination
import com.almeidajr.molda.snackbar
import com.almeidajr.molda.testing.Beta2
import com.almeidajr.molda.testing.FakeUpdateRepository
import com.almeidajr.molda.update.domain.UpdateCheck
import com.almeidajr.molda.update.domain.UpdateDownload
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The update notice Molda shows when it starts, through a fake of GitHub Releases. */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class StartupUpdateTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @Test
    fun start_withAnUpdate_showsItsVersion_andOffersAtualizar() {
        // When
        composeRule.launchApp(updates = FakeUpdateRepository(UpdateCheck.Available(Beta2)))

        // Then
        composeRule.onNodeWithText("Versão 0.3.0-beta.2 disponível").assertIsDisplayed()
        composeRule.onNodeWithText("Atualizar").assertIsDisplayed()
    }

    @Test
    fun start_choosingAtualizar_downloadsTheUpdate() {
        // Given
        val updates = FakeUpdateRepository(UpdateCheck.Available(Beta2))
        composeRule.launchApp(updates = updates)

        // When
        composeRule.onNodeWithText("Atualizar").performClick()

        // Then
        composeRule.snackbar().assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(UpdateDownload.Downloading(progress = null), updates.download)
        }
    }

    @Test
    fun atualizarOnTheNotice_whenTheDownloadFinishesOutsideAbout_startsTheInstall() {
        // Given
        val updates = FakeUpdateRepository(UpdateCheck.Available(Beta2))
        composeRule.launchApp(updates = updates)
        composeRule.onNodeWithText("Atualizar").performClick()
        composeRule.runOnIdle { assertEquals(emptyList(), updates.installed) }

        // When
        composeRule.runOnIdle { updates.download = UpdateDownload.Downloaded }

        // Then
        composeRule.runOnIdle { assertEquals(listOf(Beta2), updates.installed) }
    }

    @Test
    fun start_withTheUpdateDownloaded_installsItFromInstalar() {
        // Given
        val updates = FakeUpdateRepository(UpdateCheck.Available(Beta2), UpdateDownload.Downloaded)
        composeRule.launchApp(updates = updates)
        composeRule.onNodeWithText("Versão 0.3.0-beta.2 disponível").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(emptyList(), updates.installed) }

        // When
        composeRule.onNodeWithText("Instalar").performClick()

        // Then
        composeRule.runOnIdle { assertEquals(listOf(Beta2), updates.installed) }
    }

    @Test
    fun instalarOnTheNotice_overUnsavedChanges_asksToDiscardThemBeforeInstalling() {
        // Given
        val updates = FakeUpdateRepository(UpdateCheck.Available(Beta2), UpdateDownload.Downloaded)
        composeRule.launchApp(updates = updates)
        composeRule.selectDestination("Perfis")
        composeRule.click("Novo perfil")
        composeRule.onNodeWithText("Nome ou apelido").performTextReplacement("Rascunho")

        // When
        composeRule.onNodeWithText("Instalar").performClick()

        // Then
        composeRule.onNodeWithText("Descartar alterações?").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(emptyList(), updates.installed) }
    }

    @Test
    fun start_afterDismissingTheNotice_doesNotShowItAgainOnRecreation() {
        // Given
        composeRule.launchApp(updates = FakeUpdateRepository(UpdateCheck.Available(Beta2)))
        composeRule.dismissSnackbar()

        // When
        composeRule.recreate()

        // Then
        composeRule.screenTitle("Moldes").assertIsDisplayed()
        composeRule.snackbar().assertDoesNotExist()
    }
}
