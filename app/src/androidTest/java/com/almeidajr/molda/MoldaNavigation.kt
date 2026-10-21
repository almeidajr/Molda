package com.almeidajr.molda

import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.project.ui.ProjectsViewModel
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel
import com.almeidajr.molda.testing.FakeProfileRepository
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.October13Clock
import com.almeidajr.molda.ui.theme.MoldaTheme
import java.time.Clock

/**
 * Shows the app on [profiles] and [projects], at [size] when given, inside [frame] (the theme by
 * default). Recreation restores the app's own content, so call it again after recreating the
 * activity.
 */
internal fun AndroidComposeTestRule<*, MainActivity>.showRoute(
    profiles: ProfileRepository,
    projects: ProjectRepository,
    clock: Clock,
    size: DpSize? = null,
    frame: @Composable (route: @Composable () -> Unit) -> Unit = { MoldaTheme(content = it) },
) = runOnUiThread {
    activity.setContent {
        val profilesViewModel: ProfilesViewModel = viewModel(
            key = "isolatedProfiles",
            factory = viewModelFactory {
                initializer { ProfilesViewModel(profiles, createSavedStateHandle(), clock) }
            },
        )
        val projectsViewModel: ProjectsViewModel = viewModel(
            key = "isolatedProjects",
            factory = viewModelFactory {
                initializer { ProjectsViewModel(projects, clock, createSavedStateHandle()) }
            },
        )
        val calculator: SkirtCalculatorViewModel = viewModel()
        val route = @Composable {
            frame {
                MoldaRoute(
                    calculator = calculator,
                    profiles = profilesViewModel,
                    projects = projectsViewModel,
                )
            }
        }
        if (size == null) {
            route()
        } else {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size), route)
        }
    }
}

/**
 * Shows the app with no stored profiles or projects, at [size] when given, inside [frame] (the
 * theme by default). Recreation restores the app's own content, so call it again after recreating.
 */
internal fun AndroidComposeTestRule<*, MainActivity>.showEmptyApp(
    size: DpSize? = null,
    frame: @Composable (route: @Composable () -> Unit) -> Unit = { MoldaTheme(content = it) },
) = showRoute(
    FakeProfileRepository(),
    FakeProjectRepository(),
    October13Clock,
    size,
    frame,
)

/**
 * Sends a real key press with [metaState] to the focused window once Compose is idle. Key events
 * from Instrumentation bypass Compose's synchronization: sent right after an action, they reach
 * the UI from before it, for example before Back handling was enabled.
 */
internal fun ComposeTestRule.pressKey(keyCode: Int, metaState: Int = 0) {
    waitForIdle()
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP).forEach { action ->
        instrumentation.sendKeySync(KeyEvent(0, 0, action, keyCode, 0, metaState))
    }
}

/** Sends a real Back key to the focused window once Compose is idle. */
internal fun ComposeTestRule.pressBack() = pressKey(KeyEvent.KEYCODE_BACK)

/**
 * Recreates the activity once Compose is idle. Recreation bypasses Compose's synchronization:
 * started right after an action, it saves the state from before that action took effect.
 */
internal fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.recreate() {
    waitForIdle()
    activityRule.scenario.recreate()
}

/** The navigation suite item labelled [label]: Moldes, Projetos, Guias or Perfis. */
internal fun SemanticsNodeInteractionsProvider.destination(
    label: String,
): SemanticsNodeInteraction = onNode(hasText(label) and isSelectable())

/** Selects the top-level destination labelled [label]. */
internal fun SemanticsNodeInteractionsProvider.selectDestination(label: String) {
    destination(label).performClick()
}

/** The top app bar heading reading [title]. */
internal fun SemanticsNodeInteractionsProvider.screenTitle(
    title: String,
): SemanticsNodeInteraction = onNode(hasText(title) and isHeading())

/** Opens the full-circle skirt calculator from the Moldes catalog the app starts on. */
internal fun SemanticsNodeInteractionsProvider.openSkirtCalculator() {
    onNode(hasText(SkirtTitle) and !isHeading()).performClick()
    screenTitle(SkirtTitle).assertExists()
}

/** Shows the skirt calculator: selects Moldes when needed, then opens the skirt from the catalog. */
internal fun SemanticsNodeInteractionsProvider.showSkirtCalculator() {
    showDestination("Moldes")
    if (onAllNodes(hasText(SkirtTitle) and isHeading()).fetchSemanticsNodes().isEmpty()) {
        openSkirtCalculator()
    }
}

/** Shows Perfis, keeping any open editor: re-selecting it would return to its root. */
internal fun SemanticsNodeInteractionsProvider.showProfiles() = showDestination("Perfis")

private fun SemanticsNodeInteractionsProvider.showDestination(label: String) {
    if (onAllNodes(hasText(label) and isSelectable() and isSelected()).fetchSemanticsNodes()
            .isEmpty()
    ) {
        selectDestination(label)
    }
}

/** The skirt's catalog entry and calculator title as the app renders it. */
internal val SkirtTitle: String
    get() = InstrumentationRegistry.getInstrumentation().targetContext.getString(
        R.string.skirt_title,
    )
