package com.almeidajr.molda.project.ui

import com.almeidajr.molda.data.LoadResult
import com.almeidajr.molda.testing.FakeProjectRepository
import com.almeidajr.molda.testing.MainDispatcherRule
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ProjectListViewModelTest {
    private val storage = FakeProjectRepository(listOf(saved))

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun projects_whenStorageChanges_followIt() = runTest {
        // Given
        val list = ProjectListViewModel(storage)

        // When
        storage.delete(saved.id).getOrThrow()

        // Then
        assertEquals(LoadResult.Success(emptyList()), list.state.value)
    }

    @Test
    fun load_whenStorageFails_isError() = runTest {
        // Given
        storage.failsLoad = true

        // When/Then
        assertIs<LoadResult.Error>(ProjectListViewModel(storage).state.value)
    }

    @Test
    fun load_afterLoadFailure_showsStoredProjects() = runTest {
        // Given
        storage.failsLoad = true
        val list = ProjectListViewModel(storage)
        storage.failsLoad = false

        // When
        list.load()

        // Then
        assertEquals(LoadResult.Success(listOf(saved)), list.state.value)
    }
}
