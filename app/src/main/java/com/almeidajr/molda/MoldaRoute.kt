package com.almeidajr.molda

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.runtime.result.rememberResultEventBus
import com.almeidajr.molda.about.ui.aboutSection
import com.almeidajr.molda.guide.ui.guideSection
import com.almeidajr.molda.pattern.navigation.PatternCatalogKey
import com.almeidajr.molda.pattern.ui.patternSection
import com.almeidajr.molda.profile.navigation.EditedProfile
import com.almeidajr.molda.profile.navigation.ProfileManagementKey
import com.almeidajr.molda.profile.ui.ProfileFloatingAction
import com.almeidajr.molda.profile.ui.profileSection
import com.almeidajr.molda.project.navigation.ProjectGuideKey
import com.almeidajr.molda.project.navigation.SaveProjectKey
import com.almeidajr.molda.project.ui.projectSection
import com.almeidajr.molda.skirt.ui.SavedSkirtResult
import com.almeidajr.molda.skirt.ui.skirtSection
import com.almeidajr.molda.ui.navigation.EditorKind
import com.almeidajr.molda.ui.navigation.EditorReport
import com.almeidajr.molda.ui.navigation.EditorStatusHolder
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
fun MoldaRoute(modifier: Modifier = Modifier, versionName: String = "") {
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
    // The holder forgets an editor's report once its key leaves the back stacks.
    LaunchedEffect(navigator, editors) {
        snapshotFlow { navigator.state.allKeys }.collect { editors.retainOnly(it) }
    }
    val app = MoldaApp(navigator, editors)
    // Outside any entry, so the bus is passed rather than read from LocalResultEventBus.
    ResultEffect<AfterDiscard>(DISCARD_RESULT_KEY, resultBus) { app.discard(it) }
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
            ProfileFloatingAction(navigator, isTwoPane, onCreate = app::createProfile)
        }
    }
}

private val TopLevelBackStacks.allKeys: List<NavKey> get() = roots.flatMap(::stackOf)

/** The transitions' view of an editor's [EditorReport], or of no editor when null. */
private fun EditorReport?.editorStatus(): EditorStatus = when (this) {
    null -> EditorStatus.None

    else -> when (kind) {
        EditorKind.Project -> EditorStatus.Project(isDirty, isUpdating = isSaving)
        EditorKind.Profile -> EditorStatus.Profile(isDirty, isSaving)
    }
}

/**
 * The app's [navigator], which wires the features' entries together and has [carryOut] perform the
 * transitions decided from the status of the editor an action on a stack concerns, which
 * [editors] answer.
 */
private class MoldaApp(private val navigator: Navigator, private val editors: EditorStatusHolder) {
    private val stacks get() = navigator.state

    fun select(destination: Destination) = perform(
        selectDestination(
            destination.root,
            stacks.selected,
            stacks.current,
            status(stacks.stackOf(destination.root)),
        ),
    )

    fun back() = perform(goBack(stacks.current, status(stacks.current)))

    /** Carries out what the confirmed discard dialog asked for. */
    fun discard(afterDiscard: AfterDiscard) = perform(afterDiscard.transition())

    fun createProfile() = perform(requestProfileEditor(EditedProfile.New, perfisStatus()))

    fun EntryProviderScope<NavKey>.entries(versionName: String, onShowSnackbar: (Int) -> Unit) {
        patternSection(navigator)
        skirtSection(
            navigator,
            onSaveProject = { state ->
                state.snapshot()?.let { navigator.push(SaveProjectKey(it)) }
            },
            onCalculateOwn = {
                perform(Transition.Select(PatternCatalogKey))
                perform(calculateOwn(stacks.current, status(stacks.current)))
            },
        )
        projectSection(navigator, onShowSnackbar) { project ->
            SavedSkirtResult(project.snapshot.draft, project.snapshot.radii, onOpenGuide = {
                navigator.push(ProjectGuideKey(project.id))
            })
        }
        projectDraftSection(
            navigator,
            onShowSnackbar,
            onCloseEditor = { perform(closeProjectEditor(status(stacks.current))) },
        )
        guideSection(navigator)
        profileSection(
            navigator,
            onRequestEditor = { perform(requestProfileEditor(it, perfisStatus())) },
            onCancelEditor = { perform(cancelProfileEditor(status(stacks.current))) },
            onShowSnackbar = onShowSnackbar,
        )
        aboutSection(navigator, versionName)
        overlaySection(
            navigator,
            onCreateProfile = { perform(requestPickerEditor(it, status(stacks.current))) },
        )
    }

    private fun perfisStatus() = status(stacks.stackOf(ProfileManagementKey))

    /** The status of the editor an action on [stack] concerns, read when the sewist acts. */
    private fun status(stack: List<NavKey>) = editors.editorOn(stack)?.report.editorStatus()

    private fun perform(transition: Transition) = navigator.carryOut(transition, editors)
}
