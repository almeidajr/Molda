package com.almeidajr.molda.update

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.createLaunchLaterComposeRule
import com.almeidajr.molda.dismissSnackbar
import com.almeidajr.molda.launchApp
import com.almeidajr.molda.openAbout
import com.almeidajr.molda.testing.Beta2
import com.almeidajr.molda.testing.FakeUpdateRepository
import com.almeidajr.molda.update.domain.UpdateCheck
import com.almeidajr.molda.update.domain.UpdateDownload
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.assertEquals
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Installing without the "install unknown apps" permission, through a fake of GitHub Releases;
 * the system setting is stubbed, answering as the sewist returning from it.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class UpdateInstallPermissionTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createLaunchLaterComposeRule()

    @get:Rule(order = 2)
    val intentsRule = IntentsRule()

    private val updates = FakeUpdateRepository(
        UpdateCheck.Available(Beta2),
        UpdateDownload.Downloaded,
        canInstall = false,
    )

    @Test
    fun instalar_returningWithoutThePermission_explains() {
        // Given
        returnFromSetting { }
        composeRule.launchApp(updates = updates)
        composeRule.dismissSnackbar()
        composeRule.openAbout()

        // When
        composeRule.onNodeWithText("Instalar").performClick()

        // Then
        composeRule.onNodeWithText(EXPLANATION).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(emptyList(), updates.installed) }
    }

    @Test
    fun instalar_withoutThePermission_opensMoldasSetting_andContinuesOnReturningWithIt() {
        // Given
        returnFromSetting { updates.canInstall = true }
        composeRule.launchApp(updates = updates)
        composeRule.dismissSnackbar()
        composeRule.openAbout()

        // When
        composeRule.onNodeWithText("Instalar").performClick()

        // Then
        composeRule.runOnIdle { assertEquals(listOf(Beta2), updates.installed) }
        composeRule.onNodeWithText(EXPLANATION).assertDoesNotExist()
        // Last: Espresso's check waits for the activity's window focus, which a dialog would take.
        val packageUri = "package:${InstrumentationRegistry.getInstrumentation().targetContext
            .packageName}"
        intended(allOf(hasAction(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES), hasData(packageUri)))
    }

    @Test
    fun explanation_tentarNovamente_retriesTheInstall() {
        // Given
        returnFromSetting { }
        composeRule.launchApp(updates = updates)
        composeRule.dismissSnackbar()
        composeRule.openAbout()
        composeRule.onNodeWithText("Instalar").performClick()
        composeRule.onNodeWithText(EXPLANATION).assertIsDisplayed()
        updates.canInstall = true

        // When
        composeRule.onNodeWithText("Tentar novamente").performClick()

        // Then
        composeRule.runOnIdle { assertEquals(listOf(Beta2), updates.installed) }
        composeRule.onNodeWithText(EXPLANATION).assertDoesNotExist()
    }

    /** Stubs Molda's setting, running [onReturn] as the sewist comes back from it. */
    private fun returnFromSetting(onReturn: () -> Unit) {
        intending(hasAction(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)).respondWithFunction {
            onReturn()
            ActivityResult(Activity.RESULT_CANCELED, null)
        }
    }

    private companion object {
        const val EXPLANATION =
            "Para instalar a atualização, permita que o Molda instale apps nas configurações."
    }
}
