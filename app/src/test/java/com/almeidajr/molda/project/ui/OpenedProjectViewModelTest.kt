package com.almeidajr.molda.project.ui

import com.almeidajr.molda.project.domain.ProjectName
import com.almeidajr.molda.project.navigation.ProjectKey
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import com.almeidajr.molda.testing.October18Clock
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class OpenedProjectViewModelTest {
    private val storage = FakeProjectRepository(listOf(saved))

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun state_beforeProjectsLoad_isLoading() = runTest {
        // Given
        storage.loadGate = CompletableDeferred()

        // When/Then
        assertEquals(OpenedProjectState.Loading, openedViewModel().state.value)
    }

    @Test
    fun state_whenTheProjectChanges_showsItsLatestVersion() = runTest {
        // Given
        val opened = openedViewModel()

        // When
        storage.rename(saved.id, ProjectName("Saia nova"), October18Clock.instant()).getOrThrow()

        // Then
        assertEquals(
            OpenedProjectState.Shown(storage.current.single()),
            opened.state.value,
        )
        assertEquals(ProjectName("Saia nova"), storage.current.single().name)
    }

    @Test
    fun state_afterTheProjectIsDeleted_isGone() = runTest {
        // Given
        val opened = openedViewModel()

        // When
        storage.delete(saved.id).getOrThrow()

        // Then
        assertEquals(OpenedProjectState.Gone, opened.state.value)
    }

    @Test
    fun state_whenTheLoadFails_isFailed() = runTest {
        // Given
        storage.failsLoad = true

        // When/Then
        assertEquals(OpenedProjectState.Failed, openedViewModel().state.value)
    }

    @Test
    fun retry_afterTheLoadFailed_showsTheProject() = runTest {
        // Given
        storage.failsLoad = true
        val opened = openedViewModel()
        storage.failsLoad = false

        // When
        opened.retry()

        // Then
        assertEquals(OpenedProjectState.Shown(saved), opened.state.value)
    }

    private fun openedViewModel() = OpenedProjectViewModel(storage, ProjectKey(saved.id))
}
