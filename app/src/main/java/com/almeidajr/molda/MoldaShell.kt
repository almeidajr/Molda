package com.almeidajr.molda

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.almeidajr.molda.guide.navigation.ExampleFabricFoldingGuideKey
import com.almeidajr.molda.project.navigation.ProjectEditorGuideKey
import com.almeidajr.molda.project.navigation.ProjectGuideKey
import com.almeidajr.molda.skirt.navigation.FabricFoldingGuideKey
import com.almeidajr.molda.ui.adaptive.HingeSafeContent
import com.almeidajr.molda.ui.adaptive.LocalIsTwoPane
import com.almeidajr.molda.ui.adaptive.rememberReadableListDetailStrategy
import com.almeidajr.molda.ui.navigation.LocalTitleFocus
import com.almeidajr.molda.ui.navigation.NavigationLabel
import com.almeidajr.molda.ui.navigation.Navigator
import com.almeidajr.molda.ui.navigation.TitleFocus
import com.almeidajr.molda.ui.navigation.WindowOverlaySceneStrategy
import com.almeidajr.molda.ui.navigation.materialTransition

/** Windows narrower than this show a navigation bar; wider ones show a rail. */
private val NavigationRailMinWidth = 600.dp

/** The guide players, which take the whole window. */
private val NavKey.isCalculatedGuide: Boolean
    get() = this is FabricFoldingGuideKey || this is ProjectEditorGuideKey ||
        this is ProjectGuideKey

/**
 * The navigation suite around the selected destination's entries from [entryProvider], with
 * storage feedback in [snackbar] and the [floatingActionButton] for whether panes sit side by side.
 */
@Composable
internal fun MoldaShell(
    navigator: Navigator,
    titleFocus: TitleFocus,
    snackbar: SnackbarHostState,
    onSelect: (Destination) -> Unit,
    onBack: () -> Unit,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    modifier: Modifier = Modifier,
    floatingActionButton: @Composable (isTwoPane: Boolean) -> Unit,
) {
    val top = navigator.state.current.last()
    val isGuideOpen = top.isCalculatedGuide
    val isAnyGuideOpen = isGuideOpen || top == ExampleFabricFoldingGuideKey
    BoxWithConstraints(modifier.fillMaxSize()) {
        val isCompact = maxWidth < NavigationRailMinWidth
        val suiteState = rememberNavigationSuiteScaffoldState()
        // The guide player's own step bar is the only bottom control on a compact window.
        LaunchedEffect(isCompact && isAnyGuideOpen) {
            if (isCompact && isAnyGuideOpen) suiteState.hide() else suiteState.show()
        }
        val suiteType = if (isCompact) {
            NavigationSuiteType.ShortNavigationBarCompact
        } else {
            NavigationSuiteType.WideNavigationRailCollapsed
        }
        NavigationSuiteScaffold(
            navigationItems = { NavigationItems(navigator, suiteType, onSelect) },
            navigationSuiteType = suiteType,
            state = suiteState,
        ) {
            // The player is one column even on a wide window: keep it off a book-posture fold only.
            HingeSafeContent(Modifier.fillMaxSize(), isSingleColumn = isGuideOpen) {
                ShellContent(
                    navigator,
                    titleFocus,
                    snackbar,
                    onBack,
                    entryProvider,
                    floatingActionButton,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun ShellContent(
    navigator: Navigator,
    titleFocus: TitleFocus,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    floatingActionButton: @Composable (isTwoPane: Boolean) -> Unit,
) {
    var snackbarHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val snackbarInset = with(density) { snackbarHeight.toDp() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val listDetail = rememberReadableListDetailStrategy<NavKey>(maxWidth)
        val isTwoPane = listDetail != null
        val overlayStrategy = remember { WindowOverlaySceneStrategy<NavKey>() }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = {
                SnackbarHost(snackbar, Modifier.onSizeChanged { snackbarHeight = it.height })
            },
            floatingActionButtonPosition = if (isTwoPane) FabPosition.Start else FabPosition.End,
            floatingActionButton = { floatingActionButton(isTwoPane) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            CompositionLocalProvider(
                LocalIsTwoPane provides isTwoPane,
                LocalTitleFocus provides titleFocus,
            ) {
                NavDisplay(
                    entries = navigator.state.entries(entryProvider),
                    // Keep bottom actions reachable while storage feedback is visible.
                    modifier = Modifier
                        .padding(padding)
                        .padding(bottom = snackbarInset),
                    // A narrow window shows one destination at a time, in a single pane.
                    sceneStrategies = listOfNotNull(overlayStrategy, listDetail),
                    transitionSpec = { materialTransition(density, isForward = true) },
                    popTransitionSpec = { materialTransition(density, isForward = false) },
                    predictivePopTransitionSpec = {
                        materialTransition(density, isForward = false)
                    },
                    onBack = onBack,
                )
            }
        }
    }
}

/** One item per destination; a filled glyph, label colour and selected state mark the current. */
@Composable
private fun NavigationItems(
    navigator: Navigator,
    suiteType: NavigationSuiteType,
    onSelect: (Destination) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val first = remember { FocusRequester() }
    val last = remember { FocusRequester() }
    Destination.entries.forEach { destination ->
        val isSelected = navigator.state.selected == destination.root
        val edge = when (destination) {
            Destination.entries.first() -> Modifier.focusRequester(first)
            Destination.entries.last() -> Modifier.focusRequester(last)
            else -> Modifier
        }
        NavigationSuiteItem(
            selected = isSelected,
            onClick = { onSelect(destination) },
            // One focus group: arrow keys move between items, Tab and Shift+Tab leave them.
            modifier = edge.onPreviewKeyEvent { event ->
                val isTab = event.key == Key.Tab && event.type == KeyEventType.KeyDown
                if (isTab) {
                    val isBackward = event.isShiftPressed
                    (if (isBackward) first else last).requestFocus()
                    focusManager.moveFocus(
                        if (isBackward) FocusDirection.Previous else FocusDirection.Next,
                    )
                }
                isTab
            },
            icon = {
                Icon(
                    painterResource(if (isSelected) destination.selectedIcon else destination.icon),
                    contentDescription = null,
                )
            },
            label = { NavigationLabel(stringResource(destination.label)) },
            navigationSuiteType = suiteType,
        )
    }
}
