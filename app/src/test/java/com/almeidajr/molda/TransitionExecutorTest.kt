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
import com.almeidajr.molda.ui.navigation.Navigator
import com.almeidajr.molda.ui.navigation.navigatorOf
import com.almeidajr.molda.ui.navigation.stacks
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
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
        navigator.carryOut(Transition.None)

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
        navigator.carryOut(Transition.Back)

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
        navigator.carryOut(Transition.Select(ProjectsKey))

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
        navigator.carryOut(AfterDiscard.ShowProjects.transition())

        // Then
        assertEquals(listOf(ProjectsKey), navigator.state.current.toList())
    }

    @Test
    fun carryOut_askToDiscard_opensTheDialogOverTheSelectedStack() {
        // Given
        val navigator = app(ProjectsKey, listOf(ProjectsKey, projectEditor))
        val ask = Transition.AskToDiscard(AfterDiscard.CloseProjectEditor)

        // When
        navigator.carryOut(ask)

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
        navigator.carryOut(Transition.CloseProjectEditor)

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
        navigator.carryOut(Transition.ShowCalculator)

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
        navigator.carryOut(Transition.ShowCalculator)

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
        navigator.carryOut(Transition.ShowCalculator)

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
        navigator.carryOut(Transition.FinishProfileEditor)

        // Then
        assertEquals(listOf(ProfileManagementKey), navigator.state.current.toList())
    }

    @Test
    fun carryOut_finishPickerEditor_returnsToThePicker() {
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

        // When
        navigator.carryOut(Transition.FinishProfileEditor)

        // Then
        assertEquals(
            listOf(ProjectsKey, projectEditor, ProfilePickerKey(projectEditor.profileResultKey)),
            navigator.state.current.toList(),
        )
    }

    @Test
    fun carryOut_openTheAlreadyOpenProfileEditor_keepsIt() {
        // Given
        val open = ProfileEditorKey(existing)
        val navigator = app(ProfileManagementKey, listOf(ProfileManagementKey, open))

        // When
        navigator.carryOut(Transition.OpenProfileEditor(existing))

        // Then
        assertEquals(listOf(ProfileManagementKey, open), navigator.state.current.toList())
    }

    @Test
    fun carryOut_openTheOpenProfileEditorAfterDiscard_replacesItWithAFreshOne() {
        // Given
        val open = ProfileEditorKey(existing)
        val navigator = app(ProfileManagementKey, listOf(ProfileManagementKey, open))

        // When
        navigator.carryOut(AfterDiscard.OpenEditor(existing).transition())

        // Then
        val perfis = navigator.state.current.toList()
        assertEquals(2, perfis.size)
        assertEquals(existing, assertIs<ProfileEditorKey>(perfis.last()).profile)
        assertNotEquals<NavKey>(open, perfis.last())
    }

    @Test
    fun carryOut_openPickerEditor_opensItOverThePicker() {
        // Given
        val navigator = app(
            PatternCatalogKey,
            listOf(PatternCatalogKey, SkirtCalculatorKey, moldesPicker),
        )

        // When
        navigator.carryOut(Transition.OpenPickerEditor(MOLDES_PROFILE_RESULT_KEY))

        // Then
        val moldes = navigator.state.current.toList()
        val opening = assertIs<PickerProfileEditorKey>(moldes.last()).opening
        assertEquals(
            listOf(
                PatternCatalogKey,
                SkirtCalculatorKey,
                moldesPicker,
                PickerProfileEditorKey(MOLDES_PROFILE_RESULT_KEY, opening),
            ),
            moldes,
        )
    }

    @Test
    fun carryOut_calculateOwnFromTheExampleGuide_landsOnTheCalculatorAndKeepsGuias() {
        // Given
        val guias = listOf(GuideLibraryKey, ExampleFabricFoldingGuideKey)
        val navigator = app(GuideLibraryKey, guias)

        // When
        navigator.carryOut(Transition.Select(PatternCatalogKey))
        navigator.carryOut(calculateOwn(navigator.state.current, EditorStatus.None))

        // Then
        assertEquals(PatternCatalogKey, navigator.state.selected)
        assertEquals(
            listOf(PatternCatalogKey, SkirtCalculatorKey),
            navigator.stackOf(PatternCatalogKey),
        )
        assertEquals(guias, navigator.stackOf(GuideLibraryKey))
    }
}
