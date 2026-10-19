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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.almeidajr.molda.about.ui.AboutScreen
import com.almeidajr.molda.about.ui.LicencesScreen
import com.almeidajr.molda.guide.ui.GuideLibrary
import com.almeidajr.molda.pattern.ui.PatternCatalog
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.ui.ProfileCreateAction
import com.almeidajr.molda.profile.ui.ProfileEditor
import com.almeidajr.molda.profile.ui.ProfileForm
import com.almeidajr.molda.profile.ui.ProfilePicker
import com.almeidajr.molda.profile.ui.ProfilesContent
import com.almeidajr.molda.profile.ui.ProfilesState
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.profile.ui.profileWaistText
import com.almeidajr.molda.project.domain.MeasurementSource
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.project.ui.DeleteProjectDialog
import com.almeidajr.molda.project.ui.ProjectSaveFeedback
import com.almeidajr.molda.project.ui.ProjectsScreen
import com.almeidajr.molda.project.ui.ProjectsViewModel
import com.almeidajr.molda.project.ui.RenameProjectDialog
import com.almeidajr.molda.project.ui.SaveProjectDialog
import com.almeidajr.molda.skirt.ui.ExampleSkirtRadii
import com.almeidajr.molda.skirt.ui.FabricFoldingPlayer
import com.almeidajr.molda.skirt.ui.SkirtCalculatorScreen
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel
import com.almeidajr.molda.ui.adaptive.HingeSafeContent
import com.almeidajr.molda.ui.adaptive.ReadablePane
import com.almeidajr.molda.ui.adaptive.rememberReadableListDetailStrategy
import com.almeidajr.molda.ui.navigation.CatalogSelection
import com.almeidajr.molda.ui.navigation.LocalTitleFocus
import com.almeidajr.molda.ui.navigation.MoldaTopAppBar
import com.almeidajr.molda.ui.navigation.NavigationLabel
import com.almeidajr.molda.ui.navigation.TitleFocus
import com.almeidajr.molda.ui.navigation.TopLevelBackStacks
import com.almeidajr.molda.ui.navigation.WindowOverlaySceneStrategy
import com.almeidajr.molda.ui.navigation.WindowOverlaySceneStrategy.Companion.windowOverlay
import com.almeidajr.molda.ui.navigation.materialTransition
import com.almeidajr.molda.ui.navigation.rememberTopLevelBackStacks
import java.time.LocalDate
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

/** The fabric-folding guide on the calculated radii, opened from a result. */
@Serializable
private data object FabricFoldingGuideKey : NavKey

/** The fabric-folding guide on example dimensions, opened from Guias. */
@Serializable
private data object ExampleFabricFoldingGuideKey : NavKey

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

/** A new profile's editor, opened from the picker on the Moldes stack. */
@Serializable
private data object PickerProfileEditorKey : NavKey

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

    @Serializable
    data object OpenPickerEditor : AfterDiscard

    @Serializable
    data object CalculateOwn : AfterDiscard
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

/** Whether the content shows list and detail panes side by side. */
private val LocalIsTwoPane = staticCompositionLocalOf { false }

// The back stacks and every transition that edits them read together in one place.
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun MoldaRoute(
    modifier: Modifier = Modifier,
    versionName: String = "",
    calculator: SkirtCalculatorViewModel = hiltViewModel(),
    profiles: ProfilesViewModel = hiltViewModel(),
    projects: ProjectsViewModel = hiltViewModel(),
) {
    val profileState by profiles.state.collectAsStateWithLifecycle()
    val projectState by projects.state.collectAsStateWithLifecycle()
    val calculatorState by calculator.state.collectAsStateWithLifecycle()
    val titleFocus = remember { TitleFocus() }
    val navigation = rememberTopLevelBackStacks(TopLevelRoots, onSwitch = titleFocus::request)
    val moldes = navigation.stackOf(PatternCatalogKey)
    val guias = navigation.stackOf(GuideLibraryKey)
    val perfis = navigation.stackOf(ProfileManagementKey)
    // ProfilesViewModel owns whether the editor is open (a save or delete also closes it).
    LaunchedEffect(profileState.isEditorOpen, profileState.editingId, perfis.size, moldes.size) {
        syncProfileEditor(navigation, profileState.form)
    }
    val openEditor: (EditedProfile) -> Unit = { profile ->
        navigation.remove(PickerProfileEditorKey)
        profiles.openEditor(profile)
    }
    val requestEditor: (EditedProfile) -> Unit = { profile ->
        requestProfileEditor(profileState, onOpen = { openEditor(profile) }) {
            navigation.push(DiscardChangesKey(AfterDiscard.OpenEditor(profile)))
        }
    }
    val openPickerEditor: () -> Unit = {
        navigation.removeAll { it is ProfileEditorKey }
        profiles.startCreating()
        navigation.push(PickerProfileEditorKey)
    }
    val snackbar = remember { SnackbarHostState() }
    var snackbarHeight by remember { mutableIntStateOf(0) }
    val snackbarInset = with(LocalDensity.current) { snackbarHeight.toDp() }
    // Closing the picker's editor without saving returns to the picker below it.
    val finishEditor: () -> Unit = {
        profiles.cancel()
        navigation.remove(PickerProfileEditorKey)
    }
    val cancelEditor: () -> Unit = {
        if (profileState.isDirty) {
            navigation.push(DiscardChangesKey(AfterDiscard.CloseEditor))
        } else {
            finishEditor()
        }
    }
    val select: (Destination) -> Unit = { destination ->
        val top = navigation.current.last()
        when {
            destination.root != navigation.selected -> navigation.select(destination.root)

            // An open discard dialog is already asking; popping it would orphan the draft.
            top is DiscardChangesKey -> Unit

            // An open editor closes the usual way when its destination is re-selected.
            top.isProfileEditor -> if (!profileState.isSaving) cancelEditor()

            else -> navigation.select(destination.root)
        }
    }
    // Keeps the calculator and its draft, closing what is open above it; the picker's editor
    // closes the usual way first.
    val calculateOwn: () -> Unit = {
        navigation.select(PatternCatalogKey)
        val hasPickerEditor = PickerProfileEditorKey in navigation.current
        when {
            navigation.current.last() is DiscardChangesKey -> Unit

            hasPickerEditor && profileState.isDirty ->
                navigation.push(DiscardChangesKey(AfterDiscard.CalculateOwn))

            else -> {
                if (hasPickerEditor) finishEditor()
                navigation.popTo(SkirtCalculatorKey)
            }
        }
    }
    val openAbout: () -> Unit = { navigation.push(AboutKey) }
    val routes = entryProvider {
        entry<PatternCatalogKey> {
            PatternCatalog(onOpenSkirt = {
                navigation.push(SkirtCalculatorKey)
            }, onAbout = openAbout)
        }
        entry<ProjectsKey> {
            ProjectsScreen(
                projectState,
                projects::load,
                onSeePatterns = { navigation.select(PatternCatalogKey) },
                onAbout = openAbout,
                onRename = projects::startRenaming,
                onDelete = projects::startDeleting,
            )
        }
        entry<GuideLibraryKey>(
            metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = {
                ReadablePane {
                    Text(stringResource(R.string.guide_choose), Modifier.padding(16.dp))
                }
            }),
        ) {
            GuideLibrary(
                fabricFoldingSelection = when {
                    !LocalIsTwoPane.current -> CatalogSelection.None
                    guias.last() == ExampleFabricFoldingGuideKey -> CatalogSelection.Selected
                    else -> CatalogSelection.Unselected
                },
                onOpenFabricFolding = {
                    if (guias.last() != ExampleFabricFoldingGuideKey) {
                        navigation.push(ExampleFabricFoldingGuideKey)
                    }
                },
                onAbout = openAbout,
            )
        }
        entry<ExampleFabricFoldingGuideKey>(metadata = ListDetailSceneStrategy.detailPane()) {
            FabricFoldingPlayer(
                ExampleSkirtRadii,
                onClose = { navigation.remove(ExampleFabricFoldingGuideKey) },
                // Beside the list the guide is a pane, closed by choosing from the list or Back.
                isCloseShown = !LocalIsTwoPane.current,
                onCalculateOwn = calculateOwn,
            )
        }
        entry<AboutKey> {
            AboutScreen(
                versionName = versionName,
                onUp = navigation::back,
                onOpenLicences = { navigation.push(LicencesKey) },
            )
        }
        entry<LicencesKey> { LicencesScreen(onUp = navigation::back) }
        entry<SkirtCalculatorKey> {
            CalculatorDestination(
                calculator,
                onUp = navigation::back,
                onUseProfile = { navigation.push(ProfilePickerKey) },
                onOpenGuide = { navigation.push(FabricFoldingGuideKey) },
                onSaveProject = { patternName ->
                    val draft = calculatorState.draft
                    val radii = calculatorState.radii
                    if (draft != null && radii != null) {
                        val form = calculatorState.form
                        val source = form.sourceProfileName?.let { name ->
                            form.sourceMeasurementDate?.let {
                                MeasurementSource(name, LocalDate.parse(it))
                            }
                        }
                        projects.startSaving(SkirtSnapshot(draft, radii, source), patternName)
                    }
                },
            )
        }
        entry<FabricFoldingGuideKey> {
            val close: () -> Unit = {
                navigation.remove(FabricFoldingGuideKey)
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
                onDismiss = { navigation.remove(ProfilePickerKey) },
                onCreate = {
                    requestProfileEditor(profileState, onOpen = openPickerEditor) {
                        navigation.push(DiscardChangesKey(AfterDiscard.OpenPickerEditor))
                    }
                },
            )
        }
        entry<DiscardChangesKey>(metadata = windowOverlay()) { key ->
            DiscardDialog(onDismiss = { navigation.remove(key) }, onDiscard = {
                navigation.remove(key)
                when (val afterDiscard = key.afterDiscard) {
                    AfterDiscard.CloseEditor -> finishEditor()

                    is AfterDiscard.OpenEditor -> {
                        profiles.cancel()
                        openEditor(afterDiscard.profile)
                    }

                    AfterDiscard.OpenPickerEditor -> {
                        profiles.cancel()
                        openPickerEditor()
                    }

                    AfterDiscard.CalculateOwn -> {
                        finishEditor()
                        navigation.popTo(SkirtCalculatorKey)
                    }
                }
            })
        }
        val editor = @Composable {
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
        entry<ProfileEditorKey>(metadata = ListDetailSceneStrategy.detailPane()) { editor() }
        entry<PickerProfileEditorKey> { editor() }
    }
    ProfileSaveFeedback(profiles, snackbar, onCreated = { profile ->
        // A profile created from the picker is the one to use: back to the calculator with it.
        if (PickerProfileEditorKey in moldes) {
            val measurements = profile.measurements
            calculator.selectProfile(
                profileWaistText(measurements.waist),
                measurements.name.text,
                measurements.measurementDate,
            )
            navigation.removeAll { it == PickerProfileEditorKey || it == ProfilePickerKey }
        }
    })
    projectState.saving?.let { saving ->
        SaveProjectDialog(saving, projects::editName, projects::save, projects::cancel)
    }
    projectState.renaming?.let { renaming ->
        RenameProjectDialog(
            renaming,
            projects::editName,
            projects::rename,
            projects::cancel,
        )
    }
    projectState.deleting?.let { deleting ->
        DeleteProjectDialog(deleting, projects::delete, projects::cancel)
    }
    ProjectSaveFeedback(projectState, snackbar, projects::dismissMessage)
    val isManaging = navigation.selected == ProfileManagementKey &&
        navigation.current.last() !in setOf(AboutKey, LicencesKey)
    val isGuideOpen = navigation.current.last() == FabricFoldingGuideKey
    val isAnyGuideOpen = isGuideOpen || navigation.current.last() == ExampleFabricFoldingGuideKey
    val density = LocalDensity.current
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
            navigationItems = {
                NavigationItems(navigation, suiteType, select)
            },
            navigationSuiteType = suiteType,
            state = suiteState,
        ) {
            // The player is one column even on a wide window: keep it off a book-posture fold only.
            HingeSafeContent(Modifier.fillMaxSize(), isSingleColumn = isGuideOpen) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val listDetail = rememberReadableListDetailStrategy<NavKey>(maxWidth)
                    val isTwoPane = listDetail != null
                    val fabPosition = if (isTwoPane) FabPosition.Start else FabPosition.End
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
                            if (isManaging &&
                                (perfis.none { it is ProfileEditorKey } || isTwoPane)
                            ) {
                                ProfileCreateAction(
                                    profileState,
                                    { requestEditor(EditedProfile.New) },
                                )
                            }
                        },
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    ) { padding ->
                        CompositionLocalProvider(
                            LocalIsTwoPane provides isTwoPane,
                            LocalTitleFocus provides titleFocus,
                        ) {
                            NavDisplay(
                                entries = navigation.entries(routes),
                                // Keep bottom actions reachable while storage feedback is visible.
                                modifier = Modifier
                                    .padding(padding)
                                    .padding(bottom = snackbarInset),
                                // A narrow window shows one destination at a time, in a single pane.
                                sceneStrategies = listOfNotNull(overlayStrategy, listDetail),
                                transitionSpec = { materialTransition(density, isForward = true) },
                                popTransitionSpec = {
                                    materialTransition(density, isForward = false)
                                },
                                predictivePopTransitionSpec = {
                                    materialTransition(density, isForward = false)
                                },
                                onBack = {
                                    if (!navigation.current.last().isProfileEditor) {
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
}

/** One item per destination; a filled glyph, label colour and selected state mark the current. */
@Composable
private fun NavigationItems(
    navigation: TopLevelBackStacks,
    suiteType: NavigationSuiteType,
    onSelect: (Destination) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val first = remember { FocusRequester() }
    val last = remember { FocusRequester() }
    Destination.entries.forEach { destination ->
        val isSelected = navigation.selected == destination.root
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

private fun ProfilesViewModel.openEditor(profile: EditedProfile) = when (profile) {
    EditedProfile.New -> startCreating()
    is EditedProfile.Existing -> startEditing(profile.id)
}

private val NavKey.isProfileEditor: Boolean
    get() = this is ProfileEditorKey || this == PickerProfileEditorKey

/**
 * Shows the editor [form] has open above profile management, unless the picker's editor on the
 * Moldes stack holds it, and drops any other editor from profile management.
 */
private fun syncProfileEditor(navigation: TopLevelBackStacks, form: ProfileForm) {
    val profile = form.editingId?.let(EditedProfile::Existing) ?: EditedProfile.New
    val editor = ProfileEditorKey(profile).takeIf { form.isEditorOpen }
    navigation.removeAll { it is ProfileEditorKey && it != editor }
    if (editor != null && navigation.stackOf(ProfileManagementKey).last() == ProfileManagementKey &&
        PickerProfileEditorKey !in navigation.stackOf(PatternCatalogKey)
    ) {
        navigation.push(editor, root = ProfileManagementKey)
    }
}

@Composable
private fun CalculatorDestination(
    calculator: SkirtCalculatorViewModel,
    onUp: () -> Unit,
    onUseProfile: () -> Unit,
    onOpenGuide: () -> Unit,
    onSaveProject: (patternName: String) -> Unit,
) {
    val state by calculator.state.collectAsStateWithLifecycle()
    val patternName = stringResource(R.string.project_pattern_skirt)
    Column(Modifier.fillMaxSize()) {
        MoldaTopAppBar(stringResource(R.string.skirt_title), onUp = onUp)
        SkirtCalculatorScreen(
            state,
            calculator::edit,
            calculator::calculate,
            Modifier.weight(1f),
            onBlur = calculator::blur,
            onUseProfile = onUseProfile,
            onOpenGuide = onOpenGuide,
            onSaveProject = { onSaveProject(patternName) },
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
        calculator.selectProfile(
            profileWaistText(measurements.waist),
            measurements.name.text,
            measurements.measurementDate,
        )
        onDismiss()
    }, onCreate = onCreate)
}

@Composable
private fun ProfileSaveFeedback(
    profiles: ProfilesViewModel,
    snackbar: SnackbarHostState,
    onCreated: (MeasurementProfile) -> Unit,
) {
    val state by profiles.state.collectAsStateWithLifecycle()
    val message = state.savedMessage?.let { stringResource(it) }
    LaunchedEffect(message) {
        message?.let {
            state.createdProfile?.let(onCreated)
            snackbar.showSnackbar(it)
            profiles.dismissMessage()
        }
    }
}

/** Opens an editor with [onOpen], first asking with [onDiscard] when the open one is dirty. */
private fun requestProfileEditor(state: ProfilesState, onOpen: () -> Unit, onDiscard: () -> Unit) {
    if (state.isSaving || state.isLoading || state.loadError != null) return
    if (state.isEditorOpen && state.isDirty) onDiscard() else onOpen()
}
