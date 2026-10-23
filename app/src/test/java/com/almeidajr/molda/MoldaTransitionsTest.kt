package com.almeidajr.molda

import com.almeidajr.molda.guide.navigation.ExampleFabricFoldingGuideKey
import com.almeidajr.molda.guide.navigation.GuideLibraryKey
import com.almeidajr.molda.pattern.navigation.PatternCatalogKey
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.navigation.EditedProfile
import com.almeidajr.molda.profile.navigation.PickerProfileEditorKey
import com.almeidajr.molda.profile.navigation.ProfileEditorKey
import com.almeidajr.molda.profile.navigation.ProfileManagementKey
import com.almeidajr.molda.profile.navigation.ProfilePickerKey
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.navigation.ProjectEditorKey
import com.almeidajr.molda.project.navigation.ProjectKey
import com.almeidajr.molda.project.navigation.ProjectsKey
import com.almeidajr.molda.skirt.navigation.MOLDES_PROFILE_RESULT_KEY
import com.almeidajr.molda.skirt.navigation.SkirtCalculatorKey
import kotlin.test.assertEquals
import org.junit.Test

class MoldaTransitionsTest {
    private val project = ProjectId(1)
    private val projectEditor = listOf(ProjectsKey, ProjectKey(project), ProjectEditorKey(project))
    private val profileEditor = listOf(ProfileManagementKey, ProfileEditorKey(EditedProfile.New))

    @Test
    fun selectDestination_anotherDestination_selectsIt() {
        // When/Then
        assertEquals(
            Transition.Select(ProjectsKey),
            selectDestination(
                ProjectsKey,
                GuideLibraryKey,
                listOf(GuideLibraryKey),
                EditorStatus(),
            ),
        )
    }

    @Test
    fun selectDestination_currentOne_returnsToItsRoot() {
        // When/Then
        assertEquals(
            Transition.Select(GuideLibraryKey),
            selectDestination(
                GuideLibraryKey,
                GuideLibraryKey,
                listOf(GuideLibraryKey, ExampleFabricFoldingGuideKey),
                EditorStatus(),
            ),
        )
    }

    @Test
    fun selectDestination_currentOneUnderDiscardDialog_keepsTheDialog() {
        // Given
        val stack = profileEditor + DiscardChangesKey(AfterDiscard.CloseEditor)

        // When/Then
        assertEquals(
            Transition.None,
            selectDestination(ProfileManagementKey, ProfileManagementKey, stack, EditorStatus()),
        )
    }

    @Test
    fun selectDestination_currentOneOverDirtyProfileEditor_asksToDiscard() {
        // When/Then
        assertEquals(
            Transition.AskToDiscard(AfterDiscard.CloseEditor),
            selectDestination(
                ProfileManagementKey,
                ProfileManagementKey,
                profileEditor,
                EditorStatus(isProfileEditorOpen = true, isProfileDirty = true),
            ),
        )
    }

    @Test
    fun selectDestination_currentOneOverSavingProfileEditor_waits() {
        // When/Then
        assertEquals(
            Transition.None,
            selectDestination(
                ProfileManagementKey,
                ProfileManagementKey,
                profileEditor,
                EditorStatus(isProfileDirty = true, isProfileSaving = true),
            ),
        )
    }

    @Test
    fun selectDestination_currentOneOverDirtyProjectEditor_asksToDiscard() {
        // When/Then
        assertEquals(
            Transition.AskToDiscard(AfterDiscard.ShowProjects),
            selectDestination(
                ProjectsKey,
                ProjectsKey,
                projectEditor,
                EditorStatus(isProjectEditorDirty = true),
            ),
        )
    }

    @Test
    fun selectDestination_currentOneOverUpdatingProjectEditor_waits() {
        // When/Then
        assertEquals(
            Transition.None,
            selectDestination(
                ProjectsKey,
                ProjectsKey,
                projectEditor,
                EditorStatus(isProjectEditorDirty = true, isProjectUpdating = true),
            ),
        )
    }

    @Test
    fun goBack_ordinaryScreen_popsIt() {
        // When/Then
        assertEquals(
            Transition.Back,
            goBack(listOf(PatternCatalogKey, SkirtCalculatorKey), EditorStatus()),
        )
    }

    @Test
    fun goBack_cleanProjectEditor_closesIt() {
        // When/Then
        assertEquals(Transition.CloseProjectEditor, goBack(projectEditor, EditorStatus()))
    }

    @Test
    fun goBack_dirtyProjectEditor_asksToDiscard() {
        // When/Then
        assertEquals(
            Transition.AskToDiscard(AfterDiscard.CloseProjectEditor),
            goBack(projectEditor, EditorStatus(isProjectEditorDirty = true)),
        )
    }

    @Test
    fun goBack_updatingProjectEditor_waits() {
        // When/Then
        assertEquals(
            Transition.None,
            goBack(
                projectEditor,
                EditorStatus(isProjectEditorDirty = true, isProjectUpdating = true),
            ),
        )
    }

    @Test
    fun goBack_cleanProfileEditor_finishesIt() {
        // When/Then
        assertEquals(Transition.FinishProfileEditor, goBack(profileEditor, EditorStatus()))
    }

    @Test
    fun goBack_savingProfileEditor_waits() {
        // When/Then
        assertEquals(
            Transition.None,
            goBack(profileEditor, EditorStatus(isProfileSaving = true)),
        )
    }

    @Test
    fun calculateOwn_withoutPickerEditor_showsTheCalculator() {
        // When/Then
        assertEquals(
            Transition.ShowCalculator(finishesPickerEditor = false),
            calculateOwn(listOf(PatternCatalogKey, SkirtCalculatorKey), EditorStatus()),
        )
    }

    @Test
    fun calculateOwn_overCleanPickerEditor_finishesItFirst() {
        // Given
        val picker = ProfilePickerKey(MOLDES_PROFILE_RESULT_KEY)
        val editor = PickerProfileEditorKey(MOLDES_PROFILE_RESULT_KEY)
        val stack = listOf(PatternCatalogKey, SkirtCalculatorKey, picker, editor)

        // When/Then
        assertEquals(
            Transition.ShowCalculator(finishesPickerEditor = true),
            calculateOwn(stack, EditorStatus()),
        )
    }

    @Test
    fun calculateOwn_overDirtyPickerEditor_asksToDiscard() {
        // Given
        val editor = PickerProfileEditorKey(MOLDES_PROFILE_RESULT_KEY)
        val stack = listOf(PatternCatalogKey, SkirtCalculatorKey, editor)

        // When/Then
        assertEquals(
            Transition.AskToDiscard(AfterDiscard.CalculateOwn),
            calculateOwn(stack, EditorStatus(isProfileDirty = true)),
        )
    }

    @Test
    fun calculateOwn_underDiscardDialog_keepsTheDialog() {
        // Given
        val stack = listOf(PatternCatalogKey, DiscardChangesKey(AfterDiscard.CalculateOwn))

        // When/Then
        assertEquals(Transition.None, calculateOwn(stack, EditorStatus()))
    }

    @Test
    fun requestProfileEditor_noOpenChanges_opensIt() {
        // Given
        val profile = EditedProfile.Existing(ProfileId(2))

        // When/Then
        assertEquals(
            Transition.OpenProfileEditor(profile),
            requestProfileEditor(profile, EditorStatus(isProfileEditorOpen = true)),
        )
    }

    @Test
    fun requestProfileEditor_openEditorHasChanges_asksToDiscard() {
        // When/Then
        assertEquals(
            Transition.AskToDiscard(AfterDiscard.OpenEditor(EditedProfile.New)),
            requestProfileEditor(
                EditedProfile.New,
                EditorStatus(isProfileEditorOpen = true, isProfileDirty = true),
            ),
        )
    }

    @Test
    fun requestProfileEditor_profilesUnavailableOrSaving_ignoresIt() {
        // When/Then
        assertEquals(
            Transition.None,
            requestProfileEditor(EditedProfile.New, EditorStatus(isProfileUnavailable = true)),
        )
        assertEquals(
            Transition.None,
            requestProfileEditor(EditedProfile.New, EditorStatus(isProfileSaving = true)),
        )
    }

    @Test
    fun requestPickerEditor_openEditorHasChanges_asksToDiscard() {
        // When/Then
        assertEquals(
            Transition.AskToDiscard(AfterDiscard.OpenPickerEditor(MOLDES_PROFILE_RESULT_KEY)),
            requestPickerEditor(
                MOLDES_PROFILE_RESULT_KEY,
                EditorStatus(isProfileEditorOpen = true, isProfileDirty = true),
            ),
        )
    }

    @Test
    fun transition_afterDiscard_repeatsTheRequestDroppingTheDraft() {
        // Given
        val profile = EditedProfile.Existing(ProfileId(2))

        // When/Then
        assertEquals(
            listOf(
                Transition.FinishProfileEditor,
                Transition.OpenProfileEditor(profile, discardsDraft = true),
                Transition.OpenPickerEditor(MOLDES_PROFILE_RESULT_KEY, discardsDraft = true),
                Transition.ShowCalculator(finishesPickerEditor = true),
                Transition.CloseProjectEditor,
                Transition.Select(ProjectsKey),
            ),
            listOf(
                AfterDiscard.CloseEditor,
                AfterDiscard.OpenEditor(profile),
                AfterDiscard.OpenPickerEditor(MOLDES_PROFILE_RESULT_KEY),
                AfterDiscard.CalculateOwn,
                AfterDiscard.CloseProjectEditor,
                AfterDiscard.ShowProjects,
            ).map { it.transition() },
        )
    }
}
