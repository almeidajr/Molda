package com.almeidajr.molda

import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.guide.navigation.ExampleFabricFoldingGuideKey
import com.almeidajr.molda.guide.navigation.GuideLibraryKey
import com.almeidajr.molda.pattern.navigation.PatternCatalogKey
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.navigation.EditedProfile
import com.almeidajr.molda.profile.navigation.PickerProfileEditorKey
import com.almeidajr.molda.profile.navigation.ProfileEditorKey
import com.almeidajr.molda.profile.navigation.ProfileManagementKey
import com.almeidajr.molda.profile.navigation.ProfilePickerKey
import com.almeidajr.molda.project.ReferenceSnapshot
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.navigation.ProjectEditorGuideKey
import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.project.navigation.ProjectKey
import com.almeidajr.molda.project.navigation.ProjectsKey
import com.almeidajr.molda.skirt.navigation.MOLDES_PROFILE_RESULT_KEY
import com.almeidajr.molda.skirt.navigation.SkirtCalculatorKey
import com.almeidajr.molda.testing.Beta2
import com.almeidajr.molda.ui.navigation.EditorKind
import kotlin.test.assertEquals
import org.junit.Test

/** Each case names the request it decides, so a failure reads as that case. */
class MoldaTransitionsTest {
    private val project = ProjectId(1)
    private val update = Beta2
    private val projectEditor =
        listOf(ProjectsKey, ProjectKey(project), ProjectEditorKey(project, ReferenceSnapshot))
    private val profileEditor = listOf(ProfileManagementKey, ProfileEditorKey(EditedProfile.New))
    private val existing = EditedProfile.Existing(ProfileId(2))

    private data class Case<T>(val name: String, val expected: T, val actual: T)

    private fun <T> assertCases(vararg cases: Case<T>) =
        cases.forEach { assertEquals(it.expected, it.actual, it.name) }

    /** Selecting [root]'s destination again while it shows [stack]. */
    private fun reselect(root: NavKey, stack: List<NavKey>, status: EditorStatus) =
        selectDestination(root, root, stack, status)

    @Test
    fun selectDestination_withoutAnOpenEditor_decidesTheTransition() {
        // Given
        val guias = listOf(GuideLibraryKey, ExampleFabricFoldingGuideKey)
        val underDialog = profileEditor + DiscardChangesKey(AfterDiscard.CloseEditor)

        // When/Then
        assertCases(
            Case(
                "another destination",
                Transition.Select(ProjectsKey),
                selectDestination(ProjectsKey, GuideLibraryKey, guias, EditorStatus.None),
            ),
            Case(
                "the current one returns to its root",
                Transition.Select(GuideLibraryKey),
                reselect(GuideLibraryKey, guias, EditorStatus.None),
            ),
            Case(
                "the current one under a discard dialog keeps the dialog",
                Transition.None,
                reselect(ProfileManagementKey, underDialog, EditorStatus.None),
            ),
        )
    }

    @Test
    fun selectDestination_currentOneOverAnEditor_decidesByItsStatus() {
        // Given
        val dirtyProfile = EditorStatus.Profile(isDirty = true)
        val dirtyProject = EditorStatus.Project(isDirty = true)
        val underGuide = projectEditor + ProjectEditorGuideKey(ReferenceSnapshot.radii)

        // When/Then
        assertCases(
            Case(
                "a dirty profile editor asks",
                Transition.AskToDiscard(AfterDiscard.CloseEditor),
                reselect(ProfileManagementKey, profileEditor, dirtyProfile),
            ),
            Case(
                "a saving profile editor waits",
                Transition.None,
                reselect(ProfileManagementKey, profileEditor, dirtyProfile.copy(isSaving = true)),
            ),
            Case(
                "a dirty project editor asks",
                Transition.AskToDiscard(AfterDiscard.ShowProjects),
                reselect(ProjectsKey, projectEditor, dirtyProject),
            ),
            Case(
                "a dirty project editor under a guide asks",
                Transition.AskToDiscard(AfterDiscard.ShowProjects),
                reselect(ProjectsKey, underGuide, dirtyProject),
            ),
            Case(
                "an updating project editor waits",
                Transition.None,
                reselect(ProjectsKey, projectEditor, dirtyProject.copy(isUpdating = true)),
            ),
        )
    }

    @Test
    fun goBack_eachTopOfTheStack_decidesTheTransition() {
        // When/Then
        assertCases(
            Case(
                "an ordinary screen pops",
                Transition.Back,
                goBack(listOf(PatternCatalogKey, SkirtCalculatorKey), EditorStatus.None),
            ),
            Case(
                "a clean project editor closes",
                Transition.CloseProjectEditor,
                goBack(projectEditor, EditorStatus.Project()),
            ),
            Case(
                "a dirty project editor asks",
                Transition.AskToDiscard(AfterDiscard.CloseProjectEditor),
                goBack(projectEditor, EditorStatus.Project(isDirty = true)),
            ),
            Case(
                "an updating project editor waits",
                Transition.None,
                goBack(projectEditor, EditorStatus.Project(isDirty = true, isUpdating = true)),
            ),
            Case(
                "a clean profile editor finishes",
                Transition.FinishProfileEditor,
                goBack(profileEditor, EditorStatus.Profile()),
            ),
            Case(
                "a saving profile editor waits",
                Transition.None,
                goBack(profileEditor, EditorStatus.Profile(isSaving = true)),
            ),
        )
    }

    @Test
    fun closeProjectEditor_withADirtyProfileEditorOnTop_closesIt() {
        // When/Then
        assertEquals(
            Transition.CloseProjectEditor,
            closeProjectEditor(EditorStatus.Profile(isDirty = true)),
        )
    }

    @Test
    fun calculateOwn_eachPickerEditorStatus_decidesTheTransition() {
        // Given
        val picker = ProfilePickerKey(MOLDES_PROFILE_RESULT_KEY)
        val editor = PickerProfileEditorKey(MOLDES_PROFILE_RESULT_KEY)

        // When/Then
        assertCases(
            Case(
                "no picker editor",
                Transition.ShowCalculator,
                calculateOwn(listOf(PatternCatalogKey, SkirtCalculatorKey), EditorStatus.None),
            ),
            Case(
                "over a clean picker editor",
                Transition.ShowCalculator,
                calculateOwn(
                    listOf(PatternCatalogKey, SkirtCalculatorKey, picker, editor),
                    EditorStatus.Profile(),
                ),
            ),
            Case(
                "over a dirty picker editor",
                Transition.AskToDiscard(AfterDiscard.CalculateOwn),
                calculateOwn(
                    listOf(PatternCatalogKey, SkirtCalculatorKey, editor),
                    EditorStatus.Profile(isDirty = true),
                ),
            ),
            Case(
                "under a discard dialog",
                Transition.None,
                calculateOwn(
                    listOf(PatternCatalogKey, DiscardChangesKey(AfterDiscard.CalculateOwn)),
                    EditorStatus.None,
                ),
            ),
        )
    }

    @Test
    fun requestProfileEditor_eachOpenEditorStatus_decidesTheTransition() {
        // When/Then
        assertCases(
            Case(
                "no open changes",
                Transition.OpenProfileEditor(existing),
                requestProfileEditor(existing, EditorStatus.Profile()),
            ),
            Case(
                "over a dirty project editor",
                Transition.OpenProfileEditor(EditedProfile.New),
                requestProfileEditor(EditedProfile.New, EditorStatus.Project(isDirty = true)),
            ),
            Case(
                "the open editor has changes",
                Transition.AskToDiscard(AfterDiscard.OpenEditor(EditedProfile.New)),
                requestProfileEditor(EditedProfile.New, EditorStatus.Profile(isDirty = true)),
            ),
            Case(
                "the open editor saves",
                Transition.None,
                requestProfileEditor(EditedProfile.New, EditorStatus.Profile(isSaving = true)),
            ),
        )
    }

    @Test
    fun requestPickerEditor_eachOpenEditorStatus_decidesTheTransition() {
        // When/Then
        assertCases(
            Case(
                "the open editor has changes",
                Transition.AskToDiscard(AfterDiscard.OpenPickerEditor(MOLDES_PROFILE_RESULT_KEY)),
                requestPickerEditor(
                    MOLDES_PROFILE_RESULT_KEY,
                    EditorStatus.Profile(isDirty = true),
                ),
            ),
            Case(
                "over a dirty project editor",
                Transition.OpenPickerEditor(MOLDES_PROFILE_RESULT_KEY),
                requestPickerEditor(
                    MOLDES_PROFILE_RESULT_KEY,
                    EditorStatus.Project(isDirty = true),
                ),
            ),
        )
    }

    @Test
    fun afterDiscard_eachFollowUp_repeatsTheRequestAndNamesTheEditorItDrops() {
        // When/Then
        assertCases(
            Case(
                "close editor",
                Transition.FinishProfileEditor to EditorKind.Profile,
                AfterDiscard.CloseEditor.outcome(),
            ),
            Case(
                "open editor",
                Transition.OpenProfileEditor(existing, discardsDraft = true) to EditorKind.Profile,
                AfterDiscard.OpenEditor(existing).outcome(),
            ),
            Case(
                "open picker editor",
                Transition.OpenPickerEditor(MOLDES_PROFILE_RESULT_KEY) to EditorKind.Profile,
                AfterDiscard.OpenPickerEditor(MOLDES_PROFILE_RESULT_KEY).outcome(),
            ),
            Case(
                "calculate own",
                Transition.ShowCalculator to EditorKind.Profile,
                AfterDiscard.CalculateOwn.outcome(),
            ),
            Case(
                "close project editor",
                Transition.CloseProjectEditor to EditorKind.Project,
                AfterDiscard.CloseProjectEditor.outcome(),
            ),
            Case(
                "show projects",
                Transition.Select(ProjectsKey) to EditorKind.Project,
                AfterDiscard.ShowProjects.outcome(),
            ),
            Case(
                "install update",
                Transition.InstallUpdate(update) to EditorKind.Project,
                AfterDiscard.InstallUpdate(update, EditorKind.Project).outcome(),
            ),
        )
    }

    // What confirming the discard does, and which editor the dialog names.
    private fun AfterDiscard.outcome() = transition() to discardedEditor

    @Test
    fun installUpdate_eachDirtyEditorSet_decidesTheTransition() {
        // When/Then
        assertCases(
            Case(
                "no unsaved changes",
                Transition.InstallUpdate(update),
                installUpdate(update, emptyList()),
            ),
            Case(
                "an editor holds unsaved changes",
                Transition.AskToDiscard(AfterDiscard.InstallUpdate(update, EditorKind.Project)),
                installUpdate(update, listOf(EditorKind.Project)),
            ),
        )
    }
}
