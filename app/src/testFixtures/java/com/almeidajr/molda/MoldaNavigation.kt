package com.almeidajr.molda

import android.animation.ValueAnimator
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.DpSize
import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.project.domain.ProjectRepository
import com.almeidajr.molda.ui.theme.MoldaTheme
import com.almeidajr.molda.update.domain.UpdateRepository
import java.time.Clock

/** Wraps the app's route, for example in a theme or a configuration override. */
typealias RouteFrame = @Composable (route: @Composable () -> Unit) -> Unit

/**
 * Seeds the app's [profiles], [projects], [clock] and [updates], then starts it showing the route
 * inside [frame] (the theme by default), at [size] when given. A null [frame] keeps the app's own
 * content, which alone reads the installed version name. Once launched, the bindings cannot
 * change, and [recreate] shows the same content again.
 */
fun <A : ComponentActivity> AndroidComposeTestRule<AppLauncher<A>, A>.launchApp(
    profiles: ProfileRepository = TestBindings.profiles,
    projects: ProjectRepository = TestBindings.projects,
    clock: Clock = TestBindings.clock,
    size: DpSize? = null,
    frame: RouteFrame? = DefaultFrame,
    updates: UpdateRepository = TestBindings.updates,
) {
    require(frame != null || size == null) { "The app's own content takes no size" }
    TestBindings.profiles = profiles
    TestBindings.projects = projects
    TestBindings.updates = updates
    TestBindings.clock = clock
    activityRule.launch(frame?.let { routeContent(size, it) })
}

/**
 * Shows the route inside [frame] (the theme by default), at [size] when given, from now on:
 * recreated activities show it too.
 */
fun <A : ComponentActivity> AndroidComposeTestRule<AppLauncher<A>, A>.showRoute(
    size: DpSize? = null,
    frame: RouteFrame = DefaultFrame,
) = activityRule.show(routeContent(size, frame))

/**
 * Recreates the activity once Compose is idle; it shows the content the test launched or last
 * showed. Recreation bypasses Compose's synchronization: started right after an action, it saves
 * the state from before that action took effect.
 */
fun <A : ComponentActivity> AndroidComposeTestRule<AppLauncher<A>, A>.recreate() {
    waitForIdle()
    activityRule.scenario.recreate()
}

private val DefaultFrame: RouteFrame = { MoldaTheme(content = it) }

private fun routeContent(size: DpSize?, frame: RouteFrame): @Composable () -> Unit = {
    val route = @Composable { frame { MoldaRoute() } }
    if (size == null) {
        route()
    } else {
        DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size), route)
    }
}

/**
 * Sends a real key press with [metaState] to the focused window once Compose is idle. Key events
 * from Instrumentation bypass Compose's synchronization: sent right after an action, they reach
 * the UI from before it, for example before Back handling was enabled. A navigation or typing key
 * pressed in touch mode leaves it for every later test too: send those keys only after
 * [KeyboardModeRule.enter].
 */
fun ComposeTestRule.pressKey(keyCode: Int, metaState: Int = 0) {
    waitForIdle()
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP).forEach { action ->
        instrumentation.sendKeySync(KeyEvent(0, 0, action, keyCode, 0, metaState))
    }
}

/** Sends a real Back key to the focused window once Compose is idle. */
fun ComposeTestRule.pressBack() = pressKey(KeyEvent.KEYCODE_BACK)

/** The navigation suite item labeled [label]: Moldes, Projetos, Guias or Perfis. */
fun SemanticsNodeInteractionsProvider.destination(label: String): SemanticsNodeInteraction =
    onNode(hasText(label) and isSelectable())

/** Selects the top-level destination labeled [label]. */
fun SemanticsNodeInteractionsProvider.selectDestination(label: String) {
    destination(label).performClick()
}

/**
 * Clicks the node whose text or content description is [label], scrolling to it when hidden. Pass
 * [useUnmergedTree] to tap the label itself rather than the merged node around it.
 */
fun SemanticsNodeInteractionsProvider.click(label: String, useUnmergedTree: Boolean = false) {
    val node = onNode(hasText(label) or hasContentDescription(label), useUnmergedTree)
    if (!node.isDisplayed()) node.performScrollTo()
    node.performClick()
}

/**
 * Clicks the enabled node labeled [label] in the open dialog. A semantics click needs no window
 * focus, unlike a touch on a dialog still attaching.
 */
fun SemanticsNodeInteractionsProvider.clickInDialog(label: String) {
    onNode(hasText(label) and hasAnyAncestor(isDialog()) and isEnabled())
        .performSemanticsAction(SemanticsActions.OnClick)
}

/** The text field of the open dialog, such as a project's name or the calendar's typed date. */
fun SemanticsNodeInteractionsProvider.dialogTextField(): SemanticsNodeInteraction =
    onNode(hasSetTextAction() and hasAnyAncestor(isDialog()))

/**
 * Asserts that no text field holds focus once Compose is idle, so nothing can open the keyboard:
 * a Compose field opens it only while focused. Reading the keyboard's insets instead would race the
 * IME window, which shows after Compose is idle.
 */
fun SemanticsNodeInteractionsProvider.assertNoTextFieldFocused() {
    onAllNodes(hasSetTextAction() and isFocused()).assertCountEquals(0)
}

/** The snackbar shown, the only node a screen reader can dismiss. */
fun SemanticsNodeInteractionsProvider.snackbar(): SemanticsNodeInteraction =
    onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))

/** Dismisses the snackbar shown, as a screen reader does. */
fun SemanticsNodeInteractionsProvider.dismissSnackbar() {
    snackbar().performSemanticsAction(SemanticsActions.Dismiss)
}

/** The root top app bar's About action. */
fun SemanticsNodeInteractionsProvider.aboutAction(): SemanticsNodeInteraction = onNode(
    SemanticsMatcher.expectValue(
        SemanticsProperties.ContentDescription,
        listOf("Sobre o Molda"),
    ),
)

/** Opens About from a root screen. */
fun SemanticsNodeInteractionsProvider.openAbout() {
    aboutAction().performClick()
}

/** The top app bar heading reading [title]. */
fun SemanticsNodeInteractionsProvider.screenTitle(title: String): SemanticsNodeInteraction =
    onNode(hasText(title) and isHeading())

/** Opens the full-circle skirt calculator from the Moldes catalog the app starts on. */
fun SemanticsNodeInteractionsProvider.openSkirtCalculator() {
    onNode(hasText(SkirtTitle) and !isHeading()).performClick()
    screenTitle(SkirtTitle).assertExists()
}

/**
 * Shows the skirt calculator: selects Moldes when needed, then opens the skirt from the catalog.
 */
fun SemanticsNodeInteractionsProvider.showSkirtCalculator() {
    showDestination("Moldes")
    if (onNode(hasText(SkirtTitle) and isHeading()).isNotDisplayed()) openSkirtCalculator()
}

/**
 * Opens the fabric-folding guide from the skirt's results. With motion on a playing drawing keeps
 * Compose busy, so the test clock drives the guide from here: each wait for a drawing advances it.
 */
fun AndroidComposeTestRule<*, *>.openGuideFromResults() {
    val entry = onNodeWithText("Guia de dobra do tecido").performScrollTo()
    if (ValueAnimator.areAnimatorsEnabled()) mainClock.autoAdvance = false
    entry.performClick()
    // Past the transition into the player.
    mainClock.advanceTimeBy(1_000)
}

/** Shows Perfis, keeping any open editor: re-selecting it would return to its root. */
fun SemanticsNodeInteractionsProvider.showProfiles() = showDestination("Perfis")

private fun SemanticsNodeInteractionsProvider.showDestination(label: String) {
    if (onNode(hasText(label) and isSelectable() and isSelected()).isNotDisplayed()) {
        selectDestination(label)
    }
}

/** The skirt's catalog entry and calculator title as the app renders it. */
val SkirtTitle: String
    get() = InstrumentationRegistry.getInstrumentation().targetContext.getString(
        R.string.skirt_title,
    )
