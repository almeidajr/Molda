package com.almeidajr.molda.profile.navigation

import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.about.navigation.AboutKey
import com.almeidajr.molda.about.navigation.LicencesKey
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.skirt.navigation.SkirtCalculatorKey
import com.almeidajr.molda.ui.navigation.navigatorOf
import com.almeidajr.molda.ui.navigation.stacks
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Test

class ProfileNavigatorTest {
    private val existing = EditedProfile.Existing(ProfileId(2))
    private val picker = ProfilePickerKey("picker")
    private val pickerEditor = PickerProfileEditorKey("picker", opening = "first")

    /** Moldes then Perfis, with Moldes selected. */
    private fun app(
        moldes: List<NavKey> = listOf(SkirtCalculatorKey),
        perfis: List<NavKey> = listOf(ProfileManagementKey),
    ) = navigatorOf(listOf(moldes, perfis))

    @Test
    fun openProfileEditor_withNoneOpen_opensItBesideTheList() {
        // Given
        val navigator = app()

        // When
        navigator.openProfileEditor(existing)

        // Then
        val (moldes, perfis) = navigator.stacks
        assertEquals(listOf(SkirtCalculatorKey), moldes)
        assertEquals(existing, (perfis.last() as ProfileEditorKey).profile)
        assertEquals(2, perfis.size)
    }

    @Test
    fun openProfileEditor_whenItAlreadyShowsThatProfile_keepsIt() {
        // Given
        val open = ProfileEditorKey(existing, "first")
        val navigator = app(perfis = listOf(ProfileManagementKey, open))

        // When
        navigator.openProfileEditor(existing)

        // Then
        assertEquals(listOf(ProfileManagementKey, open), navigator.stacks[1])
    }

    @Test
    fun openProfileEditor_afterAConfirmedDiscard_replacesItWithAFreshOne() {
        // Given
        val open = ProfileEditorKey(existing, "first")
        val navigator = app(perfis = listOf(ProfileManagementKey, open))

        // When
        navigator.openProfileEditor(existing, discardsDraft = true)

        // Then
        val perfis = navigator.stacks[1]
        assertEquals(2, perfis.size)
        assertNotEquals(open, perfis.last())
    }

    @Test
    fun openProfileEditor_forAnotherProfile_replacesTheOpenOne() {
        // Given
        val navigator =
            app(perfis = listOf(ProfileManagementKey, ProfileEditorKey(existing, "first")))

        // When
        navigator.openProfileEditor(EditedProfile.New)

        // Then
        val perfis = navigator.stacks[1]
        assertEquals(2, perfis.size)
        assertEquals(EditedProfile.New, (perfis.last() as ProfileEditorKey).profile)
    }

    @Test
    fun openProfileEditor_leavesThePickersEditorOpen() {
        // Given
        val navigator = app(moldes = listOf(SkirtCalculatorKey, picker, pickerEditor))

        // When
        navigator.openProfileEditor(existing)

        // Then
        assertEquals(listOf(SkirtCalculatorKey, picker, pickerEditor), navigator.stacks[0])
    }

    @Test
    fun openPickerEditor_overThePicker_opensIt() {
        // Given
        val navigator = app(moldes = listOf(SkirtCalculatorKey, picker))

        // When
        navigator.openPickerEditor("picker")

        // Then
        val moldes = navigator.stacks[0]
        assertEquals(listOf(SkirtCalculatorKey, picker), moldes.take(2))
        assertEquals("picker", (moldes.last() as PickerProfileEditorKey).resultKey)
        assertEquals(3, moldes.size)
    }

    @Test
    fun openPickerEditor_whileOneIsOpenOnTheSameResultKey_leavesOneFreshEditor() {
        // Given
        val navigator = app(moldes = listOf(SkirtCalculatorKey, picker, pickerEditor))

        // When
        navigator.openPickerEditor("picker")

        // Then
        val moldes = navigator.stacks[0]
        assertEquals(3, moldes.size)
        assertNotEquals(pickerEditor, moldes.last())
    }

    @Test
    fun closePickerEditor_closesTheEditorAndItsPicker() {
        // Given
        val navigator = app(moldes = listOf(SkirtCalculatorKey, picker, pickerEditor))

        // When
        navigator.closePickerEditor(pickerEditor)

        // Then
        assertEquals(listOf(SkirtCalculatorKey), navigator.stacks[0])
    }

    @Test
    fun closePickerEditor_keepsAnotherCalculatorsPicker() {
        // Given
        val other = ProfilePickerKey("other")
        val navigator = app(
            moldes = listOf(SkirtCalculatorKey, picker, pickerEditor),
            perfis = listOf(ProfileManagementKey, other),
        )

        // When
        navigator.closePickerEditor(pickerEditor)

        // Then
        assertEquals(
            listOf(listOf(SkirtCalculatorKey), listOf(ProfileManagementKey, other)),
            navigator.stacks,
        )
    }

    @Test
    fun closeEditorOf_aDeletedProfile_closesItsEditor() {
        // Given
        val navigator =
            app(perfis = listOf(ProfileManagementKey, ProfileEditorKey(existing, "first")))

        // When
        navigator.closeEditorOf(ProfileId(2))

        // Then
        assertEquals(listOf(ProfileManagementKey), navigator.stacks[1])
    }

    @Test
    fun closeEditorOf_anotherProfile_keepsTheOpenEditor() {
        // Given
        val open = ProfileEditorKey(existing, "first")
        val navigator = app(perfis = listOf(ProfileManagementKey, open))

        // When
        navigator.closeEditorOf(ProfileId(3))

        // Then
        assertEquals(listOf(ProfileManagementKey, open), navigator.stacks[1])
    }

    @Test
    fun closeEditorOf_withNoEditorOpen_changesNothing() {
        // Given
        val navigator = app()

        // When
        navigator.closeEditorOf(ProfileId(2))

        // Then
        assertEquals(
            listOf(listOf(SkirtCalculatorKey), listOf(ProfileManagementKey)),
            navigator.stacks,
        )
    }

    private fun showsCreateAction(perfis: List<NavKey>, isTwoPane: Boolean) =
        navigatorOf(listOf(listOf(SkirtCalculatorKey), perfis), selected = ProfileManagementKey)
            .state.showsProfileCreateAction(isTwoPane)

    @Test
    fun showsProfileCreateAction_overTheList_isShown() {
        assertTrue(showsCreateAction(listOf(ProfileManagementKey), isTwoPane = false))
        assertTrue(showsCreateAction(listOf(ProfileManagementKey), isTwoPane = true))
    }

    @Test
    fun showsProfileCreateAction_overAboutOrTheLicences_isHidden() {
        assertFalse(showsCreateAction(listOf(ProfileManagementKey, AboutKey), isTwoPane = true))
        assertFalse(
            showsCreateAction(
                listOf(ProfileManagementKey, AboutKey, LicencesKey),
                isTwoPane = false,
            ),
        )
    }

    @Test
    fun showsProfileCreateAction_overAnEditor_isShownOnlyBesideTheList() {
        val perfis = listOf(ProfileManagementKey, ProfileEditorKey(existing, "first"))

        assertFalse(showsCreateAction(perfis, isTwoPane = false))
        assertTrue(showsCreateAction(perfis, isTwoPane = true))
    }

    @Test
    fun showsProfileCreateAction_onAnotherDestination_isHidden() {
        val navigator = app()

        assertFalse(navigator.state.showsProfileCreateAction(isTwoPane = true))
    }
}
