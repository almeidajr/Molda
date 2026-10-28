package com.almeidajr.molda.project.navigation

import androidx.navigation3.runtime.NavKey
import com.almeidajr.molda.project.ReferenceSnapshot
import com.almeidajr.molda.project.domain.ProjectId
import com.almeidajr.molda.ui.navigation.navigatorOf
import com.almeidajr.molda.ui.navigation.stacks
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class ProjectNavigatorTest {
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
    fun isShowingProject_withAProjectOnTop_isTrueOnlyForIt() {
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
            ProjectEditorKey(first, ReferenceSnapshot),
            ProjectEditorGuideKey(ReferenceSnapshot.radii),
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
        val navigator = projetos(ProjectKey(first), ProjectEditorKey(first, ReferenceSnapshot))

        // When
        navigator.closeProjectEditor()

        // Then
        assertEquals(listOf(listOf(ProjectsKey, ProjectKey(first))), navigator.stacks)
    }

    @Test
    fun closeProjectEditor_withItsGuideAbove_closesItsGuideToo() {
        // Given
        val guide = ProjectEditorGuideKey(ReferenceSnapshot.radii)
        val navigator =
            projetos(ProjectKey(first), ProjectEditorKey(first, ReferenceSnapshot), guide)

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
            ProjectEditorKey(first, ReferenceSnapshot),
            ProjectEditorGuideKey(ReferenceSnapshot.radii),
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
            ProjectEditorKey(second, ReferenceSnapshot),
            ProjectEditorGuideKey(ReferenceSnapshot.radii),
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

    private data object OtherKey : NavKey
}
