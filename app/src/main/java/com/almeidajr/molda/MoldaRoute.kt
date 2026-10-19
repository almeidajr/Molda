package com.almeidajr.molda

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.almeidajr.molda.about.ui.AboutScreen
import com.almeidajr.molda.about.ui.LicencesScreen
import com.almeidajr.molda.guide.ui.GuideLibrary
import com.almeidajr.molda.pattern.ui.PatternCatalog
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.ui.ProfileCreateAction
import com.almeidajr.molda.profile.ui.ProfileEditor
import com.almeidajr.molda.profile.ui.ProfileForm
import com.almeidajr.molda.profile.ui.ProfilePicker
import com.almeidajr.molda.profile.ui.ProfilesContent
import com.almeidajr.molda.profile.ui.ProfilesState
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.profile.ui.profileWaistText
import com.almeidajr.molda.project.ui.ProjectsScreen
import com.almeidajr.molda.skirt.ui.FabricFoldingPlayer
import com.almeidajr.molda.skirt.ui.SkirtCalculatorScreen
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel
import com.almeidajr.molda.ui.adaptive.HingeSafeContent
import com.almeidajr.molda.ui.adaptive.ReadablePane
import com.almeidajr.molda.ui.adaptive.rememberReadableListDetailStrategy
import com.almeidajr.molda.ui.navigation.MoldaTopAppBar
import com.almeidajr.molda.ui.navigation.NavigationLabel
import com.almeidajr.molda.ui.navigation.TopLevelBackStacks
import com.almeidajr.molda.ui.navigation.WindowOverlaySceneStrategy
import com.almeidajr.molda.ui.navigation.WindowOverlaySceneStrategy.Companion.windowOverlay
import com.almeidajr.molda.ui.navigation.materialTransition
import com.almeidajr.molda.ui.navigation.rememberTopLevelBackStacks
import kotlinx.serialization.Serializable

/** The Moldes root: the catalog of pattern drafts. */
@Serializable
private data object PatternCatalogKey : NavKey

/** The Projetos root. */
@Serializable
private data object ProjectsKey : NavKey

/** The Guias root: the library of guides. */
@Serializable
private data object GuideLibraryKey : NavKey

/** The Perfis root: profile management. */
@Serializable
private data object ProfileManagementKey : NavKey

@Serializable
private data object SkirtCalculatorKey : NavKey

@Serializable
private data object FabricFoldingTutorialKey : NavKey

/** About Molda, pushed on the stack of the destination whose root opened it. */
@Serializable
private data object AboutKey : NavKey

/** The open-source licences, opened from About. */
@Serializable
private data object LicencesKey : NavKey

/** The profile an editor works on. */
@Serializable
private sealed interface EditedProfile {
    @Serializable
    data object New : EditedProfile

    @Serializable
    data class Existing(val id: ProfileId) : EditedProfile
}

/** The open profile editor. */
@Serializable
private data class ProfileEditorKey(val profile: EditedProfile) : NavKey

/** The profile picker sheet over the calculator. */
@Serializable
private data object ProfilePickerKey : NavKey

/** What confirming the discard of a dirty editor does next. */
@Serializable
private sealed interface AfterDiscard {
    @Serializable
    data object CloseEditor : AfterDiscard

    @Serializable
    data class OpenEditor(val profile: EditedProfile) : AfterDiscard
}

/** Asks to discard the dirty editor, then does [afterDiscard]. */
@Serializable
private data class DiscardChangesKey(val afterDiscard: AfterDiscard) : NavKey

/** The top-level destinations, in navigation order; the first is the start destination. */
private enum class Destination(
    val root: NavKey,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
) {
    Moldes(
        PatternCatalogKey,
        R.string.navigation_moldes,
        R.drawable.ic_design_services,
        R.drawable.ic_design_services_filled,
    ),
    Projetos(
        ProjectsKey,
        R.string.navigation_projetos,
        R.drawable.ic_folder,
        R.drawable.ic_folder_filled,
    ),
    Guias(
        GuideLibraryKey,
        R.string.navigation_guias,
        R.drawable.ic_menu_book,
        R.drawable.ic_menu_book_filled,
    ),
    Perfis(
        ProfileManagementKey,
        R.string.navigation_perfis,
        R.drawable.ic_group,
        R.drawable.ic_group_filled,
    ),
}

private val TopLevelRoots: List<NavKey> = Destination.entries.map { it.root }

/** Windows narrower than this show a navigation bar; wider ones show a rail. */
private val NavigationRailMinWidth = 600.dp

// The back stacks and every transition that edits them read together in one place.
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun MoldaRoute(
    modifier: Modifier = Modifier,
    calculator: SkirtCalculatorViewModel = hiltViewModel(),
    profiles: ProfilesViewModel = hiltViewModel(),
) {
    val profileState by profiles.state.collectAsStateWithLifecycle()
    val calculatorState by calculator.state.collectAsStateWithLifecycle()
    val navigation = rememberTopLevelBackStacks(TopLevelRoots)
    val moldes = navigation.stackOf(PatternCatalogKey)
    val perfis = navigation.stackOf(ProfileManagementKey)
    // ProfilesViewModel owns whether the editor is open (a save or delete also closes it).
    LaunchedEffect(profileState.isEditorOpen, profileState.editingId, perfis.size) {
        syncProfileEditor(perfis, profileState.form)
    }
    val requestEditor: (EditedProfile) -> Unit = { profile ->
        requestProfileEditor(profileState, profiles, profile) {
            perfis.add(DiscardChangesKey(AfterDiscard.OpenEditor(profile)))
        }
    }
    val snackbar = remember { SnackbarHostState() }
    var snackbarHeight by remember { mutableIntStateOf(0) }
    val snackbarInset = with(LocalDensity.current) { snackbarHeight.toDp() }
    // shortcut: the picker's "Criar perfil" opens the editor in Perfis, so closing that editor
    // goes back to the picker, still open on the Moldes stack; #90 keeps the editor in Moldes.
    val returnToPickerIfOpen: () -> Unit = {
        if (navigation.selected == ProfileManagementKey && moldes.last() == ProfilePickerKey) {
            navigation.select(PatternCatalogKey)
        }
    }
    val finishEditor = {
        profiles.cancel()
        returnToPickerIfOpen()
    }
    val cancelEditor: () -> Unit = {
        if (profileState.isDirty) {
            perfis.add(DiscardChangesKey(AfterDiscard.CloseEditor))
        } else {
            finishEditor()
        }
    }
    val select: (Destination) -> Unit = { destination ->
        // Profiles own their open editor, so returning Perfis to its root closes it the usual way.
        if (destination.root == navigation.selected && perfis.last() is ProfileEditorKey) {
            if (!profileState.isSaving) cancelEditor()
        } else {
            navigation.select(destination.root)
        }
    }
    val openAbout: () -> Unit = { navigation.current.add(AboutKey) }
    val routes = entryProvider {
        entry<PatternCatalogKey> {
            PatternCatalog(onOpenSkirt = { moldes.add(SkirtCalculatorKey) }, onAbout = openAbout)
        }
        entry<ProjectsKey> {
            ProjectsScreen(onSeePatterns = {
                navigation.select(PatternCatalogKey)
            }, onAbout = openAbout)
        }
        entry<GuideLibraryKey> { GuideLibrary(onAbout = openAbout) }
        entry<AboutKey> {
            val context = LocalContext.current
            val versionName = remember(context) {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
            }
            AboutScreen(
                versionName = versionName,
                onUp = navigation::back,
                onOpenLicences = { navigation.current.add(LicencesKey) },
            )
        }
        entry<LicencesKey> { LicencesScreen(onUp = navigation::back) }
        entry<SkirtCalculatorKey> {
            CalculatorDestination(
                calculator,
                onUp = navigation::back,
                onUseProfile = { moldes.add(ProfilePickerKey) },
                onOpenTutorial = { moldes.add(FabricFoldingTutorialKey) },
            )
        }
        entry<FabricFoldingTutorialKey> {
            val close: () -> Unit = {
                moldes.remove(FabricFoldingTutorialKey)
            }
            val radii = calculatorState.radii
            if (radii != null) {
                FabricFoldingPlayer(radii, close)
            } else {
                // Results do not survive process death; show the calculator.
                LaunchedEffect(Unit) { close() }
            }
        }
        entry<ProfileManagementKey>(
            metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = {
                ProfilePane {
                    Text(
                        stringResource(R.string.profile_select_or_create),
                        Modifier.padding(16.dp),
                    )
                }
            }),
        ) {
            Column(Modifier.fillMaxSize()) {
                MoldaTopAppBar(stringResource(R.string.profile_manage), onAbout = openAbout)
                ProfilePane { ProfileList(profiles, requestEditor) }
            }
        }
        entry<ProfilePickerKey>(metadata = windowOverlay()) {
            AppProfilePicker(
                profiles,
                calculator,
                onDismiss = { moldes.remove(ProfilePickerKey) },
                onCreate = {
                    profiles.startCreating()
                    navigation.select(ProfileManagementKey)
                },
            )
        }
        entry<DiscardChangesKey>(metadata = windowOverlay()) { key ->
            DiscardDialog(onDismiss = { perfis.remove(key) }, onDiscard = {
                perfis.remove(key)
                when (val afterDiscard = key.afterDiscard) {
                    AfterDiscard.CloseEditor -> finishEditor()

                    is AfterDiscard.OpenEditor -> {
                        profiles.cancel()
                        profiles.openEditor(afterDiscard.profile)
                    }
                }
            })
        }
        entry<ProfileEditorKey>(
            metadata = ListDetailSceneStrategy.detailPane(),
        ) {
            ProfilePane {
                ProfileEditor(
                    profileState,
                    profiles::edit,
                    profiles::save,
                    cancelEditor,
                    Modifier,
                    profiles.today,
                    onRetry = profiles::load,
                )
            }
        }
    }
    ProfileSaveFeedback(profiles, snackbar, onEditorClosed = returnToPickerIfOpen)
    val isManaging = navigation.selected == ProfileManagementKey &&
        navigation.current.last() !in setOf(AboutKey, LicencesKey)
    val isTutorialOpen = navigation.current.last() == FabricFoldingTutorialKey
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        val isCompact = maxWidth < NavigationRailMinWidth
        val suiteState = rememberNavigationSuiteScaffoldState()
        // The guide player's own step bar is the only bottom control on a compact window.
        LaunchedEffect(isCompact && isTutorialOpen) {
            if (isCompact && isTutorialOpen) suiteState.hide() else suiteState.show()
        }
        val suiteType = if (isCompact) {
            NavigationSuiteType.ShortNavigationBarCompact
        } else {
            NavigationSuiteType.WideNavigationRailCollapsed
        }
        NavigationSuiteScaffold(
            navigationItems = {
                NavigationItems(navigation, suiteType, select)
            },
            navigationSuiteType = suiteType,
            state = suiteState,
        ) {
            // The player is one column even on a wide window: keep it off a book-posture fold only.
            HingeSafeContent(Modifier.fillMaxSize(), isSingleColumn = isTutorialOpen) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val listDetail = rememberReadableListDetailStrategy<NavKey>(maxWidth)
                    val wide = listDetail != null
                    val fabPosition = if (wide) FabPosition.Start else FabPosition.End
                    val overlayStrategy = remember { WindowOverlaySceneStrategy<NavKey>() }
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = {
                            SnackbarHost(
                                snackbar,
                                Modifier.onSizeChanged { snackbarHeight = it.height },
                            )
                        },
                        floatingActionButtonPosition = fabPosition,
                        floatingActionButton = {
                            if (isManaging && (!profileState.isEditorOpen || wide)) {
                                ProfileCreateAction(
                                    profileState,
                                    { requestEditor(EditedProfile.New) },
                                )
                            }
                        },
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    ) { padding ->
                        NavDisplay(
                            entries = navigation.entries(routes),
                            // Keep bottom actions reachable while storage feedback is visible.
                            modifier = Modifier.padding(padding).padding(bottom = snackbarInset),
                            // A narrow window shows one destination at a time, in a single pane.
                            sceneStrategies = listOfNotNull(overlayStrategy, listDetail),
                            transitionSpec = { materialTransition(density, isForward = true) },
                            popTransitionSpec = { materialTransition(density, isForward = false) },
                            onBack = {
                                if (navigation.current.last() !is ProfileEditorKey) {
                                    navigation.back()
                                } else if (!profileState.isSaving) {
                                    cancelEditor()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** One item per destination; a filled glyph, label colour and selected state mark the current. */
@Composable
private fun NavigationItems(
    navigation: TopLevelBackStacks,
    suiteType: NavigationSuiteType,
    onSelect: (Destination) -> Unit,
) {
    Destination.entries.forEach { destination ->
        val isSelected = navigation.selected == destination.root
        NavigationSuiteItem(
            selected = isSelected,
            onClick = { onSelect(destination) },
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

private fun ProfilesViewModel.openEditor(profile: EditedProfile) = when (profile) {
    EditedProfile.New -> startCreating()
    is EditedProfile.Existing -> startEditing(profile.id)
}

/** Shows the editor [form] has open above profile management, and drops any other editor. */
private fun syncProfileEditor(perfis: NavBackStack<NavKey>, form: ProfileForm) {
    val profile = form.editingId?.let(EditedProfile::Existing) ?: EditedProfile.New
    val editor = ProfileEditorKey(profile).takeIf { form.isEditorOpen }
    perfis.removeAll { it is ProfileEditorKey && it != editor }
    if (editor != null && perfis.last() == ProfileManagementKey) perfis.add(editor)
}

@Composable
private fun CalculatorDestination(
    calculator: SkirtCalculatorViewModel,
    onUp: () -> Unit,
    onUseProfile: () -> Unit,
    onOpenTutorial: () -> Unit,
) {
    val state by calculator.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        MoldaTopAppBar(stringResource(R.string.skirt_title), onUp = onUp)
        SkirtCalculatorScreen(
            state,
            calculator::edit,
            calculator::calculate,
            Modifier.weight(1f),
            onBlur = calculator::blur,
            onUseProfile = onUseProfile,
            onOpenTutorial = onOpenTutorial,
        )
    }
}

@Composable
private fun ProfileList(profiles: ProfilesViewModel, requestEditor: (EditedProfile) -> Unit) {
    val state by profiles.state.collectAsStateWithLifecycle()
    ProfilesContent(
        state,
        profiles::load,
        { requestEditor(EditedProfile.Existing(it)) },
        profiles::delete,
        Modifier.padding(16.dp),
    )
}

@Composable
private fun ProfilePane(content: @Composable () -> Unit) {
    Box(Modifier.imePadding()) { ReadablePane(content) }
}

@Composable
private fun DiscardDialog(onDismiss: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.profile_discard_title)) },
        text = { Text(stringResource(R.string.profile_discard_body)) },
        confirmButton = {
            TextButton(onClick = {
                onDiscard()
            }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.profile_discard))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
            }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.profile_keep_editing))
            }
        },
    )
}

@Composable
private fun AppProfilePicker(
    profiles: ProfilesViewModel,
    calculator: SkirtCalculatorViewModel,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
) {
    val state by profiles.state.collectAsStateWithLifecycle()
    ProfilePicker(state, onDismiss, profiles::load, onSelect = {
        val measurements = it.measurements
        calculator.selectProfile(profileWaistText(measurements.waist), measurements.name.text)
        onDismiss()
    }, onCreate = onCreate)
}

@Composable
private fun ProfileSaveFeedback(
    profiles: ProfilesViewModel,
    snackbar: SnackbarHostState,
    onEditorClosed: () -> Unit,
) {
    val state by profiles.state.collectAsStateWithLifecycle()
    val message = state.savedMessage?.let { stringResource(it) }
    LaunchedEffect(message) {
        message?.let {
            if (!state.isEditorOpen) onEditorClosed()
            snackbar.showSnackbar(it)
            profiles.dismissMessage()
        }
    }
}

private fun requestProfileEditor(
    state: ProfilesState,
    profiles: ProfilesViewModel,
    profile: EditedProfile,
    onDiscard: () -> Unit,
) {
    if (state.isSaving || state.isLoading || state.loadError != null) return
    if (state.isEditorOpen && state.isDirty) onDiscard() else profiles.openEditor(profile)
}
