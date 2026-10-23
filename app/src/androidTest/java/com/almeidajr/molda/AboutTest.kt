package com.almeidajr.molda

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.di.ClockModule
import com.almeidajr.molda.di.RepositoryModule
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.testing.FakeProfileRepository
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.October13Clock
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.time.Clock
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The About screen, opened from every root top app bar, and its open-source licences. It runs on
 * the app's own content, since only [MainActivity] reads the installed version name, on empty
 * fakes.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@UninstallModules(RepositoryModule::class, ClockModule::class)
class AboutTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @BindValue @JvmField
    val profileRepository: ProfileRepository = FakeProfileRepository()

    @BindValue @JvmField
    val projectRepository: ProjectRepository = FakeProjectRepository()

    @BindValue @JvmField
    val clock: Clock = October13Clock

    @Test
    fun aboutAction_onEveryRoot_opensAbout_andBackReturnsThere() {
        listOf(
            "Moldes" to "Moldes",
            "Projetos" to "Projetos",
            "Guias" to "Guias",
            "Perfis" to "Perfis de medidas",
        ).forEach { (destination, title) ->
            // Given
            composeRule.selectDestination(destination)

            // When
            aboutAction()
                .assertHasClickAction()
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
                .performClick()

            // Then
            composeRule.screenTitle("Sobre o Molda").assertIsDisplayed()
            // Pushed screens carry an up arrow, not the About action.
            aboutAction().assertDoesNotExist()
            // Profile management's FAB belongs to its own screens.
            composeRule.onNodeWithContentDescription("Novo perfil").assertDoesNotExist()
            composeRule.pressBack()
            composeRule.screenTitle(title).assertIsDisplayed()
            composeRule.destination(destination).assertIsSelected()
        }
    }

    @Test
    fun about_showsAppNameAndInstalledVersion() {
        // Given
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName

        // When
        aboutAction().performClick()

        // Then
        composeRule.onNode(hasText("Molda") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Versão").assertIsDisplayed()
        composeRule.onNodeWithText(checkNotNull(versionName)).assertIsDisplayed()
    }

    @Test
    fun pushedScreen_hasNoAboutAction() {
        // When
        composeRule.openSkirtCalculator()

        // Then
        aboutAction().assertDoesNotExist()
    }

    @Test
    fun licencesRow_opensOpenSourceLicences_andUpReturnsToAbout() {
        // Given
        aboutAction().performClick()

        // When
        composeRule.onNodeWithText("Licenças de código aberto").performClick()

        // Then
        composeRule.screenTitle("Licenças de código aberto").assertIsDisplayed()
        // A dependency the app ships with, listed from its build-time metadata.
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Hilt Android"))
        composeRule.onNodeWithText("Hilt Android").assertIsDisplayed()
        composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Voltar")),
        ).performClick()
        composeRule.screenTitle("Sobre o Molda").assertIsDisplayed()
    }

    private fun aboutAction() = composeRule.onNode(
        SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription,
            listOf("Sobre o Molda"),
        ),
    )
}
