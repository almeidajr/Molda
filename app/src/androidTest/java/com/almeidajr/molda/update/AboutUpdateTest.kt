package com.almeidajr.molda.update

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.launchApp
import com.almeidajr.molda.screenTitle
import com.almeidajr.molda.testing.FakeUpdateRepository
import com.almeidajr.molda.update.domain.ReleaseAsset
import com.almeidajr.molda.update.domain.Update
import com.almeidajr.molda.update.domain.UpdateCheck
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
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
        openAbout()

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
        openAbout()

        // Then
        composeRule.onNodeWithText("O Molda está atualizado").assertIsDisplayed()
        composeRule.onNodeWithText("Ver a página da versão").assertDoesNotExist()
    }

    @Test
    fun about_whenTheCheckFails_saysSoQuietly_andCanCheckAgain() {
        // Given
        val updates = FakeUpdateRepository(UpdateCheck.Failed)
        composeRule.launchApp(updates = updates)
        openAbout()
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
        openAbout()

        // Then
        composeRule.onNodeWithText("Verificando atualizações…").assertIsDisplayed()
        composeRule.onNodeWithText("Verificar atualizações").assertIsNotEnabled()
    }

    @Test
    fun about_onALocalBuild_offersNoCheck() {
        // Given
        composeRule.launchApp(updates = FakeUpdateRepository(UpdateCheck.NotChecked))

        // When
        openAbout()

        // Then
        composeRule.screenTitle("Sobre o Molda").assertIsDisplayed()
        composeRule.onNodeWithText("Verificar atualizações").assertDoesNotExist()
    }

    private fun openAbout() = composeRule.onNode(
        SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription,
            listOf("Sobre o Molda"),
        ),
    ).performClick()

    private companion object {
        val Beta2 = Update(
            version = "0.3.0-beta.2",
            page = "https://github.com/almeidajr/Molda/releases/tag/v0.3.0-beta.2",
            apk = ReleaseAsset(
                name = "molda-v0.3.0-beta.2.apk",
                downloadUrl = "https://github.com/almeidajr/Molda/releases/download/" +
                    "v0.3.0-beta.2/molda-v0.3.0-beta.2.apk",
                digest = "sha256:0123",
            ),
        )
    }
}
