package com.almeidajr.molda

import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.guide.navigation.ExampleFabricFoldingGuideKey
import com.almeidajr.molda.guide.navigation.GuideLibraryKey
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.pattern.navigation.PatternCatalogKey
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.navigation.EditedProfile
import com.almeidajr.molda.profile.navigation.PickerProfileEditorKey
import com.almeidajr.molda.profile.navigation.ProfileEditorKey
import com.almeidajr.molda.profile.navigation.ProfileManagementKey
import com.almeidajr.molda.profile.navigation.ProfilePickerKey
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.project.navigation.ProjectKey
import com.almeidajr.molda.project.navigation.ProjectsKey
import com.almeidajr.molda.project.navigation.profileResultKey
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.skirt.navigation.FabricFoldingGuideKey
import com.almeidajr.molda.skirt.navigation.MOLDES_PROFILE_RESULT_KEY
import com.almeidajr.molda.skirt.navigation.SkirtCalculatorKey
import com.almeidajr.molda.ui.navigation.EditorKind
import com.almeidajr.molda.ui.navigation.EditorReport
import com.almeidajr.molda.ui.navigation.EditorStatusHolder
import com.almeidajr.molda.ui.navigation.Navigator
import com.almeidajr.molda.ui.navigation.navigatorOf
import com.almeidajr.molda.ui.navigation.stacks
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test

class TransitionExecutorTest {
    private val snapshot = SkirtSnapshot(
        SkirtDraft(Length(80.0), Length(2.0), Length(60.0), Length(1.0), Length(2.0)),
        SkirtRadii(Length(13.05), Length(73.05), Length(12.05), Length(75.05)),
        source = null,
    )
    private val projectEditor = ProjectEditorKey(ProjectId(1), snapshot)
    private val moldesPicker = ProfilePickerKey(MOLDES_PROFILE_RESULT_KEY)
    private val existing = EditedProfile.Existing(ProfileId(2))
    private val editors = EditorStatusHolder()

    /** The app's stacks: [stacks] for the destinations they start at, the others at their roots. */
    private fun app(selected: NavKey, vararg stacks: List<NavKey>): Navigator = navigatorOf(
        TopLevelRoots.map { root -> stacks.find { it.first() == root } ?: listOf(root) },
        selected,
    )

    private fun Navigator.stackOf(root: NavKey) = stacks[TopLevelRoots.indexOf(root)]

    @Test
    fun carryOut_none_leavesTheStacks() {
        // Given
        val navigator = app(PatternCatalogKey, listOf(PatternCatalogKey, SkirtCalculatorKey))

        // When
        navigator.carryOut(Transition.None, editors)

        // Then
        assertEquals(
            listOf(PatternCatalogKey, SkirtCalculatorKey),
            navigator.state.current.toList(),
        )
    }

    @Test
    fun carryOut_back_popsTheSelectedStack() {
        // Given
        val navigator = app(GuideLibraryKey, listOf(GuideLibraryKey, ExampleFabricFoldingGuideKey))

        // When
        navigator.carryOut(Transition.Back, editors)

        // Then
        assertEquals(listOf(GuideLibraryKey), navigator.state.current.toList())
    }

    @Test
    fun carryOut_selectAnotherDestination_showsItsStack() {
        // Given
        val navigator = app(
            PatternCatalogKey,
            listOf(ProjectsKey, ProjectKey(ProjectId(1))),
        )

        // When
        navigator.carryOut(Transition.Select(ProjectsKey), editors)

        // Then
        assertEquals(
            listOf(ProjectsKey, ProjectKey(ProjectId(1))),
            navigator.state.current.toList(),
        )
    }

    @Test
    fun carryOut_showProjectsAfterDiscard_returnsProjetosToItsRoot() {
        // Given
        val navigator = app(
            ProjectsKey,
            listOf(ProjectsKey, ProjectKey(ProjectId(1)), projectEditor),
        )

        // When
        navigator.carryOut(AfterDiscard.ShowProjects.transition(), editors)

        // Then
        assertEquals(listOf(ProjectsKey), navigator.state.current.toList())
    }

    @Test
    fun carryOut_askToDiscard_opensTheDialogOverTheSelectedStack() {
        // Given
        val navigator = app(ProjectsKey, listOf(ProjectsKey, projectEditor))
        val ask = Transition.AskToDiscard(AfterDiscard.CloseProjectEditor)

        // When
        navigator.carryOut(ask, editors)

        // Then
        assertEquals(
            listOf(ProjectsKey, projectEditor, DiscardChangesKey(AfterDiscard.CloseProjectEditor)),
            navigator.state.current.toList(),
        )
    }

    @Test
    fun carryOut_closeProjectEditor_returnsToTheProject() {
        // Given
        val navigator = app(
            ProjectsKey,
            listOf(ProjectsKey, ProjectKey(ProjectId(1)), projectEditor),
        )

        // When
        navigator.carryOut(Transition.CloseProjectEditor, editors)

        // Then
        assertEquals(
            listOf(ProjectsKey, ProjectKey(ProjectId(1))),
            navigator.state.current.toList(),
        )
    }

    @Test
    fun carryOut_showCalculatorOverThePickersEditor_closesThePickerAndItsEditor() {
        // Given
        val navigator = app(
            PatternCatalogKey,
            listOf(
                PatternCatalogKey,
                SkirtCalculatorKey,
                moldesPicker,
                PickerProfileEditorKey(MOLDES_PROFILE_RESULT_KEY),
            ),
        )

        // When
        navigator.carryOut(Transition.ShowCalculator, editors)

        // Then
        assertEquals(
            listOf(PatternCatalogKey, SkirtCalculatorKey),
            navigator.state.current.toList(),
        )
    }

    @Test
    fun carryOut_showCalculatorOnMoldesRoot_opensTheCalculator() {
        // Given
        val navigator = app(PatternCatalogKey)

        // When
        navigator.carryOut(Transition.ShowCalculator, editors)

        // Then
        assertEquals(
            listOf(PatternCatalogKey, SkirtCalculatorKey),
            navigator.state.current.toList(),
        )
    }

    @Test
    fun carryOut_showCalculatorOverItsGuide_closesTheGuide() {
        // Given
        val guide = FabricFoldingGuideKey(snapshot.radii)
        val navigator = app(PatternCatalogKey, listOf(PatternCatalogKey, SkirtCalculatorKey, guide))

        // When
        navigator.carryOut(Transition.ShowCalculator, editors)

        // Then
        assertEquals(
            listOf(PatternCatalogKey, SkirtCalculatorKey),
            navigator.state.current.toList(),
        )
    }

    @Test
    fun carryOut_finishProfileEditor_closesTheEditorOnTheSelectedStack() {
        // Given
        val editor = ProfileEditorKey(existing)
        val navigator = app(ProfileManagementKey, listOf(ProfileManagementKey, editor))

        // When
        navigator.carryOut(Transition.FinishProfileEditor, editors)

        // Then
        assertEquals(listOf(ProfileManagementKey), navigator.state.current.toList())
    }

    @Test
    fun carryOut_finishPickerEditorBeforeItReports_returnsToThePicker() {
        // Given
        val navigator = app(
            ProjectsKey,
            listOf(
                ProjectsKey,
                projectEditor,
                ProfilePickerKey(projectEditor.profileResultKey),
                PickerProfileEditorKey(projectEditor.profileResultKey),
            ),
        )
        editors.report(projectEditor, MutableStateFlow(EditorReport(EditorKind.Project)))

        // When
        navigator.carryOut(Transition.FinishProfileEditor, editors)

        // Then
        assertEquals(
            listOf(ProjectsKey, projectEditor, ProfilePickerKey(projectEditor.profileResultKey)),
            navigator.state.current.toList(),
        )
    }

    @Test
    fun carryOut_openProfileEditorFromAnotherTab_opensItBesideTheList() {
        // Given
        val navigator = app(PatternCatalogKey)

        // When
        navigator.carryOut(Transition.OpenProfileEditor(existing), editors)

        // Then
        val perfis = navigator.stackOf(ProfileManagementKey)
        assertEquals(
            listOf(ProfileManagementKey, ProfileEditorKey(existing, opening(perfis))),
            perfis,
        )
    }

    @Test
    fun carryOut_openTheAlreadyOpenProfileEditor_keepsIt() {
        // Given
        val open = ProfileEditorKey(existing)
        val navigator = app(ProfileManagementKey, listOf(ProfileManagementKey, open))

        // When
        navigator.carryOut(Transition.OpenProfileEditor(existing), editors)

        // Then
        assertEquals(listOf(ProfileManagementKey, open), navigator.state.current.toList())
    }

    @Test
    fun carryOut_openTheOpenProfileEditorAfterDiscard_replacesItWithAFreshOne() {
        // Given
        val open = ProfileEditorKey(existing)
        val navigator = app(ProfileManagementKey, listOf(ProfileManagementKey, open))

        // When
        navigator.carryOut(AfterDiscard.OpenEditor(existing).transition(), editors)

        // Then
        val perfis = navigator.state.current.toList()
        assertEquals(2, perfis.size)
        assertEquals(existing, assertIs<ProfileEditorKey>(perfis.last()).profile)
        assertNotEquals<NavKey>(open, perfis.last())
    }

    @Test
    fun carryOut_openAnotherProfileEditor_replacesTheOpenOne() {
        // Given
        val navigator = app(
            ProfileManagementKey,
            listOf(ProfileManagementKey, ProfileEditorKey(existing)),
        )

        // When
        navigator.carryOut(Transition.OpenProfileEditor(EditedProfile.New), editors)

        // Then
        val perfis = navigator.state.current.toList()
        assertEquals(
            listOf(ProfileManagementKey, ProfileEditorKey(EditedProfile.New, opening(perfis))),
            perfis,
        )
    }

    @Test
    fun carryOut_openPickerEditor_opensItOverThePicker() {
        // Given
        val navigator = app(
            PatternCatalogKey,
            listOf(PatternCatalogKey, SkirtCalculatorKey, moldesPicker),
        )

        // When
        navigator.carryOut(Transition.OpenPickerEditor(MOLDES_PROFILE_RESULT_KEY), editors)

        // Then
        val moldes = navigator.state.current.toList()
        assertEquals(
            listOf(
                PatternCatalogKey,
                SkirtCalculatorKey,
                moldesPicker,
                PickerProfileEditorKey(MOLDES_PROFILE_RESULT_KEY, opening(moldes)),
            ),
            moldes,
        )
    }

    @Test
    fun carryOut_openPickerEditorWhileOneIsOpenOnTheSameResultKey_leavesOneFreshEditor() {
        // Given
        val open = PickerProfileEditorKey(MOLDES_PROFILE_RESULT_KEY)
        val navigator = app(
            PatternCatalogKey,
            listOf(PatternCatalogKey, SkirtCalculatorKey, moldesPicker, open),
        )

        // When
        navigator.carryOut(
            AfterDiscard.OpenPickerEditor(MOLDES_PROFILE_RESULT_KEY).transition(),
            editors,
        )

        // Then
        val moldes = navigator.state.current.toList()
        assertEquals(1, moldes.count { it is PickerProfileEditorKey })
        assertNotEquals<NavKey>(open, moldes.last())
    }

    @Test
    fun pickerEditorKey_openedTwice_hasUniqueContentKeys() {
        // When/Then
        // Navigation 3's default content key is toString(); the opening keeps it unique on a stack.
        assertNotEquals(
            PickerProfileEditorKey(MOLDES_PROFILE_RESULT_KEY).toString(),
            PickerProfileEditorKey(MOLDES_PROFILE_RESULT_KEY).toString(),
        )
    }

    @Test
    fun carryOut_calculateOwnFromTheExampleGuide_landsOnTheCalculatorAndKeepsGuias() {
        // Given
        val guias = listOf(GuideLibraryKey, ExampleFabricFoldingGuideKey)
        val navigator = app(GuideLibraryKey, guias)

        // When
        navigator.carryOut(Transition.Select(PatternCatalogKey), editors)
        navigator.carryOut(calculateOwn(navigator.state.current, EditorStatus.None), editors)

        // Then
        assertEquals(PatternCatalogKey, navigator.state.selected)
        assertEquals(
            listOf(PatternCatalogKey, SkirtCalculatorKey),
            navigator.stackOf(PatternCatalogKey),
        )
        assertEquals(guias, navigator.stackOf(GuideLibraryKey))
    }

    /** The opening of the editor on top of [stack], fresh to each opening. */
    private fun opening(stack: List<NavKey>): String = when (val top = stack.last()) {
        is ProfileEditorKey -> top.opening
        is PickerProfileEditorKey -> top.opening
        else -> error("No editor on top of $stack")
    }
}
