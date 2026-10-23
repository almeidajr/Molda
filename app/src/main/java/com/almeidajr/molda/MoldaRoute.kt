package com.almeidajr.molda

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.runtime.result.rememberResultEventBus
import com.almeidajr.molda.about.navigation.AboutKey
import com.almeidajr.molda.about.navigation.LicencesKey
import com.almeidajr.molda.about.ui.aboutSection
import com.almeidajr.molda.guide.ui.guideSection
import com.almeidajr.molda.pattern.navigation.PatternCatalogKey
import com.almeidajr.molda.pattern.ui.patternSection
import com.almeidajr.molda.profile.navigation.EditedProfile
import com.almeidajr.molda.profile.navigation.PickerProfileEditorKey
import com.almeidajr.molda.profile.navigation.ProfileEditorKey
import com.almeidajr.molda.profile.navigation.ProfileManagementKey
import com.almeidajr.molda.profile.navigation.isProfileEditor
import com.almeidajr.molda.profile.ui.ProfileCreateAction
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.profile.ui.SyncProfileEditor
import com.almeidajr.molda.profile.ui.openEditor
import com.almeidajr.molda.profile.ui.profileSection
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.project.navigation.ProjectGuideKey
import com.almeidajr.molda.project.navigation.SaveProjectKey
import com.almeidajr.molda.project.ui.ProjectsViewModel
import com.almeidajr.molda.project.ui.projectSection
import com.almeidajr.molda.skirt.navigation.SkirtCalculatorKey
import com.almeidajr.molda.skirt.ui.SavedSkirtResult
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel
import com.almeidajr.molda.skirt.ui.skirtSection
import com.almeidajr.molda.skirt.ui.snapshot
import com.almeidajr.molda.ui.navigation.EditorReport
import com.almeidajr.molda.ui.navigation.LocalEditorStatusHolder
import com.almeidajr.molda.ui.navigation.Navigator
import com.almeidajr.molda.ui.navigation.TitleFocus
import com.almeidajr.molda.ui.navigation.TopLevelBackStacks
import com.almeidajr.molda.ui.navigation.rememberEditorStatusHolder
import com.almeidajr.molda.ui.navigation.rememberNavigator
import com.almeidajr.molda.ui.navigation.rememberTopLevelBackStacks
import kotlinx.coroutines.launch

/**
 * The app: one back stack per top-level destination, each feature's section of entries, the
 * entries where features meet, and the transitions between features, which the pure functions in
 * MoldaTransitions.kt decide from an [EditorStatus].
 */
@Composable
fun MoldaRoute(
    modifier: Modifier = Modifier,
    versionName: String = "",
    calculator: SkirtCalculatorViewModel = hiltViewModel(),
    profiles: ProfilesViewModel = hiltViewModel(),
    projects: ProjectsViewModel = hiltViewModel(),
    // Its own draft, so editing a project never touches the calculator in Moldes.
    projectCalculator: SkirtCalculatorViewModel = hiltViewModel(key = "projectEditor"),
) {
    val profileState by profiles.state.collectAsStateWithLifecycle()
    val titleFocus = remember { TitleFocus() }
    // One bus for every stack, so a result reaches its requester whichever stack holds it.
    val resultBus = rememberResultEventBus()
    val navigator = rememberNavigator(
        rememberTopLevelBackStacks(TopLevelRoots, resultBus),
        onSwitch = titleFocus::request,
    )
    val editors = rememberEditorStatusHolder()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    // One callback for every section; launched here, so it outlives the entry that asked.
    val showSnackbar: (Int) -> Unit = { message ->
        scope.launch {
            // The latest outcome replaces the shown one instead of queuing behind it.
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(resources.getString(message))
        }
    }
    val app = MoldaApp(
        navigator,
        calculator,
        profiles,
        projects,
        projectCalculator,
        // Read when the user acts: entries are remembered, so a captured status would go stale.
        status = { navigator.state.allKeys.let { editorStatus(it, editors.reports(it)) } },
    )
    // Outside any entry, so the bus is passed rather than read from LocalResultEventBus.
    ResultEffect<AfterDiscard>(DISCARD_RESULT_KEY, resultBus) { app.discard(it) }
    SyncProfileEditor(navigator, profiles)
    CompositionLocalProvider(LocalEditorStatusHolder provides editors) {
        MoldaShell(
            navigator,
            titleFocus,
            snackbar,
            onSelect = app::select,
            onBack = app::back,
            entryProvider = entryProvider { with(app) { entries(versionName, showSnackbar) } },
            modifier = modifier,
        ) { isTwoPane ->
            if (app.isManagingProfiles(isTwoPane)) {
                ProfileCreateAction(profileState, app::createProfile)
            }
        }
    }
}

private val TopLevelBackStacks.allKeys: List<NavKey> get() = roots.flatMap(::stackOf)

/** The editors' status from the back stacks' [keys] and what their editor entries [reports]. */
private fun editorStatus(keys: List<NavKey>, reports: Map<NavKey, EditorReport>): EditorStatus {
    val profileEditors = reports.filterKeys { it.isProfileEditor }.values
    val projectEditors = reports.filterKeys { it is ProjectEditorKey }.values
    return EditorStatus(
        isProfileEditorOpen = keys.any { it.isProfileEditor },
        isProfileDirty = profileEditors.any { it.isDirty },
        isProfileSaving = profileEditors.any { it.isSaving },
        isProfileUnavailable = profileEditors.any { it.isUnavailable },
        isProjectEditorDirty = projectEditors.any { it.isDirty },
        isProjectUpdating = projectEditors.any { it.isSaving },
    )
}

/**
 * The app's ViewModels and [navigator], which wire the features' entries together and carry out
 * the transitions decided from [status].
 */
private class MoldaApp(
    private val navigator: Navigator,
    private val calculator: SkirtCalculatorViewModel,
    private val profiles: ProfilesViewModel,
    private val projects: ProjectsViewModel,
    private val projectCalculator: SkirtCalculatorViewModel,
    private val status: () -> EditorStatus,
) {
    private val stacks get() = navigator.state

    fun select(destination: Destination) =
        perform(selectDestination(destination.root, stacks.selected, stacks.current, status()))

    fun back() = perform(goBack(stacks.current, status()))

    /** Carries out what the confirmed discard dialog asked for. */
    fun discard(afterDiscard: AfterDiscard) = perform(afterDiscard.transition())

    fun createProfile() = perform(requestProfileEditor(EditedProfile.New, status()))

    /** Whether Perfis shows the new-profile action: not over About nor a single-pane editor. */
    fun isManagingProfiles(isTwoPane: Boolean): Boolean = stacks.selected == ProfileManagementKey &&
        stacks.current.last() !in setOf(AboutKey, LicencesKey) &&
        (stacks.stackOf(ProfileManagementKey).none { it is ProfileEditorKey } || isTwoPane)

    fun EntryProviderScope<NavKey>.entries(versionName: String, onShowSnackbar: (Int) -> Unit) {
        patternSection(navigator)
        skirtSection(
            navigator,
            calculator,
            onSaveProject = {
                calculator.state.value.snapshot()?.let { navigator.push(SaveProjectKey(it)) }
            },
            onCalculateOwn = {
                navigator.select(PatternCatalogKey)
                perform(calculateOwn(stacks.current, status()))
            },
        )
        projectSection(navigator, projects, onShowSnackbar, onEdit = ::editProject) { project ->
            SavedSkirtResult(project.snapshot.draft, project.snapshot.radii, onOpenGuide = {
                navigator.push(ProjectGuideKey(project.id))
            })
        }
        projectDraftSection(
            navigator,
            projects,
            projectCalculator,
            onShowSnackbar,
            onCloseEditor = { perform(closeProjectEditor(status())) },
        )
        guideSection(navigator)
        profileSection(
            navigator,
            profiles,
            onRequestEditor = { perform(requestProfileEditor(it, status())) },
            onCancelEditor = { perform(cancelProfileEditor(status())) },
            onShowSnackbar = onShowSnackbar,
        )
        aboutSection(navigator, versionName)
        overlaySection(
            navigator,
            profiles,
            onCreateProfile = { perform(requestPickerEditor(it, status())) },
        )
    }

    private fun editProject(project: Project) {
        projectCalculator.load(project.snapshot.draft, project.snapshot.source)
        navigator.push(ProjectEditorKey(project.id))
    }

    /** Carries out [transition] on the back stacks and the profile editor. */
    private fun perform(transition: Transition) {
        when (transition) {
            Transition.None -> Unit

            is Transition.Select -> navigator.select(transition.root)

            Transition.Back -> navigator.back()

            is Transition.AskToDiscard -> navigator.push(DiscardChangesKey(transition.afterDiscard))

            Transition.FinishProfileEditor -> finishProfileEditor()

            Transition.CloseProjectEditor -> navigator.removeAll { it is ProjectEditorKey }

            is Transition.ShowCalculator -> {
                if (transition.finishesPickerEditor) finishProfileEditor()
                navigator.popTo(SkirtCalculatorKey)
            }

            is Transition.OpenProfileEditor -> {
                if (transition.discardsDraft) profiles.cancel()
                navigator.removeAll { it is PickerProfileEditorKey }
                profiles.openEditor(transition.profile)
            }

            is Transition.OpenPickerEditor -> {
                if (transition.discardsDraft) profiles.cancel()
                openPickerEditor(transition.resultKey)
            }
        }
    }

    private fun openPickerEditor(resultKey: String) {
        // Moldes and Projetos each have a picker; the one profile form backs the latest.
        navigator.removeAll { it is ProfileEditorKey || it is PickerProfileEditorKey }
        profiles.startCreating()
        // A busy store ignores the request; an entry pushed anyway would send a stale profile.
        if (profiles.state.value.isEditorOpen) navigator.push(PickerProfileEditorKey(resultKey))
    }

    /** Drops the profile draft; closing the picker's editor returns to the picker below it. */
    private fun finishProfileEditor() {
        profiles.cancel()
        navigator.removeAll { it is PickerProfileEditorKey }
    }
}
