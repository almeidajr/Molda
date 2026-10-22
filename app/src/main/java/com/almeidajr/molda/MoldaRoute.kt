package com.almeidajr.molda

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.almeidajr.molda.about.navigation.AboutKey
import com.almeidajr.molda.about.navigation.LicencesKey
import com.almeidajr.molda.about.ui.aboutSection
import com.almeidajr.molda.guide.ui.guideSection
import com.almeidajr.molda.measurement.lengthText
import com.almeidajr.molda.pattern.navigation.PatternCatalogKey
import com.almeidajr.molda.pattern.ui.patternSection
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.navigation.EditedProfile
import com.almeidajr.molda.profile.navigation.PickerProfileEditorKey
import com.almeidajr.molda.profile.navigation.ProfileEditorKey
import com.almeidajr.molda.profile.navigation.ProfileManagementKey
import com.almeidajr.molda.profile.navigation.ProfilePickerKey
import com.almeidajr.molda.profile.ui.ProfileCreateAction
import com.almeidajr.molda.profile.ui.ProfilesState
import com.almeidajr.molda.profile.ui.ProfilesViewModel
import com.almeidajr.molda.profile.ui.SyncProfileEditor
import com.almeidajr.molda.profile.ui.openEditor
import com.almeidajr.molda.profile.ui.profileSection
import com.almeidajr.molda.project.domain.Project
import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.project.navigation.ProjectGuideKey
import com.almeidajr.molda.project.navigation.ProjectsKey
import com.almeidajr.molda.project.ui.DeleteProjectDialog
import com.almeidajr.molda.project.ui.ProjectFeedback
import com.almeidajr.molda.project.ui.ProjectsState
import com.almeidajr.molda.project.ui.ProjectsViewModel
import com.almeidajr.molda.project.ui.RenameProjectDialog
import com.almeidajr.molda.project.ui.SaveProjectDialog
import com.almeidajr.molda.project.ui.ShowSavedProject
import com.almeidajr.molda.project.ui.projectSection
import com.almeidajr.molda.skirt.navigation.SkirtCalculatorKey
import com.almeidajr.molda.skirt.ui.CalculatorState
import com.almeidajr.molda.skirt.ui.SavedSkirtResult
import com.almeidajr.molda.skirt.ui.SkirtCalculatorViewModel
import com.almeidajr.molda.skirt.ui.skirtSection
import com.almeidajr.molda.skirt.ui.snapshot
import com.almeidajr.molda.ui.navigation.Navigator
import com.almeidajr.molda.ui.navigation.TitleFocus
import com.almeidajr.molda.ui.navigation.rememberNavigator
import com.almeidajr.molda.ui.navigation.rememberTopLevelBackStacks

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
    val projectState by projects.state.collectAsStateWithLifecycle()
    val projectCalculatorState by projectCalculator.state.collectAsStateWithLifecycle()
    val titleFocus = remember { TitleFocus() }
    val navigator = rememberNavigator(
        rememberTopLevelBackStacks(TopLevelRoots),
        onSwitch = titleFocus::request,
    )
    val app = MoldaApp(
        navigator,
        calculator,
        profiles,
        projects,
        projectCalculator,
        // Read when the user acts: entries are remembered, so a captured status would go stale.
        status = { editorStatus(profileState, projectState, projectCalculatorState) },
    )
    val snackbar = remember { SnackbarHostState() }
    SyncProfileEditor(navigator, profiles)
    ShowSavedProject(navigator, projects)
    ProfileSaveFeedback(profiles, snackbar, onCreated = app::useCreatedProfile)
    ProjectDialogs(projectState, projects, snackbar)
    MoldaShell(
        navigator,
        titleFocus,
        snackbar,
        onSelect = app::select,
        onBack = app::back,
        entryProvider = entryProvider { with(app) { entries(versionName) } },
        modifier = modifier,
    ) { isTwoPane ->
        if (app.isManagingProfiles(isTwoPane)) {
            ProfileCreateAction(profileState, app::createProfile)
        }
    }
}

private fun editorStatus(
    profiles: ProfilesState,
    projects: ProjectsState,
    projectEditor: CalculatorState,
) = EditorStatus(
    isProfileEditorOpen = profiles.isEditorOpen,
    isProfileDirty = profiles.isDirty,
    isProfileSaving = profiles.isSaving,
    isProfileUnavailable = profiles.isLoading || profiles.loadError != null,
    isProjectEditorDirty = projectEditor.isDirty,
    isProjectUpdating = projects.isUpdating,
)

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

    fun createProfile() = perform(requestProfileEditor(EditedProfile.New, status()))

    /** Whether Perfis shows the new-profile action: not over About nor a single-pane editor. */
    fun isManagingProfiles(isTwoPane: Boolean): Boolean = stacks.selected == ProfileManagementKey &&
        stacks.current.last() !in setOf(AboutKey, LicencesKey) &&
        (stacks.stackOf(ProfileManagementKey).none { it is ProfileEditorKey } || isTwoPane)

    /** A profile created from a picker is the one to use: back to its calculator with it. */
    fun useCreatedProfile(profile: MeasurementProfile) {
        val target = when (PickerProfileEditorKey) {
            in stacks.stackOf(PatternCatalogKey) -> calculator
            in stacks.stackOf(ProjectsKey) -> projectCalculator
            else -> null
        }
        if (target != null) {
            val measurements = profile.measurements
            target.selectProfile(lengthText(measurements.waist), measurements.source)
            navigator.removeAll { it == PickerProfileEditorKey || it == ProfilePickerKey }
        }
    }

    fun EntryProviderScope<NavKey>.entries(versionName: String) {
        patternSection(navigator)
        skirtSection(
            navigator,
            calculator,
            onSaveProject = { patternName ->
                calculator.state.value.snapshot()?.let { projects.startSaving(it, patternName) }
            },
            onCalculateOwn = {
                navigator.select(PatternCatalogKey)
                perform(calculateOwn(stacks.current, status()))
            },
        )
        projectSection(navigator, projects, onEdit = ::editProject) { project ->
            SavedSkirtResult(project.snapshot.draft, project.snapshot.radii, onOpenGuide = {
                navigator.push(ProjectGuideKey(project.id))
            })
        }
        projectDraftSection(navigator, projects, projectCalculator, onCloseEditor = {
            perform(closeProjectEditor(status()))
        })
        guideSection(navigator)
        profileSection(
            navigator,
            profiles,
            onRequestEditor = { perform(requestProfileEditor(it, status())) },
            onCancelEditor = { perform(cancelProfileEditor(status())) },
        )
        aboutSection(navigator, versionName)
        overlaySection(
            navigator,
            profiles,
            pickerCalculator = {
                if (stacks.selected == ProjectsKey) projectCalculator else calculator
            },
            onCreateProfile = { perform(requestPickerEditor(status())) },
            onDiscard = { perform(it.transition()) },
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
                navigator.remove(PickerProfileEditorKey)
                profiles.openEditor(transition.profile)
            }

            is Transition.OpenPickerEditor -> {
                if (transition.discardsDraft) profiles.cancel()
                // Moldes and Projetos each have a picker; the one profile form backs the latest.
                navigator.removeAll { it is ProfileEditorKey || it == PickerProfileEditorKey }
                profiles.startCreating()
                navigator.push(PickerProfileEditorKey)
            }
        }
    }

    /** Drops the profile draft; closing the picker's editor returns to the picker below it. */
    private fun finishProfileEditor() {
        profiles.cancel()
        navigator.remove(PickerProfileEditorKey)
    }
}

@Composable
private fun ProjectDialogs(
    state: ProjectsState,
    projects: ProjectsViewModel,
    snackbar: SnackbarHostState,
) {
    state.saving?.let { saving ->
        SaveProjectDialog(saving, projects::editName, projects::save, projects::cancel)
    }
    state.renaming?.let { renaming ->
        RenameProjectDialog(renaming, projects::editName, projects::rename, projects::cancel)
    }
    state.deleting?.let { deleting ->
        DeleteProjectDialog(deleting, projects::delete, projects::cancel)
    }
    ProjectFeedback(state, snackbar, projects::dismissMessage)
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
