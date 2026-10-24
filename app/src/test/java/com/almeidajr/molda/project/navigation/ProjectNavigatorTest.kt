package com.almeidajr.molda.project.navigation

import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.measurement.Length
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.project.domain.SkirtSnapshot
import com.almeidajr.molda.skirt.domain.SkirtDraft
import com.almeidajr.molda.skirt.domain.SkirtRadii
import com.almeidajr.molda.ui.navigation.navigatorOf
import com.almeidajr.molda.ui.navigation.stacks
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class ProjectNavigatorTest {
    private val snapshot = SkirtSnapshot(
        SkirtDraft(Length(80.0), Length(2.0), Length(60.0), Length(1.0), Length(2.0)),
        SkirtRadii(Length(13.05), Length(73.05), Length(12.05), Length(75.05)),
        source = null,
    )
    private val first = ProjectId(1)
    private val second = ProjectId(2)

    /** Projetos only, selected. */
    private fun projetos(vararg above: NavKey) = navigatorOf(listOf(listOf(ProjectsKey, *above)))

    @Test
    fun openProject_withNoneOpen_opensIt() {
        // Given
        val navigator = projetos()

        // When
        navigator.openProject(first)

        // Then
        assertEquals(listOf(listOf(ProjectsKey, ProjectKey(first))), navigator.stacks)
    }

    @Test
    fun openProject_whenItIsShown_changesNothing() {
        // Given
        val navigator = projetos(ProjectKey(first))

        // When
        navigator.openProject(first)

        // Then
        assertEquals(listOf(listOf(ProjectsKey, ProjectKey(first))), navigator.stacks)
    }

    @Test
    fun openProject_whenItsGuideIsShown_replacesTheProjectAndItsGuide() {
        // Given
        val navigator = projetos(ProjectKey(first), ProjectGuideKey(first))

        // When
        navigator.openProject(first)

        // Then
        assertEquals(listOf(listOf(ProjectsKey, ProjectKey(first))), navigator.stacks)
    }

    @Test
    fun openProject_whenAnotherIsOpen_replacesIt() {
        // Given
        val navigator = projetos(ProjectKey(first))

        // When
        navigator.openProject(second)

        // Then
        assertEquals(listOf(listOf(ProjectsKey, ProjectKey(second))), navigator.stacks)
    }

    @Test
    fun isShowingProject_onlyForTheProjectOnTop() {
        // Given
        val navigator = projetos(ProjectKey(first))

        // When/Then
        assertTrue(navigator.isShowingProject(first))
        assertFalse(navigator.isShowingProject(second))
    }

    @Test
    fun isShowingProject_overItsGuide_isFalse() {
        // Given
        val navigator = projetos(ProjectKey(first), ProjectGuideKey(first))

        // When/Then
        assertFalse(navigator.isShowingProject(first))
    }

    @Test
    fun showProject_overTheEditorAndItsGuide_closesThemAndShowsTheProject() {
        // Given
        val navigator = projetos(
            ProjectKey(first),
            ProjectEditorKey(first, snapshot),
            ProjectEditorGuideKey(snapshot.radii),
        )

        // When
        navigator.showProject(second)

        // Then
        assertEquals(listOf(listOf(ProjectsKey, ProjectKey(second))), navigator.stacks)
    }

    @Test
    fun showProject_fromAnotherDestination_showsItOnProjetos() {
        // Given
        val navigator =
            navigatorOf(listOf(listOf(OtherKey), listOf(ProjectsKey)), selected = OtherKey)

        // When
        navigator.showProject(first)

        // Then
        assertEquals(
            listOf(listOf(OtherKey), listOf(ProjectsKey, ProjectKey(first))),
            navigator.stacks,
        )
    }

    @Test
    fun closeProjectEditor_withNoneOpen_changesNothing() {
        // Given
        val navigator = projetos(ProjectKey(first))

        // When
        navigator.closeProjectEditor()

        // Then
        assertEquals(listOf(listOf(ProjectsKey, ProjectKey(first))), navigator.stacks)
    }

    @Test
    fun closeProjectEditor_withTheEditorOpen_returnsToTheProject() {
        // Given
        val navigator = projetos(ProjectKey(first), ProjectEditorKey(first, snapshot))

        // When
        navigator.closeProjectEditor()

        // Then
        assertEquals(listOf(listOf(ProjectsKey, ProjectKey(first))), navigator.stacks)
    }

    @Test
    fun closeProjectEditor_withItsGuideAbove_closesItsGuideToo() {
        // Given
        val guide = ProjectEditorGuideKey(snapshot.radii)
        val navigator = projetos(ProjectKey(first), ProjectEditorKey(first, snapshot), guide)

        // When
        navigator.closeProjectEditor()

        // Then
        assertEquals(listOf(listOf(ProjectsKey, ProjectKey(first))), navigator.stacks)
    }

    @Test
    fun closeProject_withItsEntriesOpen_closesThemAll() {
        // Given
        val navigator = projetos(
            ProjectKey(first),
            ProjectEditorKey(first, snapshot),
            ProjectEditorGuideKey(snapshot.radii),
            DeleteProjectKey(first),
        )

        // When
        val closed = navigator.closeProject(first)

        // Then
        assertTrue(closed)
        assertEquals(listOf(listOf(ProjectsKey)), navigator.stacks)
    }

    @Test
    fun closeProject_withItsGuideAndRenameDialogOpen_closesThem() {
        // Given
        val navigator = projetos(ProjectKey(first), ProjectGuideKey(first), RenameProjectKey(first))

        // When
        val closed = navigator.closeProject(first)

        // Then
        assertTrue(closed)
        assertEquals(listOf(listOf(ProjectsKey)), navigator.stacks)
    }

    @Test
    fun closeProject_whileAnotherIsEdited_keepsItsEntries() {
        // Given
        val open = listOf(
            ProjectsKey,
            ProjectKey(second),
            ProjectEditorKey(second, snapshot),
            ProjectEditorGuideKey(snapshot.radii),
        )
        val navigator = navigatorOf(listOf(open))

        // When
        val closed = navigator.closeProject(first)

        // Then
        assertFalse(closed)
        assertEquals(listOf(open), navigator.stacks)
    }

    @Test
    fun closeDeletedProject_withItsEntriesOpen_closesThem() {
        // Given
        val navigator = projetos(ProjectKey(first), ProjectGuideKey(first))

        // When
        val closed = navigator.closeDeletedProject(first)

        // Then
        assertTrue(closed)
        assertEquals(listOf(listOf(ProjectsKey)), navigator.stacks)
    }

    @Test
    fun closeDeletedProject_whileItsDeleteDialogIsOpen_leavesThemToTheDialog() {
        // Given
        val open = listOf(ProjectsKey, ProjectKey(first), DeleteProjectKey(first))
        val navigator = navigatorOf(listOf(open))

        // When
        val closed = navigator.closeDeletedProject(first)

        // Then
        assertFalse(closed)
        assertEquals(listOf(open), navigator.stacks)
    }

    @Test
    fun isDeletingProject_withItsDeleteDialogOpen_isTrueOnlyForThatProject() {
        // Given
        val navigator = projetos(ProjectKey(first), DeleteProjectKey(first))

        // When/Then
        assertTrue(navigator.isDeletingProject(first))
        assertFalse(navigator.isDeletingProject(second))
    }

    @Test
    fun isDeletingProject_withItsDeleteDialogClosed_isFalse() {
        // Given
        val navigator = projetos(ProjectKey(first), DeleteProjectKey(first))

        // When
        navigator.remove(DeleteProjectKey(first))

        // Then
        assertFalse(navigator.isDeletingProject(first))
    }

    private data object OtherKey : NavKey
}
